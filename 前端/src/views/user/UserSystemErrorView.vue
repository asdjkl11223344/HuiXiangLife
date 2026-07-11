<script setup lang="ts">
import { RefreshRight, ShoppingBag } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

function reloadPage() {
  window.location.reload()
}

function backUserHome() {
  void router.replace('/user')
}
</script>

<template>
  <div class="user-status-page">
    <section class="user-status-card">
      <div class="user-status-card__badge warning">500</div>
      <h1>用户端服务异常</h1>
      <p>
        当前请求发生服务异常或网络不可用。请先确认后端服务是你自己启动好的，再刷新页面重试。
      </p>
      <div class="user-status-card__tips">
        <el-tag type="warning">优先检查后端是否启动</el-tag>
        <el-tag type="info">再检查登录态是否过期</el-tag>
      </div>
      <div class="quick-action-row user-status-card__actions">
        <el-button type="primary" :icon="RefreshRight" @click="reloadPage">刷新重试</el-button>
        <el-button :icon="ShoppingBag" @click="backUserHome">回到用户首页</el-button>
      </div>
    </section>
  </div>
</template>
