package com.shop.orderservice.service;

import com.shop.events.OrderCancelledEvent;
import com.shop.events.OrderConfirmedEvent;
import com.shop.events.OrderCreatedEvent;
import com.shop.events.OrderLine;
import com.shop.events.StockReservationFailedEvent;
import com.shop.events.StockReservedEvent;
import com.shop.orderservice.client.ProductClient;
import com.shop.orderservice.client.ProductPrice;
import com.shop.orderservice.domain.Order;
import com.shop.orderservice.domain.OrderItem;
import com.shop.orderservice.domain.OrderStatus;
import com.shop.orderservice.domain.ProcessedEvent;
import com.shop.orderservice.messaging.OrderEventPublisher;
import com.shop.orderservice.repository.OrderRepository;
import com.shop.orderservice.repository.ProcessedEventRepository;
import com.shop.orderservice.web.dto.PlaceOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Owns the checkout saga from order-service's side. It is CHOREOGRAPHED: this
 * service reacts to inventory's events; no central coordinator tells it what to do.
 *
 * Flow (no payment yet):
 *   place() -> save CREATED, publish OrderCreated
 *   StockReserved       -> CONFIRMED, publish OrderConfirmed
 *   StockReservationFailed -> CANCELLED, publish OrderCancelled
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final ProcessedEventRepository processedEvents;
    private final ProductClient productClient;
    private final OrderEventPublisher publisher;

    public OrderService(OrderRepository orders,
                        ProcessedEventRepository processedEvents,
                        ProductClient productClient,
                        OrderEventPublisher publisher) {
        this.orders = orders;
        this.processedEvents = processedEvents;
        this.productClient = productClient;
        this.publisher = publisher;
    }

    /**
     * Place an order. For each line we fetch the AUTHORITATIVE price from
     * product-service (sync). If pricing is unavailable the circuit-breaker
     * fallback throws and the whole placement fails — we never invent a price.
     */
    @Transactional
    public Order place(UUID userId, PlaceOrderRequest request) {
        List<OrderItem> items = request.lines().stream()
                .map(line -> {
                    ProductPrice price = productClient.getPrice(line.productId());
                    return new OrderItem(line.productId(), line.quantity(), price.price());
                })
                .toList();

        Order order = orders.save(new Order(userId, items));

        List<OrderLine> eventLines = order.getItems().stream()
                .map(i -> new OrderLine(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        publisher.publish(new OrderCreatedEvent(
                UUID.randomUUID(), Instant.now(), order.getId(), userId,
                eventLines, order.getTotalAmount()));

        log.info("Order {} placed (CREATED), saga started", order.getId());
        return order;
    }

    @Transactional(readOnly = true)
    public Order getById(UUID id) {
        return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> forUser(UUID userId) {
        return orders.findByUserId(userId);
    }

    /** Stock reserved -> confirm the order (no payment step yet). */
    @Transactional
    public void onStockReserved(StockReservedEvent event) {
        if (alreadyProcessed(event.eventId())) return;
        orders.findById(event.orderId()).ifPresent(order -> {
            order.transitionTo(OrderStatus.STOCK_RESERVED);
            order.transitionTo(OrderStatus.CONFIRMED);
            orders.save(order);
            publisher.publish(new OrderConfirmedEvent(
                    UUID.randomUUID(), Instant.now(), order.getId()));
            log.info("Order {} CONFIRMED", order.getId());
        });
        markProcessed(event.eventId());
    }

    /** Stock reservation failed -> cancel the order. */
    @Transactional
    public void onStockReservationFailed(StockReservationFailedEvent event) {
        if (alreadyProcessed(event.eventId())) return;
        orders.findById(event.orderId()).ifPresent(order -> {
            order.cancel("Stock reservation failed: " + event.reason());
            orders.save(order);
            publisher.publish(new OrderCancelledEvent(
                    UUID.randomUUID(), Instant.now(), order.getId(), event.reason()));
            log.info("Order {} CANCELLED (stock)", order.getId());
        });
        markProcessed(event.eventId());
    }

    private boolean alreadyProcessed(UUID eventId) {
        if (processedEvents.existsById(eventId)) {
            log.info("Skipping duplicate event {}", eventId);
            return true;
        }
        return false;
    }

    private void markProcessed(UUID eventId) {
        processedEvents.save(new ProcessedEvent(eventId));
    }
}
