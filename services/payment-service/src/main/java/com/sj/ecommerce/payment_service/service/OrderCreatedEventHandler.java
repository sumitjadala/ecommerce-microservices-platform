package com.sj.ecommerce.payment_service.service;

import com.sj.ecommerce.payment_service.event.OrderCreatedEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Lightweight SQS listener that delegates processing to `PaymentService`.
 */
@Service
public class OrderCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventHandler.class);

    private final PaymentService paymentService;

    public OrderCreatedEventHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @SqsListener("${aws.sqs.queue-name}")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreated event, delegating to PaymentService: orderId={}, eventId={}", event.getOrderId(), event.getEventId());
        paymentService.processOrderCreatedEvent(event);
    }
}
