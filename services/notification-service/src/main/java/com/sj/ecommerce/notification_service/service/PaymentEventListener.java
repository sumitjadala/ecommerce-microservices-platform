package com.sj.ecommerce.notification_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sj.ecommerce.notification_service.event.PaymentCompletedEvent;
import com.sj.ecommerce.notification_service.event.PaymentFailedEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

/**
 * SQS Listener for payment events using Spring Cloud AWS.
 * Replaces the manual polling approach with declarative @SqsListener annotation.
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final ObjectMapper objectMapper;
    private final PaymentEventHandler handler;

    public PaymentEventListener(ObjectMapper objectMapper, PaymentEventHandler handler) {
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
            String eventType = null;
            
            if (rootNode.has("Message")) {
                // SNS envelope: extract the actual message
                eventPayload = rootNode.get("Message").asText();
                log.info("Extracted event from SNS envelope: {}", eventPayload);
                
                // Try to get eventType from SNS MessageAttributes
                if (rootNode.has("MessageAttributes") && rootNode.get("MessageAttributes").has("eventType")) {
                    eventType = rootNode.get("MessageAttributes").get("eventType").get("Value").asText();
                }
            } else {
                // Direct message (not wrapped by SNS)
                eventPayload = messageBody;
                log.info("Processing direct message (no SNS envelope)");
                
                // Try to get eventType from message headers (Spring Cloud AWS maps SQS attributes to headers)
                Object eventTypeHeader = headers.get("eventType");
                if (eventTypeHeader != null) {
                    eventType = eventTypeHeader.toString();
                }
            }
            
            log.info("Event type: {}", eventType);
            
            // Route to appropriate handler based on event type
            if ("PaymentCompleted".equals(eventType)) {
                PaymentCompletedEvent event = objectMapper.readValue(eventPayload, PaymentCompletedEvent.class);
                handler.handlePaymentCompleted(event);
            } else if ("PaymentFailed".equals(eventType)) {
                PaymentFailedEvent event = objectMapper.readValue(eventPayload, PaymentFailedEvent.class);
                handler.handlePaymentFailed(event);
            } else {
                log.warn("Unknown event type: {}", eventType);
            }
            
            // Message is automatically deleted by Spring Cloud AWS upon successful processing
            log.info("Message processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing SQS message", e);
            // Throwing the exception will prevent message deletion and it will be retried
            throw new RuntimeException("Failed to process message", e);
        }
    }
}
