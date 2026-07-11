package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mq_consume_log")
public class MqConsumeLog extends BaseEntity {

    /**
     * 消息ID
     */
    @TableField("msg_id")
    private String msgId;

    /**
     * 业务类型
     */
    private String bizType;
    /**
     * 业务唯一键
     */
    private String bizKey;
    /**
     * 消费状态
     */
    private Integer consumeStatus;
    /**
     * 消费时间
     */
    private LocalDateTime consumeTime;
    /**
     * 备注
     */
    private String remark;
}
