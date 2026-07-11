package com.huixiang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponUpdateDTO {

    @NotNull(message = "优惠券ID不能为空")
    private Long id;

    @NotBlank(message = "优惠券名称不能为空")
    private String name;

    @NotNull(message = "优惠券类型不能为空")
    private Integer type;

    @NotNull(message = "优惠方式不能为空")
    private Integer discountType;

    @NotNull(message = "优惠值不能为空")
    private BigDecimal discountValue;

    @NotNull(message = "使用门槛金额不能为空")
    private BigDecimal thresholdAmount;

    @NotNull(message = "库存数量不能为空")
    private Integer stock;

    @NotNull(message = "每人限领数量不能为空")
    private Integer limitPerUser;

    private Long merchantId;

    private Long productId;

    @NotNull(message = "生效时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "失效时间不能为空")
    private LocalDateTime endTime;
}