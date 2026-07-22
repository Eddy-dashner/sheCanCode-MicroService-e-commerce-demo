package com.shop.orderservice.messaging;

import com.shop.events.DomainEvent;
import com.shop.events.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes order events to the order-events topic, keyed by orderId. */
@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DomainEvent event) {
        kafkaTemplate.send(Topics.ORDER_EVENTS, event.correlationId().toString(), event);
    }
}
