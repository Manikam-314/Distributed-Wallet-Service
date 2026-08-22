# Multi-stage build for all backend microservices
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy full source tree
COPY . .

# Install common-events if present and build all modules
RUN if [ -d "common-events" ]; then mvn clean package -DskipTests; else mvn package -DskipTests; fi

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy packaged JARs
COPY --from=build /app/auth-service/target/*.jar auth-service.jar
COPY --from=build /app/wallet-service/target/*.jar wallet-service.jar
COPY --from=build /app/transaction-service/target/*.jar transaction-service.jar
COPY --from=build /app/notification-service/target/*.jar notification-service.jar
COPY --from=build /app/api-gateway/target/*.jar api-gateway.jar
COPY --from=build /app/entrypoint.sh entrypoint.sh

RUN chmod +x entrypoint.sh

EXPOSE 8090

ENTRYPOINT ["/bin/sh", "entrypoint.sh"]
