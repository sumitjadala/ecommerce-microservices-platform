package com.sj.ecommerce.payment_service.dto;

import java.math.BigDecimal;

public record CreatePaymentRequest(Long orderId, Long userId, BigDecimal amount, String idempotencyKey) {}
