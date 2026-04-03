# Use multi-stage build for better performance
FROM openjdk:21-jdk-slim as builder

# Set working directory
WORKDIR /app

# Copy gradle wrapper and gradle files
COPY gradlew .
COPY gradle/wrapper/ gradle/wrapper/
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x ./gradlew

# Download dependencies
RUN ./gradlew dependencies --configuration compileClasspath

# Copy source code
COPY src src

# Build the application
RUN ./gradlew build -x test --no-daemon

# Production stage
FROM openjdk:21-jdk-slim

# Install required packages
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to appuser
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for production
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
