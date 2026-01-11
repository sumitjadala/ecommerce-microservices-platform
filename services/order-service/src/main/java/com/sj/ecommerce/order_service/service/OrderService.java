package com.sj.ecommerce.order_service.service;

import com.ecommerce.contracts.events.OrderCreatedV1;
import com.sj.ecommerce.order_service.dto.CreateOrderRequest;
import com.sj.ecommerce.order_service.dto.OrderResponse;
import com.sj.ecommerce.order_service.enitity.Order;
import com.sj.ecommerce.order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final SnsEventPublisher snsEventPublisher;

    public OrderService(OrderRepository orderRepository, SnsEventPublisher snsEventPublisher) {
        this.orderRepository = orderRepository;
        this.snsEventPublisher = snsEventPublisher;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        List<Long> productIds = request.productIds();

        // calculate amount based on products we should not reply on frontend for field like amount

        Double totalOrderAmt = 1500.0;

        Order order = new Order(request.userId(), totalOrderAmt, productIds);
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
        snsEventPublisher.publish(event);

        List<Long> savedProductIds = saved.getProductIds() == null ? List.of() : saved.getProductIds();
        return new OrderResponse(saved.getId(), saved.getStatus().name(), saved.getCreatedAt(), savedProductIds);
    }

    public Optional<OrderResponse> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(o -> {
                   List<Long> ids = o.getProductIds() == null ? List.of() : o.getProductIds();
                    return new OrderResponse(o.getId(), o.getStatus().name(), o.getCreatedAt(), ids);
                });
    }
}
