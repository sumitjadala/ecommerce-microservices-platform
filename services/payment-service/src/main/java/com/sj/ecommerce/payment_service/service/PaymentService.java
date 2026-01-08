package com.sj.ecommerce.payment_service.service;

import com.ecommerce.contracts.events.OrderCreatedV1;
import com.ecommerce.contracts.events.PaymentCompletedV1;
import com.ecommerce.contracts.events.PaymentFailedV1;
import com.sj.ecommerce.payment_service.dto.CreatePaymentRequest;
import com.sj.ecommerce.payment_service.dto.PaymentResponse;
import com.sj.ecommerce.payment_service.entity.Payment;
import com.sj.ecommerce.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public PaymentResponse createPayment(CreatePaymentRequest req) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(req.idempotencyKey());
        if (existing.isPresent()) {
            Payment p = existing.get();
            return new PaymentResponse(p.getId(), p.getOrderId(), p.getUserId(), p.getAmount(), p.getIdempotencyKey(), p.getStatus(), p.getCreatedAt());
        }

        Payment p = new Payment(req.orderId(), req.userId(), req.amount(), req.idempotencyKey(), "SUCCESS", Instant.now());
        Payment saved = paymentRepository.save(p);
        return new PaymentResponse(saved.getId(), saved.getOrderId(), saved.getUserId(), saved.getAmount(), saved.getIdempotencyKey(), saved.getStatus(), saved.getCreatedAt());
    }

    public Optional<PaymentResponse> getPaymentById(Long id) {
        return paymentRepository.findById(id).map(p -> new PaymentResponse(p.getId(), p.getOrderId(), p.getUserId(), p.getAmount(), p.getIdempotencyKey(), p.getStatus(), p.getCreatedAt()));
    }

    /**
     * Process an OrderCreatedEvent idempotently and publish result events.
     * This method is transactional: payment persistence and publishing are done within the transaction.
     */
    @Transactional
    public void processOrderCreatedEvent(OrderCreatedV1 event) {
        log.info("Processing OrderCreated event in PaymentService: orderId={}, eventId={}", event.getOrderId(), event.getEventId());

        Optional<Payment> existingPayment = paymentRepository.findByOrderId(event.getOrderId());
        if (existingPayment.isPresent()) {
            log.info("Payment already exists for orderId={}, skipping duplicate", event.getOrderId());
            return;
        }

        // build payment
        String idempotencyKey = "order-" + event.getOrderId();
        String status = "SUCCESS";

        Payment payment = new Payment(
                event.getOrderId(),
                event.getUserId(),
                BigDecimal.valueOf(event.getAmount()),
                idempotencyKey,
                status,
                Instant.now()
        );

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment persisted: paymentId={}, orderId={}, status={}", savedPayment.getId(), savedPayment.getOrderId(), savedPayment.getStatus());

        // publish result
        if ("SUCCESS".equals(savedPayment.getStatus())) {
            PaymentCompletedV1 completedEvent = new PaymentCompletedV1()
                    .withEventId(UUID.randomUUID().toString())
                    .withEventVersion("1.0")
                    .withOccurredAt(java.util.Date.from(Instant.now()))
                    .withPaymentId(savedPayment.getId())
                    .withOrderId(savedPayment.getOrderId())
                    .withUserId(savedPayment.getUserId())
                    .withAmount(savedPayment.getAmount().doubleValue())
                    .withStatus(savedPayment.getStatus());
            eventPublisher.publishPaymentCompleted(completedEvent);
        } else {
            PaymentFailedV1 failedEvent = new PaymentFailedV1()
                    .withEventId(UUID.randomUUID().toString())
                    .withEventVersion("1.0")
                    .withOccurredAt(java.util.Date.from(Instant.now()))
                    .withOrderId(savedPayment.getOrderId())
                    .withUserId(savedPayment.getUserId())
                    .withAmount(savedPayment.getAmount().doubleValue())
                    .withReason("Payment processing failed");
            eventPublisher.publishPaymentFailed(failedEvent);
        }
    }
}
