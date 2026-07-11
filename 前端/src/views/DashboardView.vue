<script setup lang="ts">
import { Connection, DataAnalysis, Goods, Histogram, RefreshRight, Shop } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchCouponTemplatePage,
  fetchMerchantPage,
  fetchOperationLogPage,
  fetchOrderPage,
  fetchProductPage,
  fetchReviewPage,
  fetchUserPage,
} from '../api/admin'
import type { OperationLogItem } from '../types'
import { formatDateTime } from '../utils'

const router = useRouter()
const loading = ref(false)
const refreshedAt = ref('')
const recentLogs = ref<OperationLogItem[]>([])

const summary = ref({
  productTotal: 0,
  merchantTotal: 0,
  orderTotal: 0,
  userTotal: 0,
  reviewTotal: 0,
  couponTotal: 0,
  logTotal: 0,
})

const totalBusinessCount = computed(
  () =>
    summary.value.productTotal +
    summary.value.merchantTotal +
    summary.value.orderTotal +
    summary.value.userTotal +
    summary.value.reviewTotal +
    summary.value.couponTotal,
)

const stats = computed(() => [
  {
    label: '商品总量',
    value: `${summary.value.productTotal}`,
    desc: '来自商品分页接口聚合',
    icon: Goods,
    type: 'primary',
  },
  {
    label: '商户总量',
    value: `${summary.value.merchantTotal}`,
    desc: '来自商户分页接口聚合',
    icon: Shop,
    type: 'success',
  },
  {
    label: '订单总量',
    value: `${summary.value.orderTotal}`,
    desc: '来自订单分页接口聚合',
    icon: DataAnalysis,
    type: 'warning',
  },
  {
    label: '审计日志',
    value: `${summary.value.logTotal}`,
    desc: '已落库的后台操作记录',
    icon: Histogram,
    type: 'info',
  },
])

const businessDistribution = computed(() => {
  const total = totalBusinessCount.value || 1
  const items = [
    { label: '订单', value: summary.value.orderTotal, color: '#f59e0b' },
    { label: '商品', value: summary.value.productTotal, color: '#3b82f6' },
    { label: '用户', value: summary.value.userTotal, color: '#10b981' },
    { label: '商户', value: summary.value.merchantTotal, color: '#8b5cf6' },
    { label: '评价', value: summary.value.reviewTotal, color: '#ef4444' },
    { label: '优惠券', value: summary.value.couponTotal, color: '#06b6d4' },
  ]

  return items
    .map((item) => ({
      ...item,
      percent: Math.max(6, Math.round((item.value / total) * 100)),
      ratioText: `${((item.value / total) * 100).toFixed(1)}%`,
    }))
    .sort((a, b) => b.value - a.value)
})

const healthIndicators = computed(() => {
  const productBase = Math.max(summary.value.productTotal, 1)
  const merchantBase = Math.max(summary.value.merchantTotal, 1)
  const orderBase = Math.max(summary.value.orderTotal, 1)

  return [
    {
      label: '订单/商品活跃度',
      value: `${((summary.value.orderTotal / productBase) * 100).toFixed(1)}%`,
      desc: '订单量相对商品量的活跃比值',
      accent: 'warning',
    },
    {
      label: '商户承载密度',
      value: `${(summary.value.productTotal / merchantBase).toFixed(1)}`,
      desc: '平均每个商户承载的商品数量',
      accent: 'primary',
    },
    {
      label: '评价覆盖率',
      value: `${((summary.value.reviewTotal / orderBase) * 100).toFixed(1)}%`,
      desc: '评价量相对订单量的覆盖情况',
      accent: 'success',
    },
  ]
})

const operationHeat = computed(() => {
  const max = Math.max(recentLogs.value.length, 1)
  return recentLogs.value.map((item, index) => ({
    ...item,
    width: `${100 - index * (70 / max)}%`,
  }))
})

const moduleStats = computed(() => [
  { label: '用户', value: summary.value.userTotal },
  { label: '评价', value: summary.value.reviewTotal },
  { label: '优惠券模板', value: summary.value.couponTotal },
])

const dataSourceCards = [
  { label: 'MySQL', desc: '商品、商户、订单、用户、评价、优惠券、日志等核心业务数据。', type: 'primary' },
  { label: 'Redis', desc: '秒杀库存预热、用户抢购结果、缓存状态。', type: 'success' },
  { label: 'Elasticsearch', desc: '商品与商户全文检索，以及索引重建与单条同步。', type: 'warning' },
  { label: 'RabbitMQ', desc: '秒杀异步建单削峰，最终订单仍回写 MySQL。', type: 'info' },
]

const quickActions = [
  { label: '商品管理', desc: '维护商品、上下架并重建搜索索引', path: '/products' },
  { label: '商户管理', desc: '维护商户、删除校验与索引同步', path: '/merchants' },
  { label: '秒杀管理', desc: '预热库存、重置缓存、查看状态', path: '/seckill' },
  { label: '操作日志', desc: '查看后台写操作审计记录', path: '/operation-logs' },
]

const projectHighlights = [
  {
    title: '搜索链路升级',
    desc: '商品与商户搜索统一切到 Elasticsearch，后台支持单条同步和全量重建。',
    tag: 'ES 搜索',
  },
  {
    title: '高并发秒杀',
    desc: 'Redis + Lua 预扣库存，RabbitMQ 异步建单，管理端支持预热、重置与状态检查。',
    tag: '秒杀治理',
  },
  {
    title: '后台审计',
    desc: '管理端写接口自动记录操作日志，支持分页查询、快捷筛选和详情查看。',
    tag: '审计日志',
  },
  {
    title: '真实联调',
    desc: '当前前端页面直接对接后端真实接口，具备完整后台演示链路。',
    tag: '可联调',
  },
]

const interviewSummary = [
  '商品/商户搜索走 ES，详情仍回表 MySQL，兼顾搜索能力和业务一致性。',
  '秒杀场景用 Redis 做库存治理，用 MQ 做削峰，最终订单仍落 MySQL。',
  '管理后台不是静态页面，而是直接复用真实管理接口做聚合展示。',
]

async function safePageTotal(request: Promise<{ total: number }>) {
  try {
    const result = await request
    return result.total || 0
  } catch {
    return 0
  }
}

async function loadDashboard() {
  loading.value = true
  try {
    const [
      productTotal,
      merchantTotal,
      orderTotal,
      userTotal,
      reviewTotal,
      couponTotal,
      logPage,
    ] = await Promise.all([
      safePageTotal(fetchProductPage({ pageNo: 1, pageSize: 1, keyword: '', merchantId: undefined, status: undefined })),
      safePageTotal(fetchMerchantPage({ pageNo: 1, pageSize: 1, keyword: '', categoryId: undefined, status: undefined })),
      safePageTotal(fetchOrderPage({ pageNo: 1, pageSize: 1, orderNo: '', userId: undefined, merchantId: undefined, productId: undefined, status: undefined })),
      safePageTotal(fetchUserPage({ pageNo: 1, pageSize: 1, keyword: '', role: '', status: undefined })),
      safePageTotal(fetchReviewPage({ pageNo: 1, pageSize: 1, merchantId: undefined, productId: undefined, userId: undefined, status: undefined })),
      safePageTotal(fetchCouponTemplatePage({ pageNo: 1, pageSize: 1, merchantId: undefined, productId: undefined, type: undefined, status: undefined })),
      fetchOperationLogPage({ pageNo: 1, pageSize: 6, operatorId: undefined, module: '', action: '', bizId: undefined }).catch(() => ({
        records: [],
        total: 0,
        current: 1,
        size: 6,
        pages: 0,
      })),
    ])

    summary.value = {
      productTotal,
      merchantTotal,
      orderTotal,
      userTotal,
      reviewTotal,
      couponTotal,
      logTotal: logPage.total || 0,
    }
    recentLogs.value = logPage.records || []
    refreshedAt.value = formatDateTime(new Date().toISOString())
  } finally {
    loading.value = false
  }
}

function go(path: string) {
  void router.push(path)
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <div>
          <h2 class="page-title">控制台总览</h2>
          <p class="page-subtitle">基于现有管理接口聚合关键数量、数据源分层和最近后台操作。</p>
        </div>

        <div class="filter-actions">
          <el-tag type="info">最近刷新：{{ refreshedAt || '-' }}</el-tag>
          <el-button type="primary" :icon="RefreshRight" :loading="loading" @click="loadDashboard">刷新数据</el-button>
        </div>
      </div>
    </el-card>

    <div class="stat-grid">
      <el-card v-for="item in stats" :key="item.label" class="stat-card" shadow="hover" v-loading="loading">
        <div class="stat-header">
          <div class="stat-label">{{ item.label }}</div>
          <el-tag :type="item.type">{{ item.label }}</el-tag>
        </div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="page-subtitle">{{ item.desc }}</div>
      </el-card>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card class="page-card" shadow="never" v-loading="loading">
          <template #header>
            <span>模块规模</span>
          </template>

          <div class="mini-stat-grid">
            <div v-for="item in moduleStats" :key="item.label" class="mini-stat-card">
              <div class="mini-stat-label">{{ item.label }}</div>
              <div class="mini-stat-value">{{ item.value }}</div>
            </div>
          </div>

          <div class="section-title">快捷入口</div>
          <div class="action-grid">
            <el-card v-for="item in quickActions" :key="item.path" class="action-card" shadow="hover" @click="go(item.path)">
              <div class="action-title">{{ item.label }}</div>
              <div class="page-subtitle">{{ item.desc }}</div>
            </el-card>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card class="page-card" shadow="never">
          <template #header>
            <span>数据源分层</span>
          </template>

          <div class="source-list">
            <div v-for="item in dataSourceCards" :key="item.label" class="source-item">
              <div class="source-head">
                <div class="source-title">
                  <el-icon><Connection /></el-icon>
                  <span>{{ item.label }}</span>
                </div>
                <el-tag :type="item.type">{{ item.label }}</el-tag>
              </div>
              <div class="page-subtitle">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card class="page-card" shadow="never" v-loading="loading">
          <template #header>
            <span>业务分布图</span>
          </template>

          <div class="distribution-list">
            <div v-for="item in businessDistribution" :key="item.label" class="distribution-item">
              <div class="distribution-meta">
                <div class="distribution-left">
                  <span class="distribution-dot" :style="{ background: item.color }"></span>
                  <span class="distribution-label">{{ item.label }}</span>
                </div>
                <div class="distribution-value">{{ item.value }} / {{ item.ratioText }}</div>
              </div>
              <div class="distribution-track">
                <div class="distribution-bar" :style="{ width: item.percent + '%', background: item.color }"></div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card class="page-card" shadow="never" v-loading="loading">
          <template #header>
            <span>健康指标</span>
          </template>

          <div class="indicator-list">
            <div v-for="item in healthIndicators" :key="item.label" class="indicator-card">
              <div class="indicator-head">
                <span class="indicator-label">{{ item.label }}</span>
                <el-tag :type="item.accent">{{ item.label }}</el-tag>
              </div>
              <div class="indicator-value">{{ item.value }}</div>
              <div class="page-subtitle">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="15">
        <el-card class="page-card" shadow="never">
          <template #header>
            <span>项目亮点</span>
          </template>

          <div class="highlight-grid">
            <div v-for="item in projectHighlights" :key="item.title" class="highlight-card">
              <div class="highlight-head">
                <div class="highlight-title">{{ item.title }}</div>
                <el-tag type="primary">{{ item.tag }}</el-tag>
              </div>
              <div class="page-subtitle">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="9">
        <el-card class="page-card" shadow="never">
          <template #header>
            <span>讲解提纲</span>
          </template>

          <ol class="tips-list">
            <li v-for="item in interviewSummary" :key="item">{{ item }}</li>
          </ol>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="page-card" shadow="never" v-loading="loading">
      <template #header>
        <span>最近操作</span>
      </template>

      <el-empty v-if="recentLogs.length === 0" description="暂无操作日志数据" />
      <div v-else class="activity-grid">
        <div class="activity-timeline">
          <el-timeline>
            <el-timeline-item
              v-for="item in recentLogs"
              :key="item.id"
              :timestamp="formatDateTime(item.createTime)"
              placement="top"
            >
              <div class="timeline-title">{{ item.module }} / {{ item.action }}</div>
              <div class="page-subtitle">
                {{ item.operatorName || `操作人ID:${item.operatorId || '-'}` }}，业务 ID：{{ item.bizId || '-' }}
              </div>
              <div class="timeline-detail">{{ item.detail || '无详情' }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div class="activity-heatmap">
          <div class="section-title compact">操作热度</div>
          <div v-for="item in operationHeat" :key="item.id" class="heat-row">
            <div class="heat-meta">
              <span>{{ item.module || '未知模块' }}</span>
              <span>{{ item.action || '未知动作' }}</span>
            </div>
            <div class="heat-track">
              <div class="heat-bar" :style="{ width: item.width }"></div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.mini-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.mini-stat-card {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 18px;
  background: #f9fafb;
}

.mini-stat-label {
  color: #6b7280;
  font-size: 13px;
}

.mini-stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.section-title {
  margin: 24px 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.action-card {
  cursor: pointer;
  border-radius: 14px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
  border: 1px solid #e5e7eb;
}

.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.22);
}

.action-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.distribution-list,
.indicator-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.highlight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.highlight-card {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 248, 255, 0.9));
}

.highlight-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.highlight-title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

.distribution-item,
.indicator-card {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.9));
}

.distribution-meta,
.indicator-head,
.heat-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.distribution-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.distribution-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.distribution-label,
.indicator-label {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.distribution-value {
  font-size: 13px;
  color: #475569;
}

.distribution-track,
.heat-track {
  width: 100%;
  height: 10px;
  border-radius: 999px;
  background: #e5edf7;
  overflow: hidden;
  margin-top: 12px;
}

.distribution-bar,
.heat-bar {
  height: 100%;
  border-radius: 999px;
}

.indicator-value {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 700;
  color: #0f172a;
}

.source-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.source-item {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px;
}

.source-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.source-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.timeline-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.timeline-detail {
  margin-top: 8px;
  color: #374151;
  line-height: 1.6;
}

.activity-grid {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: 20px;
  align-items: start;
}

.activity-heatmap {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.9));
}

.compact {
  margin-top: 0;
}

.heat-row + .heat-row {
  margin-top: 14px;
}

.heat-meta {
  font-size: 13px;
  color: #475569;
}

.heat-bar {
  background: linear-gradient(90deg, #60a5fa, #2563eb);
}

@media (max-width: 992px) {
  .mini-stat-grid,
  .action-grid,
  .activity-grid,
  .highlight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
