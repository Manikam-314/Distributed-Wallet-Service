#!/bin/sh
set -e

echo "Starting auth-service..."
java -Dspring.profiles.active=default \
     -Dserver.port=8093 \
     -Dspring.datasource.url="jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;MODE=MySQL" \
     -Dspring.datasource.driver-class-name=org.h2.Driver \
     -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
     -Dspring.kafka.bootstrap-servers=localhost:9092 \
     -jar auth-service.jar &

echo "Starting wallet-service..."
java -Dspring.profiles.active=default \
     -Dserver.port=8091 \
     -Dspring.datasource.url="jdbc:h2:mem:walletdb;DB_CLOSE_DELAY=-1;MODE=MySQL" \
     -Dspring.datasource.driver-class-name=org.h2.Driver \
     -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
     -Dspring.kafka.bootstrap-servers=localhost:9092 \
     -Dauth.service.url=http://localhost:8093 \
     -jar wallet-service.jar &

echo "Starting transaction-service..."
java -Dspring.profiles.active=default \
     -Dserver.port=8092 \
     -Dspring.datasource.url="jdbc:h2:mem:txdb;DB_CLOSE_DELAY=-1;MODE=MySQL" \
     -Dspring.datasource.driver-class-name=org.h2.Driver \
     -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
     -Dspring.kafka.bootstrap-servers=localhost:9092 \
     -Dwallet.service.url=http://localhost:8091 \
     -jar transaction-service.jar &

echo "Starting notification-service..."
java -Dspring.profiles.active=default \
     -Dserver.port=8094 \
     -Dspring.datasource.url="jdbc:h2:mem:notifdb;DB_CLOSE_DELAY=-1;MODE=MySQL" \
     -Dspring.datasource.driver-class-name=org.h2.Driver \
     -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
     -Dspring.kafka.bootstrap-servers=localhost:9092 \
     -jar notification-service.jar &

echo "Waiting 10 seconds for backend microservices to initialize..."
sleep 10

echo "Starting api-gateway on port 8090..."
exec java -Dspring.profiles.active=default \
          -Dserver.port=8090 \
          -Dspring.cloud.gateway.routes[0].id=wallet-service \
          -Dspring.cloud.gateway.routes[0].uri=http://localhost:8091 \
          -Dspring.cloud.gateway.routes[0].predicates[0]=Path=/api/wallet/** \
          -Dspring.cloud.gateway.routes[0].filters[0]=AuthenticationFilter \
          -Dspring.cloud.gateway.routes[0].filters[1]=StripPrefix=1 \
          -Dspring.cloud.gateway.routes[1].id=transaction-service \
          -Dspring.cloud.gateway.routes[1].uri=http://localhost:8092 \
          -Dspring.cloud.gateway.routes[1].predicates[0]=Path=/api/transactions/**,/api/requests/** \
          -Dspring.cloud.gateway.routes[1].filters[0]=AuthenticationFilter \
          -Dspring.cloud.gateway.routes[1].filters[1]=StripPrefix=1 \
          -Dspring.cloud.gateway.routes[2].id=auth-service \
          -Dspring.cloud.gateway.routes[2].uri=http://localhost:8093 \
          -Dspring.cloud.gateway.routes[2].predicates[0]=Path=/api/auth/** \
          -Dspring.cloud.gateway.routes[2].filters[0]=StripPrefix=1 \
          -jar api-gateway.jar
