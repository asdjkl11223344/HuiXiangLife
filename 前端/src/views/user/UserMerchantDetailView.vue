<script setup lang="ts">
import { ArrowLeft, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MerchantDetail, PageResult, ProductDetail } from '../../types'
import {
  createUserFavorite,
  deleteUserFavorite,
  fetchUserFavoritePage,
  fetchUserMerchantDetail,
  fetchUserProductPage,
  fetchUserReviewPage,
} from '../../api/user'
import { useUserAuthStore } from '../../stores/user-auth'
import type { UserFavoriteItem, UserReviewItem } from '../../types/user'
import { createEmptyPage, formatCurrency, formatDateTime } from '../../utils'

const route = useRoute()
const router = useRouter()
const userAuthStore = useUserAuthStore()
const loading = ref(false)
const merchant = ref<MerchantDetail | null>(null)
const products = ref<PageResult<ProductDetail>>(createEmptyPage<ProductDetail>())
const reviews = ref<PageResult<UserReviewItem>>(createEmptyPage<UserReviewItem>())
const favoriteList = ref<UserFavoriteItem[]>([])

const merchantId = computed(() => Number(route.params.id))
const isFavorited = computed(() => favoriteList.value.some((item) => item.targetId === merchantId.value))

function ensureLogin() {
  if (userAuthStore.isLoggedIn) {
    return true
  }
  router.push({
    path: '/user/login',
    query: { redirect: route.fullPath },
  })
  return false
}

async function loadFavoriteState() {
  if (!userAuthStore.isLoggedIn) {
    favoriteList.value = []
    return
  }

  const result = await fetchUserFavoritePage({
    pageNo: 1,
    pageSize: 100,
    targetType: 1,
  })
  favoriteList.value = result.records
}

async function loadData() {
  loading.value = true
  try {
    const [detail, productPage, reviewPage] = await Promise.all([
      fetchUserMerchantDetail(merchantId.value),
      fetchUserProductPage({
        pageNo: 1,
        pageSize: 6,
        merchantId: merchantId.value,
        status: 1,
      }),
      fetchUserReviewPage({
        pageNo: 1,
        pageSize: 6,
        merchantId: merchantId.value,
        status: 1,
      }),
    ])
    merchant.value = detail
    products.value = productPage
    reviews.value = reviewPage
    await loadFavoriteState()
  } finally {
    loading.value = false
  }
}

async function handleToggleFavorite() {
  if (!merchant.value || !ensureLogin()) {
    return
  }

  if (isFavorited.value) {
    await deleteUserFavorite(merchant.value.id, 1)
    ElMessage.success('已取消收藏商户')
  } else {
    await createUserFavorite({
      targetId: merchant.value.id,
      targetType: 1,
    })
    ElMessage.success('商户已加入收藏')
  }
  await loadFavoriteState()
}

watch(
  () => route.params.id,
  () => {
    loadData()
  },
  { immediate: true },
)
</script>

<template>
  <div class="user-page" v-loading="loading">
    <section v-if="merchant" class="user-detail-hero">
      <el-button text :icon="ArrowLeft" @click="router.back()">返回上一页</el-button>
      <div class="user-detail-hero__grid">
        <img
          :src="merchant.coverUrl || 'https://dummyimage.com/800x520/dbeafe/1d4ed8&text=Merchant'"
          class="user-detail-hero__image"
        />
        <div class="user-detail-hero__info">
          <div class="user-section-badge">商户详情</div>
          <h1>{{ merchant.name }}</h1>
          <p>{{ merchant.description || merchant.address || '暂无商户介绍' }}</p>
          <div class="user-detail-hero__price">
            <strong>{{ formatCurrency(merchant.avgPrice) }}</strong>
            <span>评分 {{ merchant.score || '-' }}</span>
          </div>
          <div class="user-detail-hero__meta">
            <span>分类：{{ merchant.categoryName || '-' }}</span>
            <span>地址：{{ merchant.address || '-' }}</span>
            <span>电话：{{ merchant.phone || '-' }}</span>
          </div>
          <div class="user-detail-hero__actions">
            <el-button type="primary" plain @click="router.push({ path: '/user/products', query: { merchantId: merchant.id } })">
              查看店内商品
            </el-button>
            <el-button :icon="Star" @click="handleToggleFavorite">
              {{ isFavorited ? '取消收藏' : '收藏商户' }}
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>商户商品</h2>
          <p>从商户详情直接跳到可购买商品。</p>
        </div>
      </div>
      <div class="user-card-grid">
        <article v-for="item in products.records" :key="item.id" class="user-card">
          <img :src="item.coverUrl || 'https://dummyimage.com/600x360/e2e8f0/64748b&text=Product'" class="user-card__image" />
          <div class="user-card__body">
            <h3>{{ item.name }}</h3>
            <p>{{ item.subTitle || item.content || '暂无商品简介' }}</p>
            <div class="user-card__meta">
              <span>销量 {{ item.soldCount || 0 }}</span>
              <strong>{{ formatCurrency(item.salePrice) }}</strong>
            </div>
            <el-button type="primary" plain @click="router.push(`/user/products/${item.id}`)">立即查看</el-button>
          </div>
        </article>
      </div>
    </section>

    <section class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>用户评价</h2>
          <p>来自 `/user/review/page` 的商户评价。</p>
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
      <el-empty v-else description="当前商户还没有评价" />
    </section>
  </div>
</template>
