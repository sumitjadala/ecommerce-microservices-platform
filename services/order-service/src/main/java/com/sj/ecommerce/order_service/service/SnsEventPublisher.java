package com.sj.ecommerce.order_service.service;

import com.sj.ecommerce.order_service.event.OrderCreatedEvent;
import com.sj.ecommerce.order_service.exception.EventPublishingException;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SnsEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(SnsEventPublisher.class);

    private final SnsTemplate snsTemplate;

    @Value("${aws.sns.topic.order-events}")
    private String orderEventsTopicArn;

    public SnsEventPublisher(SnsTemplate snsTemplate) {
        this.snsTemplate = snsTemplate;
    }

    public void publish(Object event) {
        try {
            String subject = event instanceof OrderCreatedEvent ? "ORDER_CREATED" : event.getClass().getSimpleName();
            
            snsTemplate.sendNotification(orderEventsTopicArn, event, subject);
            
            logger.info("Successfully published {} to SNS", subject);

        } catch (Exception e) {
            logger.error("Failed to publish event {}", event.getClass().getSimpleName(), e);
            throw new EventPublishingException("SNS publishing failed", null, e);
        }
    }
}
