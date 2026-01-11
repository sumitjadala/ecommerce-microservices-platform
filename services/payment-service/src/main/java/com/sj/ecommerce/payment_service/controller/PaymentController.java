package com.sj.ecommerce.payment_service.controller;

import com.sj.ecommerce.payment_service.dto.CreatePaymentRequest;
import com.sj.ecommerce.payment_service.dto.PaymentResponse;
import com.sj.ecommerce.payment_service.service.PaymentService;
import com.sj.ecommerce.payment_service.dto.RazorpayOrderResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody CreatePaymentRequest req) {
        PaymentResponse resp = paymentService.createPayment(req);
        return ResponseEntity.created(URI.create("/payments/" + resp.id())).body(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}/razorpay")
    public ResponseEntity<RazorpayOrderResponse> getRazorpayOrder(@PathVariable Long orderId) {
        return paymentService.getRazorpayOrderForOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
