package com.huixiang.vo;

import lombok.Data;

@Data
public class SeckillResultVO {

    /**
     * 秒杀结果状态码：0-无记录，1-排队中，2-成功，3-失败
     */
    private Integer statusCode;

    /**
     * 秒杀结果状态：EMPTY/PENDING/SUCCESS/FAILED
     */
    private String status;

    /**
     * 是否终态
     */
    private Boolean finished;

    /**
     * 建议下次轮询间隔，单位：毫秒
     */
    private Long nextPollIntervalMillis;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 失败原因码
     */
    private Integer failureCode;

    /**
     * 提示信息
     */
    private String message;
}
