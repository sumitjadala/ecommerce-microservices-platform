package com.sj.ecommerce.payment_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sj.ecommerce.payment_service.event.PaymentCompletedEvent;
import com.sj.ecommerce.payment_service.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

/**
 * Fire-and-forget SNS event publisher.
 * Publishes payment result events to SNS topic.
 */
@Service
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sns.topic-arn}")
    private String snsTopicArn;

    public PaymentEventPublisher(SnsClient snsClient, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes PaymentCompleted event to SNS.
     * Fire-and-forget, no retries.
     */
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);
            
            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicArn)
                    .message(messageBody)
                    .messageAttributes(Map.of(
                            "eventType", MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue("PaymentCompleted")
                                    .build()
                    ))
                    .build();

            snsClient.publish(request);
            log.info("Published PaymentCompleted event for orderId={}, paymentId={}", 
                    event.getOrderId(), event.getPaymentId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PaymentCompleted event: orderId={}", event.getOrderId(), e);
        } catch (Exception e) {
            log.error("Failed to publish PaymentCompleted event: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * Publishes PaymentFailed event to SNS.
     * Fire-and-forget, no retries.
     */
    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);
            
            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicArn)
                    .message(messageBody)
                    .messageAttributes(Map.of(
                            "eventType", MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue("PaymentFailed")
                                    .build()
                    ))
                    .build();

            snsClient.publish(request);
            log.info("Published PaymentFailed event for orderId={}, reason={}", 
                    event.getOrderId(), event.getReason());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PaymentFailed event: orderId={}", event.getOrderId(), e);
        } catch (Exception e) {
            log.error("Failed to publish PaymentFailed event: orderId={}", event.getOrderId(), e);
        }
    }
}
