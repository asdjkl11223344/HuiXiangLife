package com.huixiang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantCategoryUpdateDTO {

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long id;

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