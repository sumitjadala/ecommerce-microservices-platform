package com.sj.ecommerce.notification_service.service;

import com.ecommerce.contracts.events.PaymentCompletedV1;
import com.ecommerce.contracts.events.PaymentFailedV1;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SQS listener for payment events.
 *
 * Uses Spring Cloud AWS 4.x implicit acknowledgment model:
 * - Successful execution → message deleted
 * - Exception thrown → message retried / DLQ
 *
 * Assumes:
 * - Raw SNS message delivery is enabled
 * - Event contracts are enforced via generated records
 * - NotificationService is idempotent
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final NotificationService notificationService;

    public PaymentEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @SqsListener("${aws.sqs.queue-name}")
    public void listen(Object event) {

        try {
            if (event instanceof PaymentCompletedV1 completed) {
                log.info(
                        "Received PaymentCompleted event. paymentId={}, orderId={}",
                        completed.getPaymentId(),
                        completed.getOrderId()
                );

                notificationService.sendPaymentSuccessNotification(completed);
                return;
            }

            if (event instanceof PaymentFailedV1 failed) {
                log.info(
                        "Received PaymentFailed event. orderId={}", failed.getOrderId()
                );

                notificationService.sendPaymentFailureNotification(failed);
                return;
            }

            // If we reached here, the payload matched no known contract
            throw new IllegalStateException(
                    "Unsupported payment event type: " + event.getClass().getName()
            );

        } catch (Exception ex) {
            log.error("Failed to process payment event", ex);
            // Throwing exception signals failure to Spring Cloud AWS:
            // message will be retried and eventually sent to DLQ
            throw ex;
        }
    }
}
