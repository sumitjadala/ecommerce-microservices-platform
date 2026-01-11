package com.sj.ecommerce.order_service.service;

import com.ecommerce.contracts.events.PaymentCompletedV1;
import com.ecommerce.contracts.events.PaymentFailedV1;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * SQS Listener for payment events using Spring Cloud AWS.
 * 
 * Listens to PaymentCompleted and PaymentFailed events published by Payment Service
 * and updates the order status accordingly.
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public PaymentEventListener(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @SqsListener(value = "${aws.sqs.payment-events-queue}")
    public void listen(@Payload String message) {
        try {
            log.info("Received SQS message: {}", message);
            
            // With Raw Message Delivery enabled, SQS receives the event JSON directly
            // Determine event type by checking for "reason" field
            if (message.contains("\"reason\"")) {
                PaymentFailedV1 event = objectMapper.readValue(message, PaymentFailedV1.class);
                handlePaymentFailed(event);
            } else {
                PaymentCompletedV1 event = objectMapper.readValue(message, PaymentCompletedV1.class);
                handlePaymentCompleted(event);
            }

            log.info("Payment event processed successfully");
        } catch (Exception e) {
            log.error("Error processing SQS message", e);
            throw new RuntimeException("Failed to process message", e);
        }
    }

    private void handlePaymentCompleted(PaymentCompletedV1 event) {
        log.info("Processing PaymentCompleted event: orderId={}, paymentId={}, eventId={}", 
                 event.getOrderId(), event.getPaymentId(), event.getEventId());

        orderService.updatePaymentStatus(event.getOrderId(), "COMPLETED")
                .ifPresentOrElse(
                    order -> log.info("Order {} payment status updated to COMPLETED", event.getOrderId()),
                    () -> log.warn("Order {} not found when processing PaymentCompleted", event.getOrderId())
                );
    }

    private void handlePaymentFailed(PaymentFailedV1 event) {
        log.info("Processing PaymentFailed event: orderId={}, paymentId={}, reason={}, eventId={}", 
                 event.getOrderId(), event.getPaymentId(), event.getReason(), event.getEventId());

        orderService.updatePaymentStatus(event.getOrderId(), "FAILED")
                .ifPresentOrElse(
                    order -> log.info("Order {} payment status updated to FAILED", event.getOrderId()),
                    () -> log.warn("Order {} not found when processing PaymentFailed", event.getOrderId())
                );
    }
}
