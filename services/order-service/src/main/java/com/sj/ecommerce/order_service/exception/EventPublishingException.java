package com.sj.ecommerce.order_service.exception;

/**
 * Exception thrown when an event fails to publish to the message broker.
 */
public class EventPublishingException extends RuntimeException {
    
    private final Long orderId;
    
    public EventPublishingException(String message, Long orderId, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
    }
    
    public Long getOrderId() {
        return orderId;
    }
}
