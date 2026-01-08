package com.sj.ecommerce.payment_service.service;

import com.ecommerce.contracts.events.OrderCreatedV1;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

/**
 * SQS Listener for order events using Spring Cloud AWS.
 * Replaces the manual polling approach with declarative @SqsListener annotation.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    public OrderEventListener(ObjectMapper objectMapper, PaymentService paymentService) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
    }

    @SqsListener(value = "${aws.sqs.queue-name}")
    public void listen(Message<String> message) {
        try {
            String messageBody = message.getPayload();
            log.info("Received SQS message: {}", messageBody);
            
            OrderCreatedV1 event = objectMapper.readValue(messageBody, OrderCreatedV1.class);
            paymentService.processOrderCreatedEvent(event);
            
            log.info("OrderCreated event processed successfully");
        } catch (Exception e) {
            log.error("Error processing SQS message", e);
            throw new RuntimeException("Failed to process message", e);
        }
    }
}
