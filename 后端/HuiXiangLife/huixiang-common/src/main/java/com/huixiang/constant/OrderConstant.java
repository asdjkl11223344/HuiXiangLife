package com.huixiang.constant;

public final class OrderConstant {

    public static final Integer STATUS_PENDING_PAY = 0; // 待支付
    public static final Integer STATUS_PAID = 1; // 已支付
    public static final Integer STATUS_CANCELED = 2; // 已取消
    public static final Integer STATUS_FINISHED = 3; // 已完成
    public static final Integer STATUS_REFUNDED = 4; // 已退款

    private OrderConstant() {
    }
}
