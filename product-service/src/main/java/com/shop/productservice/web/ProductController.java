package com.shop.productservice.web;

import com.shop.productservice.client.UserSummary;
import com.shop.productservice.domain.Product;
import com.shop.productservice.service.ProductService;
import com.shop.productservice.web.dto.CreateProductRequest;
import com.shop.productservice.web.dto.PriceResponse;
import com.shop.productservice.web.dto.ProductResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List/search products (public)")
    public List<ProductResponse> list(@RequestParam(required = false) String name) {
        return productService.search(name).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id (public)")
    public ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.from(productService.getById(id));
    }

    @GetMapping("/{id}/price")
    @Operation(summary = "Authoritative current price — the source of truth other services query")
    public PriceResponse price(@PathVariable UUID id) {
        Product product = productService.getById(id);
        return new PriceResponse(product.getId(), product.getPrice());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product (admin). Records the creator from the gateway identity header.")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request,
                                                  @RequestHeader("X-User-Id") UUID createdBy,
                                                  UriComponentsBuilder uriBuilder) {
        Product created = productService.create(request, createdBy);
        var location = uriBuilder.path("/products/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(created));
    }

    /**
     * Demonstrates the sync call + circuit breaker: returns the product's creator,
     * fetched live from user-service. Stop user-service and hit this repeatedly to
     * watch the circuit open (the response degrades to an "unknown" creator).
     */
    @GetMapping("/{id}/creator")
    @Operation(summary = "Get a product's creator via a live call to user-service (circuit-breaker demo)")
    public UserSummary creator(@PathVariable UUID id) {
        return productService.getCreator(id);
    }
}
