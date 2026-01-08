## Multi-stage Dockerfile with build step
## Builds the JAR inside Docker and skips tests

# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application (skip tests)
RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/build/libs/*.jar /app/app.jar

# Expose the application port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
