# E-Commerce Microservices Platform

Production-ready event-driven microservices with AWS SNS/SQS, demonstrating enterprise patterns and scalability.

## 🏗️ Architecture

```
┌─────────────────┐      SNS Topic       ┌──────────────────┐
│                 │  ┌──────────────┐    │                  │
│  Order Service  │──►              ├───►│ Payment Service  │
│                 │  │ ORDER_TOPIC  │    │                  │
└─────────────────┘  └──────────────┘    └──────────────────┘
                                                   │
                                          SNS Topic│
                                         ┌─────────▼──────────┐
                                         │  PAYMENT_TOPIC     │
                                         └────────┬───────────┘
                                                  │
                                         ┌────────▼─────────────┐
                                         │ Notification Service │
                                         └──────────────────────┘
```

## 🎯 Key Features

- **Event-Driven Architecture**: Async communication via AWS SNS/SQS with pub/sub pattern
- **Centralized Event Contracts**: JSON Schema-based type-safe events with auto-generated Java classes
- **Production Patterns**: Idempotency, transactional outbox, circuit breaking, structured logging
- **Scalability**: Horizontal scaling, queue-based load leveling, independent service deployment
- **Reliability**: SQS retries, dead letter queues, message deduplication

## 🔄 Event Flow

```
User Request → Order Service → SNS (OrderCreated)
                                  ↓
                            Payment Service → SNS (PaymentCompleted/Failed)
                                                ↓
                                          Notification Service → User Alert
```

## 📦 Services

| Service | Responsibility | Events |
|---------|---------------|--------|
| **Order Service** | REST API, order management | Publishes: `OrderCreatedV1` |
| **Payment Service** | Payment processing | Consumes: `OrderCreatedV1`<br>Publishes: `PaymentCompletedV1`, `PaymentFailedV1` |
| **Notification Service** | User notifications | Consumes: `PaymentCompletedV1`, `PaymentFailedV1` |
| **Event Schemas** | Shared event contracts | JSON Schema → Java classes (12KB JAR) |

## 📊 Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 4.0.1
- **Messaging**: AWS SNS/SQS (Spring Cloud AWS 4.0.0-M1)
- **Database**: PostgreSQL
- **Build**: Gradle 8.x
- **Schema Gen**: jsonschema2pojo 1.2.1


**⭐ Enterprise-grade microservices showcasing AWS messaging, event sourcing, and production engineering.**
