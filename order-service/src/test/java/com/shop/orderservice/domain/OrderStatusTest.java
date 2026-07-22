package com.shop.orderservice.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void legalTransitions() {
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.STOCK_RESERVED)).isTrue();
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.STOCK_RESERVED.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.STOCK_RESERVED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void illegalTransitionsAreRejected() {
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
