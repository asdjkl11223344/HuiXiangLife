package com.huixiang.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductUpdateDTO {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long id;

    /**
     * 商户ID
     */
    private Long merchantId;

    /**
     * 商品名称
     */
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
    @DecimalMin(value = "0.0", message = "原价不能小于0")
    private BigDecimal originPrice;

    /**
     * 售价
     */
    @DecimalMin(value = "0.0", message = "售价不能小于0")
    private BigDecimal salePrice;

    /**
     * 库存
     */
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;

    /**
     * 状态
     */
    private Integer status;
    /**
     * 上架开始时间
     */
    private LocalDateTime startTime;
    /**
     * 上架结束时间
     */
    private LocalDateTime endTime;
}
