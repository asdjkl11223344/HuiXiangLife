package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("search_log")
public class SearchLog extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 搜索关键词
     */
    private String keyword;
    /**
     * 搜索时间
     */
    private LocalDateTime searchTime;
}
