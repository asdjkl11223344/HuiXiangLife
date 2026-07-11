package com.huixiang.constant;

public final class CouponConstant {

    public static final Integer STATUS_DISABLED = 0; // 模板停用
    public static final Integer STATUS_ENABLED = 1; // 模板启用

    public static final Integer USER_COUPON_UNUSED = 0; // 用户优惠券未使用
    public static final Integer USER_COUPON_USED = 1; // 用户优惠券已使用
    public static final Integer USER_COUPON_EXPIRED = 2; // 用户优惠券已过期

    private CouponConstant() {
    }
}
