# PowerShell script to test Auth API with a unique email address
$email = "newuser_" + (Get-Random -Minimum 1000 -Maximum 9999) + "@example.com"
$registerBody = @{
    name = "New Test User"
    email = $email
    password = "Password123"
    mobileNumber = "1234567890"
} | ConvertTo-Json -Compress

Write-Host "Registering with unique email: $email"
try {
    $resp = Invoke-WebRequest -Uri "http://localhost:8093/auth/register" -Method Post -ContentType "application/json" -Body $registerBody -UseBasicParsing
    Write-Host "Registration Status Code: $($resp.StatusCode)"
    Write-Host "Registration Response Body: $($resp.Content)"
} catch {
    Write-Host "Registration failed!"
    Write-Host "Exception Message: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $respBody = $reader.ReadToEnd()
        Write-Host "Error Response Body: $respBody"
    }
}

$loginBody = @{
    email = $email
    password = "Password123"
} | ConvertTo-Json -Compress

Write-Host "Logging in as: $email"
try {
    $resp = Invoke-WebRequest -Uri "http://localhost:8093/auth/login" -Method Post -ContentType "application/json" -Body $loginBody -UseBasicParsing
    Write-Host "Login Status Code: $($resp.StatusCode)"
    Write-Host "Login Response Body: $($resp.Content)"
} catch {
    Write-Host "Login failed!"
    Write-Host "Exception Message: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $respBody = $reader.ReadToEnd()
        Write-Host "Error Response Body: $respBody"
    }
}
