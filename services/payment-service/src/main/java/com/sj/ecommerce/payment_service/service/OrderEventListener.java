package com.sj.ecommerce.payment_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sj.ecommerce.payment_service.event.OrderCreatedEvent;
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
    private final OrderCreatedEventHandler handler;

    public OrderEventListener(ObjectMapper objectMapper, OrderCreatedEventHandler handler) {
        this.objectMapper = objectMapper;
        this.handler = handler;
    }

    @SqsListener(value = "${aws.sqs.queue-name}")
    public void listen(Message<String> message) {
        try {
            String messageBody = message.getPayload();
            MessageHeaders headers = message.getHeaders();
            
            log.info("Received SQS message: {}", messageBody);
            log.debug("Message headers: {}", headers);
            
            // Try to detect if this is an SNS-wrapped message
            JsonNode rootNode = objectMapper.readTree(messageBody);
            String eventPayload;
            
            if (rootNode.has("Message")) {
                // SNS envelope: extract the actual message
                eventPayload = rootNode.get("Message").asText();
                log.info("Extracted event from SNS envelope: {}", eventPayload);
            } else {
                // Direct message (not wrapped by SNS)
                eventPayload = messageBody;
                log.info("Processing direct message (no SNS envelope)");
            }
            
            // Parse and handle the OrderCreatedEvent
            OrderCreatedEvent event = objectMapper.readValue(eventPayload, OrderCreatedEvent.class);
            handler.handleOrderCreatedEvent(event);
            
            // Message is automatically deleted by Spring Cloud AWS upon successful processing
            log.info("OrderCreated event processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing SQS message", e);
            // Throwing the exception will prevent message deletion and it will be retried
            throw new RuntimeException("Failed to process message", e);
        }
    }
}
