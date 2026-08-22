#!/usr/bin/env pwsh
# ============================================================
#  ▶️  RESUME SCRIPT — Distributed Wallet Microservices
#  Scales EKS nodes back up + Starts RDS
#  Run this 5-10 minutes before your interview!
# ============================================================

$CLUSTER_NAME   = "distributed-wallet"
$NODEGROUP_NAME = "wallet-nodes"
$RDS_INSTANCE   = "wallet-mysql-db"
$REGION         = "us-east-1"
$NAMESPACE      = "distributed-wallet"

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  🚀 RESUMING Distributed Wallet Microservices    ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host "⏱️  Estimated time to fully ready: 5-10 minutes" -ForegroundColor Cyan
Write-Host ""

# ── Step 1: Start RDS instance ───────────────────────────────
Write-Host "[ 1/5 ] 🗄️  Starting RDS MySQL..." -ForegroundColor Yellow
$rdsStatus = aws rds describe-db-instances `
    --db-instance-identifier $RDS_INSTANCE `
    --query "DBInstances[0].DBInstanceStatus" `
    --output text `
    --region $REGION 2>$null

if ($rdsStatus -eq "stopped") {
    aws rds start-db-instance `
        --db-instance-identifier $RDS_INSTANCE `
        --region $REGION | Out-Null
    Write-Host "   ✅ RDS start initiated (takes ~3 min in background)"
} elseif ($rdsStatus -eq "available") {
    Write-Host "   ℹ️  RDS already running"
} else {
    Write-Host "   ⏳ RDS status: $rdsStatus — waiting..."
}

# ── Step 2: Scale EKS node group back up ─────────────────────
Write-Host ""
Write-Host "[ 2/5 ] 🖥️  Scaling EKS node group to 3 nodes..." -ForegroundColor Yellow
aws eks update-nodegroup-config `
    --cluster-name $CLUSTER_NAME `
    --nodegroup-name $NODEGROUP_NAME `
    --scaling-config minSize=2,maxSize=5,desiredSize=3 `
    --region $REGION | Out-Null
Write-Host "   ✅ Node group scaling up (EC2 instances provisioning...)"

# ── Step 3: Wait for nodes to be Ready ───────────────────────
Write-Host ""
Write-Host "[ 3/5 ] ⏳ Waiting for K8s nodes to become Ready..." -ForegroundColor Yellow
Write-Host "   (This takes 2-4 minutes)" -ForegroundColor DarkGray

$maxWait = 300  # 5 minutes
$elapsed = 0
$nodeReady = $false

while ($elapsed -lt $maxWait) {
    Start-Sleep -Seconds 15
    $elapsed += 15
    $readyNodes = (kubectl get nodes --no-headers 2>$null | Where-Object { $_ -match "Ready" } | Measure-Object).Count
    Write-Host "   [$elapsed`s] Ready nodes: $readyNodes/3" -ForegroundColor DarkGray
    if ($readyNodes -ge 2) {
        $nodeReady = $true
        Write-Host "   ✅ Nodes ready!" -ForegroundColor Green
        break
    }
}

if (-not $nodeReady) {
    Write-Host "   ⚠️  Nodes taking longer than expected. Continuing anyway..." -ForegroundColor DarkYellow
}

# ── Step 4: Scale deployments back up ────────────────────────
Write-Host ""
Write-Host "[ 4/5 ] 📦 Scaling deployments back up..." -ForegroundColor Yellow

$deploymentReplicas = @{
    "auth-service"         = 2
    "wallet-service"       = 2
    "transaction-service"  = 2
    "notification-service" = 1
    "ai-service"           = 1
    "api-gateway"          = 2
    "consumer-wallet"      = 1
}

foreach ($dep in $deploymentReplicas.Keys) {
    $replicas = $deploymentReplicas[$dep]
    kubectl scale deployment $dep --replicas=$replicas -n $NAMESPACE 2>$null
    Write-Host "   ✅ $dep → $replicas replicas"
}

# ── Step 5: Wait for all pods ready + print URLs ─────────────
Write-Host ""
Write-Host "[ 5/5 ] ⏳ Waiting for pods to become Ready..." -ForegroundColor Yellow

$criticalDeployments = @("api-gateway", "auth-service", "consumer-wallet")
foreach ($dep in $criticalDeployments) {
    kubectl rollout status deployment/$dep -n $NAMESPACE --timeout=180s 2>$null
    Write-Host "   ✅ $dep is Ready"
}

# ── Print final URLs ─────────────────────────────────────────
Write-Host ""
Write-Host "[ 🎯 ] Getting public endpoints..." -ForegroundColor Cyan
Start-Sleep -Seconds 10  # Give LBs a moment

$FRONTEND_URL = kubectl get svc consumer-wallet -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>$null
$GATEWAY_URL  = kubectl get svc api-gateway     -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>$null
$GRAFANA_URL  = kubectl get svc grafana         -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>$null

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  ✅ System LIVE! Ready for interview!                    ║" -ForegroundColor Green
Write-Host "╠══════════════════════════════════════════════════════════╣" -ForegroundColor Green
if ($FRONTEND_URL) {
    Write-Host "║  🌐 Frontend   : http://$FRONTEND_URL" -ForegroundColor White
}
if ($GATEWAY_URL) {
    Write-Host "║  🔌 API Gateway: http://${GATEWAY_URL}:8090" -ForegroundColor White
}
if ($GRAFANA_URL) {
    Write-Host "║  📊 Grafana    : http://${GRAFANA_URL}:3000  (admin/admin)" -ForegroundColor White
}
Write-Host "╠══════════════════════════════════════════════════════════╣" -ForegroundColor Green
Write-Host "║  💡 Run ./aws-pause.ps1 after interview to save costs    ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

# Quick health check
Write-Host "🩺 Quick health check on API Gateway..." -ForegroundColor Yellow
if ($GATEWAY_URL) {
    try {
        $response = Invoke-WebRequest "http://${GATEWAY_URL}:8090/actuator/health" -TimeoutSec 10 2>$null
        Write-Host "   ✅ API Gateway: $($response.StatusCode) OK" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  API Gateway not responding yet — give it 1-2 more minutes" -ForegroundColor DarkYellow
    }
}
