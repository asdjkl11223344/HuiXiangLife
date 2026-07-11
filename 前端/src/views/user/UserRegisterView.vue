<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerUser } from '../../api/user'
import type { UserRegisterPayload } from '../../types/user'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<UserRegisterPayload & { confirmPassword: string }>({
  phone: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  avatar: '',
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
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 32, message: '昵称长度不能超过 32 位', trigger: 'blur' },
  ],
  avatar: [{ max: 255, message: '头像地址长度不能超过 255 位', trigger: 'blur' }],
}

async function handleSubmit() {
  if (!formRef.value) {
    return
  }

  await formRef.value.validate()
  loading.value = true
  try {
    await registerUser({
      phone: form.phone,
      password: form.password,
      nickname: form.nickname,
      avatar: form.avatar,
    })
    ElMessage.success('注册成功，请登录体验')
    router.push('/user/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="user-auth-page">
    <div class="user-auth-card">
      <div class="user-auth-card__hero">
        <div class="user-auth-card__badge accent">用户端注册</div>
        <h1>3 分钟搭好你的演示账号</h1>
        <p>当前页直接对接 `/user/auth/register`，注册后即可立即登录，进入订单、收藏和领券闭环。</p>
        <div class="user-auth-card__tips">
          <span>昵称头像可选配</span>
          <span>后端真实校验</span>
          <span>注册即用</span>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="user-auth-form">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="头像 URL" prop="avatar">
          <el-input v-model="form.avatar" placeholder="可选，填写可访问的头像地址" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
          />
        </el-form-item>
        <div class="user-auth-actions">
          <el-button type="primary" :loading="loading" @click="handleSubmit">完成注册</el-button>
          <RouterLink to="/user/login" class="muted-text">已有账号？去登录</RouterLink>
        </div>
      </el-form>
    </div>
  </div>
</template>
