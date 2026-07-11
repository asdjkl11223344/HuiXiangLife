package com.huixiang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MerchantCategoryCreateDTO {

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 状态
     */
    private Integer status;
}