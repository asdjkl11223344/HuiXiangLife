package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("refund_record")
public class RefundRecord extends BaseEntity {

    /**
     * 订单ID
     */
    private Long orderId;
    /**
     * 退款编号
     */
    private String refundNo;
    /**
     * 退款金额
     */
    private BigDecimal refundAmount;
    /**
     * 退款状态
     */
    private Integer refundStatus;
    /**
     * 退款原因
     */
    private String reason;
    /**
     * 申请时间
     */
    private LocalDateTime applyTime;
    /**
     * 退款时间
     */
    private LocalDateTime refundTime;
}
