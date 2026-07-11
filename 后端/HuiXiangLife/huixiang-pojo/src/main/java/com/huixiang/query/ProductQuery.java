package com.huixiang.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductQuery extends PageQuery {

    /**
     * 商户ID
     */
    private Long merchantId;
    /**
     * 搜索关键词
     */
    private String keyword;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 最低售价
     */
    private BigDecimal minSalePrice;
    /**
     * 最高售价
     */
    private BigDecimal maxSalePrice;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
