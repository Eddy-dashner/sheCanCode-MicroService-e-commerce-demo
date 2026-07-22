package com.shop.inventoryservice.messaging;

import com.shop.events.DomainEvent;
import com.shop.events.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes inventory events. We key each message by the correlation id (orderId)
 * so all events for one order land on the same partition and are therefore
 * ordered relative to each other.
 */
@Component
public class InventoryEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DomainEvent event) {
        kafkaTemplate.send(Topics.INVENTORY_EVENTS, event.correlationId().toString(), event);
    }
}
