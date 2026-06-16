@echo off
echo [SYSTEM] Starting all microservices in the 809x range (Non-Interactive)...

echo [1/8] Starting API Gateway (8090)...
start "API GATEWAY - 8090" cmd /k "cd api-gateway && title API Gateway && mvn spring-boot:run"
ping 127.0.0.1 -n 16 > nul

echo [2/8] Starting Auth Service (8093)...
start "AUTH SERVICE - 8093" cmd /k "cd auth-service && title Auth Service && mvn spring-boot:run"
ping 127.0.0.1 -n 16 > nul

echo [3/8] Starting Wallet Service (8091)...
start "WALLET SERVICE - 8091" cmd /k "cd wallet-service && title Wallet Service && mvn spring-boot:run"
ping 127.0.0.1 -n 16 > nul

echo [4/8] Starting Transaction Service (8092)...
start "TRANSACTION SERVICE - 8092" cmd /k "cd transaction-service && title Transaction Service && mvn spring-boot:run"
ping 127.0.0.1 -n 16 > nul

echo [5/8] Starting Notification Service (8094)...
start "NOTIFICATION SERVICE - 8094" cmd /k "cd notification-service && title Notification Service && mvn spring-boot:run"
ping 127.0.0.1 -n 16 > nul

echo [6/8] Starting AI Service (8095)...
start "AI SERVICE - 8095" cmd /k "cd ai-service && title AI Service && if exist ..\.venv\Scripts\activate (call ..\.venv\Scripts\activate) && uvicorn main:app --reload --port 8095"
ping 127.0.0.1 -n 5 > nul

echo [7/8] Starting Frontend UI (5173)...
start "CONSUMER WALLET UI" cmd /k "cd consumer-wallet && title Consumer Wallet UI && npm run dev"

echo [8/8] Recovery Complete. All services are booting.
echo Please wait ~90 seconds for full system readiness.
