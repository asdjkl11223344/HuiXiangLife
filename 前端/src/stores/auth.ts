import { defineStore } from 'pinia'
import type { UserInfo } from '../types'

const TOKEN_KEY = 'huixiang_admin_token'
const TOKEN_TYPE_KEY = 'huixiang_admin_token_type'
const USER_KEY = 'huixiang_admin_user'

interface AuthState {
  token: string
  tokenType: string
  userInfo: UserInfo | null
}

function loadUserInfo() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_KEY) ?? '',
    tokenType: localStorage.getItem(TOKEN_TYPE_KEY) ?? 'Bearer',
    userInfo: loadUserInfo(),
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
  },
  actions: {
    setSession(token: string, tokenType: string | undefined, userInfo: UserInfo | null) {
      this.token = token
      this.tokenType = tokenType || 'Bearer'
      this.userInfo = userInfo

      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(TOKEN_TYPE_KEY, this.tokenType)

      if (userInfo) {
        localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
      } else {
        localStorage.removeItem(USER_KEY)
      }
    },
    clearSession() {
      this.token = ''
      this.tokenType = 'Bearer'
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(TOKEN_TYPE_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
