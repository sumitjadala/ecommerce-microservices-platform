package com.sj.ecommerce.payment_service.dto;

public record CreatePaymentRequest(Long orderId, String idempotencyKey) {}
