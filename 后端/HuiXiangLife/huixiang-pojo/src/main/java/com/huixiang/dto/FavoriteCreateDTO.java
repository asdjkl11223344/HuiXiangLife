package com.huixiang.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FavoriteCreateDTO {

    /**
     * 收藏目标ID
     */
    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    /**
     * 收藏目标类型
     */
    @NotNull(message = "目标类型不能为空")
    private Integer targetType;
}
