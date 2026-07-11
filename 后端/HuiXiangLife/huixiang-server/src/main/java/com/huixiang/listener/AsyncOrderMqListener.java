package com.huixiang.listener;

import com.huixiang.constant.MqConstant;
import com.huixiang.dto.AsyncOrderCreateDTO;
import com.huixiang.service.OrderService;
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
public class AsyncOrderMqListener {

    private final OrderService orderService;

    @RabbitListener(queues = MqConstant.ASYNC_ORDER_CREATE_QUEUE)
    public void onAsyncOrderCreate(AsyncOrderCreateDTO asyncOrderCreateDTO,
                                   Message message,
                                   Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            orderService.createSeckillOrderAsync(asyncOrderCreateDTO);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费异步创建订单消息失败, messageId={}", asyncOrderCreateDTO.getMessageId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
