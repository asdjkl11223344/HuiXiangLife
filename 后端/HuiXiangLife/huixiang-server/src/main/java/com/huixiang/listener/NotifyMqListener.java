package com.huixiang.listener;

import com.huixiang.constant.MqConstant;
import com.huixiang.dto.MqNotifyDTO;
import com.huixiang.service.NotifyService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotifyMqListener {

    private final NotifyService notifyService;

    @RabbitListener(queues = MqConstant.ORDER_TIMEOUT_QUEUE)
    public void onOrderTimeout(MqNotifyDTO mqNotifyDTO, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            notifyService.handleOrderTimeout(mqNotifyDTO);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费订单超时消息失败, messageId={}", mqNotifyDTO.getMessageId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = MqConstant.COUPON_EXPIRE_QUEUE)
    public void onCouponExpire(MqNotifyDTO mqNotifyDTO, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            notifyService.handleCouponExpire(mqNotifyDTO);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费优惠券过期消息失败, messageId={}", mqNotifyDTO.getMessageId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = MqConstant.ORDER_STATUS_SYNC_QUEUE)
    public void onOrderStatusSync(MqNotifyDTO mqNotifyDTO, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            notifyService.handleOrderStatusSync(mqNotifyDTO);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费订单状态同步消息失败, messageId={}", mqNotifyDTO.getMessageId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
