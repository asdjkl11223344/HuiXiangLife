package com.huixiang.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FavoriteVO {

    /**
     * 收藏ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 目标类型
     */
    private Integer targetType;

    /**
     * 目标名称
     */
    private String targetName;

    /**
     * 目标封面图
     */
    private String targetCoverUrl;

    /**
     * 收藏时间
     */
    private LocalDateTime createTime;
}