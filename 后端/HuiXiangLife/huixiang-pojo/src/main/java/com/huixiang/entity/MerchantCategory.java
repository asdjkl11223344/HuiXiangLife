package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_category")
public class MerchantCategory extends BaseEntity {

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
