# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy gradle wrapper and configuration files first to leverage build cache
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Make gradlew executable and warm up the build cache (downloads dependencies)
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# Copy source files and build target executable
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create upload directory to prevent errors when uploading files
RUN mkdir -p /app/uploads

# Copy built artifact from build stage
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
