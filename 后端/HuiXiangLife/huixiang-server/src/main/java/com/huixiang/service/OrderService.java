package com.huixiang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.AdminOrderRefundDTO;
import com.huixiang.dto.AsyncOrderCreateDTO;
import com.huixiang.dto.OrderCreateDTO;
import com.huixiang.dto.PaymentCreateDTO;
import com.huixiang.dto.SeckillOrderCreateDTO;
import com.huixiang.query.OrderQuery;
import com.huixiang.vo.OrderDetailVO;
import com.huixiang.vo.PaymentSubmitVO;
import com.huixiang.vo.SeckillResultVO;
import com.huixiang.vo.SeckillSubmitVO;
import jakarta.validation.Valid;

public interface OrderService {

    Long create(OrderCreateDTO orderCreateDTO);

    SeckillSubmitVO createSeckill(SeckillOrderCreateDTO seckillOrderCreateDTO);

    SeckillResultVO getSeckillResult(Long productId);

    void createSeckillOrderAsync(AsyncOrderCreateDTO asyncOrderCreateDTO);

    Page<OrderDetailVO> page(OrderQuery orderQuery);

    OrderDetailVO detail(Long id);

    Boolean cancel(Long id);

    PaymentSubmitVO pay(Long id, @Valid PaymentCreateDTO paymentCreateDTO);

    Page<OrderDetailVO> adminPage(OrderQuery orderQuery);

    OrderDetailVO adminDetail(Long id);

    Boolean adminCancel(Long id);

    Boolean adminRefund(Long id, AdminOrderRefundDTO refundDTO);
}
