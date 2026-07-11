<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  fetchProductPage,
  preheatSeckillStock,
  batchPreheatSeckillStock,
  resetSeckillStock,
  batchResetSeckillStock,
  getSeckillAdminStatus,
  triggerUpcomingSeckillPreheat,
} from '../api/admin'
import type { ProductItem, SeckillAdminStatus } from '../types'

const loading = ref(false)
const list = ref<ProductItem[]>([])
const total = ref(0)
const query = ref({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
})

const selectedIds = ref<number[]>([])

const statusDialogVisible = ref(false)
const currentStatus = ref<SeckillAdminStatus | null>(null)
const debugUserId = ref<number>()

async function loadData() {
  try {
    loading.value = true
    const res = await fetchProductPage(query.value)
    // 过滤出具有秒杀时间设置的商品
    // 注意：实际项目中秒杀商品可能有特定标记或走特定接口，这里仅在前端做筛选演示，或由后端支持。
    // 我们目前直接展示商品列表。
    list.value = res.records
    total.value = res.total
  } catch (err: any) {
    ElMessage.error(err.message || '加载商品列表失败')
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(val: ProductItem[]) {
  selectedIds.value = val.map((v) => v.id)
}

async function handlePreheat(row: ProductItem) {
  try {
    await preheatSeckillStock(row.id)
    ElMessage.success(`商品 ${row.id} 秒杀库存预热成功`)
  } catch (err: any) {
    // 错误在拦截器已处理
  }
}

async function handleReset(row: ProductItem) {
  try {
    await ElMessageBox.confirm('确认重置该商品的秒杀库存吗？', '提示', { type: 'warning' })
    await resetSeckillStock(row.id)
    ElMessage.success(`商品 ${row.id} 秒杀库存重置成功`)
  } catch (err: any) {
    if (err !== 'cancel') {
      console.error(err)
    }
  }
}

async function handleCheckStatus(row: ProductItem) {
  try {
    const res = await getSeckillAdminStatus(row.id, debugUserId.value)
    currentStatus.value = res
    statusDialogVisible.value = true
  } catch (err: any) {
    console.error(err)
  }
}

async function refreshStatus() {
  if (currentStatus.value) {
    try {
      const res = await getSeckillAdminStatus(currentStatus.value.productId, debugUserId.value)
      currentStatus.value = res
      ElMessage.success('刷新成功')
    } catch (err: any) {
      console.error(err)
    }
  }
}

async function handleBatchPreheat() {
  if (!selectedIds.value.length) return ElMessage.warning('请先选择商品')
  try {
    const count = await batchPreheatSeckillStock(selectedIds.value)
    ElMessage.success(`批量预热成功，共处理 ${count} 条`)
  } catch (err: any) {
    console.error(err)
  }
}

async function handleBatchReset() {
  if (!selectedIds.value.length) return ElMessage.warning('请先选择商品')
  try {
    await ElMessageBox.confirm(`确认重置选中的 ${selectedIds.value.length} 个商品的秒杀库存吗？`, '批量重置', { type: 'warning' })
    const count = await batchResetSeckillStock(selectedIds.value)
    ElMessage.success(`批量重置成功，共处理 ${count} 条`)
  } catch (err: any) {
    if (err !== 'cancel') {
      console.error(err)
    }
  }
}

async function handleTriggerUpcoming() {
  try {
    const advanceMinutes = 30 // 默认提前30分钟，可做成弹窗输入
    await ElMessageBox.confirm(`触发自动预热（扫描未来 ${advanceMinutes} 分钟内的活动）？`, '触发预热任务', { type: 'info' })
    const count = await triggerUpcomingSeckillPreheat(advanceMinutes)
    ElMessage.success(`触发任务成功，共预热了 ${count} 个商品`)
  } catch (err: any) {
    if (err !== 'cancel') {
      console.error(err)
    }
  }
}

function handleSizeChange(val: number) {
  query.value.pageSize = val
  loadData()
}

function handleCurrentChange(val: number) {
  query.value.pageNo = val
  loadData()
}

function isSeckillActive(row: ProductItem) {
  if (!row.startTime || !row.endTime) return false
  const now = dayjs()
  return dayjs(row.startTime).isBefore(now) && dayjs(row.endTime).isAfter(now)
}

function isSeckillUpcoming(row: ProductItem) {
  if (!row.startTime) return false
  const now = dayjs()
  return dayjs(row.startTime).isAfter(now)
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="seckill-management-container">
    <el-card shadow="never" class="toolbar-card">
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="商品名称">
          <el-input v-model="query.keyword" placeholder="模糊搜索商品" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>

      <div class="batch-actions">
        <el-button type="success" @click="handleBatchPreheat" :disabled="!selectedIds.length">
          批量预热
        </el-button>
        <el-button type="danger" @click="handleBatchReset" :disabled="!selectedIds.length">
          批量重置
        </el-button>
        <el-button type="warning" @click="handleTriggerUpcoming">
          扫描并预热临近活动
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="list" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="id" label="商品ID" width="80" align="center" />
        <el-table-column prop="name" label="商品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="salePrice" label="售价" width="80" align="center" />
        <el-table-column prop="stock" label="DB库存" width="90" align="center" />
        <el-table-column label="活动时间" width="280" align="center">
          <template #default="{ row }">
            <div v-if="row.startTime && row.endTime" class="time-range">
              <div>起：{{ row.startTime }}</div>
              <div>止：{{ row.endTime }}</div>
            </div>
            <div v-else class="no-time">-</div>
          </template>
        </el-table-column>
        <el-table-column label="秒杀状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="isSeckillActive(row)" type="success">进行中</el-tag>
            <el-tag v-else-if="isSeckillUpcoming(row)" type="warning">未开始</el-tag>
            <el-tag v-else type="info">已结束/无活动</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleCheckStatus(row)">
              监控状态
            </el-button>
            <el-button size="small" type="success" link @click="handlePreheat(row)">
              预热
            </el-button>
            <el-button size="small" type="danger" link @click="handleReset(row)">
              重置
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 状态监控弹窗 -->
    <el-dialog v-model="statusDialogVisible" title="秒杀运行状态监控" width="600px">
      <div v-if="currentStatus" class="status-monitor">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="商品ID" :span="2">{{ currentStatus.productId }}</el-descriptions-item>
          <el-descriptions-item label="Redis库存值">
            <el-tag :type="(currentStatus.redisStock ?? 0) > 0 ? 'success' : 'danger'">
              {{ currentStatus.redisStock ?? '未预热' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="预热标识">
            <el-tag :type="currentStatus.stockPreheated ? 'success' : 'info'">
              {{ currentStatus.stockPreheated ? '已预热' : '未标记' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="库存Key" :span="2">{{ currentStatus.stockKey }}</el-descriptions-item>
          <el-descriptions-item label="MQ堆积映射" :span="2">{{ currentStatus.relatedOrderMappingCount ?? 0 }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">单用户链路诊断 (可选)</el-divider>
        <div class="debug-user-input">
          <el-input v-model.number="debugUserId" placeholder="输入用户ID诊断排队状态" style="width: 200px" />
          <el-button type="primary" @click="refreshStatus">刷新诊断</el-button>
        </div>

        <el-descriptions v-if="debugUserId && currentStatus.userOrderValue" :column="1" border size="small" style="margin-top: 16px;">
          <el-descriptions-item label="占位标识">{{ currentStatus.userOrderValue }}</el-descriptions-item>
          <el-descriptions-item label="最终结果">{{ currentStatus.result?.status || '无' }}</el-descriptions-item>
          <el-descriptions-item label="结果信息" v-if="currentStatus.result?.message">
            {{ currentStatus.result.message }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.seckill-management-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card {
  border-radius: 8px;
}

.search-form {
  margin-bottom: -18px;
}

.batch-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.table-card {
  border-radius: 8px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.time-range {
  font-size: 12px;
  line-height: 1.4;
  color: #606266;
}

.no-time {
  color: #909399;
}

.status-monitor {
  display: flex;
  flex-direction: column;
}

.debug-user-input {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
