export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

export interface UserInfo {
  id: number
  phone: string
  nickname: string
  avatar?: string
  role?: string
  status?: number
  lastLoginTime?: string
}

export interface LoginPayload {
  phone: string
  password: string
}

export interface LoginResult {
  token: string
  tokenType?: string
  expireIn?: number
  userInfo: UserInfo
}

export interface PageResult<T> {
  records: T[]
  current: number
  size: number
  total: number
  pages: number
}

export interface BatchActionResult {
  successCount: number
  failureCount: number
  failedIds: number[]
}

export interface StatusPayload {
  status: number
}

export interface ProductQuery {
  pageNo: number
  pageSize: number
  merchantId?: number
  keyword?: string
  status?: number
}

export interface MerchantQuery {
  pageNo: number
  pageSize: number
  categoryId?: number
  keyword?: string
  status?: number
}

export interface OrderQuery {
  pageNo: number
  pageSize: number
  userId?: number
  orderNo?: string
  merchantId?: number
  productId?: number
  status?: number
}

export interface UserQuery {
  pageNo: number
  pageSize: number
  keyword?: string
  role?: string
  status?: number
}

export interface ReviewQuery {
  pageNo: number
  pageSize: number
  merchantId?: number
  productId?: number
  userId?: number
  status?: number
}

export interface CouponQuery {
  pageNo: number
  pageSize: number
  merchantId?: number
  productId?: number
  type?: number
  status?: number
}

export interface OperationLogQuery {
  pageNo: number
  pageSize: number
  operatorId?: number
  module?: string
  action?: string
  bizId?: number
}

export interface ProductItem {
  id: number
  merchantId: number
  merchantName: string
  name: string
  subTitle?: string
  content?: string
  coverUrl?: string
  salePrice?: number
  originPrice?: number
  stock?: number
  soldCount?: number
  status?: number
  startTime?: string
  endTime?: string
}

export type ProductDetail = ProductItem

export interface ProductForm {
  id?: number
  merchantId?: number
  name: string
  subTitle?: string
  content?: string
  coverUrl?: string
  originPrice?: number
  salePrice?: number
  stock?: number
  status?: number
  startTime?: string
  endTime?: string
}

export interface MerchantItem {
  id: number
  name: string
  categoryId?: number
  categoryName?: string
  coverUrl?: string
  address?: string
  phone?: string
  description?: string
  score?: number
  avgPrice?: number
  status?: number
}

export type MerchantDetail = MerchantItem

export interface MerchantForm {
  id?: number
  name: string
  categoryId?: number
  coverUrl?: string
  address: string
  phone?: string
  description?: string
  avgPrice?: number
  status?: number
}

export interface OrderItem {
  id: number
  orderNo: string
  userId?: number
  userNickname?: string
  merchantId?: number
  merchantName?: string
  productId?: number
  productName?: string
  couponId?: number
  totalAmount?: number
  discountAmount?: number
  payAmount?: number
  status?: number
  remark?: string
  payTime?: string
  cancelTime?: string
  finishTime?: string
  createTime?: string
}

export type OrderDetail = OrderItem

export interface RefundForm {
  refundAmount?: number
  reason: string
}

export interface ReviewItem {
  id: number
  orderId?: number
  userId?: number
  userNickname?: string
  userAvatar?: string
  merchantId?: number
  productId?: number
  score?: number
  content?: string
  status?: number
  createTime?: string
}

export interface CouponItem {
  id: number
  name: string
  type?: number
  discountType?: number
  discountValue?: number
  thresholdAmount?: number
  stock?: number
  limitPerUser?: number
  merchantId?: number
  productId?: number
  status?: number
  startTime?: string
  endTime?: string
}

export interface CouponForm {
  id?: number
  name: string
  type?: number
  discountType?: number
  discountValue?: number
  thresholdAmount?: number
  stock?: number
  limitPerUser?: number
  merchantId?: number
  productId?: number
  status?: number
  startTime?: string
  endTime?: string
}

export interface MerchantCategoryItem {
  id: number
  name: string
  sort?: number
  status?: number
}

export interface MerchantCategoryForm {
  id?: number
  name: string
  sort?: number
  status?: number
}

export interface OperationLogItem {
  id: number
  operatorId?: number
  operatorName?: string
  module?: string
  action?: string
  bizId?: number
  detail?: string
  ip?: string
  createTime?: string
}

export interface SeckillResult {
  statusCode?: number
  status?: string
  finished?: boolean
  nextPollIntervalMillis?: number
  requestId?: string
  orderId?: number
  failureCode?: number
  message?: string
}

export interface SeckillAdminStatus {
  productId: number
  stockKey?: string
  redisStock?: number
  stockPreheated?: boolean
  userId?: number
  userOrderKey?: string
  userOrderValue?: string
  resultKey?: string
  resultValue?: string
  result?: SeckillResult
  relatedOrderMappingCount?: number
}
