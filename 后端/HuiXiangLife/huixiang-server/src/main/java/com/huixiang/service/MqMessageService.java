package com.huixiang.service;

import com.huixiang.dto.AsyncOrderCreateDTO;
import com.huixiang.dto.MqNotifyDTO;

public interface MqMessageService {

    void send(String exchange, String routingKey, Object payload);

    void sendDelay(String exchange, String routingKey, Object payload, long delayMillis);

    void sendOrderTimeoutMessage(MqNotifyDTO mqNotifyDTO, long delayMillis);

    void sendCouponExpireMessage(MqNotifyDTO mqNotifyDTO, long delayMillis);

    void sendOrderStatusSyncMessage(MqNotifyDTO mqNotifyDTO, long delayMillis);

    void sendAsyncOrderCreateMessage(AsyncOrderCreateDTO asyncOrderCreateDTO);
}
