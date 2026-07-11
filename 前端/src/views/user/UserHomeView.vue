<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchUserHomeAggregate } from '../../api/user'
import type { UserHomeAggregate } from '../../types/user'
import { formatCurrency } from '../../utils'

const router = useRouter()
const loading = ref(false)
const homeData = ref<UserHomeAggregate>({
  categories: [],
  recommendProducts: [],
  hotKeywords: [],
  merchants: [],
})

async function loadData() {
  loading.value = true
  try {
    homeData.value = await fetchUserHomeAggregate()
  } finally {
    loading.value = false
  }
}

function goToKeyword(keyword: string) {
  router.push({
    path: '/user/search',
    query: { keyword },
  })
}

onMounted(loadData)
</script>

<template>
  <div class="user-page">
    <section class="user-hero-card" v-loading="loading">
      <div class="user-hero-card__content">
        <div class="user-section-badge">用户端首页</div>
        <h1>搜索、商户、下单、秒杀、领券全部在一套页面里打通</h1>
        <p>
          当前首页聚合真实对接 `/user/home`，集中展示热门搜索词、推荐商品、活跃商户与商户分类，适合演示完整用户消费链路。
        </p>
        <div class="quick-action-row">
          <el-button type="primary" @click="router.push('/user/search')">去综合搜索</el-button>
          <el-button plain @click="router.push('/user/profile')">查看个人中心</el-button>
        </div>
        <div class="user-hot-keywords">
          <el-tag
            v-for="keyword in homeData.hotKeywords"
            :key="keyword"
            round
            effect="plain"
            @click="goToKeyword(keyword)"
          >
            {{ keyword }}
          </el-tag>
        </div>
      </div>
      <div class="user-hero-card__side">
        <div class="user-overview-stat">
          <span>推荐商品</span>
          <strong>{{ homeData.recommendProducts.length }}</strong>
        </div>
        <div class="user-overview-stat">
          <span>商户分类</span>
          <strong>{{ homeData.categories.length }}</strong>
        </div>
        <div class="user-overview-stat">
          <span>推荐商户</span>
          <strong>{{ homeData.merchants.length }}</strong>
        </div>
      </div>
    </section>

    <section class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>热门分类</h2>
          <p>来自首页聚合数据，可直接切到商户列表筛选。</p>
        </div>
      </div>
      <div class="user-chip-list">
        <el-tag
          v-for="category in homeData.categories"
          :key="category.id"
          size="large"
          class="clickable-tag"
          @click="router.push({ path: '/user/merchants', query: { categoryId: category.id } })"
        >
          {{ category.name }}
        </el-tag>
      </div>
    </section>

    <section class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>推荐商品</h2>
          <p>支持跳详情、下单与秒杀演示。</p>
        </div>
        <RouterLink to="/user/products" class="muted-text">查看全部商品</RouterLink>
      </div>
      <div class="user-card-grid">
        <article v-for="item in homeData.recommendProducts" :key="item.id" class="user-card">
          <img :src="item.coverUrl || 'https://dummyimage.com/600x360/e2e8f0/64748b&text=Product'" class="user-card__image" />
          <div class="user-card__body">
            <h3>{{ item.name }}</h3>
            <p>{{ item.subTitle || item.content || '高评分商品，适合首页推荐展示。' }}</p>
            <div class="user-card__meta">
              <span>{{ item.merchantName || '优选商户' }}</span>
              <strong>{{ formatCurrency(item.salePrice) }}</strong>
            </div>
            <el-button type="primary" plain @click="router.push(`/user/products/${item.id}`)">查看详情</el-button>
          </div>
        </article>
      </div>
    </section>

    <section class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>推荐商户</h2>
          <p>首页直接展示商户卡片，支持跳转商户详情与商品列表。</p>
        </div>
        <RouterLink to="/user/merchants" class="muted-text">查看全部商户</RouterLink>
      </div>
      <div class="user-card-grid merchant">
        <article v-for="item in homeData.merchants" :key="item.id" class="user-card">
          <img
            :src="item.coverUrl || 'https://dummyimage.com/600x360/dbeafe/1d4ed8&text=Merchant'"
            class="user-card__image"
          />
          <div class="user-card__body">
            <h3>{{ item.name }}</h3>
            <p>{{ item.description || item.address || '已接入真实商户列表与详情接口。' }}</p>
            <div class="user-card__meta">
              <span>{{ item.categoryName || '精选商户' }}</span>
              <strong>{{ formatCurrency(item.avgPrice) }}</strong>
            </div>
            <el-button plain @click="router.push(`/user/merchants/${item.id}`)">查看商户</el-button>
          </div>
        </article>
      </div>
    </section>

    <section v-if="loading" class="user-loading-inline">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>首页数据加载中...</span>
    </section>
  </div>
</template>
