package com.sj.ecommerce.payment_service.service;

import com.ecommerce.contracts.events.OrderCreatedV1;
import com.ecommerce.contracts.events.PaymentCompletedV1;
import com.ecommerce.contracts.events.PaymentFailedV1;
import com.sj.ecommerce.payment_service.dto.CreatePaymentRequest;
import com.sj.ecommerce.payment_service.dto.PaymentResponse;
import com.sj.ecommerce.payment_service.entity.Payment;
import com.sj.ecommerce.payment_service.entity.PaymentStatus;
import com.sj.ecommerce.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
            return new PaymentResponse(p.getId(), p.getOrderId(), p.getUserId(), p.getAmount(), p.getIdempotencyKey(), p.getStatus().name(), p.getCreatedAt());
        }

        Payment p = new Payment(req.orderId(), req.userId(), req.amount(), req.idempotencyKey(), PaymentStatus.PAID, Instant.now());
        Payment saved = paymentRepository.save(p);
        return new PaymentResponse(saved.getId(), saved.getOrderId(), saved.getUserId(), saved.getAmount(), saved.getIdempotencyKey(), saved.getStatus().name(), saved.getCreatedAt());
    }

    public Optional<PaymentResponse> getPaymentById(Long id) {
        return paymentRepository.findById(id).map(p -> new PaymentResponse(p.getId(), p.getOrderId(), p.getUserId(), p.getAmount(), p.getIdempotencyKey(), p.getStatus().name(), p.getCreatedAt()));
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

        String idempotencyKey = "order-" + event.getOrderId();

//        Double evtAmount = event.getAmount();
//        Double amount = (evtAmount != null) ? evtAmount : 0.0;
//        PaymentStatus initialStatus = (evtAmount != null) ? PaymentStatus.PAID : PaymentStatus.FAILED;

        Payment payment = new Payment(
            event.getOrderId(),
            event.getUserId(),
            event.getAmount(),
            idempotencyKey,
            PaymentStatus.PENDING,
            Instant.now()
        );

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment persisted: paymentId={}, orderId={}, status={}", savedPayment.getId(), savedPayment.getOrderId(), savedPayment.getStatus());
        if (PaymentStatus.PAID == savedPayment.getStatus()) {
                PaymentCompletedV1 completedEvent = new PaymentCompletedV1(
                    UUID.randomUUID(),
                    "1.0",
                    Instant.now(),
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getUserId(),
                    (savedPayment.getAmount() != null) ? savedPayment.getAmount() : 0.0
                );
                eventPublisher.publishPaymentCompleted(completedEvent);
        }
//        else if(PaymentStatus.FAILED == savedPayment.getStatus()) {
//            String reason = (evtAmount == null) ? "Missing amount in OrderCreated event" : "Payment processing failed";
//            PaymentFailedV1 failedEvent = new PaymentFailedV1(
//                UUID.randomUUID(),
//                "1.0",
//                Instant.now(),
//                savedPayment.getId(),
//                savedPayment.getOrderId(),
//                savedPayment.getUserId(),
//                (savedPayment.getAmount() != null) ? savedPayment.getAmount() : 0.0,
//                reason
//            );
//            eventPublisher.publishPaymentFailed(failedEvent);
//        }
    }
}
