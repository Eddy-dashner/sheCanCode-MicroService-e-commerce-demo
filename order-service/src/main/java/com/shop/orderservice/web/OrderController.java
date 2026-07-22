package com.shop.orderservice.web;

import com.shop.orderservice.domain.Order;
import com.shop.orderservice.service.OrderService;
import com.shop.orderservice.web.dto.OrderResponse;
import com.shop.orderservice.web.dto.PlaceOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order (starts the checkout saga). Returns immediately as CREATED.")
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request,
                                               @RequestHeader("X-User-Id") UUID userId,
                                               UriComponentsBuilder uriBuilder) {
        Order order = orderService.place(userId, request);
        var location = uriBuilder.path("/orders/{id}").buildAndExpand(order.getId()).toUri();
        return ResponseEntity.created(location).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order by id (poll this to watch the saga progress)")
    public OrderResponse get(@PathVariable UUID id) {
        return OrderResponse.from(orderService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List the authenticated caller's orders")
    public List<OrderResponse> mine(@RequestHeader("X-User-Id") UUID userId) {
        return orderService.forUser(userId).stream().map(OrderResponse::from).toList();
    }
}
