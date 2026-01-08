# Changelog

All notable changes to the event-contracts project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-01-09

### Added
- Initial release of event schemas library
- JSON Schema definitions for domain events:
  - `order-created.v1.json` - Order creation event (eventId, eventVersion, occurredAt, orderId, userId, amount)
  - `payment-completed.v1.json` - Successful payment event (eventId, eventVersion, occurredAt, paymentId, orderId, userId, amount, status)
  - `payment-failed.v1.json` - Failed payment event (eventId, eventVersion, occurredAt, orderId, userId, amount, reason)
- Automatic Java class generation from JSON Schemas using jsonschema2pojo
- Generated classes in `com.ecommerce.contracts.events` package
- Gradle build configuration for library packaging
- Maven publishing support
- Comprehensive README with usage instructions
- Quick start guide (USAGE.md) for microservice developers
- .gitignore for Java/Gradle projects

### Features
- Framework-agnostic design (no Spring, AWS, or framework dependencies)
- Simple flat event structure matching current service implementations
- Jackson JSON serialization support
- Schemas packaged in JAR for runtime access
- Versioned schemas for backward compatibility
- Field validation via JSON Schema constraints

### Schema Design
- Uses Long (int64) for entity IDs (orderId, userId, paymentId)
- Uses BigDecimal (number) for monetary amounts
- Uses ISO-8601 strings for timestamps (occurredAt)
- Event version format: "1.0" (not "v1")
- No envelope pattern - flat event structure
- Simple, minimal fields matching current implementation

### Dependencies
- Jackson Databind 2.15.3
- Jackson Annotations 2.15.3
- Jakarta Validation API 3.0.2 (compile-only)
- JUnit Jupiter 5.10.0 (test)

## [Unreleased]

### Planned
- Additional event types as services evolve
- Schema validation utilities
- Event versioning migration helpers
- Integration examples with AWS SNS/SQS
- Avro schema conversion support

---

## Version Numbering

This library follows [Semantic Versioning](https://semver.org/):

- **MAJOR** version for incompatible schema changes (breaking changes)
- **MINOR** version for new event schemas or backward-compatible additions
- **PATCH** version for bug fixes and documentation updates

### Examples:
- `1.0.0 → 1.1.0`: Add new event schema (backward compatible)
- `1.1.0 → 1.1.1`: Fix typo in documentation
- `1.1.1 → 2.0.0`: Remove or rename required fields (breaking change)

## Schema Versioning

Individual schemas also maintain their own versions:
- Schemas use `v1`, `v2`, `v3` suffixes
- Multiple schema versions can coexist in the same JAR
- Consumers should handle multiple versions for backward compatibility

### Example Evolution:
```
order-created.v1.json (JAR 1.0.0) → Initial schema
order-created.v2.json (JAR 1.1.0) → Add optional field
order-created.v2.json (JAR 2.0.0) → v1 deprecated, v2 becomes primary
```
