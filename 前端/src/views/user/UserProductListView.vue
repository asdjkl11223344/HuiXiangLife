<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchHotKeywords, fetchUserProductPage } from '../../api/user'
import type { ProductDetail, PageResult } from '../../types'
import type { UserProductQuery } from '../../types/user'
import { createEmptyPage, formatCurrency } from '../../utils'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const hotKeywords = ref<string[]>([])
const pageData = ref<PageResult<ProductDetail>>(createEmptyPage<ProductDetail>())

const filters = reactive<UserProductQuery>({
  pageNo: 1,
  pageSize: 8,
  keyword: '',
  merchantId: undefined,
  minSalePrice: undefined,
  maxSalePrice: undefined,
  status: 1,
})

async function loadHotKeywords() {
  hotKeywords.value = await fetchHotKeywords()
}

async function loadData() {
  loading.value = true
  try {
    pageData.value = await fetchUserProductPage(filters)
  } finally {
    loading.value = false
  }
}

function syncKeywordFromRoute() {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.merchantId =
    typeof route.query.merchantId === 'string' && route.query.merchantId ? Number(route.query.merchantId) : undefined
  filters.minSalePrice =
    typeof route.query.minSalePrice === 'string' && route.query.minSalePrice
      ? Number(route.query.minSalePrice)
      : undefined
  filters.maxSalePrice =
    typeof route.query.maxSalePrice === 'string' && route.query.maxSalePrice
      ? Number(route.query.maxSalePrice)
      : undefined
  filters.pageNo = 1
  loadData()
}

function handleSearch() {
  router.replace({
    path: '/user/products',
    query: {
      ...(filters.keyword ? { keyword: filters.keyword } : {}),
      ...(filters.merchantId ? { merchantId: String(filters.merchantId) } : {}),
      ...(filters.minSalePrice !== undefined ? { minSalePrice: String(filters.minSalePrice) } : {}),
      ...(filters.maxSalePrice !== undefined ? { maxSalePrice: String(filters.maxSalePrice) } : {}),
    },
  })
}

function resetFilters() {
  filters.keyword = ''
  filters.merchantId = undefined
  filters.minSalePrice = undefined
  filters.maxSalePrice = undefined
  filters.pageNo = 1
  router.replace('/user/products')
}

function handleCurrentChange(page: number) {
  filters.pageNo = page
  loadData()
}

watch(
  () => [route.query.keyword, route.query.merchantId, route.query.minSalePrice, route.query.maxSalePrice],
  () => {
    syncKeywordFromRoute()
  },
  { immediate: true },
)

loadHotKeywords()
</script>

<template>
  <div class="user-page">
    <section class="user-page-section compact">
      <div class="user-section-head">
        <div>
          <h2>商品列表</h2>
          <p>支持关键词搜索、价格筛选和详情跳转，数据来源于 `/user/product/page`。</p>
        </div>
      </div>

      <el-form class="user-filter-grid" @submit.prevent>
        <el-input v-model="filters.keyword" :prefix-icon="Search" placeholder="搜索商品或商户关键词" clearable />
        <el-input-number v-model="filters.minSalePrice" :min="0" :precision="2" placeholder="最低价" />
        <el-input-number v-model="filters.maxSalePrice" :min="0" :precision="2" placeholder="最高价" />
        <div class="quick-action-row">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </el-form>

      <div v-if="hotKeywords.length" class="user-hot-keywords">
        <span class="muted-text">热搜：</span>
        <el-tag v-for="keyword in hotKeywords" :key="keyword" round effect="plain" @click="router.replace({ path: '/user/products', query: { keyword } })">
          {{ keyword }}
        </el-tag>
      </div>
    </section>

    <section class="user-card-grid" v-loading="loading">
      <article v-for="item in pageData.records" :key="item.id" class="user-card">
        <img :src="item.coverUrl || 'https://dummyimage.com/600x360/e2e8f0/64748b&text=Product'" class="user-card__image" />
        <div class="user-card__body">
          <h3>{{ item.name }}</h3>
          <p>{{ item.subTitle || item.content || '暂无商品描述' }}</p>
          <div class="user-card__meta">
            <span>{{ item.merchantName || '优选商户' }}</span>
            <strong>{{ formatCurrency(item.salePrice) }}</strong>
          </div>
          <div class="user-card__actions">
            <el-tag size="small" type="success">销量 {{ item.soldCount || 0 }}</el-tag>
            <el-button type="primary" plain @click="router.push(`/user/products/${item.id}`)">查看详情</el-button>
          </div>
        </div>
      </article>
    </section>

    <section class="user-page-section compact">
      <div class="table-footer">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="pageData.current"
          :page-size="pageData.size"
          :total="pageData.total"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>
  </div>
</template>
