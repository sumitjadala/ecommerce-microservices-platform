## Simple Dockerfile (runtime-only)
## Assumes you build the JAR locally (e.g. `./gradlew build -x test`) and then build the image.

FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the locally-built Spring Boot fat JAR into the image
COPY build/libs/*.jar /app/app.jar

# Expose the application port
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
