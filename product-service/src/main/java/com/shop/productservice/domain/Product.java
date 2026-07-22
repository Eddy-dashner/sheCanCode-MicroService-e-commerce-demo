package com.shop.productservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A catalogue item. product-service is the SOURCE OF TRUTH for price: other
 * services must ask here for the current price rather than caching their own,
 * which is exactly the sync call demonstrated in this phase.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    // BigDecimal, never double, for money — avoids binary floating-point rounding.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "category_id")
    private UUID categoryId;

    // The user who created this product; used to demo the product -> user sync call.
    @Column(name = "created_by")
    private UUID createdBy;

    protected Product() { // JPA
    }

    public Product(String name, String description, BigDecimal price, UUID categoryId, UUID createdBy) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }
}
