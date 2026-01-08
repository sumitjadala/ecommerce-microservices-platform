package com.sj.ecommerce.order_service.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("eventVersion") String eventVersion,
        @JsonProperty("occurredAt") Instant occurredAt,
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("userId") Long userId,
        @JsonProperty("amount") BigDecimal amount
) {
    public static OrderCreatedEvent of(Long orderId, Long userId, BigDecimal amount) {
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                "1.0",
                Instant.now(),
                orderId,
                userId,
                amount
        );
    }
}
