<script setup lang="ts">
import {
  CollectionTag,
  DataAnalysis,
  Goods,
  Histogram,
  List,
  Shop,
  SwitchButton,
  Lightning,
  Tickets,
  User,
  ChatLineSquare,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { logoutAdmin } from '../api/admin'
import { useAuthStore } from '../stores/auth'
import { canAccessAdmin, resolveUserRole } from '../utils/permissions'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activePath = computed(() => route.path)

const userDisplayName = computed(() => authStore.userInfo?.nickname || '管理员')
const userRole = computed(() => resolveUserRole(authStore.userInfo))

const menus = [
  { path: '/dashboard', label: '控制台', icon: DataAnalysis },
  { path: '/products', label: '商品管理', icon: Goods },
  { path: '/seckill', label: '秒杀活动管理', icon: Lightning },
  { path: '/orders', label: '订单管理', icon: List },
  { path: '/merchants', label: '商户管理', icon: Shop },
  { path: '/merchant-categories', label: '商户分类', icon: CollectionTag },
  { path: '/users', label: '用户管理', icon: User },
  { path: '/reviews', label: '评价管理', icon: ChatLineSquare },
  { path: '/coupon-templates', label: '优惠券模板', icon: Tickets },
  { path: '/operation-logs', label: '操作日志', icon: Histogram },
]

const visibleMenus = computed(() => (canAccessAdmin(userRole.value) ? menus : []))

async function handleLogout() {
  await ElMessageBox.confirm('确认退出当前管理后台吗？', '退出登录', {
    type: 'warning',
  })

  try {
    await logoutAdmin()
  } catch {
    // 忽略接口异常，仍然清理本地状态，避免前端被锁死在登录态。
  }

  authStore.clearSession()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="240px" class="layout-aside">
      <div class="brand-panel">
        <div class="brand-badge">ADMIN</div>
        <div class="brand-title">HuiXiangLife</div>
        <div class="brand-subtitle">管理后台</div>
        <div class="brand-tip">搜索、秒杀、审计、订单管理一体化演示</div>
      </div>

      <el-menu
        :default-active="activePath"
        class="layout-menu"
        router
      >
        <el-menu-item
          v-for="menu in visibleMenus"
          :key="menu.path"
          :index="menu.path"
        >
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div>
          <div class="header-title">{{ route.meta.title || '后台管理' }}</div>
          <div class="header-subtitle">基于最新后端开发文档和管理接口构建</div>
        </div>

        <div class="header-actions">
          <el-tag type="success">已登录</el-tag>
          <el-tag type="primary">{{ userRole || 'UNKNOWN' }}</el-tag>
          <span class="header-user">{{ userDisplayName }}</span>
          <el-button type="danger" plain :icon="SwitchButton" @click="handleLogout">
            退出
          </el-button>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-aside {
  background: linear-gradient(180deg, #0f172a, #111827);
  color: #fff;
  min-height: 100vh;
  padding: 20px 16px;
}

.brand-panel {
  padding: 14px 14px 20px;
  margin-bottom: 8px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.16), rgba(255, 255, 255, 0.03));
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #dbeafe;
  background: rgba(59, 130, 246, 0.28);
}

.brand-title {
  margin-top: 14px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.72);
}

.brand-tip {
  margin-top: 14px;
  font-size: 12px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.64);
}

.layout-menu {
  border-right: none;
  background: transparent;
}

:deep(.layout-menu .el-menu-item) {
  color: rgba(255, 255, 255, 0.82);
  border-radius: 12px;
  margin-bottom: 6px;
}

:deep(.layout-menu .el-menu-item.is-active) {
  background: rgba(59, 130, 246, 0.2);
  color: #ffffff;
}

:deep(.layout-menu .el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.layout-header {
  height: 72px;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.header-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-user {
  font-size: 14px;
  color: #374151;
}

.layout-main {
  padding: 24px;
}
</style>
