package com.sj.ecommerce.order_service.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record OrderCreatedEvent(
        @JsonProperty("eventType") String eventType,
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("status") String status,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("timestamp") Instant timestamp
) {
    public static OrderCreatedEvent of(Long orderId, String status, Instant createdAt) {
        return new OrderCreatedEvent(
                "ORDER_CREATED",
                orderId,
                status,
                createdAt,
                Instant.now()
        );
    }
}
