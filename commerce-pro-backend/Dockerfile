# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and dependency files first for layer caching
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q || true

# Copy source and build the fat JAR (skip tests — run them in CI)
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# ─── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy only the fat JAR from the build stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Render injects $PORT — Spring reads it via server.port=${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
