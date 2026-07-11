package com.huixiang.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogQuery extends PageQuery {

    /**
     * 操作人ID
     */
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
}
