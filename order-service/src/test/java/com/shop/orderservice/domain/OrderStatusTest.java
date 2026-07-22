package com.shop.orderservice.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void legalTransitions() {
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.STOCK_RESERVED)).isTrue();
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.STOCK_RESERVED.canTransitionTo(OrderStatus.PAYMENT_PENDING)).isTrue();
        assertThat(OrderStatus.STOCK_RESERVED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void illegalTransitionsAreRejected() {
        // Stock alone can no longer confirm — payment sits in between.
        assertThat(OrderStatus.STOCK_RESERVED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
    }

    @Test
    void terminalStates() {
        assertThat(OrderStatus.CONFIRMED.isTerminal()).isTrue();
        assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(OrderStatus.CREATED.isTerminal()).isFalse();
    }
}
