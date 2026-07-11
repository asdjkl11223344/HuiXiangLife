package com.huixiang.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CouponVO {

    /**
     * 优惠券ID
     */
    private Long id;
    /**
     * 优惠券名称
     */
    private String name;
    /**
     * 优惠券类型
     */
    private Integer type;
    /**
     * 优惠方式
     */
    private Integer discountType;
    /**
     * 优惠值
     */
    private BigDecimal discountValue;
    /**
     * 使用门槛金额
     */
    private BigDecimal thresholdAmount;
    /**
     * 库存数量
     */
    private Integer stock;
    /**
     * 每人限领数量
     */
    private Integer limitPerUser;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 生效时间
     */
    private LocalDateTime startTime;
    /**
     * 失效时间
     */
    private LocalDateTime endTime;
}
