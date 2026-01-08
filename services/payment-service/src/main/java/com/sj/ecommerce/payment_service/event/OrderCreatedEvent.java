package com.sj.ecommerce.payment_service.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable local event model for OrderCreated domain events.
 * This model is NOT shared with other services.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class OrderCreatedEvent {

    private final String eventId;
    private final String eventVersion;
    private final Instant occurredAt;
    private final Long orderId;
    private final Long userId;
    private final BigDecimal amount;

    @JsonCreator
    public OrderCreatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventVersion") String eventVersion,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("amount") BigDecimal amount) {
        this.eventId = eventId;
        this.eventVersion = eventVersion;
        this.occurredAt = occurredAt;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventVersion() {
        return eventVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "OrderCreatedEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventVersion='" + eventVersion + '\'' +
                ", occurredAt=" + occurredAt +
                ", orderId=" + orderId +
                ", userId=" + userId +
                ", amount=" + amount +
                '}';
    }
}
