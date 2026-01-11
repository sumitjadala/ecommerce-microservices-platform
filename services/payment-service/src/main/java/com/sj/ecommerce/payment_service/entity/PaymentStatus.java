package com.sj.ecommerce.payment_service.entity;

/**
 * Payment status values for tracking payment lifecycle.
 */
public enum PaymentStatus {
    CREATED,
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    CANCELLED
}
