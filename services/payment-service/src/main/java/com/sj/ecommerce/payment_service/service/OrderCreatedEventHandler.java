package com.sj.ecommerce.payment_service.service;

import com.sj.ecommerce.payment_service.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Thin delegator used by the scheduled SQS poller.
 */
@Service
public class OrderCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventHandler.class);

    private final PaymentService paymentService;

    public OrderCreatedEventHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Processing OrderCreated event: orderId={}, eventId={}", event.getOrderId(), event.getEventId());
        paymentService.processOrderCreatedEvent(event);
    }
}
