package com.huixiang.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.huixiang.constant.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper.copy();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public DirectExchange notifyExchange() {
        return new DirectExchange(MqConstant.NOTIFY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange notifyDelayExchange() {
        return new DirectExchange(MqConstant.NOTIFY_DELAY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange asyncOrderExchange() {
        return new DirectExchange(MqConstant.ASYNC_ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return new Queue(MqConstant.ORDER_TIMEOUT_QUEUE, true);
    }

    @Bean
    public Queue orderTimeoutDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", MqConstant.NOTIFY_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", MqConstant.ORDER_TIMEOUT_ROUTING_KEY);
        return new Queue(MqConstant.ORDER_TIMEOUT_DELAY_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Queue couponExpireQueue() {
        return new Queue(MqConstant.COUPON_EXPIRE_QUEUE, true);
    }

    @Bean
    public Queue couponExpireDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", MqConstant.NOTIFY_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", MqConstant.COUPON_EXPIRE_ROUTING_KEY);
        return new Queue(MqConstant.COUPON_EXPIRE_DELAY_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Queue orderStatusSyncQueue() {
        return new Queue(MqConstant.ORDER_STATUS_SYNC_QUEUE, true);
    }

    @Bean
    public Queue orderStatusSyncDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", MqConstant.NOTIFY_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", MqConstant.ORDER_STATUS_SYNC_ROUTING_KEY);
        return new Queue(MqConstant.ORDER_STATUS_SYNC_DELAY_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Queue asyncOrderCreateQueue() {
        return new Queue(MqConstant.ASYNC_ORDER_CREATE_QUEUE, true);
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(notifyExchange()).with(MqConstant.ORDER_TIMEOUT_ROUTING_KEY);
    }

    @Bean
    public Binding orderTimeoutDelayBinding() {
        return BindingBuilder.bind(orderTimeoutDelayQueue()).to(notifyDelayExchange()).with(MqConstant.ORDER_TIMEOUT_DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding couponExpireBinding() {
        return BindingBuilder.bind(couponExpireQueue()).to(notifyExchange()).with(MqConstant.COUPON_EXPIRE_ROUTING_KEY);
    }

    @Bean
    public Binding couponExpireDelayBinding() {
        return BindingBuilder.bind(couponExpireDelayQueue()).to(notifyDelayExchange()).with(MqConstant.COUPON_EXPIRE_DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding orderStatusSyncBinding() {
        return BindingBuilder.bind(orderStatusSyncQueue()).to(notifyExchange()).with(MqConstant.ORDER_STATUS_SYNC_ROUTING_KEY);
    }

    @Bean
    public Binding orderStatusSyncDelayBinding() {
        return BindingBuilder.bind(orderStatusSyncDelayQueue()).to(notifyDelayExchange()).with(MqConstant.ORDER_STATUS_SYNC_DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding asyncOrderCreateBinding() {
        return BindingBuilder.bind(asyncOrderCreateQueue()).to(asyncOrderExchange()).with(MqConstant.ASYNC_ORDER_CREATE_ROUTING_KEY);
    }
}
