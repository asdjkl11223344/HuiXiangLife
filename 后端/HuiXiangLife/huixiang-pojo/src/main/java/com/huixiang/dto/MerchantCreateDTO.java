package com.huixiang.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class MerchantCreateDTO {

    /**
     * 商户名称
     */
    @NotBlank(message = "商户名称不能为空")
    @Size(max = 100, message = "商户名称长度不能超过100位")
    private String name;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 封面图地址
     */
    @Size(max = 255, message = "封面图地址长度不能超过255位")
    private String coverUrl;

    /**
     * 商户地址
     */
    @NotBlank(message = "商户地址不能为空")
    @Size(max = 255, message = "商户地址长度不能超过255位")
    private String address;

    /**
     * 联系电话
     */
    @Size(max = 20, message = "联系电话长度不能超过20位")
    private String phone;

    /**
     * 商户简介
     */
    @Size(max = 500, message = "商户简介长度不能超过500位")
    private String description;

    /**
     * 人均价格
     */
    @DecimalMin(value = "0.0", message = "人均价格不能小于0")
    private BigDecimal avgPrice;
}
