package com.shop.paymentservice.service;

import com.shop.events.OrderCreatedEvent;
import com.shop.events.PaymentAuthorizedEvent;
import com.shop.events.PaymentFailedEvent;
import com.shop.events.StockReservedEvent;
import com.shop.paymentservice.domain.Payment;
import com.shop.paymentservice.domain.ProcessedEvent;
import com.shop.paymentservice.gateway.MockPaymentGateway;
import com.shop.paymentservice.messaging.PaymentEventPublisher;
import com.shop.paymentservice.repository.PaymentRepository;
import com.shop.paymentservice.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * payment-service's half of the saga:
 *   OrderCreated  -> record a PENDING payment (so we know the amount to charge)
 *   StockReserved -> actually charge; publish PaymentAuthorized / PaymentFailed
 *
 * Splitting it this way keeps payment ignorant of prices: it learns the amount
 * from OrderCreated and only charges once inventory says stock is held.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final ProcessedEventRepository processedEvents;
    private final MockPaymentGateway gateway;
    private final PaymentEventPublisher publisher;

    public PaymentService(PaymentRepository payments,
                          ProcessedEventRepository processedEvents,
                          MockPaymentGateway gateway,
                          PaymentEventPublisher publisher) {
        this.payments = payments;
        this.processedEvents = processedEvents;
        this.gateway = gateway;
        this.publisher = publisher;
    }

    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        if (alreadyProcessed(event.eventId())) return;
        if (payments.findByOrderId(event.orderId()).isEmpty()) {
            payments.save(new Payment(event.orderId(), event.totalAmount()));
            log.info("Recorded PENDING payment for order {} amount {}",
                    event.orderId(), event.totalAmount());
        }
        markProcessed(event.eventId());
    }

    @Transactional
    public void onStockReserved(StockReservedEvent event) {
        if (alreadyProcessed(event.eventId())) return;
        payments.findByOrderId(event.orderId()).ifPresentOrElse(payment -> {
            if (payment.getStatus() != Payment.Status.PENDING) {
                log.info("Payment for order {} already {}, skipping", event.orderId(), payment.getStatus());
                return;
            }
            var result = gateway.charge(payment.getOrderId(), payment.getAmount());
            if (result.authorized()) {
                payment.authorize(result.reference());
                payments.save(payment);
                publisher.publish(new PaymentAuthorizedEvent(
                        UUID.randomUUID(), Instant.now(), payment.getOrderId(),
                        payment.getId(), payment.getAmount()));
                log.info("Order {} payment AUTHORIZED", event.orderId());
            } else {
                payment.fail();
                payments.save(payment);
                publisher.publish(new PaymentFailedEvent(
                        UUID.randomUUID(), Instant.now(), payment.getOrderId(), result.declineReason()));
                log.info("Order {} payment FAILED: {}", event.orderId(), result.declineReason());
            }
        }, () -> log.warn("StockReserved for order {} but no payment record yet", event.orderId()));
        markProcessed(event.eventId());
    }

    @Transactional(readOnly = true)
    public Payment getByOrderId(UUID orderId) {
        return payments.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }

    private boolean alreadyProcessed(UUID eventId) {
        return processedEvents.existsById(eventId);
    }

    private void markProcessed(UUID eventId) {
        processedEvents.save(new ProcessedEvent(eventId));
    }
}
