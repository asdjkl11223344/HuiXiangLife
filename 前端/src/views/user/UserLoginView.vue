<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserMe, loginUser } from '../../api/user'
import { useUserAuthStore } from '../../stores/user-auth'
import type { UserLoginPayload } from '../../types/user'

const router = useRouter()
const route = useRoute()
const userAuthStore = useUserAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<UserLoginPayload>({
  phone: '',
  password: '',
})

const rules: FormRules<typeof form> = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为 6-32 位', trigger: 'blur' },
  ],
}

async function handleSubmit() {
  if (!formRef.value) {
    return
  }

  await formRef.value.validate()
  loading.value = true
  try {
    const loginResult = await loginUser(form)
    userAuthStore.setSession(loginResult.token, loginResult.tokenType, loginResult.userInfo)

    if (!loginResult.userInfo) {
      const profile = await getUserMe()
      userAuthStore.setSession(loginResult.token, loginResult.tokenType, profile)
    }

    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/user/profile'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="user-auth-page">
    <div class="user-auth-card">
      <div class="user-auth-card__hero">
        <div class="user-auth-card__badge">用户端登录</div>
        <h1>登录后即可下单、收藏、领券</h1>
        <p>当前页对接 `/user/auth/login`、`/user/auth/me` 与用户端 JWT 登录态。</p>
        <div class="user-auth-card__tips">
          <span>手机号密码登录</span>
          <span>我的订单</span>
          <span>收藏与优惠券</span>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="user-auth-form">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <div class="user-auth-actions">
          <el-button type="primary" :loading="loading" @click="handleSubmit">立即登录</el-button>
          <RouterLink to="/user/register" class="muted-text">没有账号？去注册</RouterLink>
        </div>
      </el-form>
    </div>
  </div>
</template>
