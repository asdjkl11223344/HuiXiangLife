<script setup lang="ts">
import { Delete, Edit, Plus, RefreshRight, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import {
  batchDeleteProduct,
  batchSyncProductSearchIndex,
  batchUpdateProductStatus,
  createProduct,
  deleteProduct,
  fetchProductDetail,
  fetchProductPage,
  rebuildProductSearchIndex,
  syncProductSearchIndex,
  updateProduct,
  updateProductStatus,
} from '../api/admin'
import ImageUploadAssist from '../components/ImageUploadAssist.vue'
import { useAuthStore } from '../stores/auth'
import type { PageResult, ProductDetail, ProductForm, ProductItem, ProductQuery } from '../types'
import { createEmptyPage, formatCurrency, formatDateTime } from '../utils'
import { canWriteAdmin, resolveUserRole } from '../utils/permissions'

const loading = ref(false)
const rebuildLoading = ref(false)
const syncingId = ref<number>()
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const productDetail = ref<ProductDetail>()
const formRef = ref<FormInstance>()
const authStore = useAuthStore()
const canWrite = computed(() => canWriteAdmin(resolveUserRole(authStore.userInfo)))
const selectedRows = ref<ProductItem[]>([])
const batchLoading = ref(false)

const query = reactive<ProductQuery>({
  pageNo: 1,
  pageSize: 10,
  merchantId: undefined,
  keyword: '',
  status: undefined,
})

const form = reactive<ProductForm>({
  merchantId: undefined,
  name: '',
  subTitle: '',
  content: '',
  coverUrl: '',
  originPrice: undefined,
  salePrice: undefined,
  stock: undefined,
  status: 1,
  startTime: '',
  endTime: '',
})

const pageData = ref<PageResult<ProductItem>>(createEmptyPage<ProductItem>())
const rules: FormRules<ProductForm> = {
  merchantId: [{ required: true, message: '请输入商户 ID', trigger: 'change' }],
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 1, max: 100, message: '商品名称长度不能超过 100 位', trigger: 'blur' },
  ],
  subTitle: [{ max: 255, message: '副标题长度不能超过 255 位', trigger: 'blur' }],
  coverUrl: [{ max: 255, message: '封面图地址长度不能超过 255 位', trigger: 'blur' }],
  originPrice: [{ required: true, message: '请输入原价', trigger: 'change' }],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'change' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'change' }],
  endTime: [
    {
      validator: (_rule, value, callback) => {
        if (form.startTime && value && new Date(value).getTime() < new Date(form.startTime).getTime()) {
          callback(new Error('结束时间不能早于开始时间'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    merchantId: undefined,
    name: '',
    subTitle: '',
    content: '',
    coverUrl: '',
    originPrice: undefined,
    salePrice: undefined,
    stock: undefined,
    status: 1,
    startTime: '',
    endTime: '',
  })
}

async function loadProducts() {
  loading.value = true
  try {
    pageData.value = await fetchProductPage(query)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  query.pageNo = 1
  await loadProducts()
}

async function handleReset() {
  query.pageNo = 1
  query.pageSize = 10
  query.merchantId = undefined
  query.keyword = ''
  query.status = undefined
  await loadProducts()
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ProductItem) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    merchantId: row.merchantId,
    name: row.name,
    subTitle: row.subTitle || '',
    content: row.content || '',
    coverUrl: row.coverUrl || '',
    originPrice: row.originPrice,
    salePrice: row.salePrice,
    stock: row.stock,
    status: row.status ?? 1,
    startTime: row.startTime || '',
    endTime: row.endTime || '',
  })
  dialogVisible.value = true
}

async function openDetail(row: ProductItem) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    productDetail.value = await fetchProductDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateProduct(form)
      ElMessage.success('商品更新成功')
    } else {
      await createProduct({
        merchantId: form.merchantId,
        name: form.name,
        subTitle: form.subTitle,
        content: form.content,
        coverUrl: form.coverUrl,
        originPrice: form.originPrice,
        salePrice: form.salePrice,
        stock: form.stock,
        startTime: form.startTime || undefined,
        endTime: form.endTime || undefined,
      })
      ElMessage.success('商品创建成功')
    }
    dialogVisible.value = false
    formRef.value?.resetFields()
    await loadProducts()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: ProductItem) {
  await ElMessageBox.confirm(`确认删除商品“${row.name}”吗？`, '删除商品', { type: 'warning' })
  await deleteProduct(row.id)
  ElMessage.success('商品删除成功')
  await loadProducts()
}

async function handleStatus(row: ProductItem, status: number) {
  await updateProductStatus(row.id, { status })
  ElMessage.success(status === 1 ? '商品已上架' : '商品已下架')
  await loadProducts()
}

async function handleSync(id: number) {
  syncingId.value = id
  try {
    await syncProductSearchIndex(id)
    ElMessage.success(`商品 ${id} 索引同步成功`)
  } finally {
    syncingId.value = undefined
  }
}

async function handleRebuild() {
  rebuildLoading.value = true
  try {
    const count = await rebuildProductSearchIndex()
    ElMessage.success(`商品索引重建完成，共处理 ${count} 条数据`)
    await loadProducts()
  } finally {
    rebuildLoading.value = false
  }
}

function handleCurrentChange(pageNo: number) {
  query.pageNo = pageNo
  void loadProducts()
}

function handleSizeChange(pageSize: number) {
  query.pageSize = pageSize
  query.pageNo = 1
  void loadProducts()
}

function getStatusLabel(status?: number) {
  return status === 1 ? '上架中' : '已下架'
}

function getStatusType(status?: number) {
  return status === 1 ? 'success' : 'info'
}

const selectedIds = computed(() => selectedRows.value.map((item) => item.id))

function handleSelectionChange(rows: ProductItem[]) {
  selectedRows.value = rows
}

async function handleBatchSync() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择商品')
    return
  }
  batchLoading.value = true
  try {
    const result = await batchSyncProductSearchIndex(selectedIds.value)
    ElMessage.success(`批量同步完成，成功 ${result.successCount} 条，失败 ${result.failureCount} 条`)
  } finally {
    batchLoading.value = false
  }
}

async function handleBatchStatus(status: number) {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择商品')
    return
  }
  batchLoading.value = true
  try {
    const result = await batchUpdateProductStatus(selectedIds.value, status)
    ElMessage.success(
      `批量${status === 1 ? '上架' : '下架'}完成，成功 ${result.successCount} 条，失败 ${result.failureCount} 条`,
    )
    await loadProducts()
  } finally {
    batchLoading.value = false
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择商品')
    return
  }
  await ElMessageBox.confirm(`确认批量删除已选中的 ${selectedIds.value.length} 个商品吗？`, '批量删除商品', {
    type: 'warning',
  })
  batchLoading.value = true
  try {
    const result = await batchDeleteProduct(selectedIds.value)
    ElMessage.success(`批量删除完成，成功 ${result.successCount} 条，失败 ${result.failureCount} 条`)
    await loadProducts()
  } finally {
    batchLoading.value = false
  }
}

onMounted(() => {
  void loadProducts()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <div>
          <h2 class="page-title">商品管理</h2>
          <p class="page-subtitle">补齐商品分页、创建、编辑、上下架、删除和搜索索引管理。</p>
        </div>

        <div class="filter-actions">
          <el-button v-if="canWrite" type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
          <el-button v-if="canWrite" type="success" :icon="RefreshRight" :loading="rebuildLoading" @click="handleRebuild">
            重建商品索引
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card class="page-card" shadow="never">
      <el-form class="filter-form" label-position="top">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="商品名称关键词" />
        </el-form-item>
        <el-form-item label="商户 ID">
          <el-input-number v-model="query.merchantId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态">
            <el-option label="上架中" :value="1" />
            <el-option label="已下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作">
          <div class="filter-actions">
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="page-card" shadow="never">
      <div v-if="canWrite" class="batch-toolbar">
        <div class="batch-summary">已选中 {{ selectedIds.length }} 个商品</div>
        <div class="quick-action-row">
          <el-button :loading="batchLoading" @click="handleBatchSync">批量同步索引</el-button>
          <el-button type="success" :loading="batchLoading" @click="handleBatchStatus(1)">批量上架</el-button>
          <el-button type="warning" :loading="batchLoading" @click="handleBatchStatus(0)">批量下架</el-button>
          <el-button type="danger" :loading="batchLoading" @click="handleBatchDelete">批量删除</el-button>
        </div>
      </div>

      <el-table :data="pageData.records" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column v-if="canWrite" type="selection" width="52" />
        <el-table-column prop="id" label="商品 ID" min-width="100" />
        <el-table-column prop="name" label="商品名称" min-width="180" />
        <el-table-column prop="merchantName" label="商户" min-width="140" />
        <el-table-column prop="salePrice" label="售价" min-width="120">
          <template #default="{ row }">
            {{ formatCurrency(row.salePrice) }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" min-width="100" />
        <el-table-column prop="soldCount" label="销量" min-width="100" />
        <el-table-column prop="status" label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canWrite" type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canWrite" type="primary" link :loading="syncingId === row.id" @click="handleSync(row.id)">
              同步索引
            </el-button>
            <el-button v-if="canWrite && row.status !== 1" type="success" link @click="handleStatus(row, 1)">上架</el-button>
            <el-button v-else-if="canWrite" type="warning" link @click="handleStatus(row, 0)">下架</el-button>
            <el-button v-if="canWrite" type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="pageData.total"
          :current-page="query.pageNo"
          :page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="720px" @closed="formRef?.clearValidate()">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商户 ID" prop="merchantId">
              <el-input-number v-model="form.merchantId" :min="1" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="副标题" prop="subTitle">
              <el-input v-model="form.subTitle" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面图 URL" prop="coverUrl">
              <el-input v-model="form.coverUrl" placeholder="请输入可访问的图片 URL" />
            </el-form-item>
            <ImageUploadAssist v-model="form.coverUrl" />
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价" prop="originPrice">
              <el-input-number v-model="form.originPrice" :min="0" :precision="2" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价" prop="salePrice">
              <el-input-number v-model="form.salePrice" :min="0" :precision="2" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="full-width"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="full-width"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品详情">
              <el-input v-model="form.content" type="textarea" :rows="4" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="canWrite" type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="商品详情" size="520px">
      <div v-loading="detailLoading">
        <el-empty v-if="!productDetail && !detailLoading" description="暂无商品详情" />
        <template v-else-if="productDetail">
          <el-image v-if="productDetail.coverUrl" :src="productDetail.coverUrl" fit="cover" class="drawer-cover" />
          <el-descriptions :column="1" border>
            <el-descriptions-item label="商品 ID">{{ productDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="商品名称">{{ productDetail.name }}</el-descriptions-item>
            <el-descriptions-item label="商户">{{ productDetail.merchantName || productDetail.merchantId }}</el-descriptions-item>
            <el-descriptions-item label="副标题">{{ productDetail.subTitle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="原价">{{ formatCurrency(productDetail.originPrice) }}</el-descriptions-item>
            <el-descriptions-item label="售价">{{ formatCurrency(productDetail.salePrice) }}</el-descriptions-item>
            <el-descriptions-item label="库存 / 销量">{{ productDetail.stock || 0 }} / {{ productDetail.soldCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ getStatusLabel(productDetail.status) }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ formatDateTime(productDetail.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ formatDateTime(productDetail.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="商品详情">{{ productDetail.content || '-' }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.full-width {
  width: 100%;
}

.batch-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.batch-summary {
  font-size: 14px;
  color: #475569;
}

.preview-image {
  width: 100%;
  height: 120px;
  border-radius: 12px;
  margin-top: 8px;
}

.drawer-cover {
  width: 100%;
  height: 220px;
  border-radius: 14px;
  margin-bottom: 16px;
}
</style>
