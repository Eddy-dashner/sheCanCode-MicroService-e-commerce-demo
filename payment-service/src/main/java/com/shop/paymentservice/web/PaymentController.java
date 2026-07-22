package com.shop.paymentservice.web;

import com.shop.paymentservice.domain.Payment;
import com.shop.paymentservice.service.PaymentNotFoundException;
import com.shop.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payments are DRIVEN BY EVENTS, not by a public write API — there is no
 * "POST /payments". REST here is read-only: check a payment's status by order.
 */
@RestController
@RequestMapping("/payments")
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public record PaymentView(UUID id, UUID orderId, BigDecimal amount, String status, String gatewayReference) {
        static PaymentView from(Payment p) {
            return new PaymentView(p.getId(), p.getOrderId(), p.getAmount(),
                    p.getStatus().name(), p.getGatewayReference());
        }
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get the payment for an order")
    public PaymentView byOrder(@PathVariable UUID orderId) {
        return PaymentView.from(paymentService.getByOrderId(orderId));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    ProblemDetail handleNotFound(PaymentNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
