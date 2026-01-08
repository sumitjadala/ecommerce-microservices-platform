package com.sj.ecommerce.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable event model for PaymentCompleted events from payment-service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCompletedEvent(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("eventVersion") String eventVersion,
        @JsonProperty("occurredAt") Instant occurredAt,
        @JsonProperty("paymentId") Long paymentId,
        @JsonProperty("orderId") Long orderId,
        @JsonProperty("userId") Long userId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("status") String status
) {
}
