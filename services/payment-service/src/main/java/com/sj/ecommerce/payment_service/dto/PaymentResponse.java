package com.sj.ecommerce.payment_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(Long id, Long orderId, Long userId, BigDecimal amount, String idempotencyKey, String status, Instant createdAt) {}
