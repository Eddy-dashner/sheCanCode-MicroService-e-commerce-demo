package com.shop.inventoryservice.web;

import com.shop.inventoryservice.service.InventoryService;
import com.shop.inventoryservice.service.StockNotFoundException;
import com.shop.inventoryservice.web.dto.SetStockRequest;
import com.shop.inventoryservice.web.dto.StockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST is used ONLY for stock reads and admin seeding. Reservations are NOT a
 * REST endpoint — they happen via events (OrderCreated), because reserving stock
 * is a state change that crosses a service boundary.
 */
@RestController
@RequestMapping("/inventory")
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Read current stock for a product")
    public StockResponse get(@PathVariable UUID productId) {
        return StockResponse.from(inventoryService.getStock(productId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Set stock level for a product (admin/seed)")
    public StockResponse setStock(@Valid @RequestBody SetStockRequest request) {
        return StockResponse.from(
                inventoryService.upsertStock(request.productId(), request.quantity(), request.lowStockThreshold()));
    }

    @ExceptionHandler(StockNotFoundException.class)
    ProblemDetail handleNotFound(StockNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
