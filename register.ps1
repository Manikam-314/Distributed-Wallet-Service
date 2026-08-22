$body = @{
    name = "Test User"
    email = "test@example.com"
    password = "Password123"
    mobileNumber = "1234567890"
}
$json = $body | ConvertTo-Json -Compress
$resp = Invoke-RestMethod -Method Post -Uri http://localhost:8093/auth/register -ContentType "application/json" -Body $json
Write-Output $resp
