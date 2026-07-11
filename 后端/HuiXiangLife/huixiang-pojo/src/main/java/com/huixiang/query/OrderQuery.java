package com.huixiang.query;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderQuery extends PageQuery {

    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 商户ID
     */
    private Long merchantId;
    /**
     * 商品ID
     */
    private Long productId;
    /**
     * 订单状态
     */
    private Integer status;
    /**
     * 创建开始时间
     */
    private LocalDateTime beginCreateTime;
    /**
     * 创建结束时间
     */
    private LocalDateTime endCreateTime;
    /**
     * 支付开始时间
     */
    private LocalDateTime beginPayTime;
    /**
     * 支付结束时间
     */
    private LocalDateTime endPayTime;

}
