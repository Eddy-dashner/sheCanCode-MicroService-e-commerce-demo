package com.shop.inventoryservice.messaging;

import com.shop.events.OrderCancelledEvent;
import com.shop.events.OrderConfirmedEvent;
import com.shop.events.OrderCreatedEvent;
import com.shop.inventoryservice.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order-events. A class-level @KafkaListener plus @KafkaHandler methods
 * routes each message to the method matching its deserialized type — the type is
 * taken from the JSON type header the producer sets. The heavy lifting (and
 * idempotency) lives in InventoryService; this class is just the messaging adapter.
 */
@Component
@KafkaListener(topics = "order-events", groupId = "inventory-service")
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final InventoryService inventoryService;

    public OrderEventsListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaHandler
    void onOrderCreated(OrderCreatedEvent event) {
        inventoryService.onOrderCreated(event);
    }

    @KafkaHandler
    void onOrderCancelled(OrderCancelledEvent event) {
        inventoryService.onOrderCancelled(event);
    }

    @KafkaHandler
    void onOrderConfirmed(OrderConfirmedEvent event) {
        inventoryService.onOrderConfirmed(event);
    }

    /** Any order-event type we don't care about is ignored, not an error. */
    @KafkaHandler(isDefault = true)
    void onOther(Object event) {
        log.debug("Ignoring unhandled order-event type: {}", event.getClass().getSimpleName());
    }
}
