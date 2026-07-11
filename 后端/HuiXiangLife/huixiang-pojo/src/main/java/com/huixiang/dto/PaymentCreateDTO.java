package com.huixiang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentCreateDTO {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 支付渠道
     */
    @NotBlank(message = "支付渠道不能为空")
    @Size(max = 32, message = "支付渠道长度不能超过32位")
    private String payChannel;
}
