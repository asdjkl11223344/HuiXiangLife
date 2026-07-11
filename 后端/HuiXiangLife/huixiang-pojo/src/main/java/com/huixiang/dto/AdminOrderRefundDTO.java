package com.huixiang.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminOrderRefundDTO {

    /**
     * 退款金额
     */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    @NotNull(message = "退款原因不能为空")
    @Size(max = 255, message = "退款原因长度不能超过255位")
    private String reason;
}