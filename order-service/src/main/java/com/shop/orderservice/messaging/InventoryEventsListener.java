package com.shop.orderservice.messaging;

import com.shop.events.StockReservationFailedEvent;
import com.shop.events.StockReservedEvent;
import com.shop.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes inventory-events and drives the order state machine forward. This is
 * the "reacting to a peer's events" half of choreography.
 */
@Component
@KafkaListener(topics = "inventory-events", groupId = "order-service")
public class InventoryEventsListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventsListener.class);

    private final OrderService orderService;

    public InventoryEventsListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaHandler
    void onStockReserved(StockReservedEvent event) {
        orderService.onStockReserved(event);
    }

    @KafkaHandler
    void onStockReservationFailed(StockReservationFailedEvent event) {
        orderService.onStockReservationFailed(event);
    }

    @KafkaHandler(isDefault = true)
    void onOther(Object event) {
        log.debug("Ignoring inventory-event type: {}", event.getClass().getSimpleName());
    }
}
