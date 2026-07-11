package com.huixiang.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MerchantDetailVO {

    /**
     * 商户ID
     */
    private Long id;
    /**
     * 商户名称
     */
    private String name;
    /**
     * 分类ID
     */
    private Long categoryId;
    /**
     * 分类名称
     */
    private String categoryName;
    /**
     * 封面图地址
     */
    private String coverUrl;
    /**
     * 商户地址
     */
    private String address;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 商户简介
     */
    private String description;
    /**
     * 评分
     */
    private BigDecimal score;
    /**
     * 人均价格
     */
    private BigDecimal avgPrice;
    /**
     * 状态
     */
    private Integer status;
}
