<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { fetchUserPage, updateUserStatus } from '../api/admin'
import type { PageResult, UserInfo, UserQuery } from '../types'
import { createEmptyPage, formatDateTime } from '../utils'

const loading = ref(false)
const query = reactive<UserQuery>({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  role: '',
  status: undefined,
})
const pageData = ref<PageResult<UserInfo>>(createEmptyPage<UserInfo>())

async function loadUsers() {
  loading.value = true
  try {
    pageData.value = await fetchUserPage(query)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  query.pageNo = 1
  await loadUsers()
}

async function handleReset() {
  Object.assign(query, {
    pageNo: 1,
    pageSize: 10,
    keyword: '',
    role: '',
    status: undefined,
  })
  await loadUsers()
}

async function handleToggleStatus(row: UserInfo, status: number) {
  await updateUserStatus(row.id, { status })
  ElMessage.success(status === 1 ? '账号已启用' : '账号已禁用')
  await loadUsers()
}

function handleCurrentChange(pageNo: number) {
  query.pageNo = pageNo
  void loadUsers()
}

function handleSizeChange(pageSize: number) {
  query.pageSize = pageSize
  query.pageNo = 1
  void loadUsers()
}

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <div>
          <h2 class="page-title">用户管理</h2>
          <p class="page-subtitle">支持用户分页查询与账号启停用。</p>
        </div>
      </div>
    </el-card>

    <el-card class="page-card" shadow="never">
      <el-form class="filter-form" label-position="top">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="手机号或昵称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" clearable placeholder="全部角色">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
        <el-table-column prop="id" label="用户 ID" min-width="100" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="role" label="角色" min-width="120" />
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.lastLoginTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" type="success" link @click="handleToggleStatus(row, 1)">启用</el-button>
            <el-button v-else type="warning" link @click="handleToggleStatus(row, 0)">禁用</el-button>
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
