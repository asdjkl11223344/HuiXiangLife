package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
public class OperationLog extends BaseEntity {

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    private Long operatorId;

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
}
