package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_info")
public class OrderInfo extends BaseEntity {

    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 商户ID
     */
    private Long merchantId;
    /**
     * 商品ID
     */
    private Long productId;
    /**
     * 使用的优惠券ID
     */
    private Long couponId;
    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;
    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;
    /**
     * 实付金额
     */
    private BigDecimal payAmount;
    /**
     * 订单状态
     */
    private Integer status;
    /**
     * 订单备注
     */
    private String remark;
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;
    /**
     * 完成时间
     */
    private LocalDateTime finishTime;
}
