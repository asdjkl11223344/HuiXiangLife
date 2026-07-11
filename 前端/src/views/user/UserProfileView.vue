<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { PageResult } from '../../types'
import {
  cancelUserOrder,
  createUserReview,
  deleteUserFavorite,
  fetchMyCouponPage,
  fetchUserCouponPage,
  fetchUserFavoritePage,
  fetchUserOrderDetail,
  fetchUserOrderPage,
  fetchUserReviewPage,
  getUserMe,
  payUserOrder,
  receiveUserCoupon,
} from '../../api/user'
import { useUserAuthStore } from '../../stores/user-auth'
import type { UserCouponItem, UserFavoriteItem, UserOrderItem, UserOwnedCouponItem, UserReviewItem } from '../../types/user'
import { createEmptyPage, formatCurrency, formatDateTime } from '../../utils'
import {
  getCouponTemplateStatusLabel,
  getCouponTemplateStatusTagType,
  getFavoriteTargetTypeLabel,
  getOrderStatusLabel,
  getOrderStatusTagType,
  getUserCouponStatusLabel,
  getUserCouponStatusTagType,
} from '../../utils/user-display'

const route = useRoute()
const router = useRouter()
const userAuthStore = useUserAuthStore()
const loading = ref(false)
const activeTab = ref('overview')
const orderFilters = reactive({
  orderNo: '',
  status: undefined as number | undefined,
})
const orderPager = reactive({ pageNo: 1, pageSize: 6 })
const favoritePager = reactive({ pageNo: 1, pageSize: 8 })
const ownedCouponPager = reactive({ pageNo: 1, pageSize: 6 })
const couponCenterPager = reactive({ pageNo: 1, pageSize: 6 })
const reviewPager = reactive({ pageNo: 1, pageSize: 8 })

const orderPage = ref<PageResult<UserOrderItem>>(createEmptyPage<UserOrderItem>())
const favoritePage = ref<PageResult<UserFavoriteItem>>(createEmptyPage<UserFavoriteItem>())
const ownedCouponPage = ref<PageResult<UserOwnedCouponItem>>(createEmptyPage<UserOwnedCouponItem>())
const couponCenterPage = ref<PageResult<UserCouponItem>>(createEmptyPage<UserCouponItem>())
const reviewPage = ref<PageResult<UserReviewItem>>(createEmptyPage<UserReviewItem>())
const orderDetailVisible = ref(false)
const orderDetailLoading = ref(false)
const currentOrderDetail = ref<UserOrderItem | null>(null)
const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewFormRef = ref<FormInstance>()
const reviewForm = reactive({
  orderId: 0,
  merchantId: 0,
  productId: 0,
  score: 5,
  content: '',
})
const reviewableOrderStatuses = new Set([1, 2])

const profile = computed(() => userAuthStore.userInfo)

const reviewRules: FormRules<typeof reviewForm> = {
  score: [{ required: true, message: '请选择评分', trigger: 'change' }],
  content: [{ max: 500, message: '评价内容不能超过 500 字', trigger: 'blur' }],
}

function syncTabFromRoute() {
  const tab = typeof route.query.tab === 'string' ? route.query.tab : 'overview'
  activeTab.value = ['overview', 'orders', 'favorites', 'coupons', 'reviews'].includes(tab) ? tab : 'overview'
}

function handleTabChange(tab: string | number) {
  router.replace({
    path: '/user/profile',
    query: { tab: String(tab) },
  })
}

async function loadProfile() {
  if (!userAuthStore.userInfo) {
    const me = await getUserMe()
    userAuthStore.setSession(userAuthStore.token, userAuthStore.tokenType, me)
  }
}

async function loadData() {
  loading.value = true
  try {
    await loadProfile()
    const currentUserId = userAuthStore.userInfo?.id
    const [orders, favorites, ownedCoupons, couponCenter, reviews] = await Promise.all([
      fetchUserOrderPage({
        pageNo: orderPager.pageNo,
        pageSize: orderPager.pageSize,
        orderNo: orderFilters.orderNo || undefined,
        status: orderFilters.status,
      }),
      fetchUserFavoritePage({
        pageNo: favoritePager.pageNo,
        pageSize: favoritePager.pageSize,
      }),
      fetchMyCouponPage({
        pageNo: ownedCouponPager.pageNo,
        pageSize: ownedCouponPager.pageSize,
      }),
      fetchUserCouponPage({
        pageNo: couponCenterPager.pageNo,
        pageSize: couponCenterPager.pageSize,
        status: 1,
      }),
      fetchUserReviewPage({
        pageNo: reviewPager.pageNo,
        pageSize: reviewPager.pageSize,
        userId: currentUserId,
      }),
    ])
    orderPage.value = orders
    favoritePage.value = favorites
    ownedCouponPage.value = ownedCoupons
    couponCenterPage.value = couponCenter
    reviewPage.value = reviews
  } finally {
    loading.value = false
  }
}

async function handleCancelOrder(row: UserOrderItem) {
  await ElMessageBox.confirm(`确认取消订单 ${row.orderNo} 吗？`, '取消订单', { type: 'warning' })
  await cancelUserOrder(row.id)
  ElMessage.success('订单已取消')
  await loadData()
}

async function handlePayOrder(row: UserOrderItem) {
  const payment = await payUserOrder(row.id, {
    orderId: row.id,
    payChannel: 'MOCK_ALIPAY',
  })
  ElMessage.success('支付请求已提交')
  router.push({
    path: '/user/payment-result',
    query: {
      orderId: String(row.id),
      orderNo: row.orderNo,
      channel: payment.payChannel || 'MOCK_ALIPAY',
      ...(payment.payUrl ? { payUrl: encodeURIComponent(payment.payUrl) } : {}),
      ...(payment.transactionNo ? { transactionNo: payment.transactionNo } : {}),
    },
  })
}

async function handleReceiveCoupon(item: UserCouponItem) {
  await receiveUserCoupon({ couponTemplateId: item.id })
  ElMessage.success('领券成功')
  await loadData()
}

async function handleViewOrderDetail(row: UserOrderItem) {
  orderDetailVisible.value = true
  orderDetailLoading.value = true
  try {
    currentOrderDetail.value = await fetchUserOrderDetail(row.id)
  } finally {
    orderDetailLoading.value = false
  }
}

function canReviewOrder(row: UserOrderItem) {
  return Boolean(row.merchantId && row.productId && reviewableOrderStatuses.has(row.status ?? -1))
}

function getReviewButtonText(row: UserOrderItem) {
  if (!row.merchantId || !row.productId) {
    return '无法评价'
  }
  if (canReviewOrder(row)) {
    return '评价'
  }
  return '支付后可评'
}

function handleOpenReview(row: UserOrderItem) {
  if (!canReviewOrder(row)) {
    ElMessage.warning('仅已支付或已完成的订单可以评价')
    return
  }
  reviewForm.orderId = row.id
  reviewForm.merchantId = row.merchantId || 0
  reviewForm.productId = row.productId || 0
  reviewForm.score = 5
  reviewForm.content = ''
  reviewVisible.value = true
}

async function handleSubmitReview() {
  if (!reviewFormRef.value) {
    return
  }
  await reviewFormRef.value.validate()
  reviewSubmitting.value = true
  try {
    await createUserReview({
      orderId: reviewForm.orderId,
      merchantId: reviewForm.merchantId,
      productId: reviewForm.productId,
      score: reviewForm.score,
      content: reviewForm.content,
    })
    ElMessage.success('评价提交成功')
    reviewVisible.value = false
    await loadData()
  } finally {
    reviewSubmitting.value = false
  }
}

async function handleRemoveFavorite(item: UserFavoriteItem) {
  await ElMessageBox.confirm(`确认移除收藏 ${item.targetName || item.targetId} 吗？`, '移除收藏', { type: 'warning' })
  await deleteUserFavorite(item.targetId, item.targetType)
  ElMessage.success('已移除收藏')
  await loadData()
}

function resetOrderFilters() {
  orderFilters.orderNo = ''
  orderFilters.status = undefined
  orderPager.pageNo = 1
  void loadData()
}

function handleOrderFilterSearch() {
  orderPager.pageNo = 1
  void loadData()
}

function handleOrderPageChange(page: number) {
  orderPager.pageNo = page
  void loadData()
}

function handleFavoritePageChange(page: number) {
  favoritePager.pageNo = page
  void loadData()
}

function handleOwnedCouponPageChange(page: number) {
  ownedCouponPager.pageNo = page
  void loadData()
}

function handleCouponCenterPageChange(page: number) {
  couponCenterPager.pageNo = page
  void loadData()
}

function handleReviewPageChange(page: number) {
  reviewPager.pageNo = page
  void loadData()
}

watch(
  () => route.query.tab,
  () => {
    syncTabFromRoute()
  },
  { immediate: true },
)

onMounted(loadData)
</script>

<template>
  <div class="user-page" v-loading="loading">
    <section class="user-page-section">
      <div class="user-profile-head">
        <div class="user-profile-head__info">
          <el-avatar :src="profile?.avatar" :size="72">{{ profile?.nickname?.slice(0, 1) || 'U' }}</el-avatar>
          <div>
            <h1>{{ profile?.nickname || '用户中心' }}</h1>
            <p>{{ profile?.phone || '登录后查看手机号' }}</p>
            <div class="user-chip-list">
          <el-tag type="primary">订单 {{ orderPage.total }}</el-tag>
          <el-tag type="success">收藏 {{ favoritePage.total }}</el-tag>
          <el-tag type="warning">我的券 {{ ownedCouponPage.total }}</el-tag>
          <el-tag type="info">评价 {{ reviewPage.total }}</el-tag>
            </div>
          </div>
        </div>
        <div class="user-profile-head__meta">
          <span>最近登录：{{ formatDateTime(profile?.lastLoginTime) }}</span>
          <span>账号状态：{{ profile?.status === 1 ? '正常' : '待确认' }}</span>
        </div>
      </div>
    </section>

    <section class="user-page-section compact">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="概览" name="overview" />
        <el-tab-pane label="我的订单" name="orders" />
        <el-tab-pane label="我的收藏" name="favorites" />
        <el-tab-pane label="优惠券" name="coupons" />
        <el-tab-pane label="我的评价" name="reviews" />
      </el-tabs>
    </section>

    <section v-if="activeTab === 'overview'" class="user-page-section">
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-label">最近订单数</div>
          <div class="stat-value">{{ orderPage.records.length }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">收藏目标数</div>
          <div class="stat-value">{{ favoritePage.records.length }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">可领取优惠券</div>
          <div class="stat-value">{{ couponCenterPage.records.length }}</div>
        </div>
      </div>

      <div class="user-overview-panels">
        <div class="user-overview-panel">
          <h3>最近订单</h3>
          <div v-if="orderPage.records.length" class="user-simple-list">
            <div v-for="item in orderPage.records.slice(0, 3)" :key="item.id" class="user-simple-list__item">
              <span>{{ item.productName || item.orderNo }}</span>
              <strong>{{ formatCurrency(item.payAmount) }}</strong>
            </div>
          </div>
          <el-empty v-else description="还没有订单，去商品页下第一单吧" :image-size="72" />
        </div>

        <div class="user-overview-panel">
          <h3>最近收藏</h3>
          <div v-if="favoritePage.records.length" class="user-simple-list">
            <div v-for="item in favoritePage.records.slice(0, 3)" :key="item.id" class="user-simple-list__item">
              <span>{{ item.targetName || `目标 ${item.targetId}` }}</span>
              <strong>{{ item.targetType === 1 ? '商户' : '商品' }}</strong>
            </div>
          </div>
          <el-empty v-else description="先去收藏感兴趣的商户或商品" :image-size="72" />
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'orders'" class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>我的订单</h2>
          <p>支持查看订单、取消订单和发起支付。</p>
        </div>
      </div>
      <div class="user-filter-grid order">
        <el-input v-model="orderFilters.orderNo" placeholder="按订单号筛选" clearable />
        <el-select v-model="orderFilters.status" clearable placeholder="按订单状态筛选">
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="已取消" :value="3" />
          <el-option label="已退款" :value="4" />
        </el-select>
        <div class="quick-action-row">
          <el-button type="primary" @click="handleOrderFilterSearch">筛选订单</el-button>
          <el-button @click="resetOrderFilters">重置</el-button>
        </div>
      </div>
      <el-table :data="orderPage.records">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="productName" label="商品" min-width="160" />
        <el-table-column prop="merchantName" label="商户" min-width="140" />
        <el-table-column label="实付金额" min-width="110">
          <template #default="{ row }">{{ formatCurrency(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusTagType(row.status)">{{ getOrderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <div class="quick-action-row">
              <el-button size="small" @click="handleViewOrderDetail(row)">详情</el-button>
              <el-button size="small" type="primary" plain @click="router.push(`/user/products/${row.productId}`)">商品详情</el-button>
              <el-button size="small" :disabled="row.status !== 0" @click="handlePayOrder(row)">去支付</el-button>
              <el-button size="small" type="danger" plain :disabled="row.status !== 0" @click="handleCancelOrder(row)">
                取消
              </el-button>
              <el-button
                size="small"
                type="success"
                plain
                :disabled="!canReviewOrder(row)"
                @click="handleOpenReview(row)"
              >
                {{ getReviewButtonText(row) }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="orderPage.current"
          :page-size="orderPage.size"
          :total="orderPage.total"
          @current-change="handleOrderPageChange"
        />
      </div>
    </section>

    <section v-if="activeTab === 'favorites'" class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>我的收藏</h2>
          <p>商品与商户收藏都走 `/user/favorite` 接口。</p>
        </div>
      </div>
      <div class="user-card-grid">
        <article v-for="item in favoritePage.records" :key="item.id" class="user-card">
          <img
            :src="item.targetCoverUrl || 'https://dummyimage.com/600x360/e2e8f0/64748b&text=Favorite'"
            class="user-card__image"
          />
          <div class="user-card__body">
            <h3>{{ item.targetName || `目标 ${item.targetId}` }}</h3>
            <p>收藏时间：{{ formatDateTime(item.createTime) }}</p>
            <div class="user-card__meta">
              <span>{{ getFavoriteTargetTypeLabel(item.targetType) }}收藏</span>
              <strong>ID {{ item.targetId }}</strong>
            </div>
            <el-button
              type="primary"
              plain
              @click="router.push(item.targetType === 1 ? `/user/merchants/${item.targetId}` : `/user/products/${item.targetId}`)"
            >
              查看详情
            </el-button>
            <el-button type="danger" plain @click="handleRemoveFavorite(item)">移除收藏</el-button>
          </div>
        </article>
      </div>
      <div class="table-footer">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="favoritePage.current"
          :page-size="favoritePage.size"
          :total="favoritePage.total"
          @current-change="handleFavoritePageChange"
        />
      </div>
    </section>

    <section v-if="activeTab === 'coupons'" class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>优惠券中心</h2>
          <p>上半部分是我的券，下半部分是可领取券。</p>
        </div>
      </div>

      <h3 class="user-subsection-title">我的优惠券</h3>
      <el-table :data="ownedCouponPage.records">
        <el-table-column prop="couponName" label="券名称" min-width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getUserCouponStatusTagType(row.status)">{{ getUserCouponStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="领取时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.receiveTime) }}</template>
        </el-table-column>
        <el-table-column label="过期时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
        </el-table-column>
      </el-table>
      <div class="table-footer user-table-footer">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="ownedCouponPage.current"
          :page-size="ownedCouponPage.size"
          :total="ownedCouponPage.total"
          @current-change="handleOwnedCouponPageChange"
        />
      </div>

      <h3 class="user-subsection-title">可领取优惠券</h3>
      <div class="user-card-grid">
        <article v-for="item in couponCenterPage.records" :key="item.id" class="user-card">
          <div class="user-card__body">
            <h3>{{ item.name }}</h3>
            <p>门槛 {{ formatCurrency(item.thresholdAmount) }}，库存 {{ item.stock || 0 }}</p>
            <div class="user-card__meta">
              <span>限领 {{ item.limitPerUser || 1 }} 张</span>
              <strong>优惠 {{ item.discountValue || 0 }}</strong>
            </div>
            <div class="quick-action-row">
              <el-tag :type="getCouponTemplateStatusTagType(item.status)">{{ getCouponTemplateStatusLabel(item.status) }}</el-tag>
              <el-tag type="warning">有效期至 {{ formatDateTime(item.endTime) }}</el-tag>
            </div>
            <el-button type="primary" plain @click="handleReceiveCoupon(item)">立即领取</el-button>
          </div>
        </article>
      </div>
      <div class="table-footer">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="couponCenterPage.current"
          :page-size="couponCenterPage.size"
          :total="couponCenterPage.total"
          @current-change="handleCouponCenterPageChange"
        />
      </div>
    </section>

    <section v-if="activeTab === 'reviews'" class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>我的评价</h2>
          <p>展示当前用户已提交的评价内容，方便演示评价闭环。</p>
        </div>
      </div>
      <div v-if="reviewPage.records.length" class="user-review-list">
        <article v-for="item in reviewPage.records" :key="item.id" class="user-review-card">
          <div class="user-review-card__head">
            <strong>{{ item.userNickname || profile?.nickname || '当前用户' }}</strong>
            <el-tag type="warning">评分 {{ item.score || '-' }}</el-tag>
          </div>
          <p>{{ item.content || '该评价未填写文字内容。' }}</p>
          <div class="user-card__meta">
            <span>订单 {{ item.orderId || '-' }}</span>
            <span>商品 {{ item.productId || '-' }}</span>
            <strong>{{ formatDateTime(item.createTime) }}</strong>
          </div>
        </article>
      </div>
      <el-empty v-else description="还没有评价记录，去订单里提交一条吧" />
      <div class="table-footer">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="reviewPage.current"
          :page-size="reviewPage.size"
          :total="reviewPage.total"
          @current-change="handleReviewPageChange"
        />
      </div>
    </section>

    <el-drawer v-model="orderDetailVisible" title="订单详情" size="460px">
      <div v-loading="orderDetailLoading">
        <el-descriptions v-if="currentOrderDetail" :column="1" border>
          <el-descriptions-item label="订单号">{{ currentOrderDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="商品">{{ currentOrderDetail.productName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商户">{{ currentOrderDetail.merchantName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getOrderStatusTagType(currentOrderDetail.status)">{{ getOrderStatusLabel(currentOrderDetail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总金额">{{ formatCurrency(currentOrderDetail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="优惠金额">{{ formatCurrency(currentOrderDetail.discountAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">{{ formatCurrency(currentOrderDetail.payAmount) }}</el-descriptions-item>
          <el-descriptions-item label="订单备注">{{ currentOrderDetail.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentOrderDetail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ formatDateTime(currentOrderDetail.payTime) }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{ formatDateTime(currentOrderDetail.cancelTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatDateTime(currentOrderDetail.finishTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <el-dialog v-model="reviewVisible" title="提交评价" width="520px">
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-position="top">
        <el-form-item label="评分" prop="score">
          <el-rate v-model="reviewForm.score" />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="输入你的体验评价，便于前台商品/商户页展示真实评论"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="quick-action-row">
          <el-button @click="reviewVisible = false">取消</el-button>
          <el-button type="primary" :loading="reviewSubmitting" @click="handleSubmitReview">提交评价</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
