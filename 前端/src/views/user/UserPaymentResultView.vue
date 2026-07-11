<script setup lang="ts">
import { CircleCheck, Clock, Document, ShoppingBag } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchUserOrderDetail, mockPayUserOrderCallback } from '../../api/user'
import type { OrderDetail } from '../../types'
import { formatCurrency, formatDateTime } from '../../utils'
import { getOrderStatusLabel, getOrderStatusTagType } from '../../utils/user-display'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const orderDetail = ref<OrderDetail | null>(null)
const callbackLoading = ref(false)
const callbackMessage = ref('')
const callbackSucceeded = ref(true)

const orderId = computed(() => (typeof route.query.orderId === 'string' ? route.query.orderId : '-'))
const routeOrderNo = computed(() => (typeof route.query.orderNo === 'string' ? route.query.orderNo : ''))
const channel = computed(() => (typeof route.query.channel === 'string' ? route.query.channel : 'MOCK_ALIPAY'))
const payUrl = computed(() => (typeof route.query.payUrl === 'string' ? decodeURIComponent(route.query.payUrl) : ''))
const transactionNo = computed(() => (typeof route.query.transactionNo === 'string' ? route.query.transactionNo : ''))
const orderNo = computed(() => routeOrderNo.value || orderDetail.value?.orderNo || '')
const orderStatusText = computed(() => getOrderStatusLabel(orderDetail.value?.status))
const orderStatusTagType = computed(() => getOrderStatusTagType(orderDetail.value?.status))
const resultTitle = computed(() => {
  if (callbackLoading.value) {
    return '正在同步支付结果'
  }
  return orderDetail.value?.status === 0 ? '支付请求已提交' : '支付状态已同步'
})
const resultDesc = computed(() =>
  callbackLoading.value
    ? '正在调用后端 mock 支付回调，请稍候查看订单状态刷新结果。'
    : orderDetail.value?.status === 0
    ? '当前项目使用模拟支付展示页承接 `/user/order/{id}/pay` 返回结果，页面会自动触发一次支付回调。'
    : '当前订单状态已从后端回查成功，适合直接用于演示支付后状态同步链路。',
)
const canTriggerCallback = computed(() => Boolean(Number(orderId.value) && orderNo.value && transactionNo.value))

function openPayUrl() {
  if (!payUrl.value) {
    return
  }
  window.open(payUrl.value, '_blank', 'noopener')
}

async function triggerMockPayCallback() {
  if (!canTriggerCallback.value || orderDetail.value?.status !== 0) {
    return
  }
  callbackLoading.value = true
  callbackMessage.value = ''
  callbackSucceeded.value = true
  try {
    await mockPayUserOrderCallback({
      payChannel: channel.value,
      orderNo: orderNo.value,
      transactionNo: transactionNo.value,
      payStatus: 1,
      callbackContent: `mock pay callback from user payment result page: ${transactionNo.value}`,
    })
    callbackMessage.value = '模拟支付回调已完成，订单状态已重新回查。'
    await loadOrderDetail()
  } catch (error) {
    callbackSucceeded.value = false
    callbackMessage.value = '支付结果同步失败，请稍后重试。'
    ElMessage.error((error as Error).message || '支付结果同步失败')
  } finally {
    callbackLoading.value = false
  }
}

async function loadOrderDetail() {
  if (!Number(orderId.value)) {
    return
  }
  loading.value = true
  try {
    orderDetail.value = await fetchUserOrderDetail(Number(orderId.value))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void (async () => {
    await loadOrderDetail()
    await triggerMockPayCallback()
  })()
})
</script>

<template>
  <div class="user-page">
    <section class="user-page-section user-result-card" v-loading="loading || callbackLoading">
      <el-icon class="user-result-card__icon success">
        <CircleCheck v-if="orderDetail?.status !== 0" />
        <Clock v-else />
      </el-icon>
      <h1>{{ resultTitle }}</h1>
      <p>{{ resultDesc }}</p>

      <div class="user-result-card__meta">
        <div><span>订单 ID</span><strong>{{ orderId }}</strong></div>
        <div><span>支付流水号</span><strong>{{ transactionNo || '-' }}</strong></div>
        <div><span>支付渠道</span><strong>{{ channel }}</strong></div>
        <div>
          <span>订单状态</span>
          <strong>
            <el-tag :type="orderStatusTagType">{{ orderStatusText }}</el-tag>
          </strong>
        </div>
      </div>

      <el-alert
        :title="orderDetail ? '订单详情已回查成功，可继续查看金额、商户和创建时间。' : '支付记录已创建，可回个人中心查看订单状态，也可以打开后端返回的支付链接。'"
        :type="orderDetail ? 'success' : 'info'"
        :closable="false"
        show-icon
      />
      <el-alert
        v-if="callbackMessage"
        :title="callbackMessage"
        :type="callbackSucceeded ? 'success' : 'warning'"
        :closable="false"
        show-icon
      />

      <el-descriptions v-if="orderDetail" :column="2" border class="user-result-card__detail">
        <el-descriptions-item label="订单号">{{ orderDetail.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">{{ formatCurrency(orderDetail.payAmount) }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ orderDetail.productName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商户名称">{{ orderDetail.merchantName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatDateTime(orderDetail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatDateTime(orderDetail.payTime) }}</el-descriptions-item>
      </el-descriptions>

      <div class="quick-action-row">
        <el-button type="primary" :icon="Document" @click="router.push('/user/profile?tab=orders')">查看我的订单</el-button>
        <el-button :icon="ShoppingBag" @click="router.push('/user/products')">继续逛商品</el-button>
        <el-button v-if="orderDetail?.productId" plain @click="router.push(`/user/products/${orderDetail.productId}`)">返回商品详情</el-button>
        <el-button v-if="canTriggerCallback && orderDetail?.status === 0" plain :loading="callbackLoading" @click="triggerMockPayCallback">
          重新同步支付
        </el-button>
        <el-button v-if="payUrl" plain @click="openPayUrl">打开支付链接</el-button>
      </div>
    </section>
  </div>
</template>
