package com.sj.ecommerce.order_service.dto;

import java.time.Instant;

public record OrderResponse(Long id, String status, Instant createdAt) {}
