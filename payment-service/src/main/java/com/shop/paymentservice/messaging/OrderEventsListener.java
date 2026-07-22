package com.shop.paymentservice.messaging;

import com.shop.events.OrderCreatedEvent;
import com.shop.paymentservice.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes order-events to learn the amount to charge (records a PENDING payment). */
@Component
@KafkaListener(topics = "order-events", groupId = "payment-service")
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final PaymentService paymentService;

    public OrderEventsListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaHandler
    void onOrderCreated(OrderCreatedEvent event) {
        paymentService.onOrderCreated(event);
    }

    @KafkaHandler(isDefault = true)
    void onOther(Object event) {
        log.debug("Ignoring order-event type: {}", event.getClass().getSimpleName());
    }
}
