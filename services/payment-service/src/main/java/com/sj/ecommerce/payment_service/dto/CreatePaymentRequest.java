package com.sj.ecommerce.payment_service.dto;

public record CreatePaymentRequest(Long orderId, Long userId, Double amount, String idempotencyKey) {}
