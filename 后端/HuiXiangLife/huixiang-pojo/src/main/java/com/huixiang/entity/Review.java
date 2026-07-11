package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review")
public class Review extends BaseEntity {

    /**
     * 订单ID
     */
    private Long orderId;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 商户ID
     */
    private Long merchantId;
    /**
     * 商品ID
     */
    private Long productId;
    /**
     * 评分
     */
    private Integer score;
    /**
     * 评价内容
     */
    private String content;
    /**
     * 评价状态
     */
    private Integer status;
}
