package com.huixiang.controller.notify;

import com.huixiang.dto.MqNotifyDTO;
import com.huixiang.dto.PayNotifyDTO;
import com.huixiang.result.Result;
import com.huixiang.service.NotifyService;
import com.huixiang.vo.NotifyAckVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    @PostMapping("/pay/callback")
    public Result<Boolean> payCallback(@Valid @RequestBody PayNotifyDTO payNotifyDTO) {
        Boolean result = notifyService.payCallback(payNotifyDTO);
        return Result.success(result);
    }

    @PostMapping("/mq/order-timeout")
    public Result<NotifyAckVO> orderTimeout(@Valid @RequestBody MqNotifyDTO mqNotifyDTO) {
        NotifyAckVO result = notifyService.handleOrderTimeout(mqNotifyDTO);
        return Result.success(result);
    }

    @PostMapping("/mq/coupon-expire")
    public Result<NotifyAckVO> couponExpire(@Valid @RequestBody MqNotifyDTO mqNotifyDTO) {
        NotifyAckVO result = notifyService.handleCouponExpire(mqNotifyDTO);
        return Result.success(result);
    }

    @PostMapping("/mq/order-status-sync")
    public Result<NotifyAckVO> orderStatusSync(@Valid @RequestBody MqNotifyDTO mqNotifyDTO) {
        NotifyAckVO result = notifyService.handleOrderStatusSync(mqNotifyDTO);
        return Result.success(result);
    }
}