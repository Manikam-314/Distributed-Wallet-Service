$body = '{"email":"test@example.com","password":"Password123!"}'
$resp = Invoke-RestMethod -Uri 'http://localhost:8090/api/auth/login' -Method POST -Body $body -ContentType 'application/json'
$resp | ConvertTo-Json
