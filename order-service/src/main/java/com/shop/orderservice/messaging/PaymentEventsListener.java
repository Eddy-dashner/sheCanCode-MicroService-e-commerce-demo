package com.shop.orderservice.messaging;

import com.shop.events.PaymentAuthorizedEvent;
import com.shop.events.PaymentFailedEvent;
import com.shop.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * NEW in Phase 6: order-service now also consumes payment-events. This listener
 * is additive — none of the existing inventory-event handling changed.
 */
@Component
@KafkaListener(topics = "payment-events", groupId = "order-service")
public class PaymentEventsListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsListener.class);

    private final OrderService orderService;

    public PaymentEventsListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaHandler
    void onPaymentAuthorized(PaymentAuthorizedEvent event) {
        orderService.onPaymentAuthorized(event);
    }

    @KafkaHandler
    void onPaymentFailed(PaymentFailedEvent event) {
        orderService.onPaymentFailed(event);
    }

    @KafkaHandler(isDefault = true)
    void onOther(Object event) {
        log.debug("Ignoring payment-event type: {}", event.getClass().getSimpleName());
    }
}
