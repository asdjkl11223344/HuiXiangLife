package com.huixiang.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductDetailVO {

    /**
     * 商品ID
     */
    private Long id;
    /**
     * 商户ID
     */
    private Long merchantId;
    /**
     * 商户名称
     */
    private String merchantName;
    /**
     * 商品名称
     */
    private String name;
    /**
     * 商品副标题
     */
    private String subTitle;
    /**
     * 商品详情
     */
    private String content;
    /**
     * 封面图地址
     */
    private String coverUrl;
    /**
     * 原价
     */
    private BigDecimal originPrice;
    /**
     * 售价
     */
    private BigDecimal salePrice;
    /**
     * 库存
     */
    private Integer stock;
    /**
     * 销量
     */
    private Integer soldCount;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 上架开始时间
     */
    private LocalDateTime startTime;
    /**
     * 上架结束时间
     */
    private LocalDateTime endTime;
}
