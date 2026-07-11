package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("favorite")
public class Favorite extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 收藏目标ID
     */
    private Long targetId;
    /**
     * 收藏目标类型
     */
    private Integer targetType;
}
