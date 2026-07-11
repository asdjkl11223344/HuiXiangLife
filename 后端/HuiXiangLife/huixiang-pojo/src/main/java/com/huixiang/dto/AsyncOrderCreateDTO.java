package com.huixiang.dto;

import lombok.Data;

@Data
public class AsyncOrderCreateDTO {

    /**
     * 消息ID
     */
    private String messageId;

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
     * 订单备注
     */
    private String remark;
}
