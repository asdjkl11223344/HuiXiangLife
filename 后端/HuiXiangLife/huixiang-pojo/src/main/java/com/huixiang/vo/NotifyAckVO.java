package com.huixiang.vo;

import lombok.Data;

@Data
public class NotifyAckVO {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 返回信息
     */
    private String message;
}