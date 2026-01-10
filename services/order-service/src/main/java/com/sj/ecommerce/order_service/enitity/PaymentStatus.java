package com.sj.ecommerce.order_service.enitity;

/**
 * Industry-standard payment status values used to track the lifecycle
 * of a payment in the order processing flow.
 */
public enum PaymentStatus {
    // Payment has been created but not yet processed
    PENDING,

    // Payment completed successfully (captured/settled)
    PAID,

    // Payment attempt failed
    FAILED,

    // Payment was refunded
    REFUNDED,

    // Payment was cancelled before completion
    CANCELLED
}
