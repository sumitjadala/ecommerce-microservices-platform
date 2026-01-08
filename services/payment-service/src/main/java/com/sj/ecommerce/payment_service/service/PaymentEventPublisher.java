package com.sj.ecommerce.payment_service.service;

import com.sj.ecommerce.payment_service.event.PaymentCompletedEvent;
import com.sj.ecommerce.payment_service.event.PaymentFailedEvent;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Fire-and-forget SNS event publisher using Spring Cloud AWS SnsTemplate.
 * Publishes payment result events to SNS topic.
 */
@Service
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final SnsTemplate snsTemplate;

    @Value("${aws.sns.topic-arn}")
    private String snsTopicArn;

    public PaymentEventPublisher(SnsTemplate snsTemplate) {
        this.snsTemplate = snsTemplate;
    }

    /**
     * Publishes PaymentCompleted event to SNS.
     * Fire-and-forget, no retries.
     */
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            snsTemplate.sendNotification(snsTopicArn, event, "PaymentCompleted");
            
            log.info("Published PaymentCompleted event for orderId={}, paymentId={}", 
                    event.getOrderId(), event.getPaymentId());
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
            snsTemplate.sendNotification(snsTopicArn, event, "PaymentFailed");
            
            log.info("Published PaymentFailed event for orderId={}, reason={}", 
                    event.getOrderId(), event.getReason());
        } catch (Exception e) {
            log.error("Failed to publish PaymentFailed event: orderId={}", event.getOrderId(), e);
        }
    }
}
