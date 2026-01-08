package com.sj.ecommerce.notification_service.controller;

import com.sj.ecommerce.notification_service.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @PostMapping
    public ResponseEntity<Void> sendNotification(@Valid @RequestBody NotificationRequest req) {
        log.info("Notification type={} recipient={} message={}", req.type(), req.recipient(), req.message());
        return ResponseEntity.accepted().build();
    }
}
