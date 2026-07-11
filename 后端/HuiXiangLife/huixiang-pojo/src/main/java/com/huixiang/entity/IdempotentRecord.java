package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("idempotent_record")
public class IdempotentRecord extends BaseEntity {

    /**
     * 幂等键
     */
    @TableField("idempotent_key")
    private String idempotentKey;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 接口地址
     */
    @TableField("api_uri")
    private String apiUri;

    /**
     * 处理状态
     */
    private Integer status;
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
}
