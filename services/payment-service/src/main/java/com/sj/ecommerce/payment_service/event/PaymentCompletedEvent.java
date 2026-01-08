package com.sj.ecommerce.payment_service.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable event published when a payment is successfully completed.
 */
public final class PaymentCompletedEvent {

    private final String eventId;
    private final String eventVersion;
    private final Instant occurredAt;
    private final Long paymentId;
    private final Long orderId;
    private final Long userId;
    private final BigDecimal amount;
    private final String status;

    public PaymentCompletedEvent(
            String eventId,
            String eventVersion,
            Instant occurredAt,
            Long paymentId,
            Long orderId,
            Long userId,
            BigDecimal amount,
            String status) {
        this.eventId = eventId;
        this.eventVersion = eventVersion;
        this.occurredAt = occurredAt;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
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

    public Long getPaymentId() {
        return paymentId;
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

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "PaymentCompletedEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventVersion='" + eventVersion + '\'' +
                ", occurredAt=" + occurredAt +
                ", paymentId=" + paymentId +
                ", orderId=" + orderId +
                ", userId=" + userId +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                '}';
    }
}
