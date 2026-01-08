package com.sj.ecommerce.notification_service.service;

import com.ecommerce.contracts.events.PaymentCompletedV1;
import com.ecommerce.contracts.events.PaymentFailedV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications based on payment events.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendPaymentSuccessNotification(PaymentCompletedV1 event) {
        log.info("Processing PaymentCompleted event: paymentId={}, orderId={}, userId={}, amount={}", 
                 event.getPaymentId(), event.getOrderId(), event.getUserId(), event.getAmount());
        
        // TODO: Implement notification logic (email, SMS, push notification, etc.)
        // For now, just log the event
        log.info("Notification sent: Payment successful for order {} - Amount: {}", 
                 event.getOrderId(), event.getAmount());
    }

    public void sendPaymentFailureNotification(PaymentFailedV1 event) {
        log.warn("Processing PaymentFailed event: orderId={}, userId={}, reason={}", 
                 event.getOrderId(), event.getUserId(), event.getReason());
        
        // TODO: Implement notification logic (email, SMS, push notification, etc.)
        // For now, just log the event
        log.warn("Notification sent: Payment failed for order {} - Reason: {}", 
                 event.getOrderId(), event.getReason());
    }
}
