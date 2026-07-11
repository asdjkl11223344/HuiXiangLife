package com.huixiang.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.OrderCreateDTO;
import com.huixiang.dto.PaymentCreateDTO;
import com.huixiang.dto.SeckillOrderCreateDTO;
import com.huixiang.query.OrderQuery;
import com.huixiang.result.Result;
import com.huixiang.service.OrderService;
import com.huixiang.vo.OrderDetailVO;
import com.huixiang.vo.PaymentSubmitVO;
import com.huixiang.vo.SeckillResultVO;
import com.huixiang.vo.SeckillSubmitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/order")
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody OrderCreateDTO orderCreateDTO) {
        Long id = orderService.create(orderCreateDTO);
        return Result.success(id);
    }

    @PostMapping("/seckill")
    public Result<SeckillSubmitVO> createSeckill(@Valid @RequestBody SeckillOrderCreateDTO seckillOrderCreateDTO) {
        SeckillSubmitVO seckillSubmitVO = orderService.createSeckill(seckillOrderCreateDTO);
        return Result.success("抢购请求已提交，请尽快支付", seckillSubmitVO);
    }

    @GetMapping("/seckill/result")
    public Result<SeckillResultVO> getSeckillResult(@RequestParam Long productId) {
        SeckillResultVO seckillResultVO = orderService.getSeckillResult(productId);
        return Result.success(seckillResultVO);
    }

    @GetMapping("/page")
    public Result<Page<OrderDetailVO>> page(OrderQuery orderQuery) {
        Page<OrderDetailVO> pageResult = orderService.page(orderQuery);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        OrderDetailVO orderDetailVO = orderService.detail(id);
        return Result.success(orderDetailVO);
    }

    @PostMapping("/{id}/cancel")
    public Result<Boolean> cancel(@PathVariable Long id) {
        Boolean result = orderService.cancel(id);
        return Result.success(result);
    }

    @PostMapping("/{id}/pay")
    public Result<PaymentSubmitVO> pay(@PathVariable Long id,
                                       @Valid @RequestBody PaymentCreateDTO paymentCreateDTO) {
        PaymentSubmitVO paymentSubmitVO = orderService.pay(id, paymentCreateDTO);
        return Result.success(paymentSubmitVO);
    }
}
