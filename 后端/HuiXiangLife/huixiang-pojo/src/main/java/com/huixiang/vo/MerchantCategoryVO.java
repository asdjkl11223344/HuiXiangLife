package com.huixiang.vo;

import lombok.Data;

@Data
public class MerchantCategoryVO {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
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