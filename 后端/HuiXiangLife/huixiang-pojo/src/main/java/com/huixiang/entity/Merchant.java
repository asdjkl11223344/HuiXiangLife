package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant")
public class Merchant extends BaseEntity {

    /**
     * 商户名称
     */
    private String name;
    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 封面图地址
     */
    @TableField("cover_url")
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
