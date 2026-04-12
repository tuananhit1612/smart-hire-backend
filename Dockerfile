# ═══════════════════════════════════════════════════════════
#  SmartHire Backend — Dockerfile (Multi-stage)
# ═══════════════════════════════════════════════════════════

# Stage 1: Build dependency resolution & compilation
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Install maven wrapper + POM first to cache dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Copy the rest of the source and build
COPY src ./src
ARG PROFILE=prod
# Skip tests in container build (assuming CI runs them)
RUN ./mvnw package -DskipTests -P${PROFILE}

# Stage 2: Minimal Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring

# Create uploads directory (needed by Railway / local) and fix permissions
RUN mkdir -p /app/uploads
RUN chown -R spring:spring /app

USER spring:spring

# Extract the JAR built in the previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose standard port
EXPOSE 8080

# Environment variables could be overridden via docker-compose or Kubernetes configMap
ENV SPRING_PROFILES_ACTIVE=prod

# Execute
ENTRYPOINT ["java", "-jar", "app.jar"]
