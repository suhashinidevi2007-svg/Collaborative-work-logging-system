# Multi-stage Docker build for Spring Boot application
# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# Copy Gradle wrapper and project configurations
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Fix line endings and give execution permission to gradlew
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Copy source code and build the war
COPY src/ src/
RUN ./gradlew bootWar -x test --no-daemon

# Stage 2: Lean runtime container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy built war from build stage
COPY --from=build /workspace/build/libs/cwls.war app.war

# Set default port to 10000 (Render default)
ENV PORT=10000
EXPOSE 10000

# Optimized for cloud containers (Render 512MB RAM free tier, dynamic PORT expansion)
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xss512k -Dserver.port=${PORT:-10000} -jar app.war"]
