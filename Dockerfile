# ---- Build Stage ----
  FROM eclipse-temurin:17-jdk-alpine AS builder

  WORKDIR /app
  
  # Copy Maven wrapper and pom.xml for layer caching
  COPY mvnw pom.xml ./
  COPY .mvn .mvn
  
  RUN chmod +x ./mvnw
  RUN ./mvnw dependency:go-offline -B
  
  # Copy source code
  COPY src src
  
  # Build the application
  RUN ./mvnw clean package -DskipTests
  
  # ---- Production Stage ----
  FROM eclipse-temurin:17-jre-alpine
  
  # Create non-root user
  RUN addgroup -g 1001 -S appgroup && \
      adduser -u 1001 -S appuser -G appgroup
  
  WORKDIR /app
  
  # Copy JAR from build stage
  COPY --from=builder /app/target/*.jar app.jar
  
  # Change ownership to appuser
  RUN chown appuser:appgroup app.jar
  
  USER appuser
  
  EXPOSE 8080
  
  # Optional: Use exec form to ensure signals are passed properly (graceful shutdowns)
  ENTRYPOINT ["java"]
  CMD ["-Xmx512m", "-Xms256m", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
  