package com.huixiang.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReviewVO {

    /**
     * 评价ID
     */
    private Long id;
    /**
     * 订单ID
     */
    private Long orderId;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 用户昵称
     */
    private String userNickname;
    /**
     * 用户头像地址
     */
    private String userAvatar;
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
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
