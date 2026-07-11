package com.huixiang.vo;

import lombok.Data;

@Data
public class SeckillSubmitVO {

    /**
     * 秒杀请求ID
     */
    private String requestId;

    /**
     * 建议轮询间隔，单位：毫秒
     */
    private Long pollIntervalMillis;

    /**
     * 提示信息
     */
    private String message;
}
