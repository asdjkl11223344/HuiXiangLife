package com.huixiang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayNotifyDTO {

    /**
     * 支付渠道
     */
    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

    /**
     * 订单编号
     */
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    /**
     * 支付流水号
     */
    @NotBlank(message = "支付流水号不能为空")
    private String transactionNo;

    /**
     * 支付状态 1成功 2失败
     */
    @NotNull(message = "支付状态不能为空")
    private Integer payStatus;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 回调原文
     */
    private String callbackContent;

    /**
     * 签名
     */
    private String sign;
}