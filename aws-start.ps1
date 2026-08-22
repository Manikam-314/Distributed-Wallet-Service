$ErrorActionPreference = "Stop"

$Region = "us-east-1"
$InstanceId = "i-072617b9b436d44cc"

Write-Host "Starting EC2..." -ForegroundColor Cyan

$State = aws ec2 describe-instances `
    --instance-ids $InstanceId `
    --region $Region `
    --query "Reservations[0].Instances[0].State.Name" `
    --output text

if ($State -eq "running") {
    Write-Host "Instance already running." -ForegroundColor Green
}
else {
    aws ec2 start-instances --instance-ids $InstanceId --region $Region | Out-Null
    aws ec2 wait instance-running --instance-ids $InstanceId --region $Region
    Write-Host "Instance started." -ForegroundColor Green
}

$PublicIp = aws ec2 describe-instances `
    --instance-ids $InstanceId `
    --region $Region `
    --query "Reservations[0].Instances[0].PublicIpAddress" `
    --output text

Write-Host ""
Write-Host "Frontend    : http://$PublicIp" -ForegroundColor Cyan
Write-Host "API Gateway : http://$PublicIp:8090" -ForegroundColor Cyan
Write-Host "Grafana     : http://$PublicIp:3000" -ForegroundColor Cyan
Write-Host "Prometheus  : http://$PublicIp:9090" -ForegroundColor Cyan