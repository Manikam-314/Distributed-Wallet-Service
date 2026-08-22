# Step 1: Register
$registerBody = @{
    name           = "Test User"
    email          = "testuser@wallet.com"
    password       = "Test@12345"
    mobileNumber   = "9999999999"
} | ConvertTo-Json

Write-Output "=== STEP 1: REGISTER ==="
try {
    $regResp = Invoke-RestMethod -Method POST -Uri "http://localhost:8093/auth/register" -ContentType "application/json" -Body $registerBody
    Write-Output "REGISTER OK: $($regResp | ConvertTo-Json)"
} catch {
    $errBody = $_.ErrorDetails.Message
    Write-Output "REGISTER RESULT: $($_.Exception.Message) | $errBody"
}

# Step 2: Login
$loginBody = @{
    email    = "testuser@wallet.com"
    password = "Test@12345"
} | ConvertTo-Json

Write-Output ""
Write-Output "=== STEP 2: LOGIN ==="
try {
    $loginResp = Invoke-RestMethod -Method POST -Uri "http://localhost:8093/auth/login" -ContentType "application/json" -Body $loginBody
    $token = $loginResp.token
    if (-not $token) { $token = $loginResp.accessToken }
    if (-not $token) { $token = $loginResp.jwt }
    Write-Output "LOGIN OK"
    Write-Output "TOKEN PREVIEW: $($token.Substring(0, [Math]::Min(50, $token.Length)))..."
    
    # Save token
    $token | Out-File -FilePath "c:\Users\manik\OneDrive\Documents\distributed-wallet-microservices\jwt_token.txt" -Encoding utf8 -NoNewline
    Write-Output "Token saved to jwt_token.txt"
} catch {
    $errBody = $_.ErrorDetails.Message
    Write-Output "LOGIN FAILED: $($_.Exception.Message) | $errBody"
    exit 1
}

# Step 3: Test agent chat via gateway
Write-Output ""
Write-Output "=== STEP 3: POST http://localhost/api/agent/chat ==="
$chatBody = @{
    prompt = "Hello, what can you help me with?"
} | ConvertTo-Json

$headers = @{ Authorization = "Bearer $token" }

try {
    $chatResp = Invoke-RestMethod -Method POST -Uri "http://localhost/api/agent/chat" -ContentType "application/json" -Headers $headers -Body $chatBody
    Write-Output "CHAT STATUS: 200 OK"
    Write-Output "CHAT RESPONSE:"
    Write-Output ($chatResp | ConvertTo-Json -Depth 5)
} catch {
    $errBody = $_.ErrorDetails.Message
    Write-Output "CHAT FAILED: $($_.Exception.Message)"
    Write-Output "ERROR BODY: $errBody"
}
