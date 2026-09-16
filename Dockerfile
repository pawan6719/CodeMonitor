# ==========================================
# Stage 1: Build the Spring Boot application
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper configuration and dependencies descriptor
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Ensure the Maven wrapper has executable permissions
RUN chmod +x mvnw

# Resolve dependencies offline to cache them in intermediate Docker layers
RUN ./mvnw dependency:go-offline -B

# Copy the actual source code
COPY src ./src

# Build the production-ready fat JAR (skipping tests for speed)
RUN ./mvnw clean package -DskipTests

# ==========================================
# Stage 2: Create the lightweight runtime image
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root system user for security compliance on Cloud Run
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy only the compiled fat JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Cloud Run defaults to port 8080 by default
EXPOSE 8080

# Configure JVM tuning variables optimal for container workloads
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djdk.httpclient.keepalive.timeout=5"

# Start the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]