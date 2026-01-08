package com.sj.ecommerce.notification_service.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationRequest(
        @NotBlank String type,
        @NotBlank String recipient,
        @NotBlank String message
) {}
