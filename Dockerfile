# ==============================================================================
# Multi-stage Dockerfile for PICC-PP-Kubernetes-Integration Microservice
# ==============================================================================

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY spotbugs-exclude.xml .
COPY src src

# Make maven wrapper executable and build jar
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-privileged system user for DevSecOps compliance
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/app/target/kubernetes-integration-*.jar app.jar

ENV SERVER_PORT=8080 \
    PORT=8080 \
    K8S_URL=https://kubernetes.default.svc \
    NNP_DASH_BASEURL=http://nnp-portal-backend:8082 \
    SPRING_CLOUD_CONFIG_ENABLED=false

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
