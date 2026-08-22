$ErrorActionPreference = "Stop"

# ============================================================
# Deploy Distributed Wallet to EXISTING EC2
# ============================================================

$ProjectRoot = "C:\Users\manik\OneDrive\Documents\distributed-wallet-microservices"
Set-Location $ProjectRoot

# -------- CONFIG --------
$Region = "us-east-1"
$InstanceId = "i-072617b9b436d44cc"
$PublicIp = "18.206.179.184"
$KeyPath = "$HOME\.ssh\wallet-key.pem"
$RemoteAppDir = "/home/ubuntu/distributed-wallet-microservices"
$TarFile = Join-Path $ProjectRoot "project.tar.gz"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host " Deploying Distributed Wallet to existing EC2" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Instance ID : $InstanceId"
Write-Host "Public IP   : $PublicIp"
Write-Host "Region      : $Region"
Write-Host ""

# 1. Check PEM
if (-not (Test-Path $KeyPath)) {
    Write-Error "PEM file not found at: $KeyPath"
    exit 1
}

# 2. Check EC2 state
$State = aws ec2 describe-instances `
    --instance-ids $InstanceId `
    --region $Region `
    --query "Reservations[0].Instances[0].State.Name" `
    --output text

if ($State -ne "running") {
    Write-Host "EC2 is not running. Starting it..." -ForegroundColor Yellow
    aws ec2 start-instances --instance-ids $InstanceId --region $Region | Out-Null
    aws ec2 wait instance-running --instance-ids $InstanceId --region $Region
    Write-Host "EC2 started." -ForegroundColor Green

    $PublicIp = aws ec2 describe-instances `
        --instance-ids $InstanceId `
        --region $Region `
        --query "Reservations[0].Instances[0].PublicIpAddress" `
        --output text
}

Write-Host "Using Public IP: $PublicIp" -ForegroundColor Green

# 3. Wait for SSH
Write-Host "Waiting for SSH..." -ForegroundColor Yellow
$sshReady = $false
for ($i = 1; $i -le 30; $i++) {
    ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i $KeyPath ubuntu@$PublicIp "echo ok" 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        $sshReady = $true
        break
    }
    Start-Sleep -Seconds 10
}

if (-not $sshReady) {
    Write-Error "SSH not reachable on EC2."
    exit 1
}
Write-Host "SSH is ready." -ForegroundColor Green

# 4. Create tarball
Write-Host "Packaging project..." -ForegroundColor Yellow
if (Test-Path $TarFile) {
    Remove-Item $TarFile -Force
}

tar -czf $TarFile `
    --exclude='.git' `
    --exclude='node_modules' `
    --exclude='target' `
    --exclude='.idea' `
    --exclude='mysql-data' `
    --exclude='redis-data' `
    --exclude='kafka-data' `
    --exclude='grafana-data' `
    --exclude='prometheus-data' `
    --exclude='tempo-data' `
    .

if (-not (Test-Path $TarFile)) {
    Write-Error "Failed to create project.tar.gz"
    exit 1
}

# 5. Upload tarball
Write-Host "Uploading project to EC2..." -ForegroundColor Yellow
scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i $KeyPath $TarFile ubuntu@${PublicIp}:~/project.tar.gz

Remove-Item $TarFile -Force

# 6. Deploy on EC2
Write-Host "Deploying on EC2..." -ForegroundColor Yellow

$RemoteScript = @'
set -e

APP_DIR="$HOME/distributed-wallet-microservices"

echo "Updating apt..."
sudo apt-get update -y

echo "Installing Docker + Compose if missing..."
if ! command -v docker >/dev/null 2>&1; then
  sudo apt-get install -y docker.io
  sudo systemctl enable docker
  sudo systemctl start docker
fi

if ! docker compose version >/dev/null 2>&1; then
  sudo apt-get install -y docker-compose-plugin
fi

sudo usermod -aG docker ubuntu || true

mkdir -p "$APP_DIR"
rm -rf "$APP_DIR"/*
tar -xzf ~/project.tar.gz -C "$APP_DIR"
rm -f ~/project.tar.gz

cd "$APP_DIR"

echo "Stopping old containers if any..."
docker compose -f docker-compose.ec2.yml down || true

echo "Building and starting stack..."
docker compose -f docker-compose.ec2.yml up -d --build

echo "Waiting 20 seconds..."
sleep 20

echo "Container status:"
docker compose -f docker-compose.ec2.yml ps
'@

ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i $KeyPath ubuntu@$PublicIp $RemoteScript

Write-Host ""
Write-Host "===================================================" -ForegroundColor Green
Write-Host " Deployment completed" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
Write-Host "Frontend     : http://$PublicIp" -ForegroundColor Cyan
Write-Host "API Gateway  : http://$PublicIp:8090" -ForegroundColor Cyan
Write-Host "Grafana      : http://$PublicIp:3000" -ForegroundColor Cyan
Write-Host "Prometheus   : http://$PublicIp:9090" -ForegroundColor Cyan
Write-Host ""
Write-Host "After interview, run: .\aws-stop.ps1" -ForegroundColor Yellow