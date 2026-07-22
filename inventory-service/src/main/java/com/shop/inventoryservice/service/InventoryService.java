package com.shop.inventoryservice.service;

import com.shop.events.OrderCancelledEvent;
import com.shop.events.OrderCreatedEvent;
import com.shop.events.OrderLine;
import com.shop.events.StockReleasedEvent;
import com.shop.events.StockReservationFailedEvent;
import com.shop.events.StockReservedEvent;
import com.shop.inventoryservice.domain.ProcessedEvent;
import com.shop.inventoryservice.domain.Reservation;
import com.shop.inventoryservice.domain.StockItem;
import com.shop.inventoryservice.messaging.InventoryEventPublisher;
import com.shop.inventoryservice.repository.ProcessedEventRepository;
import com.shop.inventoryservice.repository.ReservationRepository;
import com.shop.inventoryservice.repository.StockItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core stock logic. Both event handlers are IDEMPOTENT: they record each handled
 * eventId and no-op on a replay. The whole handler runs in one DB transaction, so
 * the stock change, the reservation, and the processed-event marker all commit
 * together or not at all.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final StockItemRepository stock;
    private final ReservationRepository reservations;
    private final ProcessedEventRepository processedEvents;
    private final InventoryEventPublisher publisher;

    public InventoryService(StockItemRepository stock,
                            ReservationRepository reservations,
                            ProcessedEventRepository processedEvents,
                            InventoryEventPublisher publisher) {
        this.stock = stock;
        this.reservations = reservations;
        this.processedEvents = processedEvents;
        this.publisher = publisher;
    }

    /** Admin/seed helper — set the stock level for a product. */
    @Transactional
    public StockItem upsertStock(UUID productId, int quantity, int threshold) {
        StockItem item = stock.findById(productId)
                .orElseGet(() -> new StockItem(productId, 0, threshold));
        // Reset to the given absolute quantity for simplicity in the learning build.
        StockItem replacement = new StockItem(productId, quantity, threshold);
        stock.save(replacement);
        return replacement;
    }

    @Transactional(readOnly = true)
    public StockItem getStock(UUID productId) {
        return stock.findById(productId)
                .orElseThrow(() -> new StockNotFoundException(productId));
    }

    /**
     * React to OrderCreated: try to reserve every line. On success publish
     * StockReserved; if any line is short publish StockReservationFailed and
     * reserve nothing (all-or-nothing).
     */
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        if (alreadyProcessed(event.eventId())) {
            log.info("Skipping duplicate OrderCreated event {}", event.eventId());
            return;
        }

        Map<UUID, Integer> wanted = new LinkedHashMap<>();
        for (OrderLine line : event.lines()) {
            wanted.merge(line.productId(), line.quantity(), Integer::sum);
        }

        List<UUID> unavailable = new ArrayList<>();
        List<StockItem> items = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : wanted.entrySet()) {
            StockItem item = stock.findById(entry.getKey()).orElse(null);
            if (item == null || !item.canReserve(entry.getValue())) {
                unavailable.add(entry.getKey());
            } else {
                items.add(item);
            }
        }

        if (!unavailable.isEmpty()) {
            publisher.publish(new StockReservationFailedEvent(
                    UUID.randomUUID(), Instant.now(), event.orderId(),
                    "Insufficient stock", unavailable));
            markProcessed(event.eventId());
            return;
        }

        // All lines available: apply the holds and persist the reservation.
        for (StockItem item : items) {
            item.reserve(wanted.get(item.getProductId()));
            stock.save(item);
        }
        Reservation reservation = reservations.save(new Reservation(event.orderId(), wanted));

        publisher.publish(new StockReservedEvent(
                UUID.randomUUID(), Instant.now(), event.orderId(), reservation.getId()));

        emitLowStockWhereNeeded(items);
        markProcessed(event.eventId());
    }

    /**
     * React to OrderCancelled: release the reservation for that order (compensation)
     * and publish StockReleased.
     */
    @Transactional
    public void onOrderCancelled(OrderCancelledEvent event) {
        if (alreadyProcessed(event.eventId())) {
            log.info("Skipping duplicate OrderCancelled event {}", event.eventId());
            return;
        }
        reservations.findByOrderId(event.orderId()).ifPresent(reservation -> {
            if (reservation.getStatus() == Reservation.Status.ACTIVE) {
                reservation.getLines().forEach((productId, qty) ->
                        stock.findById(productId).ifPresent(item -> {
                            item.release(qty);
                            stock.save(item);
                        }));
                reservation.markReleased();
                reservations.save(reservation);
                publisher.publish(new StockReleasedEvent(
                        UUID.randomUUID(), Instant.now(), event.orderId(), reservation.getId()));
            }
        });
        markProcessed(event.eventId());
    }

    /**
     * React to OrderConfirmed: the sale is final, so the reservation becomes
     * CONFIRMED. The reserved units stay reserved (they're now sold); we simply
     * stop treating the hold as releasable.
     */
    @Transactional
    public void onOrderConfirmed(com.shop.events.OrderConfirmedEvent event) {
        if (alreadyProcessed(event.eventId())) {
            log.info("Skipping duplicate OrderConfirmed event {}", event.eventId());
            return;
        }
        reservations.findByOrderId(event.orderId()).ifPresent(reservation -> {
            if (reservation.getStatus() == Reservation.Status.ACTIVE) {
                reservation.markConfirmed();
                reservations.save(reservation);
            }
        });
        markProcessed(event.eventId());
    }

    private void emitLowStockWhereNeeded(List<StockItem> items) {
        for (StockItem item : items) {
            if (item.isLow()) {
                publisher.publish(new com.shop.events.LowStockEvent(
                        UUID.randomUUID(), Instant.now(),
                        item.getProductId(), item.getAvailableQuantity(), item.getLowStockThreshold()));
            }
        }
    }

    private boolean alreadyProcessed(UUID eventId) {
        return processedEvents.existsById(eventId);
    }

    private void markProcessed(UUID eventId) {
        processedEvents.save(new ProcessedEvent(eventId));
    }
}
