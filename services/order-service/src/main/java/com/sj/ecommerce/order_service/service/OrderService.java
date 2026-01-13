package com.sj.ecommerce.order_service.service;

import com.ecommerce.contracts.events.OrderCreatedV1;
import com.sj.ecommerce.order_service.dto.CreateOrderRequest;
import com.sj.ecommerce.order_service.dto.OrderResponse;
import com.sj.ecommerce.order_service.enitity.Order;
import com.sj.ecommerce.order_service.enitity.OrderStatus;
import com.sj.ecommerce.order_service.enitity.PaymentStatus;
import com.sj.ecommerce.order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        List<Long> productIds = request.productIds();
        // don't trust frontend for amount recalculate in backend based on productIds
        Double totalOrderAmt = request.amount();

        Order order = new Order(request.userId(), totalOrderAmt, OrderStatus.CREATED, productIds);
        Order saved = orderRepository.save(order);

        // Create event using constructor-only immutable event class
        OrderCreatedV1 event = new OrderCreatedV1(
            UUID.randomUUID(),
            "1.0",
            saved.getCreatedAt(),
            saved.getId(),
            saved.getUserId(),
            saved.getAmount()
        );
        orderEventPublisher.publish(event);

        List<Long> savedProductIds = saved.getProductIds() == null ? List.of() : saved.getProductIds();
        return new OrderResponse(
            saved.getId(), 
            saved.getStatus().name(), 
            saved.getPaymentStatus().name(),
            saved.getAmount(),
            saved.getCreatedAt(), 
            savedProductIds
        );
    }

    public Optional<OrderResponse> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(o -> {
                    List<Long> ids = o.getProductIds() == null ? List.of() : o.getProductIds();
                    return new OrderResponse(
                        o.getId(), 
                        o.getStatus().name(), 
                        o.getPaymentStatus() != null ? o.getPaymentStatus().name() : PaymentStatus.PENDING.name(),
                        o.getAmount(),
                        o.getCreatedAt(), 
                        ids
                    );
                });
    }

    @Transactional
    public Optional<OrderResponse> updatePaymentStatus(Long orderId, String paymentStatus) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    PaymentStatus newStatus = PaymentStatus.valueOf(paymentStatus);
                    order.setPaymentStatus(newStatus);
                    
                    // Also update order status based on payment
                    if (newStatus == PaymentStatus.COMPLETED) {
                        order.setStatus(OrderStatus.PAID);
                    } else if (newStatus == PaymentStatus.FAILED) {
                        order.setStatus(OrderStatus.PAYMENT_FAILED);
                    }
                    
                    Order saved = orderRepository.save(order);
                    List<Long> ids = saved.getProductIds() == null ? List.of() : saved.getProductIds();
                    return new OrderResponse(
                        saved.getId(),
                        saved.getStatus().name(),
                        saved.getPaymentStatus().name(),
                        saved.getAmount(),
                        saved.getCreatedAt(),
                        ids
                    );
                });
    }
}
