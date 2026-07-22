package com.shop.inventoryservice.service;

import com.shop.events.LowStockEvent;
import com.shop.events.OrderCancelledEvent;
import com.shop.events.OrderCreatedEvent;
import com.shop.events.OrderLine;
import com.shop.events.StockReleasedEvent;
import com.shop.events.StockReservationFailedEvent;
import com.shop.events.StockReservedEvent;
import com.shop.inventoryservice.domain.Reservation;
import com.shop.inventoryservice.domain.StockItem;
import com.shop.inventoryservice.messaging.InventoryEventPublisher;
import com.shop.inventoryservice.repository.ProcessedEventRepository;
import com.shop.inventoryservice.repository.ReservationRepository;
import com.shop.inventoryservice.repository.StockItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock StockItemRepository stock;
    @Mock ReservationRepository reservations;
    @Mock ProcessedEventRepository processedEvents;
    @Mock InventoryEventPublisher publisher;

    InventoryService service() {
        return new InventoryService(stock, reservations, processedEvents, publisher);
    }

    private OrderCreatedEvent orderFor(UUID productId, int qty) {
        return new OrderCreatedEvent(UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
                UUID.randomUUID(), List.of(new OrderLine(productId, qty, new BigDecimal("5.00"))),
                new BigDecimal("5.00"));
    }

    @Test
    void reservesStock_andPublishesStockReserved() {
        UUID productId = UUID.randomUUID();
        StockItem item = new StockItem(productId, 10, 2);
        when(processedEvents.existsById(any())).thenReturn(false);
        when(stock.findById(productId)).thenReturn(Optional.of(item));
        when(reservations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().onOrderCreated(orderFor(productId, 3));

        assertThat(item.getAvailableQuantity()).isEqualTo(7);
        assertThat(item.getReservedQuantity()).isEqualTo(3);
        verify(publisher).publish(any(StockReservedEvent.class));
        verify(processedEvents).save(any());
    }

    @Test
    void insufficientStock_publishesFailure_andReservesNothing() {
        UUID productId = UUID.randomUUID();
        StockItem item = new StockItem(productId, 1, 0);
        when(processedEvents.existsById(any())).thenReturn(false);
        when(stock.findById(productId)).thenReturn(Optional.of(item));

        service().onOrderCreated(orderFor(productId, 5));

        assertThat(item.getReservedQuantity()).isZero();
        verify(publisher).publish(any(StockReservationFailedEvent.class));
        verify(reservations, never()).save(any());
    }

    @Test
    void duplicateEvent_isIgnored() {
        when(processedEvents.existsById(any())).thenReturn(true);

        service().onOrderCreated(orderFor(UUID.randomUUID(), 1));

        verify(publisher, never()).publish(any());
        verify(stock, never()).save(any());
    }

    @Test
    void lowStock_afterReservation_emitsLowStockEvent() {
        UUID productId = UUID.randomUUID();
        StockItem item = new StockItem(productId, 3, 2); // drops to 1 <= threshold 2
        when(processedEvents.existsById(any())).thenReturn(false);
        when(stock.findById(productId)).thenReturn(Optional.of(item));
        when(reservations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().onOrderCreated(orderFor(productId, 2));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher, org.mockito.Mockito.atLeast(2)).publish((com.shop.events.DomainEvent) captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e instanceof LowStockEvent);
    }

    @Test
    void cancellation_releasesReservation_andPublishesReleased() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StockItem item = new StockItem(productId, 7, 2);
        item.reserve(3); // available 4, reserved 3
        Reservation reservation = new Reservation(orderId, Map.of(productId, 3));
        when(processedEvents.existsById(any())).thenReturn(false);
        when(reservations.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(stock.findById(productId)).thenReturn(Optional.of(item));

        service().onOrderCancelled(new OrderCancelledEvent(
                UUID.randomUUID(), Instant.now(), orderId, "payment failed"));

        assertThat(item.getAvailableQuantity()).isEqualTo(7);
        assertThat(item.getReservedQuantity()).isZero();
        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.RELEASED);
        verify(publisher).publish(any(StockReleasedEvent.class));
    }
}
