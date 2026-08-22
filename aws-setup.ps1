# aws-setup.ps1
$ProjectRoot = "C:\Users\manik\OneDrive\Documents\distributed-wallet-microservices"
Set-Location $ProjectRoot

# =========================
# CONFIG
# =========================
$Region = "us-east-1"
$InstanceType = "t3.large"
$KeyName = "wallet-key"
$KeyPath = "$HOME\.ssh\$KeyName.pem"
$SecurityGroupName = "wallet-sg"
$InstanceTagName = "distributed-wallet-ec2"
$EipTagName = "distributed-wallet-eip"

Write-Host "Starting one-time AWS setup for Distributed Wallet..." -ForegroundColor Cyan

# =========================
# 1. CHECK AWS AUTH
# =========================
try {
    $Identity = aws sts get-caller-identity --query Account --output text --region $Region
    Write-Host "AWS CLI authenticated. Account: $Identity" -ForegroundColor Green
}
catch {
    Write-Error "AWS CLI authentication failed. Run 'aws configure' first."
    exit 1
}

# =========================
# 2. CREATE / REUSE SSH KEY
# =========================
$AwsKeyExists = aws ec2 describe-key-pairs `
    --key-names $KeyName `
    --region $Region `
    --query "KeyPairs[0].KeyName" `
    --output text 2>$null

if ($LASTEXITCODE -eq 0 -and $AwsKeyExists -eq $KeyName) {
    Write-Host "Reusing existing AWS key pair: $KeyName" -ForegroundColor Green

    if (-not (Test-Path $KeyPath)) {
        Write-Error "AWS key pair '$KeyName' exists, but local PEM file is missing at $KeyPath. Use a new key name or create a new key pair."
        exit 1
    }

    Write-Host "Reusing local PEM file: $KeyPath" -ForegroundColor Green
}
else {
    Write-Host "Creating new EC2 key pair: $KeyName" -ForegroundColor Yellow

    if (-not (Test-Path "$HOME\.ssh")) {
        New-Item -ItemType Directory -Path "$HOME\.ssh" | Out-Null
    }

    aws ec2 create-key-pair `
        --key-name $KeyName `
        --query "KeyMaterial" `
        --output text `
        --region $Region | Out-File -Encoding ascii -FilePath $KeyPath

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to create key pair '$KeyName'."
        exit 1
    }

    icacls.exe $KeyPath /inheritance:r /grant:r "$($env:USERNAME):R" | Out-Null
    Write-Host "Key pair saved at $KeyPath" -ForegroundColor Green
}

# =========================
# 3. CREATE / REUSE SECURITY GROUP
# =========================
$SgId = aws ec2 describe-security-groups `
    --filters "Name=group-name,Values=$SecurityGroupName" `
    --region $Region `
    --query "SecurityGroups[0].GroupId" `
    --output text 2>$null

if ($LASTEXITCODE -ne 0 -or $SgId -eq "None" -or [string]::IsNullOrWhiteSpace($SgId)) {
    Write-Host "Creating security group: $SecurityGroupName" -ForegroundColor Yellow
    $SgId = aws ec2 create-security-group `
        --group-name $SecurityGroupName `
        --description "Security group for distributed wallet docker compose deployment" `
        --region $Region `
        --query "GroupId" `
        --output text

    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($SgId)) {
        Write-Error "Failed to create security group."
        exit 1
    }

    $ports = @(22, 80, 8090, 3000, 9090)
    foreach ($port in $ports) {
        aws ec2 authorize-security-group-ingress `
            --group-id $SgId `
            --protocol tcp `
            --port $port `
            --cidr 0.0.0.0/0 `
            --region $Region | Out-Null
    }

    Write-Host "Security group created: $SgId" -ForegroundColor Green
}
else {
    Write-Host "Reusing security group: $SgId" -ForegroundColor Green
}

# =========================
# 4. REUSE OR CREATE EC2 INSTANCE
# =========================
$ExistingInstanceId = aws ec2 describe-instances `
    --filters "Name=tag:Name,Values=$InstanceTagName" "Name=instance-state-name,Values=pending,running,stopping,stopped" `
    --region $Region `
    --query "Reservations[0].Instances[0].InstanceId" `
    --output text 2>$null

if ($LASTEXITCODE -eq 0 -and $ExistingInstanceId -ne "None" -and -not [string]::IsNullOrWhiteSpace($ExistingInstanceId)) {
    Write-Host "EC2 instance already exists: $ExistingInstanceId" -ForegroundColor Green
    $InstanceId = $ExistingInstanceId

    $State = aws ec2 describe-instances `
        --instance-ids $InstanceId `
        --region $Region `
        --query "Reservations[0].Instances[0].State.Name" `
        --output text

    if ($State -eq "stopped") {
        Write-Host "Starting stopped EC2 instance..." -ForegroundColor Yellow
        aws ec2 start-instances --instance-ids $InstanceId --region $Region | Out-Null
        aws ec2 wait instance-running --instance-ids $InstanceId --region $Region
    }
    elseif ($State -eq "stopping") {
        Write-Host "Instance is stopping. Waiting..." -ForegroundColor Yellow
        aws ec2 wait instance-stopped --instance-ids $InstanceId --region $Region
        aws ec2 start-instances --instance-ids $InstanceId --region $Region | Out-Null
        aws ec2 wait instance-running --instance-ids $InstanceId --region $Region
    }
    elseif ($State -eq "pending") {
        Write-Host "Instance is pending. Waiting..." -ForegroundColor Yellow
        aws ec2 wait instance-running --instance-ids $InstanceId --region $Region
    }
}
else {
    Write-Host "Launching new EC2 instance ($InstanceType)..." -ForegroundColor Yellow

    # Latest Ubuntu 22.04 LTS AMI in us-east-1
    $AmiId = aws ec2 describe-images `
        --owners 099720109477 `
        --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" "Name=state,Values=available" `
        --query "sort_by(Images, &CreationDate)[-1].ImageId" `
        --output text `
        --region $Region

    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($AmiId) -or $AmiId -eq "None") {
        Write-Error "Failed to fetch Ubuntu AMI."
        exit 1
    }

    $UserDataScript = @'
#!/bin/bash
set -eux

export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y docker.io docker-compose git

systemctl enable docker
systemctl start docker
usermod -aG docker ubuntu

mkdir -p /app/distributed-wallet
chown -R ubuntu:ubuntu /app/distributed-wallet
'@

    $UserDataFile = Join-Path $ProjectRoot "userdata.sh"
    Set-Content -Path $UserDataFile -Value $UserDataScript -Encoding UTF8

    # IMPORTANT: write block-device JSON as ASCII to avoid BOM issues in Windows PowerShell
$BlockDeviceJson = @'
[
  {
    "DeviceName": "/dev/sda1",
    "Ebs": {
      "VolumeSize": 30,
      "VolumeType": "gp3",
      "DeleteOnTermination": true
    }
  }
]
'@

$BlockDeviceFile = Join-Path $ProjectRoot "block-device-mappings.json"
$BlockDeviceJson | Out-File -FilePath $BlockDeviceFile -Encoding ascii
    $InstanceId = aws ec2 run-instances `
        --image-id $AmiId `
        --count 1 `
        --instance-type $InstanceType `
        --key-name $KeyName `
        --security-group-ids $SgId `
        --user-data file://$UserDataFile `
        --block-device-mappings file://$BlockDeviceFile `
        --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$InstanceTagName}]" `
        --query "Instances[0].InstanceId" `
        --output text `
        --region $Region

    Remove-Item $UserDataFile -Force
    Remove-Item $BlockDeviceFile -Force

    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($InstanceId) -or $InstanceId -eq "None") {
        Write-Error "EC2 launch failed."
        exit 1
    }

    Write-Host "Waiting for EC2 instance to enter running state..." -ForegroundColor Yellow
    aws ec2 wait instance-running --instance-ids $InstanceId --region $Region
}

# =========================
# 5. REUSE / ALLOCATE ELASTIC IP
# =========================
$AllocationId = aws ec2 describe-addresses `
    --filters "Name=tag:Name,Values=$EipTagName" `
    --region $Region `
    --query "Addresses[0].AllocationId" `
    --output text 2>$null

if ($LASTEXITCODE -ne 0 -or $AllocationId -eq "None" -or [string]::IsNullOrWhiteSpace($AllocationId)) {
    Write-Host "Allocating new Elastic IP..." -ForegroundColor Yellow
    $AllocationId = aws ec2 allocate-address `
        --domain vpc `
        --region $Region `
        --query "AllocationId" `
        --output text

    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($AllocationId)) {
        Write-Error "Failed to allocate Elastic IP."
        exit 1
    }

    aws ec2 create-tags `
        --resources $AllocationId `
        --tags Key=Name,Value=$EipTagName `
        --region $Region | Out-Null
}
else {
    Write-Host "Reusing existing Elastic IP allocation: $AllocationId" -ForegroundColor Green
}

$PublicIp = aws ec2 describe-addresses `
    --allocation-ids $AllocationId `
    --region $Region `
    --query "Addresses[0].PublicIp" `
    --output text

if ([string]::IsNullOrWhiteSpace($PublicIp) -or $PublicIp -eq "None") {
    Write-Error "Could not resolve Elastic IP public address."
    exit 1
}

Write-Host "Associating Elastic IP $PublicIp to instance $InstanceId..." -ForegroundColor Yellow
aws ec2 associate-address `
    --instance-id $InstanceId `
    --allocation-id $AllocationId `
    --allow-reassociation `
    --region $Region | Out-Null

# =========================
# 6. WAIT FOR EC2 TO ACCEPT SSH
# =========================
Write-Host "Waiting for SSH on $PublicIp..." -ForegroundColor Yellow
$maxTries = 40
$sshReady = $false

for ($i = 1; $i -le $maxTries; $i++) {
    ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i $KeyPath ubuntu@$PublicIp "echo ok" 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        $sshReady = $true
        break
    }
    Start-Sleep -Seconds 10
}

if (-not $sshReady) {
    Write-Error "EC2 never became reachable via SSH."
    exit 1
}

# =========================
# 7. PACKAGE PROJECT
# =========================
Write-Host "Packaging project..." -ForegroundColor Yellow
$TarFile = Join-Path $ProjectRoot "project.tar.gz"

if (Test-Path $TarFile) {
    Remove-Item $TarFile -Force
}

tar -czf $TarFile `
  --exclude=.git `
  --exclude=node_modules `
  --exclude=target `
  --exclude=.idea `
  --exclude=mysql-data `
  --exclude=redis-data `
  --exclude=kafka-data `
  --exclude=grafana-data `
  --exclude=prometheus-data `
  --exclude=tempo-data `
  .

if (-not (Test-Path $TarFile)) {
    Write-Error "Failed to create project.tar.gz"
    exit 1
}

# =========================
# 8. UPLOAD PROJECT TO EC2
# =========================
Write-Host "Uploading project to EC2..." -ForegroundColor Yellow
scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i $KeyPath $TarFile ubuntu@${PublicIp}:/app/distributed-wallet/project.tar.gz
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to upload project archive to EC2."
    exit 1
}
Remove-Item $TarFile -Force

# =========================
# 9. DEPLOY ON EC2
# =========================
Write-Host "Deploying stack on EC2..." -ForegroundColor Yellow

$RemoteDeployScript = @'
set -eux
cd /app/distributed-wallet
rm -rf current
mkdir -p current
tar -xzf project.tar.gz -C current
rm -f project.tar.gz
cd current

docker compose -f docker-compose.ec2.yml down || true
docker compose -f docker-compose.ec2.yml up -d --build
'@

ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i $KeyPath ubuntu@$PublicIp $RemoteDeployScript
if ($LASTEXITCODE -ne 0) {
    Write-Error "Remote deployment on EC2 failed."
    exit 1
}

Write-Host ""
Write-Host "AWS setup complete." -ForegroundColor Green
Write-Host "Frontend      : http://$PublicIp" -ForegroundColor Cyan
Write-Host "API Gateway   : http://$PublicIp:8090" -ForegroundColor Cyan
Write-Host "Grafana       : http://$PublicIp:3000  (admin/admin)" -ForegroundColor Cyan
Write-Host "Prometheus    : http://$PublicIp:9090" -ForegroundColor Cyan
Write-Host ""
Write-Host "After your interview/demo, run .\aws-stop.ps1 to stop compute charges." -ForegroundColor Yellow