import type { UserInfo } from '../types'

export const ADMIN_ROLE = 'ADMIN'

export function resolveUserRole(userInfo?: UserInfo | null) {
  return userInfo?.role || ''
}

export function canAccessAdmin(role?: string) {
  return role === ADMIN_ROLE
}

export function canWriteAdmin(role?: string) {
  return role === ADMIN_ROLE
}
