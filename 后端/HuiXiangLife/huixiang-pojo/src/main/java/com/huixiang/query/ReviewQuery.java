package com.huixiang.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewQuery extends PageQuery {

    /**
     * 商户ID
     */
    private Long merchantId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 评价状态
     */
    private Integer status;
}