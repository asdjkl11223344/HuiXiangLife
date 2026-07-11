package com.huixiang.vo;

import lombok.Data;

@Data
public class SeckillAdminStatusVO {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 秒杀库存 key
     */
    private String stockKey;

    /**
     * Redis 秒杀库存值
     */
    private Integer redisStock;

    /**
     * 是否已完成库存预热
     */
    private Boolean stockPreheated;

    /**
     * 用户ID，调试用户维度状态时返回
     */
    private Long userId;

    /**
     * 用户占位 key
     */
    private String userOrderKey;

    /**
     * 用户占位值
     */
    private String userOrderValue;

    /**
     * 秒杀结果 key
     */
    private String resultKey;

    /**
     * 秒杀结果原始值
     */
    private String resultValue;

    /**
     * 解析后的秒杀结果
     */
    private SeckillResultVO result;

    /**
     * 当前商品关联的订单映射数量
     */
    private Integer relatedOrderMappingCount;
}
