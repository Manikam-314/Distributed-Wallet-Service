# Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent pom and ALL modules (required by Maven multi-module resolution)
COPY pom.xml .
COPY common-events/ ./common-events/
COPY auth-service/ ./auth-service/
COPY wallet-service/ ./wallet-service/
COPY transaction-service/ ./transaction-service/
COPY api-gateway/ ./api-gateway/
COPY notification-service/ ./notification-service/

# Install common-events first
RUN mvn install -pl common-events -am -DskipTests

# Build the api-gateway
RUN mvn package -pl api-gateway -am -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
