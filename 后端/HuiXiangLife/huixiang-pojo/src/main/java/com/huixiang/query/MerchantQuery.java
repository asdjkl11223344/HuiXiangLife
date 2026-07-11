package com.huixiang.query;

import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantQuery extends PageQuery {

    /**
     * 分类ID
     */
    private Long categoryId;
    /**
     * 搜索关键词
     */
    private String keyword;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 最低评分
     */
    private BigDecimal minScore;
    /**
     * 最高人均价格
     */
    private BigDecimal maxAvgPrice;
}
