# Event Schema Analysis Report

## ✅ Folder Renamed
`event-contracts` → `event-schemas`

## 📊 Analysis Complete

I've analyzed all three microservices and identified the event schemas currently in use:

---

## 🔍 Current Event Structure Analysis

### 1. **Order Service** (Publisher)

**Events Published:**
- `OrderCreatedEvent`

**Current Implementation:**
```java
- eventId: String (UUID)
- eventVersion: String ("1.0")
- occurredAt: Instant
- orderId: Long
- userId: Long
- amount: BigDecimal
```

**Entity Structure (Order):**
```java
- id: Long (auto-generated)
- userId: Long
- amount: BigDecimal
- status: String
- createdAt: Instant
```

---

### 2. **Payment Service** (Publisher + Consumer)

**Events Consumed:**
- `OrderCreatedEvent` (from order-service)

**Events Published:**
- `PaymentCompletedEvent`
- `PaymentFailedEvent`

**Current Implementation:**

**PaymentCompletedEvent:**
```java
- eventId: String
- eventVersion: String
- occurredAt: Instant
- paymentId: Long
- orderId: Long
- userId: Long
- amount: BigDecimal
- status: String
```

**PaymentFailedEvent:**
```java
- eventId: String
- eventVersion: String
- occurredAt: Instant
- orderId: Long
- userId: Long
- amount: BigDecimal
- reason: String
```

**Entity Structure (Payment):**
```java
- id: Long (auto-generated)
- orderId: Long (unique)
- userId: Long
- amount: BigDecimal
- idempotencyKey: String (unique)
- status: String
- createdAt: Instant
```

---

### 3. **Notification Service** (Consumer)

**Events Consumed:**
- `PaymentCompletedEvent` (from payment-service)
- `PaymentFailedEvent` (from payment-service)

**Current Implementation:**
- Uses same structure as payment-service events
- Both events are record types with @JsonIgnoreProperties

---

## 📝 Required JSON Schemas

Based on the analysis, the following schemas need to be created/updated:

### ✅ Already Created (but need updates):
1. ~~`event-envelope.v1.json`~~ - **Needs Update** (current services don't use envelope pattern)
2. ~~`order-created.v1.json`~~ - **Needs Simplification** (current is too complex)
3. ~~`payment-completed.v1.json`~~ - **Needs Update** (missing paymentId)
4. ~~`payment-failed.v1.json`~~ - **Needs Update** (different structure)

### 🎯 Schema Alignment Needed

#### **order-created.v1.json** - Current vs Actual
**Current Schema (too complex):**
- Has items array with productId, quantity, unitPrice
- Has shippingAddress and billingAddress
- Currency with ISO 4217 pattern
- Status enum: PENDING, CONFIRMED, PROCESSING

**Actual Implementation (simple):**
```
- eventId: String (UUID)
- eventVersion: String
- occurredAt: Instant (ISO-8601)
- orderId: Long
- userId: Long
- amount: BigDecimal
```

#### **payment-completed.v1.json** - Current vs Actual
**Current Schema:**
- Has paymentMethod enum
- Has transactionId
- Has provider, cardLast4, receiptUrl

**Actual Implementation (simple):**
```
- eventId: String
- eventVersion: String
- occurredAt: Instant
- paymentId: Long
- orderId: Long
- userId: Long
- amount: BigDecimal
- status: String
```

#### **payment-failed.v1.json** - Current vs Actual
**Current Schema:**
- Has paymentId (missing in actual)
- Has paymentMethod, provider, failureCode
- Has retryable, attemptsRemaining

**Actual Implementation (simple):**
```
- eventId: String
- eventVersion: String
- occurredAt: Instant
- orderId: Long
- userId: Long
- amount: BigDecimal
- reason: String
```

---

## 🔄 Key Differences

### 1. **No Envelope Pattern**
- Current services use flat event structure
- Each event has its own eventId, eventVersion, occurredAt
- No separate envelope with correlationId/causationId

### 2. **Simpler Data Model**
- No order items/line items
- No addresses
- No payment method details
- Just core fields: IDs, amounts, status/reason

### 3. **Data Types**
- Uses `Long` for IDs (not UUID)
- Uses `BigDecimal` for amounts
- Uses `Instant` for timestamps (not Date)
- No currency field (implicit USD)

### 4. **Version Format**
- eventVersion: "1.0" (not "v1")

---

## 💡 Recommendations

### Option 1: **Match Current Implementation** (Simpler)
Create minimal schemas that exactly match what services currently use:
- Simple flat events
- No envelope wrapper
- Basic fields only
- Long IDs, BigDecimal amounts, Instant timestamps

### Option 2: **Enhance Implementation** (Better for Production)
Keep the richer schemas created earlier but update services to use:
- Envelope pattern with correlation tracking
- UUID for event IDs
- Currency codes
- More detailed payment information
- Order line items

### Option 3: **Hybrid Approach**
- Create v1 schemas matching current simple implementation
- Create v2 schemas with enhanced features for future migration

---

## 📋 Summary

**Services Found:**
- ✅ order-service (1 event published)
- ✅ payment-service (1 event consumed, 2 events published)
- ✅ notification-service (2 events consumed)

**Events Identified:**
1. OrderCreatedEvent (publisher: order-service, consumer: payment-service)
2. PaymentCompletedEvent (publisher: payment-service, consumer: notification-service)
3. PaymentFailedEvent (publisher: payment-service, consumer: notification-service)

**Schema Status:**
- Current schemas in event-schemas are too detailed
- Need to align with actual service implementations
- Services use simpler event structure than schemas define

---

## ✅ Ready for Next Steps

I'm ready for your next instruction. Should I:
1. Update schemas to match current simple implementation?
2. Keep rich schemas and update services to use them?
3. Create both v1 (simple) and v2 (rich) versions?
4. Something else?
