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

# Copy packaged JARs
COPY --from=build /app/auth-service/target/*.jar auth-service.jar
COPY --from=build /app/wallet-service/target/*.jar wallet-service.jar
COPY --from=build /app/transaction-service/target/*.jar transaction-service.jar
COPY --from=build /app/notification-service/target/*.jar notification-service.jar
COPY --from=build /app/api-gateway/target/*.jar api-gateway.jar

EXPOSE 8090

CMD ["sh", "-c", "\
java -Dserver.port=8093 -Dspring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;MODE=MySQL -Dspring.datasource.driver-class-name=org.h2.Driver -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect -Dspring.kafka.bootstrap-servers=localhost:9092 -jar auth-service.jar & \
java -Dserver.port=8091 -Dspring.datasource.url=jdbc:h2:mem:walletdb;DB_CLOSE_DELAY=-1;MODE=MySQL -Dspring.datasource.driver-class-name=org.h2.Driver -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect -Dspring.kafka.bootstrap-servers=localhost:9092 -Dauth.service.url=http://localhost:8093 -jar wallet-service.jar & \
java -Dserver.port=8092 -Dspring.datasource.url=jdbc:h2:mem:txdb;DB_CLOSE_DELAY=-1;MODE=MySQL -Dspring.datasource.driver-class-name=org.h2.Driver -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect -Dspring.kafka.bootstrap-servers=localhost:9092 -Dwallet.service.url=http://localhost:8091 -jar transaction-service.jar & \
java -Dserver.port=8094 -Dspring.datasource.url=jdbc:h2:mem:notifdb;DB_CLOSE_DELAY=-1;MODE=MySQL -Dspring.datasource.driver-class-name=org.h2.Driver -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect -Dspring.kafka.bootstrap-servers=localhost:9092 -jar notification-service.jar & \
sleep 10; \
java -Dserver.port=8090 -Dspring.cloud.gateway.routes[0].uri=http://localhost:8091 -Dspring.cloud.gateway.routes[1].uri=http://localhost:8092 -Dspring.cloud.gateway.routes[2].uri=http://localhost:8093 -jar api-gateway.jar \
"]
