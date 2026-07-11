package com.huixiang.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FavoriteQuery extends PageQuery {

    /**
     * 目标类型
     */
    private Integer targetType;
}