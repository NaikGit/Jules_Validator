# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy as builder

WORKDIR /workspace/app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline -B

COPY src src

# Build the application
RUN ./mvnw package -DskipTests

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /workspace/app/target/validation-service-*.jar app.jar

EXPOSE 8080 # Or whatever port the application listens on, if any. Default for Spring Boot.

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
