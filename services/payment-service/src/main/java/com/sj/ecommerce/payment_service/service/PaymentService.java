package com.sj.ecommerce.payment_service.service;

import com.ecommerce.contracts.events.OrderCreatedV1;
import com.ecommerce.contracts.events.PaymentCompletedV1;
import com.ecommerce.contracts.events.PaymentFailedV1;
import com.sj.ecommerce.payment_service.dto.CreatePaymentRequest;
import com.sj.ecommerce.payment_service.dto.PaymentResponse;
import com.sj.ecommerce.payment_service.dto.RazorpayOrderResponse;
import com.sj.ecommerce.payment_service.entity.Payment;
import com.sj.ecommerce.payment_service.entity.PaymentStatus;
import com.sj.ecommerce.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Order;
import com.razorpay.Utils;
import org.json.JSONObject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret:}")
    private String razorpayWebhookSecret;

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

        Payment payment = new Payment(
            event.getOrderId(),
            event.getUserId(),
            event.getAmount(),
            idempotencyKey,
            PaymentStatus.CREATED,
            Instant.now()
        );

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment record created: paymentId={}, orderId={}, status=CREATED (awaiting user payment intent)", 
                 savedPayment.getId(), savedPayment.getOrderId());
    }

    @Transactional
    public RazorpayOrderResponse initiatePayment(Long orderId) throws RazorpayException {
        log.info("Initiating payment for orderId={}", orderId);
        
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalStateException("No payment record found for orderId=" + orderId));
        
        // Idempotency: if already has Razorpay order, return existing details
        if (payment.getRazorpayOrderId() != null && !payment.getRazorpayOrderId().isBlank()) {
            log.info("Payment already has Razorpay order, returning existing: orderId={}, razorpayOrderId={}", 
                     orderId, payment.getRazorpayOrderId());
            return new RazorpayOrderResponse(payment.getRazorpayOrderId(), payment.getRazorpayAmount(), razorpayKeyId);
        }
        
        // Only allow initiation from CREATED state
        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new IllegalStateException("Cannot initiate payment in status=" + payment.getStatus() + 
                                            ". Payment must be in CREATED state.");
        }
        
        // NOW we call Razorpay - user has explicitly clicked "Pay Now"
        createAndAttachRazorpayOrder(payment);
        
        // Update status to PENDING (payment in progress)
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);
        
        log.info("Payment initiated: orderId={}, razorpayOrderId={}, status=PENDING", 
                 orderId, payment.getRazorpayOrderId());
        
        return new RazorpayOrderResponse(payment.getRazorpayOrderId(), payment.getRazorpayAmount(), razorpayKeyId);
    }

    /**
     * Create a Razorpay order for the given payment and persist the razorpay fields.
     * This method uses the RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables.
     */
    private void createAndAttachRazorpayOrder(Payment payment) throws RazorpayException {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new IllegalStateException("Razorpay key id/secret not configured in environment variables");
        }

        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        // Amount must be in paise (integer)
        long amountPaise = 0L;
        if (payment.getAmount() != null) {
            amountPaise = Math.round(payment.getAmount() * 100);
        }

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", String.valueOf(payment.getOrderId()));
        orderRequest.put("payment_capture", 1);

        Order order = client.orders.create(orderRequest);

        String rzOrderId = order.get("id").toString();

        payment.setRazorpayOrderId(rzOrderId);
        payment.setRazorpayAmount(amountPaise);
        // Note: Status is set by the caller (initiatePayment), not here
    }

    public Optional<RazorpayOrderResponse> getRazorpayOrderForOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).map(p -> new RazorpayOrderResponse(
            p.getRazorpayOrderId(),
            p.getRazorpayAmount(),
            razorpayKeyId
        ));
    }

    /**
     * Handle Razorpay webhook for payment events. Idempotent: skips publishing if status already final.
     */
    @Transactional
    public void handleRazorpayWebhook(String payload, String signature) throws RazorpayException {
        if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank()) {
            throw new IllegalStateException("Razorpay webhook secret not configured");
        }

        // Verify signature
        Utils.verifyWebhookSignature(payload, signature, razorpayWebhookSecret);

        JSONObject body = new JSONObject(payload);
        String event = body.optString("event", "");
        JSONObject paymentEntity = body.optJSONObject("payload") != null
                ? body.getJSONObject("payload").optJSONObject("payment") != null
                    ? body.getJSONObject("payload").getJSONObject("payment").optJSONObject("entity")
                    : null
                : null;

        if (paymentEntity == null) {
            log.warn("Razorpay webhook missing payment entity, event={}", event);
            return;
        }

        String razorpayOrderId = paymentEntity.optString("order_id", null);
        Long amountPaise = paymentEntity.has("amount") ? paymentEntity.getLong("amount") : null;
        if (razorpayOrderId == null) {
            log.warn("Razorpay webhook missing order_id, event={}", event);
            return;
        }

        Optional<Payment> paymentOpt = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for razorpayOrderId={}, event={}", razorpayOrderId, event);
            return;
        }

        Payment payment = paymentOpt.get();

        if ("payment.captured".equals(event)) {
            if (PaymentStatus.PAID == payment.getStatus()) {
                log.info("Skipping duplicate payment.captured for razorpayOrderId={}", razorpayOrderId);
                return;
            }
            payment.setStatus(PaymentStatus.PAID);
            if (payment.getRazorpayAmount() == null && amountPaise != null) {
                payment.setRazorpayAmount(amountPaise);
            }
            paymentRepository.save(payment);

            PaymentCompletedV1 completedEvent = new PaymentCompletedV1(
                UUID.randomUUID(),
                "1.0",
                Instant.now(),
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount() != null ? payment.getAmount() : 0.0
            );
            eventPublisher.publishPaymentCompleted(completedEvent);
            // Order Service will receive this event via SQS and update order status
        } else if ("payment.failed".equals(event)) {
            if (PaymentStatus.FAILED == payment.getStatus()) {
                log.info("Skipping duplicate payment.failed for razorpayOrderId={}", razorpayOrderId);
                return;
            }
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            String reason = paymentEntity.optString("error_description", "Payment failed");
            PaymentFailedV1 failedEvent = new PaymentFailedV1(
                UUID.randomUUID(),
                "1.0",
                Instant.now(),
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount() != null ? payment.getAmount() : 0.0,
                reason
            );
            eventPublisher.publishPaymentFailed(failedEvent);
            // Order Service will receive this event via SQS and update order status
        } else {
            log.info("Ignoring unsupported Razorpay event type: {}", event);
        }
    }
}
