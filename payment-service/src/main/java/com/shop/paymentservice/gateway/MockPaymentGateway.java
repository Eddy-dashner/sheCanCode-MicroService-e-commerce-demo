package com.shop.paymentservice.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A stand-in for a real payment provider (Stripe/Adyen/etc). DELIBERATE learning
 * mock — a real integration would be an outbound HTTP call with its own timeouts,
 * retries and idempotency keys.
 *
 * Deterministic demo rule: charges whose amount ends in .66 are DECLINED, so you
 * can reliably exercise the payment-failure / compensation path. Everything else
 * is authorised.
 */
@Component
public class MockPaymentGateway {

    public record Result(boolean authorized, String reference, String declineReason) {
    }

    public Result charge(UUID orderId, BigDecimal amount) {
        long cents = amount.movePointRight(2).longValueExact() % 100;
        if (cents == 66) {
            return new Result(false, null, "Card declined (demo rule: amount ends in .66)");
        }
        return new Result(true, "MOCK-" + UUID.randomUUID(), null);
    }
}
