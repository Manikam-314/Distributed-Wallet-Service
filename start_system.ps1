# Start System Script for Distributed Wallet Microservices
$ErrorActionPreference = "Continue"

# 1. Start Docker Desktop if not already running
$dockerProcess = Get-Process -Name *docker* -ErrorAction SilentlyContinue
if (!$dockerProcess) {
    Write-Host "Starting Docker Desktop..."
    Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
}

# 2. Wait for Docker daemon to become responsive
Write-Host "Waiting for Docker daemon to start (checking every 3s, up to 90s)..."
$dockerReady = $false
for ($i = 0; $i -lt 30; $i++) {
    & docker ps 2>$null
    if ($LASTEXITCODE -eq 0) {
        $dockerReady = $true
        break
    }
    Start-Sleep -Seconds 3
}

if (!$dockerReady) {
    Write-Warning "Docker daemon is taking longer than expected to start. Attempting docker compose up anyway..."
}

# 3. Start Docker Compose containers
Write-Host "Running docker compose up -d..."
& docker compose up -d

# 4. Start each backend service in background
$services = @(
    @{ name = "Auth"; port = 8093; dir = "auth-service"; cmd = "mvn compile spring-boot:run" },
    @{ name = "Wallet"; port = 8091; dir = "wallet-service"; cmd = "mvn compile spring-boot:run" },
    @{ name = "Transaction"; port = 8092; dir = "transaction-service"; cmd = "mvn compile spring-boot:run" },
    @{ name = "Notification"; port = 8094; dir = "notification-service"; cmd = "mvn compile spring-boot:run" },
    @{ name = "Gateway"; port = 8090; dir = "api-gateway"; cmd = "mvn compile spring-boot:run" },
    @{ name = "AI"; port = 8095; dir = "ai-service"; cmd = "uvicorn main:app --port 8095" }
)

if (!(Test-Path logs)) { New-Item -ItemType Directory -Path logs }

foreach ($svc in $services) {
    Write-Host "Starting $($svc.name) Service on port $($svc.port)..."
    $logFile = "logs/$($svc.name.ToLower()).log"
    Start-Process powershell -ArgumentList "-Command", "cd $($svc.dir); $($svc.cmd) 2>&1 | Out-File -FilePath ../$logFile -Encoding utf8" -WindowStyle Hidden
    Start-Sleep -Seconds 12
}

# 5. Start Frontend
Write-Host "Starting Frontend (Consumer Wallet UI)..."
Start-Process powershell -ArgumentList "-Command", "cd consumer-wallet; npm run dev" -WindowStyle Hidden

Write-Host "All services started! Check the logs/ folder for service outputs."
