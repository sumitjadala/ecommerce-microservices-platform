package com.sj.ecommerce.order_service.dto;

import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, String status, Instant createdAt, List<Long> productIds) {}
