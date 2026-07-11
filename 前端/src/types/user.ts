import type {
  LoginResult,
  MerchantDetail,
  PageResult,
  ProductDetail,
  ReviewItem,
  UserInfo,
} from './index'

export interface UserLoginPayload {
  phone: string
  password: string
}

export interface UserRegisterPayload {
  phone: string
  password: string
  nickname: string
  avatar?: string
}

export type UserLoginResult = LoginResult
export type UserProfile = UserInfo

export interface UserMerchantCategory {
  id: number
  name: string
  sort?: number
  status?: number
}

export interface UserHomeAggregate {
  categories: UserMerchantCategory[]
  recommendProducts: ProductDetail[]
  hotKeywords: string[]
  merchants: MerchantDetail[]
}

export interface UserProductQuery {
  pageNo: number
  pageSize: number
  merchantId?: number
  keyword?: string
  status?: number
  minSalePrice?: number
  maxSalePrice?: number
}

export interface UserMerchantQuery {
  pageNo: number
  pageSize: number
  categoryId?: number
  keyword?: string
  status?: number
  minScore?: number
  maxAvgPrice?: number
}

export interface UserOrderQuery {
  pageNo: number
  pageSize: number
  userId?: number
  orderNo?: string
  merchantId?: number
  productId?: number
  status?: number
}

export interface UserCouponQuery {
  pageNo: number
  pageSize: number
  merchantId?: number
  productId?: number
  type?: number
  status?: number
}

export interface UserOwnedCouponQuery {
  pageNo: number
  pageSize: number
  status?: number
}

export interface UserFavoriteQuery {
  pageNo: number
  pageSize: number
  targetType?: number
}

export interface UserReviewQuery {
  pageNo: number
  pageSize: number
  merchantId?: number
  productId?: number
  userId?: number
  status?: number
}

export interface UserOrderCreatePayload {
  merchantId: number
  productId: number
  couponId?: number
  quantity: number
  remark?: string
}

export interface UserSeckillOrderCreatePayload {
  merchantId: number
  productId: number
  remark?: string
}

export interface UserPaymentCreatePayload {
  orderId: number
  payChannel: string
}

export interface UserPaymentCallbackPayload {
  payChannel: string
  orderNo: string
  transactionNo: string
  payStatus: number
  callbackContent?: string
}

export interface UserCouponReceivePayload {
  couponTemplateId: number
}

export interface UserFavoriteCreatePayload {
  targetId: number
  targetType: number
}

export interface UserReviewCreatePayload {
  orderId: number
  merchantId: number
  productId: number
  score: number
  content?: string
}

export interface UserCouponItem {
  id: number
  name: string
  type?: number
  discountType?: number
  discountValue?: number
  thresholdAmount?: number
  stock?: number
  limitPerUser?: number
  status?: number
  startTime?: string
  endTime?: string
}

export interface UserOwnedCouponItem {
  id: number
  userId?: number
  couponTemplateId?: number
  couponName?: string
  status?: number
  receiveTime?: string
  useTime?: string
  expireTime?: string
  orderId?: number
}

export interface UserFavoriteItem {
  id: number
  userId?: number
  targetId: number
  targetType: number
  targetName?: string
  targetCoverUrl?: string
  createTime?: string
}

export type UserReviewItem = ReviewItem

export interface UserOrderItem {
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

export interface UserPaymentSubmit {
  paymentId?: number
  orderId?: number
  payChannel?: string
  payStatus?: number
  payUrl?: string
  transactionNo?: string
}

export interface UserSeckillSubmit {
  requestId?: string
  pollIntervalMillis?: number
  message?: string
}

export interface UserSeckillResult {
  statusCode?: number
  status?: string
  finished?: boolean
  nextPollIntervalMillis?: number
  requestId?: string
  orderId?: number
  failureCode?: number
  message?: string
}

export type UserProductPage = PageResult<ProductDetail>
export type UserMerchantPage = PageResult<MerchantDetail>
export type UserOrderPage = PageResult<UserOrderItem>
export type UserCouponPage = PageResult<UserCouponItem>
export type UserOwnedCouponPage = PageResult<UserOwnedCouponItem>
export type UserFavoritePage = PageResult<UserFavoriteItem>
export type UserReviewPage = PageResult<UserReviewItem>
