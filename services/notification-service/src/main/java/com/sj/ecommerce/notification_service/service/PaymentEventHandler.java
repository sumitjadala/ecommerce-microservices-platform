package com.sj.ecommerce.notification_service.service;

import com.sj.ecommerce.notification_service.event.PaymentCompletedEvent;
import com.sj.ecommerce.notification_service.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handler for payment events - delegates to notification logic.
 */
@Service
public class PaymentEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventHandler.class);

    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Processing PaymentCompleted event: paymentId={}, orderId={}, userId={}, amount={}", 
                 event.paymentId(), event.orderId(), event.userId(), event.amount());
        
        // TODO: Implement notification logic (email, SMS, push notification, etc.)
        // For now, just log the event
        log.info("Notification sent: Payment successful for order {} - Amount: {}", 
                 event.orderId(), event.amount());
    }

    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("Processing PaymentFailed event: orderId={}, userId={}, reason={}", 
                 event.orderId(), event.userId(), event.reason());
        
        // TODO: Implement notification logic (email, SMS, push notification, etc.)
        // For now, just log the event
        log.warn("Notification sent: Payment failed for order {} - Reason: {}", 
                 event.orderId(), event.reason());
    }
}
