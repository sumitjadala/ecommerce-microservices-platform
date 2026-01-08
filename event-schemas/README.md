# Event Schemas

This module defines **event schemas** for the ecommerce microservices platform using JSON Schema. It serves as the **single source of truth** for all domain events exchanged between services.

## Purpose

- **Protocol Definition**: Defines the structure and validation rules for all domain events
- **Language Agnostic**: JSON Schema enables cross-language compatibility
- **Version Control**: Schemas are versioned (v1, v2, etc.) for backward compatibility
- **Contract Enforcement**: Microservices must conform to these contracts when producing or consuming events

## Schemas

All JSON Schema files are located in `src/main/resources/schemas/`:

### Domain Events

#### `order-created.v1.json`
Published by **order-service** when a new order is created

**Fields:**
- `eventId`: String (UUID) - Unique event identifier
- `eventVersion`: String - Schema version (e.g., "1.0")
- `occurredAt`: String (ISO-8601) - When the event occurred
- `orderId`: Long - Order identifier
- `userId`: Long - User who created the order
- `amount`: BigDecimal - Order amount

#### `payment-completed.v1.json`
Published by **payment-service** when a payment is successfully completed

**Fields:**
- `eventId`: String - Unique event identifier
- `eventVersion`: String - Schema version (e.g., "1.0")
- `occurredAt`: String (ISO-8601) - When the event occurred
- `paymentId`: Long - Payment identifier
- `orderId`: Long - Associated order identifier
- `userId`: Long - User who made the payment
- `amount`: BigDecimal - Payment amount
- `status`: String - Payment status (e.g., "COMPLETED")

#### `payment-failed.v1.json`
Published by **payment-service** when a payment attempt fails

**Fields:**
- `eventId`: String - Unique event identifier
- `eventVersion`: String - Schema version (e.g., "1.0")
- `occurredAt`: String (ISO-8601) - When the event occurred
- `orderId`: Long - Associated order identifier
- `userId`: Long - User who attempted the payment
- `amount`: BigDecimal - Payment amount attempted
- `reason`: String - Reason for payment failure

## Generated Java Classes

Java classes are automatically generated from JSON Schemas at build time using the `jsonschema2pojo` Gradle plugin.

**Generated classes are:**
- **Immutable**: No setters, only getters and constructors
- **Data-only**: No business logic
- **Framework-agnostic**: No Spring, AWS, or other annotations
- **Located in**: `build/generated/sources/jsonschema2pojo/`
- **Package**: `com.ecommerce.contracts.events`

## Usage

### Building the Library

```bash
# Build the JAR
./gradlew build

# Generate Java classes from schemas
./gradlew generateJsonSchema2Pojo

# Build without tests
./gradlew build -x test
```

The output JAR includes:
- Generated Java classes
- Original JSON Schema files (in `META-INF/schemas/`)

### Consuming in Microservices

Add this dependency to your microservice's `build.gradle`:

```gradle
dependencies {
    implementation 'com.ecommerce.platform:event-schemas:1.0.0'
}
```

Or for local development (before publishing to a repository):

```gradle
dependencies {
    implementation project(':event-schemas')
}
```

### Publishing

To publish to a Maven repository:

```bash
./gradlew publish
```

Configure the repository in `build.gradle` under the `publishing` section.

## Versioning

- Schemas use semantic versioning (v1, v2, v3, etc.)
- Breaking changes require a new version
- Multiple versions can coexist for backward compatibility
- JAR artifact follows semantic versioning (1.0.0, 1.1.0, 2.0.0)

## Contract-First Development

1. **Define the contract**: Create or update JSON Schema files
2. **Generate code**: Run `./gradlew generateJsonSchema2Pojo`
3. **Review generated classes**: Check `build/generated/sources/jsonschema2pojo/`
4. **Build and publish**: Run `./gradlew build publish`
5. **Update microservices**: Bump the dependency version in consuming services

## Best Practices

- **Schemas are the source of truth** - modify schemas first, not generated code
- **Never edit generated classes** - they will be overwritten on next build
- **Use meaningful field descriptions** - they become Javadoc comments
- **Mark required fields** - use `required` array in JSON Schema
- **Validate events** - consuming services should validate against schemas
- **Version carefully** - breaking changes require new schema versions

## Dependencies

This library has minimal dependencies:
- Jackson (for JSON serialization/deserialization)
- No Spring Framework
- No AWS SDK
- No application framework dependencies

This ensures the contract library remains lightweight and reusable across any Java project.

## Project Structure

```
event-schemas/
├── build.gradle                          # Build configuration
├── settings.gradle                       # Project settings
├── src/
│   └── main/
│       └── resources/
│           └── schemas/                  # JSON Schema files (source of truth)
│               ├── order-created.v1.json
│               ├── payment-completed.v1.json
│               └── payment-failed.v1.json
└── build/
    └── generated/
        └── sources/
            └── jsonschema2pojo/          # Generated Java classes
                └── com/
                    └── ecommerce/
                        └── contracts/
                            └── events/
```

## Contributing

When adding new event types:
1. Create a new JSON Schema file with `.v1.json` suffix
2. Follow the existing naming convention (kebab-case)
3. Include comprehensive field descriptions
4. Mark all required fields
5. Add the schema to this README
6. Rebuild to generate Java classes
7. Update the version number appropriately

## License

Apache License 2.0
