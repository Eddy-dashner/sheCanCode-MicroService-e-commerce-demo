package com.shop.productservice.service;

import com.shop.productservice.client.UserClient;
import com.shop.productservice.client.UserSummary;
import com.shop.productservice.domain.Product;
import com.shop.productservice.repository.ProductRepository;
import com.shop.productservice.web.dto.CreateProductRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository products;
    @Mock
    UserClient userClient;
    @InjectMocks
    ProductService productService;

    @Test
    void create_persistsProductWithCreator() {
        UUID creator = UUID.randomUUID();
        var request = new CreateProductRequest("Widget", "A widget", new BigDecimal("9.99"), null);
        when(products.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product saved = productService.create(request, creator);

        assertThat(saved.getName()).isEqualTo("Widget");
        assertThat(saved.getPrice()).isEqualByComparingTo("9.99");
        assertThat(saved.getCreatedBy()).isEqualTo(creator);
    }

    @Test
    void getById_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(products.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getCreator_returnsWhateverUserClientProvides_includingFallback() {
        UUID productId = UUID.randomUUID();
        UUID creator = UUID.randomUUID();
        Product product = new Product("Widget", "d", new BigDecimal("1.00"), null, creator);
        when(products.findById(productId)).thenReturn(Optional.of(product));
        // Simulate the circuit-breaker fallback kicking in.
        when(userClient.getUser(creator)).thenReturn(UserSummary.unknown(creator));

        UserSummary result = productService.getCreator(productId);

        assertThat(result.firstName()).isEqualTo("unknown");
        assertThat(result.id()).isEqualTo(creator);
    }
}
