package com.sj.ecommerce.payment_service.dto;

public class RazorpayOrderResponse {
    private String razorpayOrderId;
    private Long amount; // paise
    private String keyId;

    public RazorpayOrderResponse(String razorpayOrderId, Long amount, String keyId) {
        this.razorpayOrderId = razorpayOrderId;
        this.amount = amount;
        this.keyId = keyId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getKeyId() {
        return keyId;
    }
}
