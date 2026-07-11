<script setup lang="ts">
import { House, OfficeBuilding, Search, ShoppingBag, User } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getUserMe, logoutUser } from '../api/user'
import { useUserAuthStore } from '../stores/user-auth'

const router = useRouter()
const route = useRoute()
const userAuthStore = useUserAuthStore()

const menus = [
  { label: '首页', path: '/user', icon: House },
  { label: '搜索', path: '/user/search', icon: Search },
  { label: '商品', path: '/user/products', icon: ShoppingBag },
  { label: '商户', path: '/user/merchants', icon: OfficeBuilding },
  { label: '我的', path: '/user/profile', icon: User },
]

const isLoggedIn = computed(() => userAuthStore.isLoggedIn)
const nickname = computed(() => userAuthStore.userInfo?.nickname || '游客')
const avatar = computed(() => userAuthStore.userInfo?.avatar || '')

if (userAuthStore.token && !userAuthStore.userInfo) {
  getUserMe()
    .then((profile) => {
      userAuthStore.setSession(userAuthStore.token, userAuthStore.tokenType, profile)
    })
    .catch(() => {
      userAuthStore.clearSession()
    })
}

function goToLogin() {
  router.push({
    path: '/user/login',
    query: { redirect: route.fullPath },
  })
}

async function handleLogout() {
  await ElMessageBox.confirm('确认退出当前用户端登录状态吗？', '退出登录', {
    type: 'warning',
  })
  try {
    await logoutUser()
  } finally {
    userAuthStore.clearSession()
    ElMessage.success('已退出登录')
    router.push('/user')
  }
}
</script>

<template>
  <div class="user-shell">
    <header class="user-header">
      <div class="user-header__inner">
        <RouterLink to="/user" class="user-brand">
          <div class="user-brand__badge">USER</div>
          <div>
            <div class="user-brand__title">HuiXiangLife</div>
            <div class="user-brand__subtitle">吃喝玩乐与秒杀优惠一站式体验</div>
          </div>
        </RouterLink>

        <nav class="user-nav">
          <RouterLink
            v-for="item in menus"
            :key="item.path"
            :to="item.path"
            class="user-nav__item"
            :class="{ active: route.path === item.path || route.path.startsWith(`${item.path}/`) }"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>

        <div class="user-account">
          <template v-if="isLoggedIn">
            <RouterLink to="/user/profile" class="user-account__profile">
              <el-avatar :src="avatar" :size="36">{{ nickname.slice(0, 1) }}</el-avatar>
              <div class="user-account__text">
                <strong>{{ nickname }}</strong>
                <span>已登录用户</span>
              </div>
            </RouterLink>
            <el-button text @click="handleLogout">退出</el-button>
          </template>
          <template v-else>
            <el-button @click="goToLogin">登录</el-button>
            <RouterLink to="/user/register">
              <el-button type="primary">注册</el-button>
            </RouterLink>
          </template>
        </div>
      </div>
    </header>

    <main class="user-main">
      <router-view />
    </main>

    <footer class="user-footer">
      <div>HuiXiangLife 用户端演示版</div>
      <div>商品、商户、订单、优惠券、收藏功能已接入真实后端接口</div>
    </footer>
  </div>
</template>
