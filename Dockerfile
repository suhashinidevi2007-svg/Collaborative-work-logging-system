# Multi-stage Docker build for Spring Boot application
# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# Copy Gradle wrapper and project configurations
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Give execution permission to gradlew
RUN chmod +x gradlew

# Pre-fetch dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build the war
COPY src/ src/
RUN ./gradlew bootWar -x test --no-daemon

# Stage 2: Lean runtime container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy built war from build stage
COPY --from=build /workspace/build/libs/cwls.war app.war

# Set default port
ENV PORT=8080
EXPOSE 8080

# Optimized for cloud containers (Render 512MB RAM free tier)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Xss512k", "-jar", "app.war"]
