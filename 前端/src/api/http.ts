import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '../types'
import { useAuthStore } from '../stores/auth'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `${authStore.tokenType || 'Bearer'} ${authStore.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (payload?.code === 1) {
      return payload.data as any
    }

    const message = payload?.msg || '请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  (error) => {
    const authStore = useAuthStore()
    const status = error.response?.status
    const message =
      error.response?.data?.msg ||
      error.response?.data?.message ||
      error.message ||
      '网络异常，请检查后端服务是否已启动'

    if (status === 401) {
      authStore.clearSession()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }

    if (status === 403 && window.location.pathname !== '/403') {
      window.location.href = '/403'
    }

    if ((status === 500 || !status) && window.location.pathname !== '/500') {
      window.location.href = '/500'
    }

    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default http
