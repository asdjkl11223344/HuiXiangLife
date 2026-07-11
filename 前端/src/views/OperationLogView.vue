<script setup lang="ts">
import { Search, View } from '@element-plus/icons-vue'
import { onMounted, reactive, ref } from 'vue'
import { fetchOperationLogPage } from '../api/admin'
import type { OperationLogItem, OperationLogQuery, PageResult } from '../types'
import { createEmptyPage, formatDateTime } from '../utils'

const loading = ref(false)
const detailVisible = ref(false)
const currentLog = ref<OperationLogItem>()

const query = reactive<OperationLogQuery>({
  pageNo: 1,
  pageSize: 10,
  operatorId: undefined,
  module: '',
  action: '',
  bizId: undefined,
})

const pageData = ref<PageResult<OperationLogItem>>(createEmptyPage<OperationLogItem>())

async function loadLogs() {
  loading.value = true
  try {
    pageData.value = await fetchOperationLogPage(query)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  query.pageNo = 1
  await loadLogs()
}

async function handleReset() {
  query.pageNo = 1
  query.pageSize = 10
  query.operatorId = undefined
  query.module = ''
  query.action = ''
  query.bizId = undefined
  await loadLogs()
}

async function applyQuickFilter(type: 'product' | 'merchant' | 'log' | 'search') {
  query.pageNo = 1
  query.pageSize = 10
  query.operatorId = undefined
  query.bizId = undefined

  if (type === 'product') {
    query.module = '商品管理'
    query.action = ''
  } else if (type === 'merchant') {
    query.module = '商户管理'
    query.action = ''
  } else if (type === 'log') {
    query.module = '操作日志'
    query.action = ''
  } else {
    query.module = ''
    query.action = '同步搜索索引'
  }

  await loadLogs()
}

function openDetail(row: OperationLogItem) {
  currentLog.value = row
  detailVisible.value = true
}

function handleCurrentChange(pageNo: number) {
  query.pageNo = pageNo
  void loadLogs()
}

function handleSizeChange(pageSize: number) {
  query.pageSize = pageSize
  query.pageNo = 1
  void loadLogs()
}

onMounted(() => {
  void loadLogs()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <div>
          <h2 class="page-title">操作日志</h2>
          <p class="page-subtitle">
            对接 `GET /admin/operation-log/page`，支持按操作人、模块、动作、业务 ID 分页过滤。
          </p>
        </div>
      </div>
    </el-card>

    <el-card class="page-card" shadow="never">
      <div class="quick-action-row">
        <el-button plain @click="applyQuickFilter('product')">商品管理</el-button>
        <el-button plain @click="applyQuickFilter('merchant')">商户管理</el-button>
        <el-button plain @click="applyQuickFilter('search')">同步索引</el-button>
        <el-button plain @click="applyQuickFilter('log')">操作日志模块</el-button>
      </div>
    </el-card>

    <el-card class="page-card" shadow="never">
      <el-form class="filter-form" label-position="top">
        <el-form-item label="操作人 ID">
          <el-input-number v-model="query.operatorId" :min="1" controls-position="right" />
        </el-form-item>

        <el-form-item label="模块">
          <el-input v-model="query.module" clearable placeholder="例如：商户管理" />
        </el-form-item>

        <el-form-item label="动作">
          <el-input v-model="query.action" clearable placeholder="例如：同步搜索索引" />
        </el-form-item>

        <el-form-item label="业务 ID">
          <el-input-number v-model="query.bizId" :min="1" controls-position="right" />
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
        <el-table-column prop="id" label="日志 ID" min-width="160" />
        <el-table-column prop="operatorName" label="操作人" min-width="130">
          <template #default="{ row }">
            {{ row.operatorName || `ID: ${row.operatorId || '-'}` }}
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" min-width="130" />
        <el-table-column prop="action" label="动作" min-width="150" />
        <el-table-column prop="bizId" label="业务 ID" min-width="110" />
        <el-table-column prop="detail" label="详情" min-width="360" show-overflow-tooltip />
        <el-table-column prop="ip" label="来源 IP" min-width="130" />
        <el-table-column prop="createTime" label="操作时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="日志详情" width="640px">
      <el-descriptions v-if="currentLog" :column="1" border>
        <el-descriptions-item label="日志 ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ currentLog.operatorName || `ID: ${currentLog.operatorId || '-'}` }}
        </el-descriptions-item>
        <el-descriptions-item label="模块">{{ currentLog.module || '-' }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ currentLog.action || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务 ID">{{ currentLog.bizId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源 IP">{{ currentLog.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatDateTime(currentLog.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="详情">{{ currentLog.detail || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>
