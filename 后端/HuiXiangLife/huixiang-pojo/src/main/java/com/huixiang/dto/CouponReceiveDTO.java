package com.huixiang.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponReceiveDTO {

    /**
     * 优惠券模板ID
     */
    @NotNull(message = "优惠券模板ID不能为空")
    private Long couponTemplateId;
}
