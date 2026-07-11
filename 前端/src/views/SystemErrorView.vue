<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function reloadPage() {
  window.location.reload()
}

function backDashboard() {
  void router.replace('/dashboard')
}
</script>

<template>
  <div class="status-page">
    <div class="status-card">
      <div class="status-code warning">500</div>
      <h1 class="status-title">系统异常</h1>
      <p class="status-desc">当前请求发生服务异常或网络不可用。请先确认后端服务是你自己启动好的，再刷新页面重试。</p>
      <div class="status-actions">
        <el-button type="primary" @click="reloadPage">刷新重试</el-button>
        <el-button @click="backDashboard">回到控制台</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.status-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.status-card {
  width: min(560px, 100%);
  padding: 40px 32px;
  border-radius: 24px;
  text-align: center;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
}

.status-code {
  font-size: 72px;
  font-weight: 800;
  line-height: 1;
}

.warning {
  color: #d97706;
}

.status-title {
  margin: 18px 0 0;
  font-size: 30px;
  color: #111827;
}

.status-desc {
  margin: 14px 0 0;
  font-size: 14px;
  line-height: 1.8;
  color: #64748b;
}

.status-actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
