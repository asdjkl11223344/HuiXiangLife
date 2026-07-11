package com.huixiang.service.impl;

import com.huixiang.constant.MqConstant;
import com.huixiang.dto.AsyncOrderCreateDTO;
import com.huixiang.dto.MqNotifyDTO;
import com.huixiang.service.MqMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqMessageServiceImpl implements MqMessageService {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void send(String exchange, String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        log.debug("发送 MQ 消息成功, exchange={}, routingKey={}", exchange, routingKey);
    }

    @Override
    public void sendDelay(String exchange, String routingKey, Object payload, long delayMillis) {
        if (delayMillis <= 0L) {
            send(exchange, routingKey, payload);
            return;
        }
        rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
            message.getMessageProperties().setExpiration(String.valueOf(delayMillis));
            return message;
        });
        log.debug("发送延迟 MQ 消息成功, exchange={}, routingKey={}, delayMillis={}", exchange, routingKey, delayMillis);
    }

    @Override
    public void sendOrderTimeoutMessage(MqNotifyDTO mqNotifyDTO, long delayMillis) {
        sendDelay(MqConstant.NOTIFY_DELAY_EXCHANGE, MqConstant.ORDER_TIMEOUT_DELAY_ROUTING_KEY, mqNotifyDTO, delayMillis);
    }

    @Override
    public void sendCouponExpireMessage(MqNotifyDTO mqNotifyDTO, long delayMillis) {
        sendDelay(MqConstant.NOTIFY_DELAY_EXCHANGE, MqConstant.COUPON_EXPIRE_DELAY_ROUTING_KEY, mqNotifyDTO, delayMillis);
    }

    @Override
    public void sendOrderStatusSyncMessage(MqNotifyDTO mqNotifyDTO, long delayMillis) {
        sendDelay(MqConstant.NOTIFY_DELAY_EXCHANGE, MqConstant.ORDER_STATUS_SYNC_DELAY_ROUTING_KEY, mqNotifyDTO, delayMillis);
    }

    @Override
    public void sendAsyncOrderCreateMessage(AsyncOrderCreateDTO asyncOrderCreateDTO) {
        send(MqConstant.ASYNC_ORDER_EXCHANGE, MqConstant.ASYNC_ORDER_CREATE_ROUTING_KEY, asyncOrderCreateDTO);
    }
}
