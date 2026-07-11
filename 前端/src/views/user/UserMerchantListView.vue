<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchUserHomeAggregate, fetchUserMerchantPage } from '../../api/user'
import type { MerchantDetail, PageResult } from '../../types'
import type { UserMerchantCategory, UserMerchantQuery } from '../../types/user'
import { createEmptyPage, formatCurrency } from '../../utils'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const categories = ref<UserMerchantCategory[]>([])
const pageData = ref<PageResult<MerchantDetail>>(createEmptyPage<MerchantDetail>())

const filters = reactive<UserMerchantQuery>({
  pageNo: 1,
  pageSize: 8,
  keyword: '',
  categoryId: undefined,
  maxAvgPrice: undefined,
  minScore: undefined,
  status: 1,
})

async function loadCategories() {
  const aggregate = await fetchUserHomeAggregate()
  categories.value = aggregate.categories
}

async function loadData() {
  loading.value = true
  try {
    pageData.value = await fetchUserMerchantPage(filters)
  } finally {
    loading.value = false
  }
}

function syncFromRoute() {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.categoryId =
    typeof route.query.categoryId === 'string' && route.query.categoryId ? Number(route.query.categoryId) : undefined
  filters.minScore =
    typeof route.query.minScore === 'string' && route.query.minScore ? Number(route.query.minScore) : undefined
  filters.maxAvgPrice =
    typeof route.query.maxAvgPrice === 'string' && route.query.maxAvgPrice ? Number(route.query.maxAvgPrice) : undefined
  filters.pageNo = 1
  loadData()
}

function handleSearch() {
  router.replace({
    path: '/user/merchants',
    query: {
      ...(filters.keyword ? { keyword: filters.keyword } : {}),
      ...(filters.categoryId ? { categoryId: String(filters.categoryId) } : {}),
      ...(filters.minScore !== undefined ? { minScore: String(filters.minScore) } : {}),
      ...(filters.maxAvgPrice !== undefined ? { maxAvgPrice: String(filters.maxAvgPrice) } : {}),
    },
  })
}

function resetFilters() {
  filters.keyword = ''
  filters.categoryId = undefined
  filters.maxAvgPrice = undefined
  filters.minScore = undefined
  filters.pageNo = 1
  router.replace('/user/merchants')
}

function handleCurrentChange(page: number) {
  filters.pageNo = page
  loadData()
}

watch(
  () => [route.query.keyword, route.query.categoryId, route.query.minScore, route.query.maxAvgPrice],
  () => {
    syncFromRoute()
  },
  { immediate: true },
)

loadCategories()
</script>

<template>
  <div class="user-page">
    <section class="user-page-section compact">
      <div class="user-section-head">
        <div>
          <h2>商户列表</h2>
          <p>支持分类与关键词筛选，数据来源于 `/user/merchant/page`。</p>
        </div>
      </div>

      <el-form class="user-filter-grid" @submit.prevent>
        <el-input v-model="filters.keyword" placeholder="搜索商户、菜品或地址关键词" clearable />
        <el-select v-model="filters.categoryId" clearable placeholder="选择商户分类">
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-input-number v-model="filters.minScore" :min="0" :max="5" :step="0.1" :precision="1" placeholder="最低评分" />
        <el-input-number v-model="filters.maxAvgPrice" :min="0" :precision="2" placeholder="最高人均" />
        <div class="quick-action-row">
          <el-button type="primary" @click="handleSearch">筛选</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </el-form>

      <div class="user-chip-list">
        <el-tag
          v-for="item in categories"
          :key="item.id"
          size="large"
          :type="filters.categoryId === item.id ? 'primary' : 'info'"
          class="clickable-tag"
          @click="router.replace({ path: '/user/merchants', query: { categoryId: String(item.id) } })"
        >
          {{ item.name }}
        </el-tag>
      </div>
    </section>

    <section class="user-card-grid merchant" v-loading="loading">
      <article v-for="item in pageData.records" :key="item.id" class="user-card">
        <img
          :src="item.coverUrl || 'https://dummyimage.com/600x360/dbeafe/1d4ed8&text=Merchant'"
          class="user-card__image"
        />
        <div class="user-card__body">
          <h3>{{ item.name }}</h3>
          <p>{{ item.description || item.address || '暂无商户介绍' }}</p>
          <div class="user-card__meta">
            <span>{{ item.categoryName || '精选商户' }}</span>
            <strong>{{ formatCurrency(item.avgPrice) }}</strong>
          </div>
          <div class="user-card__actions">
            <el-tag size="small" type="warning">评分 {{ item.score || '-' }}</el-tag>
            <el-button plain @click="router.push(`/user/merchants/${item.id}`)">查看详情</el-button>
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
