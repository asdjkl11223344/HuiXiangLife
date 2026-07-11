package com.huixiang.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateDTO {

    /**
     * 状态
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}