package com.huixiang.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CouponQuery extends PageQuery {

    /**
     * 商户ID
     */
    private Long merchantId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 优惠券类型
     */
    private Integer type;

    private Integer status;
}