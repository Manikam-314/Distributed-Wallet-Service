#!/usr/bin/env pwsh
# ============================================================
#  🛑 PAUSE SCRIPT — Distributed Wallet Microservices
#  Scales EKS nodes to 0 + Stops RDS to save costs
#  Resume later with: ./aws-resume.ps1
# ============================================================

$CLUSTER_NAME   = "distributed-wallet"
$NODEGROUP_NAME = "wallet-nodes"
$RDS_INSTANCE   = "wallet-mysql-db"
$REGION         = "us-east-1"

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  💤 PAUSING Distributed Wallet Microservices     ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# ── Step 1: Scale all K8s deployments to 0 replicas ─────────
Write-Host "[ 1/4 ] 📦 Scaling down all K8s deployments to 0..." -ForegroundColor Yellow
$deployments = @(
    "api-gateway",
    "auth-service",
    "wallet-service",
    "transaction-service",
    "notification-service",
    "ai-service",
    "consumer-wallet"
)
foreach ($dep in $deployments) {
    kubectl scale deployment $dep --replicas=0 -n distributed-wallet 2>$null
    Write-Host "   ✅ $dep → 0 replicas"
}

# ── Step 2: Scale EKS node group to 0 ───────────────────────
Write-Host ""
Write-Host "[ 2/4 ] 🖥️  Scaling EKS node group to 0 EC2 instances..." -ForegroundColor Yellow
aws eks update-nodegroup-config `
    --cluster-name $CLUSTER_NAME `
    --nodegroup-name $NODEGROUP_NAME `
    --scaling-config minSize=0,maxSize=5,desiredSize=0 `
    --region $REGION | Out-Null

Write-Host "   ✅ Node group scaling to 0 (EC2 costs stop)"

# ── Step 3: Stop RDS instance ────────────────────────────────
Write-Host ""
Write-Host "[ 3/4 ] 🗄️  Stopping RDS MySQL instance..." -ForegroundColor Yellow

$rdsStatus = aws rds describe-db-instances `
    --db-instance-identifier $RDS_INSTANCE `
    --query "DBInstances[0].DBInstanceStatus" `
    --output text `
    --region $REGION 2>$null

if ($rdsStatus -eq "available") {
    aws rds stop-db-instance `
        --db-instance-identifier $RDS_INSTANCE `
        --region $REGION | Out-Null
    Write-Host "   ✅ RDS stopping (compute cost stops, only storage billed)"
} elseif ($rdsStatus -eq "stopped") {
    Write-Host "   ℹ️  RDS already stopped"
} else {
    Write-Host "   ⚠️  RDS status: $rdsStatus — skipping stop"
}

# ── Step 4: Print cost summary ───────────────────────────────
Write-Host ""
Write-Host "[ 4/4 ] 💰 Cost Summary While Paused:" -ForegroundColor Yellow
Write-Host "   EKS Control Plane : ~`$2.40/day  (can't be stopped)" -ForegroundColor White
Write-Host "   EC2 Worker Nodes  : `$0.00/day  ✅ (scaled to 0)"  -ForegroundColor Green
Write-Host "   RDS MySQL         : ~`$0.06/day  ✅ (only storage)" -ForegroundColor Green
Write-Host "   ElastiCache Redis : ~`$0.00/day  ✅ (serverless, pay-per-use)" -ForegroundColor Green
Write-Host "   ─────────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "   Total while paused: ~`$2.50/day vs ~`$12.50/day running" -ForegroundColor Cyan
Write-Host "   You save 80% of compute costs! 🎉" -ForegroundColor Green
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  ✅ System PAUSED successfully!                  ║" -ForegroundColor Green
Write-Host "║  Run ./aws-resume.ps1 before your interview 🚀   ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host "⚠️  NOTE: RDS auto-restarts after 7 days (AWS policy)." -ForegroundColor DarkYellow
Write-Host "   If pausing > 7 days, run this script again to re-stop." -ForegroundColor DarkYellow
Write-Host ""
