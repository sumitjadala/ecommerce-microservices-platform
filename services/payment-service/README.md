# Payment Service - Event-Driven Architecture (Phase 2)

## Overview

This Payment Service is a Spring Boot microservice that consumes `OrderCreated` domain events from AWS SQS and processes payments asynchronously. It implements Phase-2 event-driven architecture with:

- **At-least-once delivery** guarantees
- **Idempotent consumers** using orderId as idempotency key
- **No retries in application code** (relies on SQS redelivery)
- **Transactional processing** with proper boundaries
- **Fire-and-forget event publishing** to SNS

## Architecture

```
AWS SNS (Order Events)
      ↓
AWS SQS (OrderCreated Queue) 
      ↓
[SQS Poller] → [Event Handler] → [Payment Repository]
                      ↓
                [Event Publisher] → AWS SNS (Payment Events)
```

## Key Components

### 1. Event Models

#### OrderCreatedEvent (Local Model)
Immutable event model consumed from SQS:
- `eventId` - Unique event identifier
- `eventVersion` - Event schema version
- `occurredAt` - Event timestamp
- `orderId` - Order identifier (idempotency key)
- `userId` - User identifier
- `amount` - Payment amount

**Location**: `com.sj.ecommerce.payment_service.event.OrderCreatedEvent`

#### Payment Result Events
Published to SNS after processing:
- **PaymentCompletedEvent** - Published on successful payment
- **PaymentFailedEvent** - Published on payment failure

**Location**: `com.sj.ecommerce.payment_service.event.*`

### 2. AWS Integration (SDK v2)

#### SqsClient Bean
Explicit AWS SDK v2 SQS client configuration:
- Uses `DefaultCredentialsProvider`
- Configurable region
- Long polling support (waitTimeSeconds = 20)

#### SnsClient Bean
Explicit AWS SDK v2 SNS client configuration:
- Fire-and-forget publishing
- Message attributes for event routing

**Location**: `com.sj.ecommerce.payment_service.config.AwsConfig`

### 3. SQS Message Polling

**Class**: `SqsMessagePoller`

**Behavior**:
- Scheduled polling every 5 seconds using `@Scheduled`
- Long polling with 20-second wait time
- Batch processing (max 5 messages)
- Manual message acknowledgment
- **Does NOT auto-delete messages**

**Message Processing Flow**:
1. Poll SQS with long polling
2. Pass each message to event handler
3. Only delete message if handler returns `true`
4. If handler returns `false` or throws exception, message remains in queue for redelivery

**Location**: `com.sj.ecommerce.payment_service.service.SqsMessagePoller`

### 4. Event Handler

**Class**: `OrderCreatedEventHandler`

**Idempotency Strategy**:
- Uses `orderId` as the idempotency key
- Checks if payment already exists for orderId before processing
- Database-level enforcement via unique constraint on `order_id`

**Transaction Boundaries**:
- Wrapped in `@Transactional`
- Only commits if:
  - Payment record is persisted successfully
  - Payment result event is published
- If any exception occurs:
  - Transaction rolls back
  - Message is NOT deleted
  - SQS redelivers naturally

**Processing Logic**:
1. Deserialize SQS message body to `OrderCreatedEvent`
2. Check for existing payment by `orderId`
3. If exists → return `true` (safe to delete duplicate)
4. Process payment (simulate payment gateway call)
5. Persist payment record
6. Publish payment result event (PaymentCompleted/PaymentFailed)
7. Return `true` (safe to delete message)

**Location**: `com.sj.ecommerce.payment_service.service.OrderCreatedEventHandler`

### 5. Payment Result Publishing

**Class**: `PaymentEventPublisher`

**Behavior**:
- Fire-and-forget publishing to SNS
- No retries or blocking logic
- Includes `eventType` as message attribute for filtering
- Logs errors but does not throw exceptions

**Published Events**:
- `PaymentCompleted` - on successful payment processing
- `PaymentFailed` - on payment processing failure

**Location**: `com.sj.ecommerce.payment_service.service.PaymentEventPublisher`

### 6. Payment Entity

**Database Schema**:
```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

**Idempotency Enforcement**:
- Unique constraint on `order_id` (primary idempotency key)
- Unique constraint on `idempotency_key` (legacy support)

**Location**: `com.sj.ecommerce.payment_service.entity.Payment`

## Configuration

### Environment Variables

```yaml
# Database
DB_URL=jdbc:postgresql://localhost:5432/payment_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# AWS
AWS_REGION=us-east-1
SQS_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789012/payment-order-created-queue
SNS_TOPIC_ARN=arn:aws:sns:us-east-1:123456789012:payment-events

# Server
SERVER_PORT=8082
```

### Application Configuration

**File**: `application-dev.yaml`

```yaml
spring:
  application:
    name: payment-service
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update

aws:
  region: ${AWS_REGION}
  sqs:
    queue-url: ${SQS_QUEUE_URL}
  sns:
    topic-arn: ${SNS_TOPIC_ARN}
```

## Dependencies

### AWS SDK v2
```gradle
implementation platform('software.amazon.awssdk:bom:2.28.29')
implementation 'software.amazon.awssdk:sqs'
implementation 'software.amazon.awssdk:sns'
```

### JSON Processing
```gradle
implementation 'com.fasterxml.jackson.core:jackson-databind'
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
```

## Building and Running

### Build
```bash
./gradlew clean build
```

### Run Locally
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Docker Build
```bash
docker build -t payment-service:latest .
```

## Key Design Decisions

### 1. Idempotency Key: orderId
- **Why**: OrderCreated events are unique per order
- **Implementation**: Database unique constraint on `order_id`
- **Benefit**: Automatic duplicate detection at DB level

### 2. No Auto-Delete Messages
- **Why**: Ensures message is only deleted after successful processing
- **Implementation**: Manual deletion after transaction commit
- **Benefit**: Natural redelivery on failures

### 3. Fire-and-Forget Event Publishing
- **Why**: Phase-2 architecture requirement
- **Implementation**: Log errors but don't throw exceptions
- **Trade-off**: May lose some events, but keeps processing simple

### 4. Transaction Boundaries
- **Why**: Ensures atomicity of payment processing
- **Scope**: Payment persistence + event publishing
- **Benefit**: Prevents partial state updates

### 5. Long Polling
- **Why**: Reduces empty responses and API costs
- **Configuration**: 20-second wait time
- **Benefit**: More efficient resource usage

## Error Handling

### Transient Errors
- Database connection failures
- Temporary AWS service issues
- **Behavior**: Message not deleted, SQS redelivers

### Permanent Errors
- Invalid message format
- Business validation failures
- **Behavior**: Depends on implementation (currently retries indefinitely)
- **Recommendation**: Implement Dead Letter Queue (DLQ)

### Duplicate Messages
- Already processed orderId
- **Behavior**: Log and safely delete message
- **Guarantee**: Idempotent processing

## Monitoring and Observability

### Key Metrics to Monitor
- SQS queue depth
- Message processing rate
- Payment success/failure rate
- Event publishing failures
- Database transaction errors

### Log Patterns
```
INFO  - Received X message(s) from SQS
INFO  - Processing OrderCreated event: orderId=123
INFO  - Payment persisted: paymentId=456, orderId=123
INFO  - Published PaymentCompleted event for orderId=123
INFO  - Message processed and deleted: messageId=abc
```

## Testing

### Unit Tests
Test individual components with mocked dependencies:
- OrderCreatedEventHandler
- PaymentEventPublisher
- SqsMessagePoller

### Integration Tests
Test with:
- LocalStack (AWS services locally)
- Testcontainers (PostgreSQL)

### Example Test Scenario
```java
// Given: OrderCreated event in SQS
// When: Poller fetches and processes message
// Then: Payment is created with correct orderId
// And: PaymentCompleted event is published to SNS
// And: SQS message is deleted
```

## Production Considerations

### 1. Dead Letter Queue (DLQ)
Configure SQS DLQ for messages that fail repeatedly:
```yaml
MaxReceiveCount: 3
DeadLetterTargetArn: arn:aws:sqs:us-east-1:123456789012:payment-order-created-dlq
```

### 2. Visibility Timeout
Current: 30 seconds
- Should be > max processing time
- Current implementation is suitable for quick payments

### 3. Scaling
- Horizontal scaling supported
- Multiple instances can poll same queue
- SQS handles message distribution

### 4. Monitoring
- CloudWatch metrics for SQS
- Application Performance Monitoring (APM)
- Database connection pool monitoring

### 5. Circuit Breaker
Consider implementing circuit breaker for:
- Payment gateway calls
- SNS publishing

## Troubleshooting

### Messages Not Being Processed
- Check SQS queue URL configuration
- Verify AWS credentials and IAM permissions
- Check application logs for errors

### Duplicate Payments
- Should not occur due to unique constraint on orderId
- If it does, investigate race conditions

### Events Not Published
- Check SNS topic ARN configuration
- Verify IAM permissions for SNS publish
- Review application logs for publishing errors

### High Queue Depth
- Check payment processing performance
- Review database query performance
- Consider scaling horizontally

## Security

### AWS Credentials
- Use IAM roles in production (EC2/ECS)
- Never hardcode credentials
- Use `DefaultCredentialsProvider`

### Required IAM Permissions
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:us-east-1:123456789012:payment-order-created-queue"
    },
    {
      "Effect": "Allow",
      "Action": [
        "sns:Publish"
      ],
      "Resource": "arn:aws:sns:us-east-1:123456789012:payment-events"
    }
  ]
}
```

## Future Enhancements

1. **Saga Pattern**: Implement compensation logic for failed payments
2. **Event Sourcing**: Store all payment state changes as events
3. **CQRS**: Separate read and write models
4. **Distributed Tracing**: Add OpenTelemetry for trace correlation
5. **Rate Limiting**: Implement payment gateway rate limiting
6. **Retry Policies**: Add sophisticated retry logic with exponential backoff

## References

- [AWS SQS Best Practices](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-best-practices.html)
- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [Spring Boot Transaction Management](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [Idempotent Consumer Pattern](https://microservices.io/patterns/communication-style/idempotent-consumer.html)
