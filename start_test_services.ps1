$ErrorActionPreference = "Stop"

Write-Host "Recompiling and starting isolated microservices for test environment..." -ForegroundColor Cyan

# Define isolated ports
$SERVICES = @(
    @{ Name = "auth-service"; Port = 9089; Args = "-Dserver.port=9089 -Dspring.datasource.url=jdbc:mysql://localhost:3308/auth_db -Dspring.datasource.password=rootroot1234 -Dspring.kafka.bootstrap-servers=localhost:9093" },
    @{ Name = "wallet-service"; Port = 9091; Args = "-Dserver.port=9091 -Dspring.datasource.url=jdbc:mysql://localhost:3308/wallet_db -Dspring.datasource.password=rootroot1234 -Dspring.kafka.bootstrap-servers=localhost:9093 -Dspring.data.redis.port=6380 -Dauth.service.url=http://localhost:9089" },
    @{ Name = "transaction-service"; Port = 9092; Args = "-Dserver.port=9092 -Dspring.datasource.url=jdbc:mysql://localhost:3308/transaction_db -Dspring.datasource.password=rootroot1234 -Dspring.kafka.bootstrap-servers=localhost:9093 -Dspring.data.redis.port=6380 -Dwallet.service.url=http://localhost:9091" },
    @{ Name = "notification-service"; Port = 9094; Args = "-Dserver.port=9094 -Dspring.datasource.url=jdbc:mysql://localhost:3308/notification_db -Dspring.datasource.password=rootroot1234 -Dspring.kafka.bootstrap-servers=localhost:9093" },
    @{ Name = "api-gateway"; Port = 18090; Args = "-Dserver.port=18090 -Dspring.cloud.gateway.routes[0].uri=http://localhost:9089 -Dspring.cloud.gateway.routes[1].uri=http://localhost:9091 -Dspring.cloud.gateway.routes[2].uri=http://localhost:9092" }
)

foreach ($service in $SERVICES) {
    Write-Host "Starting $($service.Name) on port $($service.Port)..." -ForegroundColor Yellow
    # Stop any existing test instances
    $process = Get-NetTCPConnection -LocalPort $service.Port -ErrorAction SilentlyContinue
    if ($process) { Stop-Process -Id $process.OwningProcess -Force -ErrorAction SilentlyContinue }
    
    cd $service.Name
    Start-Process powershell -ArgumentList "-Command", "mvn spring-boot:run -Dspring-boot.run.jvmArguments=`"$($service.Args)`" 2>&1 | Out-File -FilePath ../logs/$($service.Name)_test.log -Encoding utf8" -WindowStyle Hidden
    cd ..
    Start-Sleep -Seconds 2
}

Write-Host "Isolated test services launched successfully! API Gateway is on port 18090." -ForegroundColor Green
