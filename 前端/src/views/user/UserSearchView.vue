<script setup lang="ts">
import { Delete, Search } from '@element-plus/icons-vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MerchantDetail, PageResult, ProductDetail } from '../../types'
import { fetchHotKeywords, fetchUserMerchantPage, fetchUserProductPage } from '../../api/user'
import { createEmptyPage, formatCurrency } from '../../utils'
import { highlightKeywordText } from '../../utils/user-display'

const route = useRoute()
const router = useRouter()
const HISTORY_KEY = 'huixianglife:user-search-history'
const internalRouteChange = ref(false)
const productLoading = ref(false)
const merchantLoading = ref(false)
const hotKeywords = ref<string[]>([])
const historyKeywords = ref<string[]>([])
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const activeTab = ref<'all' | 'products' | 'merchants'>(normalizeTab(route.query.tab))
const productPage = ref<PageResult<ProductDetail>>(createEmptyPage<ProductDetail>())
const merchantPage = ref<PageResult<MerchantDetail>>(createEmptyPage<MerchantDetail>())

const hasKeyword = computed(() => Boolean(keyword.value.trim()))
const showProducts = computed(() => activeTab.value === 'all' || activeTab.value === 'products')
const showMerchants = computed(() => activeTab.value === 'all' || activeTab.value === 'merchants')

function normalizeTab(value: unknown): 'all' | 'products' | 'merchants' {
  if (value === 'products' || value === 'merchants') {
    return value
  }
  return 'all'
}

function loadSearchHistory() {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    historyKeywords.value = raw ? (JSON.parse(raw) as string[]) : []
  } catch {
    historyKeywords.value = []
  }
}

function saveSearchHistory(nextKeyword: string) {
  const actualKeyword = nextKeyword.trim()
  if (!actualKeyword) {
    return
  }
  const nextHistory = [actualKeyword, ...historyKeywords.value.filter((item) => item !== actualKeyword)].slice(0, 8)
  historyKeywords.value = nextHistory
  localStorage.setItem(HISTORY_KEY, JSON.stringify(nextHistory))
}

function clearSearchHistory() {
  historyKeywords.value = []
  localStorage.removeItem(HISTORY_KEY)
}

function syncRoute(nextKeyword: string, nextTab = activeTab.value) {
  const query: Record<string, string> = {}
  if (nextKeyword) {
    query.keyword = nextKeyword
  }
  if (nextTab !== 'all') {
    query.tab = nextTab
  }
  internalRouteChange.value = true
  void router.replace({ path: '/user/search', query })
}

async function loadProductPage(pageNo = 1, targetKeyword = keyword.value.trim()) {
  if (!targetKeyword) {
    productPage.value = createEmptyPage<ProductDetail>()
    return
  }
  productLoading.value = true
  try {
    productPage.value = await fetchUserProductPage({
      pageNo,
      pageSize: 6,
      keyword: targetKeyword,
      status: 1,
    })
  } finally {
    productLoading.value = false
  }
}

async function loadMerchantPage(pageNo = 1, targetKeyword = keyword.value.trim()) {
  if (!targetKeyword) {
    merchantPage.value = createEmptyPage<MerchantDetail>()
    return
  }
  merchantLoading.value = true
  try {
    merchantPage.value = await fetchUserMerchantPage({
      pageNo,
      pageSize: 6,
      keyword: targetKeyword,
      status: 1,
    })
  } finally {
    merchantLoading.value = false
  }
}

async function loadHotKeywords() {
  hotKeywords.value = await fetchHotKeywords()
}

async function handleSearch(nextKeyword?: string, nextTab = activeTab.value) {
  const actualKeyword = (nextKeyword ?? keyword.value).trim()
  keyword.value = actualKeyword
  activeTab.value = nextTab

  if (!actualKeyword) {
    productPage.value = createEmptyPage<ProductDetail>()
    merchantPage.value = createEmptyPage<MerchantDetail>()
    syncRoute('', nextTab)
    return
  }

  saveSearchHistory(actualKeyword)
  syncRoute(actualKeyword, nextTab)
  await Promise.all([loadProductPage(1, actualKeyword), loadMerchantPage(1, actualKeyword)])
}

function handleTabChange(tab: string | number | boolean) {
  const nextTab = normalizeTab(tab)
  void handleSearch(keyword.value, nextTab)
}

function handleProductPageChange(page: number) {
  void loadProductPage(page)
}

function handleMerchantPageChange(page: number) {
  void loadMerchantPage(page)
}

async function applyRouteQuery() {
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  activeTab.value = normalizeTab(route.query.tab)

  if (!keyword.value.trim()) {
    productPage.value = createEmptyPage<ProductDetail>()
    merchantPage.value = createEmptyPage<MerchantDetail>()
    return
  }

  await Promise.all([loadProductPage(1, keyword.value), loadMerchantPage(1, keyword.value)])
}

function highlightText(value?: string) {
  return highlightKeywordText(value, keyword.value)
}

onMounted(async () => {
  loadSearchHistory()
  await loadHotKeywords()
  await applyRouteQuery()
})

watch(
  () => [route.query.keyword, route.query.tab],
  async () => {
    if (internalRouteChange.value) {
      internalRouteChange.value = false
      return
    }
    await applyRouteQuery()
  },
)
</script>

<template>
  <div class="user-page">
    <section class="user-page-section">
      <div class="user-section-head">
        <div>
          <h2>综合搜索</h2>
          <p>一个关键词同时联动商品和商户结果，适合演示 ES 搜索链路。</p>
        </div>
      </div>

      <div class="user-search-hero">
        <el-input v-model="keyword" :prefix-icon="Search" placeholder="搜索商品、商户、菜品关键词" clearable @keyup.enter="handleSearch()">
          <template #append>
            <el-button @click="handleSearch()">搜索</el-button>
          </template>
        </el-input>
        <div class="user-hot-keywords">
          <span class="muted-text">热搜：</span>
          <el-tag v-for="item in hotKeywords" :key="item" round effect="plain" class="clickable-tag" @click="handleSearch(item)">
            {{ item }}
          </el-tag>
        </div>
        <div v-if="historyKeywords.length" class="user-hot-keywords">
          <span class="muted-text">历史：</span>
          <el-tag v-for="item in historyKeywords" :key="item" round class="clickable-tag" @click="handleSearch(item)">
            {{ item }}
          </el-tag>
          <el-button text :icon="Delete" @click="clearSearchHistory">清空</el-button>
        </div>
        <el-radio-group :model-value="activeTab" size="large" @change="handleTabChange">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="products">商品</el-radio-button>
          <el-radio-button value="merchants">商户</el-radio-button>
        </el-radio-group>
      </div>
    </section>

    <section v-if="showProducts" class="user-page-section" v-loading="productLoading">
      <div class="user-section-head">
        <div>
          <h2>商品结果</h2>
          <p>点击后可直接进入下单和秒杀流程。</p>
        </div>
        <RouterLink v-if="keyword" :to="{ path: '/user/products', query: { keyword } }" class="muted-text">查看完整商品列表</RouterLink>
      </div>
      <div v-if="productPage.records.length" class="user-card-grid">
        <article v-for="item in productPage.records" :key="item.id" class="user-card">
          <img :src="item.coverUrl || 'https://dummyimage.com/600x360/e2e8f0/64748b&text=Product'" class="user-card__image" />
          <div class="user-card__body">
            <h3 v-html="highlightText(item.name)"></h3>
            <p v-html="highlightText(item.subTitle || item.content || '暂无商品描述')"></p>
            <div class="user-card__meta">
              <span>{{ item.merchantName || '-' }}</span>
              <strong>{{ formatCurrency(item.salePrice) }}</strong>
            </div>
            <el-button type="primary" plain @click="router.push(`/user/products/${item.id}`)">查看详情</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else :description="hasKeyword ? '当前关键词没有匹配到商品结果' : '输入关键词后查看商品搜索结果'" />
      <div class="table-footer">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="productPage.current"
          :page-size="productPage.size"
          :total="productPage.total"
          @current-change="handleProductPageChange"
        />
      </div>
    </section>

    <section v-if="showMerchants" class="user-page-section" v-loading="merchantLoading">
      <div class="user-section-head">
        <div>
          <h2>商户结果</h2>
          <p>点击后可直接进入商户详情与店内商品列表。</p>
        </div>
        <RouterLink v-if="keyword" :to="{ path: '/user/merchants', query: { keyword } }" class="muted-text">查看完整商户列表</RouterLink>
      </div>
      <div v-if="merchantPage.records.length" class="user-card-grid merchant">
        <article v-for="item in merchantPage.records" :key="item.id" class="user-card">
          <img :src="item.coverUrl || 'https://dummyimage.com/600x360/dbeafe/1d4ed8&text=Merchant'" class="user-card__image" />
          <div class="user-card__body">
            <h3 v-html="highlightText(item.name)"></h3>
            <p v-html="highlightText(item.description || item.address || '暂无商户简介')"></p>
            <div class="user-card__meta">
              <span>{{ item.categoryName || '-' }}</span>
              <strong>{{ formatCurrency(item.avgPrice) }}</strong>
            </div>
            <el-button plain @click="router.push(`/user/merchants/${item.id}`)">查看商户</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else :description="hasKeyword ? '当前关键词没有匹配到商户结果' : '输入关键词后查看商户搜索结果'" />
      <div class="table-footer">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="merchantPage.current"
          :page-size="merchantPage.size"
          :total="merchantPage.total"
          @current-change="handleMerchantPageChange"
        />
      </div>
    </section>
  </div>
</template>
