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
        log.info("Payment persisted: paymentId={}, orderId={}, status={}", savedPayment.getId(), savedPayment.getOrderId(), savedPayment.getStatus());

        // Create Razorpay order (backend-only) and persist razorpay details
        try {
            createAndAttachRazorpayOrder(savedPayment);
            log.info("Razorpay order created and attached: paymentId={}, razorpayOrderId={}", savedPayment.getId(), savedPayment.getRazorpayOrderId());
        } catch (Exception ex) {
            log.error("Failed to create Razorpay order for orderId={}: {}", event.getOrderId(), ex.getMessage(), ex);
            // mark payment as FAILED if razorpay creation fails
            savedPayment.setStatus(PaymentStatus.FAILED);
        }
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
        payment.setStatus(PaymentStatus.CREATED);

        // Explicit save so changes are persisted even if this method is called outside the original transaction scope
        paymentRepository.save(payment);
    }

    public Optional<RazorpayOrderResponse> getRazorpayOrderForOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).map(p -> new RazorpayOrderResponse(
            p.getRazorpayOrderId(),
            p.getRazorpayAmount(),
            razorpayKeyId
        ));
    }
}
