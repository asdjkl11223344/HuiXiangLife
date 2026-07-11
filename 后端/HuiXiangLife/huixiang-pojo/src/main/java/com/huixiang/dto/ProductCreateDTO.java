package com.huixiang.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductCreateDTO {

    /**
     * 商户ID
     */
    @NotNull(message = "商户ID不能为空")
    private Long merchantId;

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过100位")
    private String name;

    /**
     * 商品副标题
     */
    @Size(max = 255, message = "副标题长度不能超过255位")
    private String subTitle;

    /**
     * 商品详情
     */
    private String content;

    /**
     * 封面图地址
     */
    @Size(max = 255, message = "封面图地址长度不能超过255位")
    private String coverUrl;

    /**
     * 原价
     */
    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.0", message = "原价不能小于0")
    private BigDecimal originPrice;

    /**
     * 售价
     */
    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.0", message = "售价不能小于0")
    private BigDecimal salePrice;

    /**
     * 库存
     */
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;

    /**
     * 上架开始时间
     */
    private LocalDateTime startTime;
    /**
     * 上架结束时间
     */
    private LocalDateTime endTime;
}
