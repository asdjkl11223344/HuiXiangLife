package com.huixiang.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserCouponQuery extends PageQuery {

    /**
     * 状态
     */
    private Integer status;
}
