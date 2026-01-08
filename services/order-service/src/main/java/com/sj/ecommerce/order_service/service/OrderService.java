package com.sj.ecommerce.order_service.service;

import com.ecommerce.contracts.events.OrderCreatedV1;
import com.sj.ecommerce.order_service.dto.CreateOrderRequest;
import com.sj.ecommerce.order_service.dto.OrderResponse;
import com.sj.ecommerce.order_service.enitity.Order;
import com.sj.ecommerce.order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final SnsEventPublisher snsEventPublisher;

    public OrderService(OrderRepository orderRepository, SnsEventPublisher snsEventPublisher) {
        this.orderRepository = orderRepository;
        this.snsEventPublisher = snsEventPublisher;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order(request.userId(), request.amount(), request.status(), Instant.now());
        Order saved = orderRepository.save(order);
        
        // Create event using builder pattern from event-schemas
        OrderCreatedV1 event = new OrderCreatedV1()
                .withEventId(UUID.randomUUID())
                .withEventVersion("1.0")
                .withOccurredAt(Date.from(saved.getCreatedAt()))
                .withOrderId(saved.getId())
                .withUserId(saved.getUserId())
                .withAmount(saved.getAmount().doubleValue());
        snsEventPublisher.publish(event);
        
        return new OrderResponse(saved.getId(), saved.getStatus(), saved.getCreatedAt());
    }

    public Optional<OrderResponse> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(o -> new OrderResponse(o.getId(), o.getStatus(), o.getCreatedAt()));
    }
}
