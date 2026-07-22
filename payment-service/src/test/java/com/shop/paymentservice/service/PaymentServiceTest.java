package com.shop.paymentservice.service;

import com.shop.events.OrderCreatedEvent;
import com.shop.events.PaymentAuthorizedEvent;
import com.shop.events.PaymentFailedEvent;
import com.shop.events.StockReservedEvent;
import com.shop.paymentservice.domain.Payment;
import com.shop.paymentservice.gateway.MockPaymentGateway;
import com.shop.paymentservice.messaging.PaymentEventPublisher;
import com.shop.paymentservice.repository.PaymentRepository;
import com.shop.paymentservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository payments;
    @Mock ProcessedEventRepository processedEvents;
    @Mock PaymentEventPublisher publisher;
    // Real gateway: its decline rule (amount ends in .66) is what we assert on.
    MockPaymentGateway gateway = new MockPaymentGateway();

    PaymentService service() {
        return new PaymentService(payments, processedEvents, gateway, publisher);
    }

    @Test
    void orderCreated_recordsPendingPayment() {
        UUID orderId = UUID.randomUUID();
        when(processedEvents.existsById(any())).thenReturn(false);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.empty());

        service().onOrderCreated(new OrderCreatedEvent(
                UUID.randomUUID(), Instant.now(), orderId, UUID.randomUUID(),
                List.of(), new BigDecimal("42.00")));

        verify(payments).save(any(Payment.class));
    }

    @Test
    void stockReserved_authorizesNormalAmount_andPublishesAuthorized() {
        UUID orderId = UUID.randomUUID();
        Payment payment = new Payment(orderId, new BigDecimal("42.00"));
        when(processedEvents.existsById(any())).thenReturn(false);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        service().onStockReserved(new StockReservedEvent(
                UUID.randomUUID(), Instant.now(), orderId, UUID.randomUUID()));

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.AUTHORIZED);
        verify(publisher).publish(any(PaymentAuthorizedEvent.class));
    }

    @Test
    void stockReserved_declinesMagicAmount_andPublishesFailed() {
        UUID orderId = UUID.randomUUID();
        Payment payment = new Payment(orderId, new BigDecimal("10.66")); // demo decline
        when(processedEvents.existsById(any())).thenReturn(false);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        service().onStockReserved(new StockReservedEvent(
                UUID.randomUUID(), Instant.now(), orderId, UUID.randomUUID()));

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.FAILED);
        verify(publisher).publish(any(PaymentFailedEvent.class));
    }

    @Test
    void duplicateStockReserved_isIgnored() {
        when(processedEvents.existsById(any())).thenReturn(true);

        service().onStockReserved(new StockReservedEvent(
                UUID.randomUUID(), Instant.now(), UUID.randomUUID(), UUID.randomUUID()));

        verify(payments, never()).findByOrderId(any());
        verify(publisher, never()).publish(any());
    }
}
