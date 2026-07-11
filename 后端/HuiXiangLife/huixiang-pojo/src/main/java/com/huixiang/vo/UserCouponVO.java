package com.huixiang.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCouponVO {

    /**
     * 用户优惠券ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 优惠券模板ID
     */
    private Long couponTemplateId;

    /**
     * 优惠券名称
     */
    private String couponName;

    /**
     * 使用状态
     */
    private Integer status;

    /**
     * 领取时间
     */
    private LocalDateTime receiveTime;

    /**
     * 使用时间
     */
    private LocalDateTime useTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 订单ID
     */
    private Long orderId;
}