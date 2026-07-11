<script setup lang="ts">
import { Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { cancelOrder, fetchOrderDetail, fetchOrderPage, refundOrder } from '../api/admin'
import { useAuthStore } from '../stores/auth'
import type { OrderDetail, OrderItem, OrderQuery, PageResult, RefundForm } from '../types'
import { createEmptyPage, formatCurrency, formatDateTime } from '../utils'
import { canWriteAdmin, resolveUserRole } from '../utils/permissions'

const loading = ref(false)
const refundLoading = ref(false)
const refundDialogVisible = ref(false)
const currentOrder = ref<OrderItem>()
const detailVisible = ref(false)
const detailLoading = ref(false)
const orderDetail = ref<OrderDetail>()
const refundFormRef = ref<FormInstance>()
const authStore = useAuthStore()
const canWrite = computed(() => canWriteAdmin(resolveUserRole(authStore.userInfo)))

const query = reactive<OrderQuery>({
  pageNo: 1,
  pageSize: 10,
  orderNo: '',
  userId: undefined,
  merchantId: undefined,
  productId: undefined,
  status: undefined,
})

const refundForm = reactive<RefundForm>({
  refundAmount: undefined,
  reason: '',
})

const pageData = ref<PageResult<OrderItem>>(createEmptyPage<OrderItem>())
const refundRules: FormRules<RefundForm> = {
  refundAmount: [{ required: true, message: '请输入退款金额', trigger: 'change' }],
  reason: [{ required: true, message: '请输入退款原因', trigger: 'blur' }],
}

function orderStatusLabel(status?: number) {
  switch (status) {
    case 0:
      return '待支付'
    case 1:
      return '已支付'
    case 2:
      return '已取消'
    case 3:
      return '已完成'
    case 4:
      return '已退款'
    default:
      return '未知'
  }
}

function orderStatusType(status?: number) {
  switch (status) {
    case 1:
    case 3:
      return 'success'
    case 4:
      return 'warning'
    case 2:
      return 'info'
    default:
      return 'danger'
  }
}

async function loadOrders() {
  loading.value = true
  try {
    pageData.value = await fetchOrderPage(query)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  query.pageNo = 1
  await loadOrders()
}

async function handleReset() {
  Object.assign(query, {
    pageNo: 1,
    pageSize: 10,
    orderNo: '',
    userId: undefined,
    merchantId: undefined,
    productId: undefined,
    status: undefined,
  })
  await loadOrders()
}

async function handleCancel(row: OrderItem) {
  await ElMessageBox.confirm(`确认取消订单 ${row.orderNo} 吗？`, '取消订单', { type: 'warning' })
  await cancelOrder(row.id)
  ElMessage.success('订单取消成功')
  await loadOrders()
}

async function openDetail(row: OrderItem) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    orderDetail.value = await fetchOrderDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

function openRefund(row: OrderItem) {
  currentOrder.value = row
  refundForm.refundAmount = row.payAmount
  refundForm.reason = ''
  refundDialogVisible.value = true
}

async function submitRefund() {
  if (!currentOrder.value) return
  const valid = await refundFormRef.value?.validate().catch(() => false)
  if (!valid) return
  refundLoading.value = true
  try {
    await refundOrder(currentOrder.value.id, refundForm)
    ElMessage.success('退款申请成功')
    refundDialogVisible.value = false
    await loadOrders()
  } finally {
    refundLoading.value = false
  }
}

function handleCurrentChange(pageNo: number) {
  query.pageNo = pageNo
  void loadOrders()
}

function handleSizeChange(pageSize: number) {
  query.pageSize = pageSize
  query.pageNo = 1
  void loadOrders()
}

onMounted(() => {
  void loadOrders()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <div>
          <h2 class="page-title">订单管理</h2>
          <p class="page-subtitle">支持订单分页查询、取消订单和后台退款。</p>
        </div>
      </div>
    </el-card>

    <el-card class="page-card" shadow="never">
      <el-form class="filter-form" label-position="top">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" clearable placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="用户 ID">
          <el-input-number v-model="query.userId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="商户 ID">
          <el-input-number v-model="query.merchantId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="商品 ID">
          <el-input-number v-model="query.productId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="query.status" clearable placeholder="全部状态">
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已退款" :value="4" />
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
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="userNickname" label="用户" min-width="120" />
        <el-table-column prop="merchantName" label="商户" min-width="140" />
        <el-table-column prop="productName" label="商品" min-width="160" />
        <el-table-column prop="payAmount" label="实付金额" min-width="120">
          <template #default="{ row }">
            {{ formatCurrency(row.payAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="orderStatusType(row.status)">{{ orderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canWrite && (row.status === 0 || row.status === 1)" type="warning" link @click="handleCancel(row)">
              取消订单
            </el-button>
            <el-button v-if="canWrite && row.status === 1" type="danger" link @click="openRefund(row)">
              发起退款
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

    <el-dialog v-model="refundDialogVisible" title="后台发起退款" width="520px" @closed="refundFormRef?.clearValidate()">
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-position="top">
        <el-form-item label="退款金额" prop="refundAmount">
          <el-input-number v-model="refundForm.refundAmount" :min="0.01" :precision="2" class="full-width" />
        </el-form-item>
        <el-form-item label="退款原因" prop="reason">
          <el-input v-model="refundForm.reason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button v-if="canWrite" type="primary" :loading="refundLoading" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="订单详情" size="520px">
      <div v-loading="detailLoading">
        <el-empty v-if="!orderDetail && !detailLoading" description="暂无订单详情" />
        <template v-else-if="orderDetail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="订单 ID">{{ orderDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ orderDetail.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="用户">{{ orderDetail.userNickname || orderDetail.userId }}</el-descriptions-item>
            <el-descriptions-item label="商户">{{ orderDetail.merchantName || orderDetail.merchantId }}</el-descriptions-item>
            <el-descriptions-item label="商品">{{ orderDetail.productName || orderDetail.productId }}</el-descriptions-item>
            <el-descriptions-item label="优惠券 ID">{{ orderDetail.couponId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="订单总金额">{{ formatCurrency(orderDetail.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="优惠金额">{{ formatCurrency(orderDetail.discountAmount) }}</el-descriptions-item>
            <el-descriptions-item label="实付金额">{{ formatCurrency(orderDetail.payAmount) }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ orderStatusLabel(orderDetail.status) }}</el-descriptions-item>
            <el-descriptions-item label="订单备注">{{ orderDetail.remark || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(orderDetail.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ formatDateTime(orderDetail.payTime) }}</el-descriptions-item>
            <el-descriptions-item label="取消时间">{{ formatDateTime(orderDetail.cancelTime) }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ formatDateTime(orderDetail.finishTime) }}</el-descriptions-item>
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
</style>
