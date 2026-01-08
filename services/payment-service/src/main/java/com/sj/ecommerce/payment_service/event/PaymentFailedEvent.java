package com.sj.ecommerce.payment_service.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable event published when a payment fails.
 */
public final class PaymentFailedEvent {

    private final String eventId;
    private final String eventVersion;
    private final Instant occurredAt;
    private final Long orderId;
    private final Long userId;
    private final BigDecimal amount;
    private final String reason;

    public PaymentFailedEvent(
            String eventId,
            String eventVersion,
            Instant occurredAt,
            Long orderId,
            Long userId,
            BigDecimal amount,
            String reason) {
        this.eventId = eventId;
        this.eventVersion = eventVersion;
        this.occurredAt = occurredAt;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "PaymentFailedEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventVersion='" + eventVersion + '\'' +
                ", occurredAt=" + occurredAt +
                ", orderId=" + orderId +
                ", userId=" + userId +
                ", amount=" + amount +
                ", reason='" + reason + '\'' +
                '}';
    }
}
