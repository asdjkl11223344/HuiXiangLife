import http from './http'
import type {
  BatchActionResult,
  CouponForm,
  CouponItem,
  CouponQuery,
  LoginPayload,
  LoginResult,
  MerchantDetail,
  MerchantCategoryForm,
  MerchantCategoryItem,
  MerchantForm,
  MerchantItem,
  MerchantQuery,
  OperationLogItem,
  OperationLogQuery,
  OrderDetail,
  OrderItem,
  OrderQuery,
  PageResult,
  ProductDetail,
  ProductForm,
  ProductItem,
  ProductQuery,
  RefundForm,
  ReviewItem,
  ReviewQuery,
  SeckillAdminStatus,
  StatusPayload,
  UserInfo,
  UserQuery,
} from '../types'
import { normalizePage } from '../utils'

async function runBatch(ids: number[], worker: (id: number) => Promise<unknown>): Promise<BatchActionResult> {
  const settled = await Promise.allSettled(ids.map((id) => worker(id)))
  const failedIds = ids.filter((_, index) => settled[index].status === 'rejected')
  return {
    successCount: ids.length - failedIds.length,
    failureCount: failedIds.length,
    failedIds,
  }
}

export function loginAdmin(payload: LoginPayload) {
  return http.post<any, LoginResult>('/admin/auth/login', payload)
}

export function getAdminMe() {
  return http.get<any, UserInfo>('/admin/auth/me')
}

export function logoutAdmin() {
  return http.post<any, boolean>('/admin/auth/logout')
}

export async function fetchProductPage(params: ProductQuery) {
  const data = await http.get<any, any>('/admin/product/page', { params })
  return normalizePage<ProductItem>(data) as PageResult<ProductItem>
}

export function fetchProductDetail(id: number) {
  return http.get<any, ProductDetail>(`/admin/product/${id}`)
}

export function createProduct(payload: ProductForm) {
  return http.post<any, number>('/admin/product', payload)
}

export function updateProduct(payload: ProductForm) {
  return http.put<any, boolean>('/admin/product', payload)
}

export function updateProductStatus(id: number, payload: StatusPayload) {
  return http.put<any, boolean>(`/admin/product/${id}/status`, payload)
}

export function deleteProduct(id: number) {
  return http.delete<any, boolean>(`/admin/product/${id}`)
}

export function syncProductSearchIndex(id: number) {
  return http.post<any, boolean>(`/admin/product/${id}/search/sync`)
}

export function rebuildProductSearchIndex() {
  return http.post<any, number>('/admin/product/search/rebuild')
}

export function batchSyncProductSearchIndex(ids: number[]) {
  return runBatch(ids, (id) => syncProductSearchIndex(id))
}

export function batchUpdateProductStatus(ids: number[], status: number) {
  return runBatch(ids, (id) => updateProductStatus(id, { status }))
}

export function batchDeleteProduct(ids: number[]) {
  return runBatch(ids, (id) => deleteProduct(id))
}

export async function fetchMerchantPage(params: MerchantQuery) {
  const data = await http.get<any, any>('/admin/merchant/page', { params })
  return normalizePage<MerchantItem>(data) as PageResult<MerchantItem>
}

export function fetchMerchantDetail(id: number) {
  return http.get<any, MerchantDetail>(`/admin/merchant/${id}`)
}

export function createMerchant(payload: MerchantForm) {
  return http.post<any, number>('/admin/merchant', payload)
}

export function updateMerchant(payload: MerchantForm) {
  return http.put<any, boolean>('/admin/merchant', payload)
}

export function updateMerchantStatus(id: number, payload: StatusPayload) {
  return http.put<any, boolean>(`/admin/merchant/${id}/status`, payload)
}

export function syncMerchantSearchIndex(id: number) {
  return http.post<any, boolean>(`/admin/merchant/${id}/search/sync`)
}

export function rebuildMerchantSearchIndex() {
  return http.post<any, number>('/admin/merchant/search/rebuild')
}

export function deleteMerchant(id: number) {
  return http.delete<any, boolean>(`/admin/merchant/${id}`)
}

export function batchSyncMerchantSearchIndex(ids: number[]) {
  return runBatch(ids, (id) => syncMerchantSearchIndex(id))
}

export function batchUpdateMerchantStatus(ids: number[], status: number) {
  return runBatch(ids, (id) => updateMerchantStatus(id, { status }))
}

export function batchDeleteMerchant(ids: number[]) {
  return runBatch(ids, (id) => deleteMerchant(id))
}

export async function fetchOrderPage(params: OrderQuery) {
  const data = await http.get<any, any>('/admin/order/page', { params })
  return normalizePage<OrderItem>(data) as PageResult<OrderItem>
}

export function fetchOrderDetail(id: number) {
  return http.get<any, OrderDetail>(`/admin/order/${id}`)
}

export function cancelOrder(id: number) {
  return http.put<any, boolean>(`/admin/order/${id}/cancel`)
}

export function refundOrder(id: number, payload: RefundForm) {
  return http.post<any, boolean>(`/admin/order/${id}/refund`, payload)
}

export async function fetchUserPage(params: UserQuery) {
  const data = await http.get<any, any>('/admin/user/page', { params })
  return normalizePage<UserInfo>(data) as PageResult<UserInfo>
}

export function updateUserStatus(id: number, payload: StatusPayload) {
  return http.put<any, boolean>(`/admin/user/${id}/status`, payload)
}

export async function fetchReviewPage(params: ReviewQuery) {
  const data = await http.get<any, any>('/admin/review/page', { params })
  return normalizePage<ReviewItem>(data) as PageResult<ReviewItem>
}

export function updateReviewStatus(id: number, status: number) {
  return http.put<any, boolean>(`/admin/review/${id}/status`, null, {
    params: { status },
  })
}

export async function fetchCouponTemplatePage(params: CouponQuery) {
  const data = await http.get<any, any>('/admin/coupon-template/page', { params })
  return normalizePage<CouponItem>(data) as PageResult<CouponItem>
}

export function createCouponTemplate(payload: CouponForm) {
  return http.post<any, number>('/admin/coupon-template', payload)
}

export function updateCouponTemplate(payload: CouponForm) {
  return http.put<any, boolean>('/admin/coupon-template', payload)
}

export function updateCouponTemplateStatus(id: number, payload: StatusPayload) {
  return http.put<any, boolean>(`/admin/coupon-template/${id}/status`, payload)
}

export function deleteCouponTemplate(id: number) {
  return http.delete<any, boolean>(`/admin/coupon-template/${id}`)
}

export function fetchMerchantCategoryList() {
  return http.get<any, MerchantCategoryItem[]>('/admin/merchant-category/list')
}

export function createMerchantCategory(payload: MerchantCategoryForm) {
  return http.post<any, number>('/admin/merchant-category', payload)
}

export function updateMerchantCategory(payload: MerchantCategoryForm) {
  return http.put<any, boolean>('/admin/merchant-category', payload)
}

export function deleteMerchantCategory(id: number) {
  return http.delete<any, boolean>(`/admin/merchant-category/${id}`)
}

export async function fetchOperationLogPage(params: OperationLogQuery) {
  const data = await http.get<any, any>('/admin/operation-log/page', { params })
  return normalizePage<OperationLogItem>(data) as PageResult<OperationLogItem>
}

export function preheatSeckillStock(id: number) {
  return http.post<any, boolean>(`/admin/product/${id}/seckill/preheat`)
}

export function batchPreheatSeckillStock(ids: number[]) {
  return http.post<any, number>('/admin/product/seckill/preheat/batch', null, {
    params: { ids: ids.join(',') },
  })
}

export function resetSeckillStock(id: number) {
  return http.post<any, boolean>(`/admin/product/${id}/seckill/reset`)
}

export function batchResetSeckillStock(ids: number[]) {
  return http.post<any, number>('/admin/product/seckill/reset/batch', null, {
    params: { ids: ids.join(',') },
  })
}

export function getSeckillAdminStatus(id: number, userId?: number) {
  return http.get<any, SeckillAdminStatus>(`/admin/product/${id}/seckill/status`, {
    params: { userId },
  })
}

export function triggerUpcomingSeckillPreheat(advanceMinutes?: number) {
  return http.post<any, number>('/admin/product/seckill/preheat/trigger', null, {
    params: { advanceMinutes },
  })
}
