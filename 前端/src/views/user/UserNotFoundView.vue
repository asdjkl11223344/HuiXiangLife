<script setup lang="ts">
import { ArrowLeft, House } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

function backUserHome() {
  void router.replace('/user')
}
</script>

<template>
  <div class="user-status-page">
    <section class="user-status-card">
      <div class="user-status-card__badge">404</div>
      <h1>用户端页面不存在</h1>
      <p>
        你访问的用户端页面地址不存在，可能是链接已失效，或者该页面还没有接入当前演示版本。
      </p>
      <div class="user-status-card__tips">
        <el-tag type="primary">可返回用户首页继续浏览</el-tag>
        <el-tag type="success">商品、商户、搜索、个人中心已接入</el-tag>
      </div>
      <div class="quick-action-row user-status-card__actions">
        <el-button type="primary" :icon="House" @click="backUserHome">回到用户首页</el-button>
        <el-button :icon="ArrowLeft" @click="$router.back()">返回上一页</el-button>
      </div>
    </section>
  </div>
</template>
