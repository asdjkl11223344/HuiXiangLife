package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_template")
public class CouponTemplate extends BaseEntity {

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
     * 商户ID
     */
    private Long merchantId;
    /**
     * 商品ID
     */
    private Long productId;
    /**
     * 生效时间
     */
    private LocalDateTime startTime;
    /**
     * 失效时间
     */
    private LocalDateTime endTime;
    /**
     * 状态
     */
    private Integer status;
}
