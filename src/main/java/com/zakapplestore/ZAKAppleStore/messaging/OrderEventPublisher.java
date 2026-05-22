package com.zakapplestore.ZAKAppleStore.messaging;

import com.zakapplestore.ZAKAppleStore.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderSuccess(Long orderId, String email, String orderNumber) {
        Map<String, Object> payload = Map.of(
                "orderId", orderId,
                "email", email,
                "orderNumber", orderNumber
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY, payload);
        log.info("Published order success event for order: {}", orderNumber);
    }
}
