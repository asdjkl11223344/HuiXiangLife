import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '../types'
import { useUserAuthStore } from '../stores/user-auth'

const userHttp = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
})

userHttp.interceptors.request.use((config) => {
  const userAuthStore = useUserAuthStore()
  if (userAuthStore.token) {
    config.headers.Authorization = `${userAuthStore.tokenType || 'Bearer'} ${userAuthStore.token}`
  }
  return config
})

userHttp.interceptors.response.use(
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
    const userAuthStore = useUserAuthStore()
    const status = error.response?.status
    const message =
      error.response?.data?.msg ||
      error.response?.data?.message ||
      error.message ||
      '网络异常，请检查后端服务是否已启动'

    if (status === 401) {
      userAuthStore.clearSession()
      const currentPath = `${window.location.pathname}${window.location.search}`
      const isAuthPage = currentPath.startsWith('/user/login') || currentPath.startsWith('/user/register')
      if (!isAuthPage) {
        window.location.href = `/user/login?redirect=${encodeURIComponent(currentPath)}`
      }
    }

    if (status === 403 && window.location.pathname !== '/403') {
      window.location.href = '/403'
    }

    if ((status === 500 || !status) && window.location.pathname !== '/user/500') {
      window.location.href = '/user/500'
    }

    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default userHttp
