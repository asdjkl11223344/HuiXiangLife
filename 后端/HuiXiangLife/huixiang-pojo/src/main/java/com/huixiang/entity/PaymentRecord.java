package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_record")
public class PaymentRecord extends BaseEntity {

    /**
     * 订单ID
     */
    private Long orderId;
    /**
     * 支付渠道
     */
    private String payChannel;
    /**
     * 支付状态
     */
    private Integer payStatus;
    /**
     * 支付流水号
     */
    private String transactionNo;
    /**
     * 支付回调内容
     */
    private String callbackContent;
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
}
