package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_coupon")
public class UserCoupon extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 优惠券模板ID
     */
    @TableField("coupon_template_id")
    private Long couponTemplateId;

    /**
     * 使用状态
     */
    private Integer status;
    /**
     * 领取时间
     */
    private LocalDateTime receiveTime;
    /**
     * 使用时间
     */
    private LocalDateTime useTime;
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    /**
     * 关联订单ID
     */
    private Long orderId;
}
