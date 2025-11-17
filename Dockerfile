# Dockerfile for Spring Boot Application
# Multi-stage build for optimized image size

# Stage 1: Build stage (optional - use if you want to build inside Docker)
# FROM maven:3.9-eclipse-temurin-17 AS build
# WORKDIR /app
# COPY pom.xml .
# COPY src ./src
# RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the JAR file from local target directory
# If using multi-stage build above, change to: COPY --from=build /app/target/*.jar app.jar
COPY target/*.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]