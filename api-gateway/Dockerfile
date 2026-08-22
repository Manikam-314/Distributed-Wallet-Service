# Multi-stage build for all backend microservices
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent pom and ALL modules
COPY pom.xml .
COPY common-events/ ./common-events/
COPY auth-service/ ./auth-service/
COPY wallet-service/ ./wallet-service/
COPY transaction-service/ ./transaction-service/
COPY api-gateway/ ./api-gateway/
COPY notification-service/ ./notification-service/
COPY agent-service/ ./agent-service/

# Build all modules
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy packaged JARs and entrypoint script
COPY --from=build /app/auth-service/target/*.jar auth-service.jar
COPY --from=build /app/wallet-service/target/*.jar wallet-service.jar
COPY --from=build /app/transaction-service/target/*.jar transaction-service.jar
COPY --from=build /app/notification-service/target/*.jar notification-service.jar
COPY --from=build /app/api-gateway/target/*.jar api-gateway.jar
COPY entrypoint.sh entrypoint.sh

RUN chmod +x entrypoint.sh

EXPOSE 8090

ENTRYPOINT ["/bin/sh", "entrypoint.sh"]
