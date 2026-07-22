package com.shop.inventoryservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

/**
 * Stock for one product. Keeps 'available' and 'reserved' separate: reserving
 * stock moves units from available to reserved (a soft hold) without shipping
 * them, so two concurrent orders can't both sell the last unit.
 *
 * The @Version field gives optimistic locking — if two reservations race, one
 * commit wins and the other retries, preventing oversell.
 */
@Entity
@Table(name = "stock_items")
public class StockItem {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold;

    @Version
    private long version;

    protected StockItem() { // JPA
    }

    public StockItem(UUID productId, int availableQuantity, int lowStockThreshold) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean canReserve(int quantity) {
        return availableQuantity >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient stock for product " + productId);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    /** Compensating action: give the units back to available. */
    public void release(int quantity) {
        int toRelease = Math.min(quantity, reservedQuantity);
        reservedQuantity -= toRelease;
        availableQuantity += toRelease;
    }

    public boolean isLow() {
        return availableQuantity <= lowStockThreshold;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }
}
