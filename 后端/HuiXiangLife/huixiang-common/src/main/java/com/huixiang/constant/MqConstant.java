package com.huixiang.constant;

public final class MqConstant {

    public static final String BIZ_TYPE_ORDER_TIMEOUT = "order_timeout"; // 订单超时业务类型
    public static final String BIZ_TYPE_COUPON_EXPIRE = "coupon_expire"; // 优惠券过期业务类型
    public static final String BIZ_TYPE_ORDER_STATUS_SYNC = "order_status_sync"; // 订单状态同步业务类型
    public static final String BIZ_TYPE_ASYNC_ORDER_CREATE = "async_order_create"; // 异步创建订单业务类型

    public static final Integer CONSUME_STATUS_SUCCESS = 1; // MQ 消费成功状态

    public static final long ORDER_TIMEOUT_DELAY_MILLIS = 15L * 60L * 1000L; // 订单超时延迟时间，单位：毫秒
    public static final long ORDER_STATUS_SYNC_DELAY_MILLIS = 24L * 60L * 60L * 1000L; // 订单状态同步延迟时间，单位：毫秒

    public static final String NOTIFY_EXCHANGE = "huixiang.notify.exchange"; // 通知业务交换机
    public static final String NOTIFY_DELAY_EXCHANGE = "huixiang.notify.delay.exchange"; // 延迟通知交换机
    public static final String ASYNC_ORDER_EXCHANGE = "huixiang.order.exchange"; // 异步订单交换机

    public static final String ORDER_TIMEOUT_QUEUE = "huixiang.notify.order.timeout.queue"; // 订单超时处理队列
    public static final String ORDER_TIMEOUT_DELAY_QUEUE = "huixiang.notify.order.timeout.delay.queue"; // 订单超时延迟队列
    public static final String COUPON_EXPIRE_QUEUE = "huixiang.notify.coupon.expire.queue"; // 优惠券过期处理队列
    public static final String COUPON_EXPIRE_DELAY_QUEUE = "huixiang.notify.coupon.expire.delay.queue"; // 优惠券过期延迟队列
    public static final String ORDER_STATUS_SYNC_QUEUE = "huixiang.notify.order.status.sync.queue"; // 订单状态同步队列
    public static final String ORDER_STATUS_SYNC_DELAY_QUEUE = "huixiang.notify.order.status.sync.delay.queue"; // 订单状态同步延迟队列
    public static final String ASYNC_ORDER_CREATE_QUEUE = "huixiang.order.create.queue"; // 异步创建订单队列

    public static final String ORDER_TIMEOUT_ROUTING_KEY = "notify.order.timeout"; // 订单超时处理路由键
    public static final String ORDER_TIMEOUT_DELAY_ROUTING_KEY = "notify.order.timeout.delay"; // 订单超时延迟路由键
    public static final String COUPON_EXPIRE_ROUTING_KEY = "notify.coupon.expire"; // 优惠券过期处理路由键
    public static final String COUPON_EXPIRE_DELAY_ROUTING_KEY = "notify.coupon.expire.delay"; // 优惠券过期延迟路由键
    public static final String ORDER_STATUS_SYNC_ROUTING_KEY = "notify.order.status.sync"; // 订单状态同步路由键
    public static final String ORDER_STATUS_SYNC_DELAY_ROUTING_KEY = "notify.order.status.sync.delay"; // 订单状态同步延迟路由键
    public static final String ASYNC_ORDER_CREATE_ROUTING_KEY = "order.create"; // 异步创建订单路由键

    private MqConstant() {
    }
}
