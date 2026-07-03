# ===== Stage 1: Build =====
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Cache dependencies first (Docker layer caching optimization)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw

# Download all dependencies before copying source (cached if pom.xml unchanged)
RUN ./mvnw dependency:go-offline -q

# Copy source and build the application
COPY src/ src/
RUN ./mvnw clean package -DskipTests -q

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy only the final JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# JVM tuning for containerized environments:
# -XX:+UseContainerSupport — respect container CPU/memory limits (not host)
# -XX:MaxRAMPercentage     — use up to 75% of container RAM for JVM heap
# -Djava.security.egd     — faster startup by using /dev/urandom
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
