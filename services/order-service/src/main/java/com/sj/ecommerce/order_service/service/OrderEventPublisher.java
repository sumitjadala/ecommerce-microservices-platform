package com.sj.ecommerce.order_service.service;

import com.ecommerce.contracts.events.OrderCreatedV1;
import com.sj.ecommerce.order_service.exception.EventPublishingException;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);
    private final SnsTemplate snsTemplate;

    @Value("${aws.sns.topic.order-events}")
    private String orderEventsTopicArn;

    public OrderEventPublisher(SnsTemplate snsTemplate) {
        this.snsTemplate = snsTemplate;
    }

    public void publish(OrderCreatedV1 event) {
        try {
            snsTemplate.sendNotification(orderEventsTopicArn, event, "ORDER_CREATED");
            logger.info("Successfully published ORDER_CREATED to SNS");
        } catch (Exception e) {
            logger.error("Failed to publish ORDER_CREATED event", e);
            throw new EventPublishingException("SNS publishing failed", null, e);
        }
    }
}
