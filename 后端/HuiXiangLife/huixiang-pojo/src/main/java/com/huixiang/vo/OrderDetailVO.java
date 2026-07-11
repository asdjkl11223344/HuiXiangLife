package com.huixiang.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderDetailVO {

    /**
     * 订单ID
     */
    private Long id;
    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 用户昵称
     */
    private String userNickname;
    /**
     * 商户ID
     */
    private Long merchantId;
    /**
     * 商户名称
     */
    private String merchantName;
    /**
     * 商品ID
     */
    private Long productId;
    /**
     * 商品名称
     */
    private String productName;
    /**
     * 优惠券ID
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
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
