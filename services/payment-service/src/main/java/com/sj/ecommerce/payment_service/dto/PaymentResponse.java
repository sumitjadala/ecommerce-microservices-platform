package com.sj.ecommerce.payment_service.dto;

import java.time.Instant;

public record PaymentResponse(Long id, Long orderId, Long userId, Double amount, String idempotencyKey, String status, Instant createdAt) {}
