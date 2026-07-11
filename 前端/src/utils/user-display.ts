export function getOrderStatusLabel(status?: number) {
  const mapping: Record<number, string> = {
    0: '待支付',
    1: '已支付',
    2: '已完成',
    3: '已取消',
    4: '已退款',
  }
  return status === undefined ? '-' : (mapping[status] ?? `状态${status}`)
}

export function getOrderStatusTagType(status?: number) {
  const mapping: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'success',
    3: 'info',
    4: 'danger',
  }
  return status === undefined ? 'info' : (mapping[status] ?? 'info')
}

export function getCouponTemplateStatusLabel(status?: number) {
  const mapping: Record<number, string> = {
    0: '未生效',
    1: '可领取',
    2: '已停用',
    3: '已结束',
  }
  return status === undefined ? '-' : (mapping[status] ?? `状态${status}`)
}

export function getCouponTemplateStatusTagType(status?: number) {
  const mapping: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'info',
    3: 'danger',
  }
  return status === undefined ? 'info' : (mapping[status] ?? 'info')
}

export function getUserCouponStatusLabel(status?: number) {
  const mapping: Record<number, string> = {
    0: '未使用',
    1: '已使用',
    2: '已过期',
  }
  return status === undefined ? '-' : (mapping[status] ?? `状态${status}`)
}

export function getUserCouponStatusTagType(status?: number) {
  const mapping: Record<number, string> = {
    0: 'success',
    1: 'info',
    2: 'danger',
  }
  return status === undefined ? 'info' : (mapping[status] ?? 'info')
}

export function getFavoriteTargetTypeLabel(targetType?: number) {
  const mapping: Record<number, string> = {
    1: '商户',
    2: '商品',
  }
  return targetType === undefined ? '-' : (mapping[targetType] ?? `类型${targetType}`)
}

export function escapeHtml(value?: string) {
  if (!value) {
    return ''
  }

  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

export function highlightKeywordText(value: string | undefined, keyword: string) {
  const safeText = escapeHtml(value || '')
  const actualKeyword = keyword.trim()

  if (!safeText || !actualKeyword) {
    return safeText
  }

  const escapedKeyword = actualKeyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const pattern = new RegExp(`(${escapedKeyword})`, 'gi')
  return safeText.replace(pattern, '<mark class="keyword-highlight">$1</mark>')
}
