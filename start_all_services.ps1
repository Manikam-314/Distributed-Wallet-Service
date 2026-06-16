$services = @(
    @{ name = "Gateway"; port = 8090; dir = "api-gateway"; cmd = "mvn compile spring-boot:run" },
    @{ name = "Auth"; port = 8093; dir = "auth-service"; cmd = "mvn compile spring-boot:run" },
    @{ name = "Wallet"; port = 8091; dir = "wallet-service"; cmd = "mvn compile spring-boot:run" },
    @{ name = "Transaction"; port = 8092; dir = "transaction-service"; cmd = "mvn compile spring-boot:run" },
    @{ name = "Notification"; port = 8094; dir = "notification-service"; cmd = "mvn compile spring-boot:run" },
    @{ name = "AI"; port = 8095; dir = "ai-service"; cmd = "uvicorn main:app --port 8095" }
)

if (!(Test-Path logs)) { New-Item -ItemType Directory -Path logs }

foreach ($svc in $services) {
    Write-Host "Starting $($svc.name) Service on port $($svc.port)..."
    $logFile = "logs/$($svc.name.ToLower()).log"
    
    # Start process and redirect output with utf8 encoding
    Start-Process powershell -ArgumentList "-Command", "cd $($svc.dir); $($svc.cmd) 2>&1 | Out-File -FilePath ../$logFile -Encoding utf8" -WindowStyle Hidden
    
    Start-Sleep -Seconds 12
}

Write-Host "Microservices are starting. Check logs/ for progress."
