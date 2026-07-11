import dayjs from 'dayjs'
import type { PageResult } from '../types'

export function formatDateTime(value?: string) {
  if (!value) {
    return '-'
  }
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}

export function formatCurrency(value?: number) {
  if (value === undefined || value === null) {
    return '-'
  }
  return `¥${Number(value).toFixed(2)}`
}

export function createEmptyPage<T>(): PageResult<T> {
  return {
    records: [],
    current: 1,
    size: 10,
    total: 0,
    pages: 0,
  }
}

export function normalizePage<T>(payload: any): PageResult<T> {
  if (payload?.meta) {
    return {
      records: payload.records ?? [],
      current: payload.meta.current ?? 1,
      size: payload.meta.size ?? 10,
      total: payload.meta.total ?? 0,
      pages: payload.meta.pages ?? 0,
    }
  }

  return {
    records: payload?.records ?? [],
    current: payload?.current ?? 1,
    size: payload?.size ?? 10,
    total: payload?.total ?? 0,
    pages: payload?.pages ?? 0,
  }
}
