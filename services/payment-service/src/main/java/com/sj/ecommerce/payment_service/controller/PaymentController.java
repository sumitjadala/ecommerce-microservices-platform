package com.sj.ecommerce.payment_service.controller;

import com.sj.ecommerce.payment_service.dto.CreatePaymentRequest;
import com.sj.ecommerce.payment_service.dto.PaymentResponse;
import com.sj.ecommerce.payment_service.service.PaymentService;
import com.sj.ecommerce.payment_service.dto.RazorpayOrderResponse;
import com.razorpay.RazorpayException;
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

    /**
     * STEP 3: User clicks "Pay Now" - Initiate payment for an existing order.
     * This triggers Razorpay order creation and returns checkout details.
     * 
     * Frontend should call this when user explicitly wants to pay,
     * then use the returned razorpayOrderId to open Razorpay checkout.
     */
    @PostMapping("/order/{orderId}/initiate")
    public ResponseEntity<RazorpayOrderResponse> initiatePayment(@PathVariable Long orderId) {
        try {
            RazorpayOrderResponse response = paymentService.initiatePayment(orderId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RazorpayException ex) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/order/{orderId}/razorpay")
    public ResponseEntity<RazorpayOrderResponse> getRazorpayOrder(@PathVariable Long orderId) {
        return paymentService.getRazorpayOrderForOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/webhook/razorpay", consumes = {"application/json", "text/plain", "application/x-www-form-urlencoded"})
    public ResponseEntity<Void> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature) {
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            paymentService.handleRazorpayWebhook(payload, signature);
            return ResponseEntity.ok().build();
        } catch (RazorpayException ex) {
            return ResponseEntity.status(400).build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(500).build();
        }
    }
}
