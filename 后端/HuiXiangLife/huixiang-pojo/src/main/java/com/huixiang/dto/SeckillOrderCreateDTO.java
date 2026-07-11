package com.huixiang.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SeckillOrderCreateDTO {

    /**
     * 商户ID
     */
    @NotNull(message = "商户ID不能为空")
    private Long merchantId;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 订单备注
     */
    @Size(max = 255, message = "备注长度不能超过255位")
    private String remark;
}
