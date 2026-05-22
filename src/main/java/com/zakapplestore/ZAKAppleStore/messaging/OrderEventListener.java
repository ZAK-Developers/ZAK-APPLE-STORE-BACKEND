package com.zakapplestore.ZAKAppleStore.messaging;

import com.zakapplestore.ZAKAppleStore.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class OrderEventListener {

    @RabbitListener(queues = RabbitMQConfig.ORDER_SUCCESS_QUEUE)
    public void handleOrderSuccess(Map<String, Object> payload) {
        Long orderId = Long.valueOf(payload.get("orderId").toString());
        String email = (String) payload.get("email");
        String orderNumber = (String) payload.get("orderNumber");

        log.info("Received order success event for order: {}. Triggering email and invoice generation.", orderNumber);
        
        // TODO: Implement actual email sending and invoice generation logic here
        // emailService.sendOrderConfirmation(email, orderNumber);
    }
}
