package com.huixiang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class MqNotifyDTO {

    /**
     * 消息ID
     */
    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    /**
     * 业务类型
     */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    /**
     * 业务ID
     */
    @NotBlank(message = "业务ID不能为空")
    private String bizId;

    /**
     * 扩展载荷
     */
    private Map<String, Object> payload;
}