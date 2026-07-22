package com.shop.productservice.service;

import com.shop.productservice.client.UserClient;
import com.shop.productservice.client.UserSummary;
import com.shop.productservice.domain.Product;
import com.shop.productservice.repository.ProductRepository;
import com.shop.productservice.web.dto.CreateProductRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository products;
    private final UserClient userClient;

    public ProductService(ProductRepository products, UserClient userClient) {
        this.products = products;
        this.userClient = userClient;
    }

    @Transactional
    public Product create(CreateProductRequest request, UUID createdBy) {
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.categoryId(),
                createdBy
        );
        return products.save(product);
    }

    @Transactional(readOnly = true)
    public Product getById(UUID id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Product> search(String nameFilter) {
        if (nameFilter == null || nameFilter.isBlank()) {
            return products.findAll();
        }
        return products.findByNameContainingIgnoreCase(nameFilter);
    }

    /**
     * The SYNC CALL under test: fetch the product's creator from user-service.
     * If user-service is unavailable this returns a fallback "unknown" summary
     * instead of throwing, because the circuit breaker short-circuits to the
     * UserClientFallback. The catalogue keeps working when identity is degraded.
     */
    @Transactional(readOnly = true)
    public UserSummary getCreator(UUID productId) {
        Product product = getById(productId);
        return userClient.getUser(product.getCreatedBy());
    }
}
