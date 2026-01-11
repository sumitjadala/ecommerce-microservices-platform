package com.sj.ecommerce.order_service.dto;

import java.util.List;

public record CreateOrderRequest(Long userId, Double amount, List<Long> productIds) {}
