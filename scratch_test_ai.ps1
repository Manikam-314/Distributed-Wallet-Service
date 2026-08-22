# Register a new user
$email = "mytestuser_" + (Get-Random) + "@example.com"
$regBody = @{
    email = $email
    password = "Password123!"
    firstName = "Test"
    lastName = "User"
    mobileNumber = "99" + (Get-Random -Minimum 10000000 -Maximum 99999999)
} | ConvertTo-Json

Write-Host "Registering user: $email"
try {
    $regResp = Invoke-RestMethod -Uri "http://localhost:8090/api/auth/register" -Method POST -Body $regBody -ContentType "application/json"
    Write-Host "Registration successful!"
} catch {
    Write-Error "Registration failed: $_"
    exit 1
}

# Wait for 3 seconds to ensure wallet-service processes the registration event and creates the wallet
Start-Sleep -Seconds 3

# Login to get JWT Token
$loginBody = @{
    email = $email
    password = "Password123!"
} | ConvertTo-Json

Write-Host "Logging in..."
try {
    $loginResp = Invoke-RestMethod -Uri "http://localhost:8090/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $token = $loginResp.token
    Write-Host "Login successful! Token acquired."
} catch {
    Write-Error "Login failed: $_"
    exit 1
}

# Call Conversational Agent directly on port 9100 to check balance
$agentBody = @{
    prompt = "What is my balance?"
} | ConvertTo-Json

$headers = @{
    Authorization = "Bearer $token"
}

Write-Host "Sending prompt directly to Agent Service on port 9100: 'What is my balance?'"
try {
    $agentResp = Invoke-RestMethod -Uri "http://localhost:9100/api/agent/chat" -Method POST -Body $agentBody -ContentType "application/json" -Headers $headers
    $agentResp | ConvertTo-Json
} catch {
    Write-Error "Agent chat failed: $_"
    # Show detail error response if available
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errBody = $reader.ReadToEnd()
        Write-Host "Error response body: $errBody"
    }
}
