package com.sj.ecommerce.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sj.ecommerce.order_service.event.OrderCreatedEvent;
import com.sj.ecommerce.order_service.exception.EventPublishingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

@Service
public class SnsEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(SnsEventPublisher.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sns.topic.order-events}")
    private String orderEventsTopicArn;

    public SnsEventPublisher(SnsClient snsClient, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }

    @Async("eventPublisherExecutor")
    public void publish(Object event) {
        try {
            String message = objectMapper.writeValueAsString(event);

            String subject = event instanceof OrderCreatedEvent ? ((OrderCreatedEvent) event).eventType() : event.getClass().getSimpleName();

            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(orderEventsTopicArn)
                    .message(message)
                    .subject(subject)
                    .build();

            PublishResponse response = snsClient.publish(publishRequest);
            logger.info("Successfully published {}. MessageId: {}", subject, response.messageId());

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event {}", event.getClass().getSimpleName(), e);
            throw new EventPublishingException("Event serialization failed", null, e);
        } catch (SnsException e) {
            logger.error("SNS publish error for event {}", event.getClass().getSimpleName(), e);
            throw new EventPublishingException("SNS publishing failed", null, e);
        }
    }
}
