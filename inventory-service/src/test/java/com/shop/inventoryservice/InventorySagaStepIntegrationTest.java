package com.shop.inventoryservice;

import com.shop.events.OrderCreatedEvent;
import com.shop.events.OrderLine;
import com.shop.events.StockReservedEvent;
import com.shop.events.Topics;
import com.shop.inventoryservice.repository.StockItemRepository;
import com.shop.inventoryservice.domain.StockItem;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.time.Duration.ofSeconds;

/**
 * End-to-end saga step against an IN-JVM Kafka broker (no Docker): publish
 * OrderCreated -> inventory reserves -> StockReserved appears on inventory-events,
 * and the stock in the DB is decremented.
 */
@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {Topics.ORDER_EVENTS, Topics.INVENTORY_EVENTS},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class InventorySagaStepIntegrationTest {

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    EmbeddedKafkaBroker broker;
    @Autowired
    StockItemRepository stockRepo;

    @Test
    void orderCreated_reservesStock_andEmitsStockReserved() {
        UUID productId = UUID.randomUUID();
        stockRepo.save(new StockItem(productId, 10, 2));

        // A test consumer to observe what inventory-service publishes.
        var consumerProps = KafkaTestUtils.consumerProps("test-observer", "true", broker);
        consumerProps.put("key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProps.put("value.deserializer", JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.shop.events");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, StockReservedEvent.class.getName());

        UUID orderId = UUID.randomUUID();
        var order = new OrderCreatedEvent(UUID.randomUUID(), Instant.now(), orderId,
                UUID.randomUUID(), List.of(new OrderLine(productId, 4, new BigDecimal("5.00"))),
                new BigDecimal("20.00"));

        try (Consumer<String, Object> consumer =
                     new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of(Topics.INVENTORY_EVENTS));

            kafkaTemplate.send(Topics.ORDER_EVENTS, orderId.toString(), order);

            ConsumerRecord<String, Object> record =
                    KafkaTestUtils.getSingleRecord(consumer, Topics.INVENTORY_EVENTS, ofSeconds(15));
            assertThat(record.value()).isInstanceOf(StockReservedEvent.class);
            assertThat(((StockReservedEvent) record.value()).orderId()).isEqualTo(orderId);
        }

        await().atMost(ofSeconds(5)).untilAsserted(() ->
                assertThat(stockRepo.findById(productId))
                        .get()
                        .satisfies(item -> {
                            assertThat(item.getAvailableQuantity()).isEqualTo(6);
                            assertThat(item.getReservedQuantity()).isEqualTo(4);
                        }));
    }
}
