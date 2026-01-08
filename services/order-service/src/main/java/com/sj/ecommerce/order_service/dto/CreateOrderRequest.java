package com.sj.ecommerce.order_service.dto;

import java.math.BigDecimal;

public record CreateOrderRequest(Long userId, BigDecimal amount, String status) {}
