$ErrorActionPreference = "Stop"

$Region = "us-east-1"
$InstanceId = "i-072617b9b436d44cc"

Write-Host "Stopping EC2..." -ForegroundColor Cyan

$State = aws ec2 describe-instances `
    --instance-ids $InstanceId `
    --region $Region `
    --query "Reservations[0].Instances[0].State.Name" `
    --output text

if ($State -eq "stopped") {
    Write-Host "Instance already stopped." -ForegroundColor Green
    exit 0
}

aws ec2 stop-instances --instance-ids $InstanceId --region $Region | Out-Null
aws ec2 wait instance-stopped --instance-ids $InstanceId --region $Region

Write-Host "Instance stopped. Compute billing paused." -ForegroundColor Green