package com.sj.ecommerce.payment_service.service;

import com.sj.ecommerce.payment_service.dto.CreatePaymentRequest;
import com.sj.ecommerce.payment_service.dto.PaymentResponse;
import com.sj.ecommerce.payment_service.entity.Payment;
import com.sj.ecommerce.payment_service.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse createPayment(CreatePaymentRequest req) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(req.idempotencyKey());
        if (existing.isPresent()) {
            Payment p = existing.get();
            return new PaymentResponse(p.getId(), p.getOrderId(), p.getIdempotencyKey(), p.getStatus(), p.getCreatedAt());
        }

        Payment p = new Payment(req.orderId(), req.idempotencyKey(), "SUCCESS", Instant.now());
        Payment saved = paymentRepository.save(p);
        return new PaymentResponse(saved.getId(), saved.getOrderId(), saved.getIdempotencyKey(), saved.getStatus(), saved.getCreatedAt());
    }

    public Optional<PaymentResponse> getPaymentById(Long id) {
        return paymentRepository.findById(id).map(p -> new PaymentResponse(p.getId(), p.getOrderId(), p.getIdempotencyKey(), p.getStatus(), p.getCreatedAt()));
    }
}
