package com.huixiang.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人昵称
     */
    private String operatorName;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 操作详情
     */
    private String detail;

    /**
     * 操作IP
     */
    private String ip;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
