package com.sj.ecommerce.notification_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sj.ecommerce.notification_service.event.PaymentCompletedEvent;
import com.sj.ecommerce.notification_service.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.Map;

@Component
public class SqsMessagePoller {

    private static final Logger log = LoggerFactory.getLogger(SqsMessagePoller.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final PaymentEventHandler handler;
    private final String queueName;

    public SqsMessagePoller(SqsClient sqsClient, ObjectMapper objectMapper, PaymentEventHandler handler,
                            @Value("${aws.sqs.queue-name}") String queueName) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.handler = handler;
        this.queueName = queueName;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.poll-interval-ms:5000}")
    public void poll() {
        try {
            String queueUrl = sqsClient.getQueueUrl(r -> r.queueName(queueName)).queueUrl();

            ReceiveMessageRequest req = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(5)
                    .waitTimeSeconds(20)
                    .messageAttributeNames("All")
                    .build();

            List<Message> messages = sqsClient.receiveMessage(req).messages();
            if (messages.isEmpty()) return;

            for (Message m : messages) {
                try {
                    String messageBody = m.body();
                    log.info("Raw SQS message body: {}", messageBody);
                    
                    // Try to detect if this is an SNS-wrapped message
                    JsonNode rootNode = objectMapper.readTree(messageBody);
                    String eventPayload;
                    Map<String, MessageAttributeValue> attributes = m.messageAttributes();
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
                        
                        // Get eventType from SQS message attributes
                        if (attributes.containsKey("eventType")) {
                            eventType = attributes.get("eventType").stringValue();
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

                    // delete only after successful processing
                    sqsClient.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(m.receiptHandle())
                            .build());
                } catch (Exception e) {
                    log.error("Failed to process SQS message, leaving it for retry: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.warn("SQS poll failed: {}", e.getMessage());
        }
    }
}
