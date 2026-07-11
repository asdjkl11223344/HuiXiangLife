package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
public class OrderItem extends BaseEntity {

    /**
     * 订单ID
     */
    private Long orderId;
    /**
     * 商品ID
     */
    private Long productId;
    /**
     * 商品名称
     */
    private String productName;
    /**
     * 商品封面图
     */
    private String productCover;
    /**
     * 成交单价
     */
    private BigDecimal salePrice;
    /**
     * 购买数量
     */
    private Integer quantity;
    /**
     * 小计金额
     */
    private BigDecimal amount;
}
