<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { fetchReviewPage, updateReviewStatus } from '../api/admin'
import type { PageResult, ReviewItem, ReviewQuery } from '../types'
import { createEmptyPage, formatDateTime } from '../utils'

const loading = ref(false)
const query = reactive<ReviewQuery>({
  pageNo: 1,
  pageSize: 10,
  merchantId: undefined,
  productId: undefined,
  userId: undefined,
  status: undefined,
})
const pageData = ref<PageResult<ReviewItem>>(createEmptyPage<ReviewItem>())

async function loadReviews() {
  loading.value = true
  try {
    pageData.value = await fetchReviewPage(query)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  query.pageNo = 1
  await loadReviews()
}

async function handleReset() {
  Object.assign(query, {
    pageNo: 1,
    pageSize: 10,
    merchantId: undefined,
    productId: undefined,
    userId: undefined,
    status: undefined,
  })
  await loadReviews()
}

async function handleStatus(row: ReviewItem, status: number) {
  await updateReviewStatus(row.id, status)
  ElMessage.success(status === 1 ? '评价已设为可见' : '评价已隐藏')
  await loadReviews()
}

function handleCurrentChange(pageNo: number) {
  query.pageNo = pageNo
  void loadReviews()
}

function handleSizeChange(pageSize: number) {
  query.pageSize = pageSize
  query.pageNo = 1
  void loadReviews()
}

onMounted(() => {
  void loadReviews()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <div>
          <h2 class="page-title">评价管理</h2>
          <p class="page-subtitle">支持评价查询与可见状态审核。</p>
        </div>
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
        <el-form-item label="用户 ID">
          <el-input-number v-model="query.userId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态">
            <el-option label="隐藏" :value="0" />
            <el-option label="可见" :value="1" />
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
        <el-table-column prop="id" label="评价 ID" min-width="100" />
        <el-table-column prop="userNickname" label="用户" min-width="120" />
        <el-table-column prop="merchantId" label="商户 ID" min-width="100" />
        <el-table-column prop="productId" label="商品 ID" min-width="100" />
        <el-table-column prop="score" label="评分" min-width="80" />
        <el-table-column prop="content" label="评价内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '可见' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" type="success" link @click="handleStatus(row, 1)">设为可见</el-button>
            <el-button v-else type="warning" link @click="handleStatus(row, 0)">隐藏评价</el-button>
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
  </div>
</template>
