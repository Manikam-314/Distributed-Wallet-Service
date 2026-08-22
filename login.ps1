$body = @{
    email = "test@example.com"
    password = "Password123"
}
$resp = Invoke-RestMethod -Method Post -Uri http://localhost:8093/auth/login -ContentType "application/json" -Body ($body | ConvertTo-Json -Compress)
Write-Output $resp
