<script setup lang="ts">
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import {
  createCouponTemplate,
  deleteCouponTemplate,
  fetchCouponTemplatePage,
  updateCouponTemplate,
  updateCouponTemplateStatus,
} from '../api/admin'
import type { CouponForm, CouponItem, CouponQuery, PageResult } from '../types'
import { createEmptyPage, formatCurrency, formatDateTime } from '../utils'

const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const query = reactive<CouponQuery>({
  pageNo: 1,
  pageSize: 10,
  merchantId: undefined,
  productId: undefined,
  type: undefined,
  status: undefined,
})

const form = reactive<CouponForm>({
  name: '',
  type: 1,
  discountType: 1,
  discountValue: undefined,
  thresholdAmount: undefined,
  stock: undefined,
  limitPerUser: 1,
  merchantId: undefined,
  productId: undefined,
  status: 1,
  startTime: '',
  endTime: '',
})

const pageData = ref<PageResult<CouponItem>>(createEmptyPage<CouponItem>())
const rules: FormRules<CouponForm> = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
  discountType: [{ required: true, message: '请选择优惠方式', trigger: 'change' }],
  discountValue: [{ required: true, message: '请输入优惠值', trigger: 'change' }],
  thresholdAmount: [{ required: true, message: '请输入门槛金额', trigger: 'change' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'change' }],
  limitPerUser: [{ required: true, message: '请输入每人限领数量', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择生效时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择失效时间', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (form.startTime && value && new Date(value).getTime() < new Date(form.startTime).getTime()) {
          callback(new Error('失效时间不能早于生效时间'))
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
    name: '',
    type: 1,
    discountType: 1,
    discountValue: undefined,
    thresholdAmount: undefined,
    stock: undefined,
    limitPerUser: 1,
    merchantId: undefined,
    productId: undefined,
    status: 1,
    startTime: '',
    endTime: '',
  })
}

async function loadCoupons() {
  loading.value = true
  try {
    pageData.value = await fetchCouponTemplatePage(query)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  query.pageNo = 1
  await loadCoupons()
}

async function handleReset() {
  Object.assign(query, {
    pageNo: 1,
    pageSize: 10,
    merchantId: undefined,
    productId: undefined,
    type: undefined,
    status: undefined,
  })
  await loadCoupons()
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: CouponItem) {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateCouponTemplate(form)
      ElMessage.success('优惠券模板更新成功')
    } else {
      await createCouponTemplate(form)
      ElMessage.success('优惠券模板创建成功')
    }
    dialogVisible.value = false
    formRef.value?.resetFields()
    await loadCoupons()
  } finally {
    submitting.value = false
  }
}

async function handleStatus(row: CouponItem, status: number) {
  await updateCouponTemplateStatus(row.id, { status })
  ElMessage.success(status === 1 ? '模板已启用' : '模板已停用')
  await loadCoupons()
}

async function handleDelete(row: CouponItem) {
  await ElMessageBox.confirm(`确认删除优惠券模板“${row.name}”吗？`, '删除模板', { type: 'warning' })
  await deleteCouponTemplate(row.id)
  ElMessage.success('优惠券模板删除成功')
  await loadCoupons()
}

function couponTypeLabel(type?: number) {
  return type === 1 ? '平台券' : '店铺券'
}

function discountTypeLabel(type?: number) {
  return type === 1 ? '满减' : '折扣'
}

function handleCurrentChange(pageNo: number) {
  query.pageNo = pageNo
  void loadCoupons()
}

function handleSizeChange(pageSize: number) {
  query.pageSize = pageSize
  query.pageNo = 1
  void loadCoupons()
}

onMounted(() => {
  void loadCoupons()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <div>
          <h2 class="page-title">优惠券模板管理</h2>
          <p class="page-subtitle">支持模板查询、新增、编辑、启停用和删除。</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增模板</el-button>
      </div>
    </el-card>

    <el-card class="page-card" shadow="never">
      <el-form class="filter-form" label-position="top">
        <el-form-item label="商户 ID">
          <el-input-number v-model="query.merchantId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="商品 ID">
          <el-input-number v-model="query.productId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="优惠券类型">
          <el-select v-model="query.type" clearable placeholder="全部类型">
            <el-option label="平台券" :value="1" />
            <el-option label="店铺券" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
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
      <el-table :data="pageData.records" v-loading="loading">
        <el-table-column prop="name" label="模板名称" min-width="180" />
        <el-table-column prop="type" label="类型" min-width="100">
          <template #default="{ row }">{{ couponTypeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="discountType" label="优惠方式" min-width="100">
          <template #default="{ row }">{{ discountTypeLabel(row.discountType) }}</template>
        </el-table-column>
        <el-table-column prop="discountValue" label="优惠值" min-width="120">
          <template #default="{ row }">{{ formatCurrency(row.discountValue) }}</template>
        </el-table-column>
        <el-table-column prop="thresholdAmount" label="门槛金额" min-width="120">
          <template #default="{ row }">{{ formatCurrency(row.thresholdAmount) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" min-width="90" />
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="生效时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status !== 1" type="success" link @click="handleStatus(row, 1)">启用</el-button>
            <el-button v-else type="warning" link @click="handleStatus(row, 0)">停用</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑优惠券模板' : '新增优惠券模板'" width="760px" @closed="formRef?.clearValidate()">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="模板名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="6">
            <el-form-item label="模板类型" prop="type">
              <el-select v-model="form.type">
                <el-option label="平台券" :value="1" />
                <el-option label="店铺券" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="优惠方式" prop="discountType">
              <el-select v-model="form.discountType">
                <el-option label="满减" :value="1" />
                <el-option label="折扣" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="优惠值" prop="discountValue"><el-input-number v-model="form.discountValue" :min="0" :precision="2" class="full-width" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="门槛金额" prop="thresholdAmount"><el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" class="full-width" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="库存" prop="stock"><el-input-number v-model="form.stock" :min="0" class="full-width" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="每人限领" prop="limitPerUser"><el-input-number v-model="form.limitPerUser" :min="1" class="full-width" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="商户 ID"><el-input-number v-model="form.merchantId" :min="1" class="full-width" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="商品 ID"><el-input-number v-model="form.productId" :min="1" class="full-width" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="生效时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="失效时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.full-width {
  width: 100%;
}
</style>
