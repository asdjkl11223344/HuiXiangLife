param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Count = 50,
    [int]$StartIndex = 1,
    [string]$OutputPath = ".\seckill-users.csv",
    [string]$PhonePrefix = "1380000",
    [string]$DefaultPassword = "password123"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$normalizedBaseUrl = $BaseUrl.TrimEnd("/")
$successCount = 0
$csvData = @()

Write-Host "Starting to register $Count test users from index $StartIndex..."

for ($i = $StartIndex; $i -lt ($StartIndex + $Count); $i++) {
    $phone = "{0}{1:D4}" -f $PhonePrefix, $i
    $nickname = "TestUser_$i"
    
    $body = @{
        phone = $phone
        password = $DefaultPassword
        nickname = $nickname
    } | ConvertTo-Json -Compress

    try {
        $response = Invoke-RestMethod -Method Post `
            -Uri "$normalizedBaseUrl/user/auth/register" `
            -ContentType "application/json" `
            -Body $body

        if ($response.code -eq 1) {
            Write-Host "Registered: $phone"
            $successCount++
            $csvData += [pscustomobject]@{
                phone = $phone
                password = $DefaultPassword
            }
        } else {
            # 可能是已存在，通常也算可用账号，但不一定密码一致
            Write-Host "Failed to register $($phone): $($response.msg)" -ForegroundColor Yellow
            # 依然尝试加到列表，假定密码就是 DefaultPassword
            $csvData += [pscustomobject]@{
                phone = $phone
                password = $DefaultPassword
            }
        }
    } catch {
        Write-Host "Error registering $($phone): $($_.Exception.Message)" -ForegroundColor Red
    }
}

if ($csvData.Count -gt 0) {
    $outputDirectory = Split-Path -Path $OutputPath -Parent
    if (-not [string]::IsNullOrWhiteSpace($outputDirectory) -and -not (Test-Path -LiteralPath $outputDirectory)) {
        New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    }
    $csvData | Export-Csv -Path $OutputPath -NoTypeInformation -Encoding UTF8
    Write-Host ""
    Write-Host "Successfully generated users file at: $OutputPath" -ForegroundColor Green
    Write-Host "You can now use this file for the seckill pressure test."
} else {
    Write-Host "No users were registered or recorded." -ForegroundColor Red
}
