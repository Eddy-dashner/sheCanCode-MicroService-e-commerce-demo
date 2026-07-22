package com.shop.paymentservice.messaging;

import com.shop.events.StockReservedEvent;
import com.shop.paymentservice.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes inventory-events. StockReserved is payment-service's cue to actually
 * charge — we only take money once stock is confirmed held.
 */
@Component
@KafkaListener(topics = "inventory-events", groupId = "payment-service")
public class InventoryEventsListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventsListener.class);

    private final PaymentService paymentService;

    public InventoryEventsListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaHandler
    void onStockReserved(StockReservedEvent event) {
        paymentService.onStockReserved(event);
    }

    @KafkaHandler(isDefault = true)
    void onOther(Object event) {
        log.debug("Ignoring inventory-event type: {}", event.getClass().getSimpleName());
    }
}
