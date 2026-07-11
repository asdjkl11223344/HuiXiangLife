<script setup lang="ts">
import { ArrowLeft, Clock, CircleCheck, Star, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { ProductDetail, PageResult } from '../../types'
import {
  createUserFavorite,
  createUserOrder,
  createUserSeckillOrder,
  deleteUserFavorite,
  fetchRecommendProducts,
  fetchUserFavoritePage,
  fetchUserProductDetail,
  fetchUserReviewPage,
  fetchUserSeckillResult,
} from '../../api/user'
import { useUserAuthStore } from '../../stores/user-auth'
import type { UserFavoriteItem, UserReviewItem, UserSeckillResult } from '../../types/user'
import { createEmptyPage, formatCurrency, formatDateTime } from '../../utils'
import { getOrderStatusLabel, getOrderStatusTagType } from '../../utils/user-display'

const route = useRoute()
const router = useRouter()
const userAuthStore = useUserAuthStore()
const loading = ref(false)
const actionLoading = ref(false)
const seckillPolling = ref(false)
const product = ref<ProductDetail | null>(null)
const reviews = ref<PageResult<UserReviewItem>>(createEmptyPage<UserReviewItem>())
const recommendations = ref<ProductDetail[]>([])
const favoriteList = ref<UserFavoriteItem[]>([])
const quantity = ref(1)
const remark = ref('')
const seckillMessage = ref('')
const seckillResult = ref<UserSeckillResult | null>(null)
let pollTimer: number | undefined

const productId = computed(() => Number(route.params.id))
const isLoggedIn = computed(() => userAuthStore.isLoggedIn)
const isFavorited = computed(() => favoriteList.value.some((item) => item.targetId === productId.value))
const availableStock = computed(() => Math.max(product.value?.stock ?? 0, 0))
const hasStock = computed(() => availableStock.value > 0)

function ensureLogin() {
  if (isLoggedIn.value) {
    return true
  }
  router.push({
    path: '/user/login',
    query: { redirect: route.fullPath },
  })
  return false
}

async function loadFavoriteState() {
  if (!isLoggedIn.value || !product.value) {
    favoriteList.value = []
    return
  }
  const result = await fetchUserFavoritePage({
    pageNo: 1,
    pageSize: 100,
    targetType: 2,
  })
  favoriteList.value = result.records
}

async function loadData() {
  loading.value = true
  try {
    const [detail, reviewPage, recommendList] = await Promise.all([
      fetchUserProductDetail(productId.value),
      fetchUserReviewPage({
        pageNo: 1,
        pageSize: 6,
        productId: productId.value,
        status: 1,
      }),
      fetchRecommendProducts(4),
    ])
    product.value = detail
    quantity.value = detail.stock && detail.stock > 0 ? 1 : 0
    seckillMessage.value = ''
    seckillResult.value = null
    seckillPolling.value = false
    reviews.value = reviewPage
    recommendations.value = recommendList.filter((item) => item.id !== detail.id)
    await loadFavoriteState()
  } finally {
    loading.value = false
  }
}

async function handleCreateOrder() {
  if (!product.value || !ensureLogin()) {
    return
  }
  if (!hasStock.value || quantity.value <= 0) {
    ElMessage.warning('当前商品库存不足，暂时无法下单')
    return
  }
  actionLoading.value = true
  try {
    const orderId = await createUserOrder({
      merchantId: product.value.merchantId,
      productId: product.value.id,
      quantity: quantity.value,
      remark: remark.value,
    })
    ElMessage.success(`下单成功，订单号：${orderId}`)
    router.push('/user/profile?tab=orders')
  } finally {
    actionLoading.value = false
  }
}

async function pollSeckillResult(interval = 1000) {
  window.clearTimeout(pollTimer)
  const result = await fetchUserSeckillResult(productId.value)
  seckillResult.value = result
  seckillMessage.value = result.message || result.status || '正在查询秒杀结果'
  if (result.finished) {
    seckillPolling.value = false
    if (result.orderId) {
      ElMessage.success('秒杀成功，请前往订单中心支付')
    }
    return
  }
  pollTimer = window.setTimeout(() => pollSeckillResult(result.nextPollIntervalMillis || interval), result.nextPollIntervalMillis || interval)
}

async function handleSeckill() {
  if (!product.value || !ensureLogin()) {
    return
  }
  if (!hasStock.value) {
    ElMessage.warning('当前商品已售罄，无法继续参与秒杀')
    return
  }
  if (seckillPolling.value) {
    ElMessage.warning('秒杀结果还在处理中，请稍候')
    return
  }
  actionLoading.value = true
  try {
    seckillPolling.value = true
    seckillResult.value = {
      finished: false,
      status: 'QUEUEING',
      message: '秒杀请求已提交，正在排队处理中',
    }
    const result = await createUserSeckillOrder({
      merchantId: product.value.merchantId,
      productId: product.value.id,
      remark: remark.value,
    })
    seckillResult.value = {
      requestId: result.requestId,
      finished: false,
      status: 'QUEUEING',
      message: result.message || '秒杀请求已提交',
    }
    seckillMessage.value = result.message || '秒杀请求已提交'
    await pollSeckillResult(result.pollIntervalMillis || 1000)
  } catch (error) {
    seckillPolling.value = false
    throw error
  } finally {
    actionLoading.value = false
  }
}

async function handleToggleFavorite() {
  if (!product.value || !ensureLogin()) {
    return
  }

  if (isFavorited.value) {
    await deleteUserFavorite(product.value.id, 2)
    ElMessage.success('已取消收藏')
  } else {
    await createUserFavorite({
      targetId: product.value.id,
      targetType: 2,
    })
    ElMessage.success('收藏成功')
  }
  await loadFavoriteState()
}

const seckillResultTitle = computed(() => {
  if (!seckillResult.value) {
    return ''
  }
  if (!seckillResult.value.finished) {
    return '秒杀处理中'
  }
  return seckillResult.value.orderId ? '秒杀成功' : '秒杀未命中'
})

const seckillResultTagType = computed(() => {
  if (!seckillResult.value) {
    return 'info'
  }
  if (!seckillResult.value.finished) {
    return 'warning'
  }
  return seckillResult.value.orderId ? 'success' : 'danger'
})

const seckillResultIcon = computed(() => {
  if (!seckillResult.value) {
    return Clock
  }
  if (!seckillResult.value.finished) {
    return Clock
  }
  return seckillResult.value.orderId ? CircleCheck : Warning
})

const seckillOrderStatusText = computed(() => (seckillResult.value?.orderId ? getOrderStatusLabel(0) : '未生成订单'))
const seckillOrderStatusTagType = computed(() => (seckillResult.value?.orderId ? getOrderStatusTagType(0) : 'info'))

watch(
  () => route.params.id,
  () => {
    loadData()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  window.clearTimeout(pollTimer)
  seckillPolling.value = false
})
</script>

<template>
  <div class="user-page" v-loading="loading">
    <section v-if="product" class="user-detail-hero">
      <el-button text :icon="ArrowLeft" @click="router.back()">返回上一页</el-button>
      <div class="user-detail-hero__grid">
        <img
          :src="product.coverUrl || 'https://dummyimage.com/800x520/e2e8f0/64748b&text=Product'"
          class="user-detail-hero__image"
        />
        <div class="user-detail-hero__info">
          <div class="user-section-badge">商品详情</div>
          <h1>{{ product.name }}</h1>
          <p>{{ product.subTitle || product.content || '暂无商品简介' }}</p>
          <div class="user-detail-hero__price">
            <strong>{{ formatCurrency(product.salePrice) }}</strong>
            <span>原价 {{ formatCurrency(product.originPrice) }}</span>
          </div>
          <div class="user-detail-hero__meta">
            <span>商户：{{ product.merchantName || '-' }}</span>
            <span>库存：{{ product.stock || 0 }}</span>
            <span>销量：{{ product.soldCount || 0 }}</span>
          </div>
          <div class="user-detail-hero__form">
            <el-input-number
              v-model="quantity"
              :min="hasStock ? 1 : 0"
              :max="availableStock"
              :disabled="!hasStock"
            />
            <el-input v-model="remark" placeholder="可填写口味、时间等备注" :disabled="seckillPolling" />
          </div>
          <div class="user-detail-hero__actions">
            <el-button type="primary" :loading="actionLoading" :disabled="!hasStock || seckillPolling" @click="handleCreateOrder">
              立即下单
            </el-button>
            <el-button
              type="danger"
              plain
              :loading="actionLoading || seckillPolling"
              :disabled="!hasStock || seckillPolling"
              @click="handleSeckill"
            >
              参与秒杀
            </el-button>
            <el-button :icon="Star" @click="handleToggleFavorite">
              {{ isFavorited ? '取消收藏' : '加入收藏' }}
            </el-button>
          </div>
          <div class="muted-text">上架时间：{{ formatDateTime(product.startTime) }}</div>
          <div v-if="seckillMessage" class="user-seckill-tip">{{ seckillMessage }}</div>
          <el-alert
            v-if="!hasStock"
            title="当前商品库存为 0，已禁止下单和秒杀。"
            type="warning"
            :closable="false"
            show-icon
          />
          <div v-if="seckillResult" class="user-seckill-result-card">
            <div class="user-seckill-result-card__head">
              <div class="user-loading-inline">
                <el-icon><component :is="seckillResultIcon" /></el-icon>
                <strong>{{ seckillResultTitle }}</strong>
              </div>
              <el-tag :type="seckillResultTagType">{{ seckillResult.status || '处理中' }}</el-tag>
            </div>
            <div class="user-seckill-result-card__grid">
              <div>
                <span>请求 ID</span>
                <strong>{{ seckillResult.requestId || '-' }}</strong>
              </div>
              <div>
                <span>订单状态</span>
                <strong>
                  <el-tag :type="seckillOrderStatusTagType">{{ seckillOrderStatusText }}</el-tag>
                </strong>
              </div>
              <div>
                <span>订单 ID</span>
                <strong>{{ seckillResult.orderId || '-' }}</strong>
              </div>
              <div>
                <span>结果说明</span>
                <strong>{{ seckillResult.message || '后端正在处理中' }}</strong>
              </div>
            </div>
            <div class="quick-action-row">
              <el-button v-if="seckillResult.orderId" type="primary" plain @click="router.push('/user/profile?tab=orders')">去支付秒杀订单</el-button>
              <el-button v-if="!seckillResult.finished" plain @click="pollSeckillResult()">立即刷新结果</el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>商品介绍</h2>
          <p>适合在演示时串联商品页、下单、收藏和秒杀流程。</p>
        </div>
      </div>
      <div class="user-rich-text">
        {{ product?.content || product?.subTitle || '暂无详细介绍' }}
      </div>
    </section>

    <section class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>用户评价</h2>
          <p>当前商品已接入 `/user/review/page` 评价列表。</p>
        </div>
      </div>
      <div v-if="reviews.records.length" class="user-review-list">
        <article v-for="item in reviews.records" :key="item.id" class="user-review-card">
          <div class="user-review-card__head">
            <strong>{{ item.userNickname || '匿名用户' }}</strong>
            <span>评分 {{ item.score || '-' }}</span>
          </div>
          <p>{{ item.content || '该用户未填写评价内容。' }}</p>
          <span class="muted-text">{{ formatDateTime(item.createTime) }}</span>
        </article>
      </div>
      <el-empty v-else description="暂时还没有评价，适合首个下单用户来触发。" />
    </section>

    <section class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>你可能还喜欢</h2>
          <p>继续引导用户浏览和下单。</p>
        </div>
      </div>
      <div class="user-card-grid">
        <article v-for="item in recommendations" :key="item.id" class="user-card">
          <img :src="item.coverUrl || 'https://dummyimage.com/600x360/e2e8f0/64748b&text=Product'" class="user-card__image" />
          <div class="user-card__body">
            <h3>{{ item.name }}</h3>
            <p>{{ item.subTitle || item.content || '推荐商品' }}</p>
            <div class="user-card__meta">
              <span>{{ item.merchantName || '-' }}</span>
              <strong>{{ formatCurrency(item.salePrice) }}</strong>
            </div>
            <el-button type="primary" plain @click="router.push(`/user/products/${item.id}`)">查看详情</el-button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
