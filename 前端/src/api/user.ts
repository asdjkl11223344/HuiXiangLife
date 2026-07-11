import type { MerchantDetail, PageResult, ProductDetail } from '../types'
import { normalizePage } from '../utils'
import type {
  UserCouponItem,
  UserCouponQuery,
  UserCouponReceivePayload,
  UserFavoriteCreatePayload,
  UserFavoriteItem,
  UserFavoriteQuery,
  UserHomeAggregate,
  UserLoginPayload,
  UserLoginResult,
  UserMerchantQuery,
  UserOrderCreatePayload,
  UserOrderItem,
  UserOrderQuery,
  UserOwnedCouponItem,
  UserOwnedCouponQuery,
  UserPaymentCallbackPayload,
  UserPaymentCreatePayload,
  UserPaymentSubmit,
  UserProductQuery,
  UserProfile,
  UserRegisterPayload,
  UserReviewCreatePayload,
  UserReviewItem,
  UserReviewQuery,
  UserSeckillOrderCreatePayload,
  UserSeckillResult,
  UserSeckillSubmit,
} from '../types/user'
import userHttp from './user-http'

export function registerUser(payload: UserRegisterPayload) {
  return userHttp.post<any, number>('/user/auth/register', payload)
}

export function loginUser(payload: UserLoginPayload) {
  return userHttp.post<any, UserLoginResult>('/user/auth/login', payload)
}

export function getUserMe() {
  return userHttp.get<any, UserProfile>('/user/auth/me')
}

export function logoutUser() {
  return userHttp.post<any, boolean>('/user/auth/logout')
}

export function fetchUserHomeAggregate() {
  return userHttp.get<any, UserHomeAggregate>('/user/home')
}

export function fetchHotKeywords() {
  return userHttp.get<any, string[]>('/user/search/hot')
}

export async function fetchUserProductPage(params: UserProductQuery) {
  const data = await userHttp.get<any, any>('/user/product/page', { params })
  return normalizePage<ProductDetail>(data) as PageResult<ProductDetail>
}

export function fetchUserProductDetail(id: number) {
  return userHttp.get<any, ProductDetail>(`/user/product/${id}`)
}

export function fetchRecommendProducts(limit = 6) {
  return userHttp.get<any, ProductDetail[]>('/user/product/recommend', { params: { limit } })
}

export async function fetchUserMerchantPage(params: UserMerchantQuery) {
  const data = await userHttp.get<any, any>('/user/merchant/page', { params })
  return normalizePage<MerchantDetail>(data) as PageResult<MerchantDetail>
}

export function fetchUserMerchantDetail(id: number) {
  return userHttp.get<any, MerchantDetail>(`/user/merchant/${id}`)
}

export async function fetchUserOrderPage(params: UserOrderQuery) {
  const data = await userHttp.get<any, any>('/user/order/page', { params })
  return normalizePage<UserOrderItem>(data) as PageResult<UserOrderItem>
}

export function fetchUserOrderDetail(id: number) {
  return userHttp.get<any, UserOrderItem>(`/user/order/${id}`)
}

export function createUserOrder(payload: UserOrderCreatePayload) {
  return userHttp.post<any, number>('/user/order', payload)
}

export function createUserSeckillOrder(payload: UserSeckillOrderCreatePayload) {
  return userHttp.post<any, UserSeckillSubmit>('/user/order/seckill', payload)
}

export function fetchUserSeckillResult(productId: number) {
  return userHttp.get<any, UserSeckillResult>('/user/order/seckill/result', { params: { productId } })
}

export function cancelUserOrder(id: number) {
  return userHttp.post<any, boolean>(`/user/order/${id}/cancel`)
}

export function payUserOrder(id: number, payload: UserPaymentCreatePayload) {
  return userHttp.post<any, UserPaymentSubmit>(`/user/order/${id}/pay`, payload)
}

export function mockPayUserOrderCallback(payload: UserPaymentCallbackPayload) {
  return userHttp.post<any, boolean>('/notify/pay/callback', payload)
}

export async function fetchUserCouponPage(params: UserCouponQuery) {
  const data = await userHttp.get<any, any>('/user/coupon/page', { params })
  return normalizePage<UserCouponItem>(data) as PageResult<UserCouponItem>
}

export function receiveUserCoupon(payload: UserCouponReceivePayload) {
  return userHttp.post<any, number>('/user/coupon/receive', payload)
}

export async function fetchMyCouponPage(params: UserOwnedCouponQuery) {
  const data = await userHttp.get<any, any>('/user/coupon/my/page', { params })
  return normalizePage<UserOwnedCouponItem>(data) as PageResult<UserOwnedCouponItem>
}

export async function fetchUserFavoritePage(params: UserFavoriteQuery) {
  const data = await userHttp.get<any, any>('/user/favorite/page', { params })
  return normalizePage<UserFavoriteItem>(data) as PageResult<UserFavoriteItem>
}

export function createUserFavorite(payload: UserFavoriteCreatePayload) {
  return userHttp.post<any, number>('/user/favorite', payload)
}

export function deleteUserFavorite(targetId: number, targetType: number) {
  return userHttp.delete<any, boolean>('/user/favorite', {
    params: { targetId, targetType },
  })
}

export async function fetchUserReviewPage(params: UserReviewQuery) {
  const data = await userHttp.get<any, any>('/user/review/page', { params })
  return normalizePage<UserReviewItem>(data) as PageResult<UserReviewItem>
}

export function createUserReview(payload: UserReviewCreatePayload) {
  return userHttp.post<any, number>('/user/review', payload)
}
