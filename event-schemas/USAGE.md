# Quick Start Guide - Using Event Schemas

## For Microservice Developers

### 1. Add the Dependency

Add to your microservice's `build.gradle`:

```gradle
dependencies {
    implementation 'com.ecommerce.platform:event-schemas:1.0.0'
}
```

For local development (multi-module project):
```gradle
dependencies {
    implementation project(':event-schemas')
}
```

### 2. Publishing Events

```java
import com.ecommerce.contracts.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;

public class OrderEventPublisher {
    
    private final ObjectMapper objectMapper;
    private final EventPublisher publisher; // Your messaging infrastructure
    
    public void publishOrderCreated(Order order) {
        // Create the domain event using generated class
        OrderCreatedV1 event = new OrderCreatedV1();
        event.setEventId(UUID.randomUUID());
        event.setEventVersion("1.0");
        event.setOccurredAt(Instant.now());
        event.setOrderId(order.getId().intValue());
        event.setUserId(order.getUserId().intValue());
        event.setAmount(order.getAmount().doubleValue());
        
        // Serialize and publish to message broker
        String json = objectMapper.writeValueAsString(event);
        publisher.publish("order-created", json);
    }
}
```

### 3. Consuming Events

```java
import com.ecommerce.contracts.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OrderEventConsumer {
    
    private final ObjectMapper objectMapper;
    
    @MessageListener(topic = "order-created")
    public void handleOrderCreated(String message) {
        // Deserialize event
        OrderCreatedV1 event = objectMapper.readValue(message, OrderCreatedV1.class);
        
        // Validate event version
        if (!"1.0".equals(event.getEventVersion())) {
            // Handle version migration if needed
        }
        
        // Business logic
        processOrder(event);
    }
    
    private void processOrder(OrderCreatedV1 event) {
        log.info("Processing order: orderId={}, userId={}, amount={}", 
            event.getOrderId(), event.getUserId(), event.getAmount());
        // ... your business logic
    }
}
```

### 4. Accessing JSON Schemas at Runtime

```java
import java.io.InputStream;

public class SchemaValidator {
    
    public InputStream loadSchema(String schemaName) {
        // Schemas are packaged in the JAR
        return getClass().getClassLoader()
            .getResourceAsStream("schemas/" + schemaName);
    }
    
    public void validateEvent(String eventJson) {
        InputStream schemaStream = loadSchema("order-created.v1.json");
        // Use a JSON Schema validator library to validate
        // Example: org.everit.json.schema
    }
}
```

### 5. Event Versioning

When a new version is released:

```java
// Support multiple versions in your consumer
@MessageListener(topic = "orders.created")
public void handleOrderCreated(String message) {
    EventEnvelopeV1 envelope = objectMapper.readValue(message, EventEnvelopeV1.class);
    
    switch (envelope.getEventVersion()) {
        case "v1":
            OrderCreatedV1 eventV1 = objectMapper.convertValue(
                envelope.getPayload(), OrderCreatedV1.class);
            processOrderV1(eventV1);
            break;
            
        case "v2":
            OrderCreatedV2 eventV2 = objectMapper.convertValue(
                envelope.getPayload(), OrderCreatedV2.class);
            processOrderV2(eventV2);
            break;
            
        default:
            throw new UnsupportedVersionException(envelope.getEventVersion());
    }
}
```

## Best Practices

### ✅ DO
- Always use the event envelope for all events
- Set correlationId for distributed tracing
- Validate events against schemas before publishing
- Handle multiple schema versions in consumers
- Use the generated immutable classes as-is
- Document any service-specific event handling logic

### ❌ DON'T
- Don't modify generated classes (they'll be overwritten)
- Don't add business logic to event classes
- Don't add framework-specific annotations to events
- Don't skip event versioning
- Don't break backward compatibility without incrementing version

## Common Patterns

### Pattern 1: Event Enrichment
```java
// Add metadata for debugging
envelope.setMetadata(Map.of(
    "userId", currentUser.getId(),
    "ipAddress", request.getRemoteAddr(),
    "userAgent", request.getHeader("User-Agent")
));
```

### Pattern 2: Correlation Chain
```java
// Link related events using causationId
PaymentCompletedV1 payment = new PaymentCompletedV1();
// ... set payment fields

EventEnvelopeV1 envelope = new EventEnvelopeV1();
envelope.setCorrelationId(orderCreatedEvent.getCorrelationId());
envelope.setCausationId(orderCreatedEvent.getEventId()); // This payment was caused by order creation
```

### Pattern 3: Dead Letter Handling
```java
@MessageListener(topic = "orders.created.dlq")
public void handleFailedEvent(String message) {
    EventEnvelopeV1 envelope = objectMapper.readValue(message, EventEnvelopeV1.class);
    
    // Log for investigation
    log.error("Failed to process event: eventId={}, type={}, version={}", 
        envelope.getEventId(),
        envelope.getEventType(),
        envelope.getEventVersion());
    
    // Store for manual retry or investigation
    deadLetterRepository.save(envelope);
}
```

## Testing

```java
@Test
public void testOrderCreatedEventSerialization() {
    // Arrange
    OrderCreatedV1 event = new OrderCreatedV1();
    event.setOrderId(UUID.randomUUID());
    event.setCustomerId(UUID.randomUUID());
    event.setTotalAmount(100.0);
    event.setCurrency("USD");
    // ... set other fields
    
    // Act
    String json = objectMapper.writeValueAsString(event);
    OrderCreatedV1 deserialized = objectMapper.readValue(json, OrderCreatedV1.class);
    
    // Assert
    assertEquals(event.getOrderId(), deserialized.getOrderId());
    assertEquals(event.getTotalAmount(), deserialized.getTotalAmount());
}
```

## Troubleshooting

### Issue: ClassNotFoundException for event classes
**Solution**: Ensure the event-contracts dependency is added and the project is rebuilt.

### Issue: JSON deserialization fails
**Solution**: 
- Check that Jackson dependencies are included
- Verify the JSON matches the schema structure
- Ensure the event version matches the class being used

### Issue: Schema validation errors
**Solution**:
- Verify all required fields are set
- Check that field types match (e.g., UUID format for IDs)
- Ensure enum values are valid

## Support

For issues with event contracts:
1. Check the schema definition in `src/main/resources/schemas/`
2. Review generated classes in `build/generated/sources/jsonschema2pojo/`
3. Consult the main README.md for contract-first development workflow
4. Open an issue in the event-contracts project repository
