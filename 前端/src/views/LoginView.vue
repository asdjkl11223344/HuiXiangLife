<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginAdmin } from '../api/admin'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const loginFormRef = ref<FormInstance>()
const form = reactive({
  phone: '13800000000',
  password: '123456',
})

const rules: FormRules<typeof form> = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6 到 32 位之间', trigger: 'blur' },
  ],
}

const capabilityTags = ['Elasticsearch 搜索', 'Redis 秒杀', 'RabbitMQ 异步建单', '操作日志审计']
const middlewareStatus = [
  { label: 'MySQL', desc: '业务数据主存储与分页查询', type: 'primary' },
  { label: 'Redis', desc: '秒杀库存预热、结果状态与缓存', type: 'success' },
  { label: 'Elasticsearch', desc: '商品/商户全文搜索索引', type: 'warning' },
  { label: 'RabbitMQ', desc: '秒杀异步建单削峰', type: 'info' },
]

async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    const result = await loginAdmin(form)
    authStore.setSession(result.token, result.tokenType, result.userInfo)
    ElMessage.success(`欢迎回来，${result.userInfo?.nickname || '管理员'}`)
    await router.replace('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-banner">
      <el-tag type="primary" size="large">后台演示版</el-tag>
      <h1>HuiXiangLife 管理后台</h1>
      <p>
        首批已接入管理员登录、商品/商户索引管理、商户删除、操作日志分页查询。
      </p>

      <ul class="tips-list">
        <li>商品搜索与商户搜索已切换为 Elasticsearch 方案。</li>
        <li>管理端写操作会自动记录到操作日志。</li>
        <li>商户删除会校验商品和订单关联关系。</li>
      </ul>

      <div class="capability-row">
        <el-tag v-for="item in capabilityTags" :key="item" round>{{ item }}</el-tag>
      </div>

      <div class="middleware-grid">
        <div v-for="item in middlewareStatus" :key="item.label" class="middleware-card">
          <div class="middleware-head">
            <span class="middleware-title">{{ item.label }}</span>
            <el-tag :type="item.type">{{ item.label }}</el-tag>
          </div>
          <div class="middleware-desc">{{ item.desc }}</div>
        </div>
      </div>
    </div>

    <el-card class="login-card" shadow="hover">
      <template #header>
        <div>
          <div class="login-title">管理员登录</div>
          <div class="login-subtitle">默认使用后端 OpenAPI 对应的登录接口</div>
        </div>
      </template>

      <el-form
        ref="loginFormRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入管理员手机号" :prefix-icon="User" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            :prefix-icon="Lock"
          />
        </el-form-item>

        <el-button type="primary" :loading="loading" class="login-button" @click="handleLogin">
          登录后台
        </el-button>
      </el-form>

      <div class="login-help">
        如果登录失败，请先确认你的后端服务已启动，并且管理员账号数据可用。
      </div>

      <div class="login-demo-box">
        <div class="demo-title">默认演示账号</div>
        <div class="demo-item">手机号：`13800000000`</div>
        <div class="demo-item">密码：`123456`</div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(360px, 1.1fr) minmax(360px, 440px);
  align-items: center;
  gap: 40px;
  padding: 40px 8vw;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.12), transparent 30%),
    linear-gradient(135deg, #eff6ff, #f8fafc 45%, #eef2ff);
}

.login-banner h1 {
  margin: 20px 0 12px;
  font-size: 42px;
  color: #111827;
}

.login-banner p {
  margin: 0 0 24px;
  max-width: 560px;
  color: #475569;
  line-height: 1.8;
}

.capability-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 20px 0 26px;
}

.middleware-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  max-width: 620px;
}

.middleware-card {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(6px);
}

.middleware-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.middleware-title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

.middleware-desc {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.7;
  color: #475569;
}

.login-card {
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.login-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
}

.login-button {
  width: 100%;
  margin-top: 8px;
}

.login-help {
  margin-top: 16px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.login-demo-box {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}

.demo-title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8px;
}

.demo-item {
  font-size: 13px;
  line-height: 1.8;
  color: #475569;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
    padding: 32px 20px;
  }

  .login-banner h1 {
    font-size: 32px;
  }

  .middleware-grid {
    grid-template-columns: 1fr;
  }
}
</style>
