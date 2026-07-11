package com.huixiang.vo;

import lombok.Data;

@Data
public class PaymentSubmitVO {

    /**
     * 支付记录ID
     */
    private Long paymentId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 支付渠道
     */
    private String payChannel;

    /**
     * 支付状态 0待支付 1已支付
     */
    private Integer payStatus;

    /**
     * 支付链接
     */
    private String payUrl;

    /**
     * 平台支付流水号，用于模拟支付回调
     */
    private String transactionNo;
}
