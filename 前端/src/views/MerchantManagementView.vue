<script setup lang="ts">
import { Delete, Edit, Plus, RefreshRight, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import {
  batchDeleteMerchant,
  batchSyncMerchantSearchIndex,
  batchUpdateMerchantStatus,
  createMerchant,
  deleteMerchant,
  fetchMerchantCategoryList,
  fetchMerchantDetail,
  fetchMerchantPage,
  rebuildMerchantSearchIndex,
  syncMerchantSearchIndex,
  updateMerchant,
  updateMerchantStatus,
} from '../api/admin'
import ImageUploadAssist from '../components/ImageUploadAssist.vue'
import { useAuthStore } from '../stores/auth'
import type {
  MerchantDetail,
  MerchantCategoryItem,
  MerchantForm,
  MerchantItem,
  MerchantQuery,
  PageResult,
} from '../types'
import { createEmptyPage, formatCurrency } from '../utils'
import { canWriteAdmin, resolveUserRole } from '../utils/permissions'

const loading = ref(false)
const rebuildLoading = ref(false)
const syncId = ref<number>()
const deleteId = ref<number>()
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const categories = ref<MerchantCategoryItem[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const merchantDetail = ref<MerchantDetail>()
const formRef = ref<FormInstance>()
const authStore = useAuthStore()
const canWrite = computed(() => canWriteAdmin(resolveUserRole(authStore.userInfo)))
const selectedRows = ref<MerchantItem[]>([])
const batchLoading = ref(false)

const query = reactive<MerchantQuery>({
  pageNo: 1,
  pageSize: 10,
  categoryId: undefined,
  keyword: '',
  status: undefined,
})

const form = reactive<MerchantForm>({
  name: '',
  categoryId: undefined,
  coverUrl: '',
  address: '',
  phone: '',
  description: '',
  avgPrice: undefined,
  status: 1,
})

const pageData = ref<PageResult<MerchantItem>>(createEmptyPage<MerchantItem>())
const rules: FormRules<MerchantForm> = {
  name: [
    { required: true, message: '请输入商户名称', trigger: 'blur' },
    { min: 1, max: 100, message: '商户名称长度不能超过 100 位', trigger: 'blur' },
  ],
  categoryId: [{ required: true, message: '请选择商户分类', trigger: 'change' }],
  coverUrl: [{ max: 255, message: '封面图地址长度不能超过 255 位', trigger: 'blur' }],
  address: [
    { required: true, message: '请输入商户地址', trigger: 'blur' },
    { max: 255, message: '商户地址长度不能超过 255 位', trigger: 'blur' },
  ],
  phone: [{ max: 20, message: '联系电话长度不能超过 20 位', trigger: 'blur' }],
  description: [{ max: 500, message: '商户简介长度不能超过 500 位', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    name: '',
    categoryId: undefined,
    coverUrl: '',
    address: '',
    phone: '',
    description: '',
    avgPrice: undefined,
    status: 1,
  })
}

async function loadCategories() {
  categories.value = await fetchMerchantCategoryList()
}

async function loadMerchants() {
  loading.value = true
  try {
    pageData.value = await fetchMerchantPage(query)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  query.pageNo = 1
  await loadMerchants()
}

async function handleReset() {
  query.pageNo = 1
  query.pageSize = 10
  query.categoryId = undefined
  query.keyword = ''
  query.status = undefined
  await loadMerchants()
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: MerchantItem) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    categoryId: row.categoryId,
    coverUrl: row.coverUrl || '',
    address: row.address || '',
    phone: row.phone || '',
    description: row.description || '',
    avgPrice: row.avgPrice,
    status: row.status ?? 1,
  })
  dialogVisible.value = true
}

async function openDetail(row: MerchantItem) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    merchantDetail.value = await fetchMerchantDetail(row.id)
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
      await updateMerchant(form)
      ElMessage.success('商户更新成功')
    } else {
      await createMerchant(form)
      ElMessage.success('商户创建成功')
    }
    dialogVisible.value = false
    formRef.value?.resetFields()
    await loadMerchants()
  } finally {
    submitting.value = false
  }
}

async function handleSync(id: number) {
  syncId.value = id
  try {
    await syncMerchantSearchIndex(id)
    ElMessage.success(`商户 ${id} 索引同步成功`)
  } finally {
    syncId.value = undefined
  }
}

async function handleRebuild() {
  rebuildLoading.value = true
  try {
    const count = await rebuildMerchantSearchIndex()
    ElMessage.success(`商户索引重建完成，共处理 ${count} 条数据`)
    await loadMerchants()
  } finally {
    rebuildLoading.value = false
  }
}

async function handleDelete(row: MerchantItem) {
  await ElMessageBox.confirm(
    `确认删除商户“${row.name}”吗？如果该商户仍关联商品或订单，后端会拒绝删除。`,
    '删除商户',
    { type: 'warning' },
  )

  deleteId.value = row.id
  try {
    await deleteMerchant(row.id)
    ElMessage.success(`商户 ${row.id} 删除成功`)
    await loadMerchants()
  } finally {
    deleteId.value = undefined
  }
}

async function handleStatus(row: MerchantItem, status: number) {
  await updateMerchantStatus(row.id, { status })
  ElMessage.success(status === 1 ? '商户已启用' : '商户已停用')
  await loadMerchants()
}

function handleCurrentChange(pageNo: number) {
  query.pageNo = pageNo
  void loadMerchants()
}

function handleSizeChange(pageSize: number) {
  query.pageSize = pageSize
  query.pageNo = 1
  void loadMerchants()
}

function getStatusLabel(status?: number) {
  return status === 1 ? '启用中' : '已停用'
}

function getStatusType(status?: number) {
  return status === 1 ? 'success' : 'info'
}

const selectedIds = computed(() => selectedRows.value.map((item) => item.id))

function handleSelectionChange(rows: MerchantItem[]) {
  selectedRows.value = rows
}

async function handleBatchSync() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择商户')
    return
  }
  batchLoading.value = true
  try {
    const result = await batchSyncMerchantSearchIndex(selectedIds.value)
    ElMessage.success(`批量同步完成，成功 ${result.successCount} 条，失败 ${result.failureCount} 条`)
  } finally {
    batchLoading.value = false
  }
}

async function handleBatchStatus(status: number) {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择商户')
    return
  }
  batchLoading.value = true
  try {
    const result = await batchUpdateMerchantStatus(selectedIds.value, status)
    ElMessage.success(
      `批量${status === 1 ? '启用' : '停用'}完成，成功 ${result.successCount} 条，失败 ${result.failureCount} 条`,
    )
    await loadMerchants()
  } finally {
    batchLoading.value = false
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择商户')
    return
  }
  await ElMessageBox.confirm(`确认批量删除已选中的 ${selectedIds.value.length} 个商户吗？`, '批量删除商户', {
    type: 'warning',
  })
  batchLoading.value = true
  try {
    const result = await batchDeleteMerchant(selectedIds.value)
    ElMessage.success(`批量删除完成，成功 ${result.successCount} 条，失败 ${result.failureCount} 条`)
    await loadMerchants()
  } finally {
    batchLoading.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  await loadMerchants()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <div>
          <h2 class="page-title">商户管理</h2>
          <p class="page-subtitle">补齐商户分页、创建、编辑、启停用、删除和搜索索引管理。</p>
        </div>

        <div class="filter-actions">
          <el-button v-if="canWrite" type="primary" :icon="Plus" @click="openCreate">新增商户</el-button>
          <el-button
            v-if="canWrite"
            type="success"
            :icon="RefreshRight"
            :loading="rebuildLoading"
            @click="handleRebuild"
          >
            重建商户索引
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card class="page-card" shadow="never">
      <el-form class="filter-form" label-position="top">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="商户名称关键词" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" clearable placeholder="全部分类">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态">
            <el-option label="启用中" :value="1" />
            <el-option label="已停用" :value="0" />
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
        <div class="batch-summary">已选中 {{ selectedIds.length }} 个商户</div>
        <div class="quick-action-row">
          <el-button :loading="batchLoading" @click="handleBatchSync">批量同步索引</el-button>
          <el-button type="success" :loading="batchLoading" @click="handleBatchStatus(1)">批量启用</el-button>
          <el-button type="warning" :loading="batchLoading" @click="handleBatchStatus(0)">批量停用</el-button>
          <el-button type="danger" :loading="batchLoading" @click="handleBatchDelete">批量删除</el-button>
        </div>
      </div>

      <el-table :data="pageData.records" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column v-if="canWrite" type="selection" width="52" />
        <el-table-column prop="id" label="商户 ID" min-width="110" />
        <el-table-column prop="name" label="商户名称" min-width="180" />
        <el-table-column prop="categoryName" label="分类" min-width="120" />
        <el-table-column prop="score" label="评分" min-width="100" />
        <el-table-column prop="avgPrice" label="人均价格" min-width="120">
          <template #default="{ row }">
            {{ formatCurrency(row.avgPrice) }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" min-width="140" />
        <el-table-column prop="status" label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canWrite" type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canWrite" type="primary" link :loading="syncId === row.id" @click="handleSync(row.id)">
              同步索引
            </el-button>
            <el-button v-if="canWrite && row.status !== 1" type="success" link @click="handleStatus(row, 1)">启用</el-button>
            <el-button v-else-if="canWrite" type="warning" link @click="handleStatus(row, 0)">停用</el-button>
            <el-button
              v-if="canWrite"
              type="danger"
              link
              :icon="Delete"
              :loading="deleteId === row.id"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商户' : '新增商户'" width="720px" @closed="formRef?.clearValidate()">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商户名称" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商户分类" prop="categoryId">
              <el-select v-model="form.categoryId" class="full-width" placeholder="请选择分类">
                <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面图 URL" prop="coverUrl">
              <el-input v-model="form.coverUrl" placeholder="请输入可访问的图片 URL" />
            </el-form-item>
            <ImageUploadAssist v-model="form.coverUrl" />
          </el-col>
          <el-col :span="12">
            <el-form-item label="商户地址" prop="address">
              <el-input v-model="form.address" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人均价格">
              <el-input-number v-model="form.avgPrice" :min="0" :precision="2" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商户简介" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="4" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="canWrite" type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="商户详情" size="520px">
      <div v-loading="detailLoading">
        <el-empty v-if="!merchantDetail && !detailLoading" description="暂无商户详情" />
        <template v-else-if="merchantDetail">
          <el-image v-if="merchantDetail.coverUrl" :src="merchantDetail.coverUrl" fit="cover" class="drawer-cover" />
          <el-descriptions :column="1" border>
            <el-descriptions-item label="商户 ID">{{ merchantDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="商户名称">{{ merchantDetail.name }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ merchantDetail.categoryName || merchantDetail.categoryId }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ merchantDetail.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商户地址">{{ merchantDetail.address || '-' }}</el-descriptions-item>
            <el-descriptions-item label="评分">{{ merchantDetail.score ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="人均价格">{{ formatCurrency(merchantDetail.avgPrice) }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ getStatusLabel(merchantDetail.status) }}</el-descriptions-item>
            <el-descriptions-item label="商户简介">{{ merchantDetail.description || '-' }}</el-descriptions-item>
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
