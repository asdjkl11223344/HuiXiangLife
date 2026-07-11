param(
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)]
    [long]$MerchantId,
    [Parameter(Mandatory = $true)]
    [long]$ProductId,
    [Parameter(Mandatory = $true)]
    [string]$UsersFile,
    [string]$Remark = "stress-test",
    [switch]$PollResult,
    [int]$MaxWaitSeconds = 30,
    [int]$RequestTimeoutSeconds = 15,
    [string]$OutputPath = ".\seckill-pressure-report.json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-GroupCount {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Items,
        [Parameter(Mandatory = $true)]
        [string]$PropertyName
    )

    $result = [ordered]@{}
    foreach ($item in $Items) {
        $value = $item.$PropertyName
        $key = if ($null -eq $value -or [string]::IsNullOrWhiteSpace([string]$value)) { "(empty)" } else { [string]$value }
        if (-not $result.Contains($key)) {
            $result[$key] = 0
        }
        $result[$key]++
    }
    return $result
}

function Invoke-ApiJson {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Method,
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [hashtable]$Headers,
        [object]$Body,
        [int]$TimeoutSeconds = 15
    )

    $invokeParams = @{
        Method      = $Method
        Uri         = $Uri
        ContentType = "application/json"
        TimeoutSec  = [Math]::Max($TimeoutSeconds, 1)
    }

    if ($Headers) {
        $invokeParams.Headers = $Headers
    }

    if ($null -ne $Body) {
        $invokeParams.Body = ($Body | ConvertTo-Json -Depth 10)
    }

    return Invoke-RestMethod @invokeParams
}

if (-not (Test-Path -LiteralPath $UsersFile)) {
    throw "Users file not found: $UsersFile"
}

$users = @(Import-Csv -LiteralPath $UsersFile)
if ($null -eq $users -or $users.Count -eq 0) {
    throw "Users file is empty: $UsersFile"
}

$normalizedBaseUrl = $BaseUrl.TrimEnd("/")
$loginSuccessUsers = @()
$loginFailures = @()

Write-Host "Loading test users: $($users.Count)"
Write-Host "Logging in users..."

foreach ($user in $users) {
    if (-not $user.phone -or -not $user.password) {
        $loginFailures += [pscustomobject]@{
            phone = $user.phone
            error = "CSV must contain phone and password columns"
        }
        continue
    }

    try {
        $loginResponse = Invoke-ApiJson -Method "Post" `
            -Uri "$normalizedBaseUrl/user/auth/login" `
            -TimeoutSeconds $RequestTimeoutSeconds `
            -Body @{
                phone    = [string]$user.phone
                password = [string]$user.password
            }

        if ($loginResponse.code -ne 1 -or $null -eq $loginResponse.data -or [string]::IsNullOrWhiteSpace([string]$loginResponse.data.token)) {
            $loginFailures += [pscustomobject]@{
                phone = [string]$user.phone
                error = "Login failed: code=$($loginResponse.code), msg=$($loginResponse.msg)"
            }
            continue
        }

        $loginSuccessUsers += [pscustomobject]@{
            phone = [string]$user.phone
            token = [string]$loginResponse.data.token
        }
    } catch {
        $loginFailures += [pscustomobject]@{
            phone = [string]$user.phone
            error = $_.Exception.Message
        }
    }
}

if ($loginSuccessUsers.Count -eq 0) {
    throw "No users logged in successfully. Please verify your test accounts."
}

Write-Host "Login success: $($loginSuccessUsers.Count)"
Write-Host "Login failed : $($loginFailures.Count)"
Write-Host "Submitting seckill requests..."

$jobScript = {
    param(
        [string]$BaseUrlArg,
        [long]$MerchantIdArg,
        [long]$ProductIdArg,
        [string]$PhoneArg,
        [string]$TokenArg,
        [string]$RemarkArg,
        [bool]$PollEnabledArg,
        [int]$MaxWaitSecondsArg,
        [int]$RequestTimeoutSecondsArg
    )

    $result = [ordered]@{
        phone        = $PhoneArg
        submitCode   = $null
        submitMsg    = $null
        requestId    = $null
        pollStatus   = $null
        failureCode  = $null
        orderId      = $null
        error        = $null
    }

    try {
        $headers = @{
            Authorization = "Bearer $TokenArg"
        }

        $submitResponse = Invoke-RestMethod -Method Post `
            -Uri ($BaseUrlArg.TrimEnd("/") + "/user/order/seckill") `
            -Headers $headers `
            -TimeoutSec ([Math]::Max($RequestTimeoutSecondsArg, 1)) `
            -ContentType "application/json" `
            -Body (@{
                merchantId = $MerchantIdArg
                productId  = $ProductIdArg
                remark     = $RemarkArg
            } | ConvertTo-Json -Depth 10)

        $result.submitCode = $submitResponse.code
        $result.submitMsg = $submitResponse.msg

        if ($null -ne $submitResponse.data) {
            $result.requestId = $submitResponse.data.requestId
        }

        if ($PollEnabledArg -and $submitResponse.code -eq 1) {
            $deadline = (Get-Date).AddSeconds([Math]::Max($MaxWaitSecondsArg, 1))

            while ((Get-Date) -lt $deadline) {
                $pollResponse = Invoke-RestMethod -Method Get `
                    -Uri ($BaseUrlArg.TrimEnd("/") + "/user/order/seckill/result?productId=$ProductIdArg") `
                    -Headers $headers `
                    -TimeoutSec ([Math]::Max($RequestTimeoutSecondsArg, 1))

                if ($pollResponse.code -ne 1) {
                    $result.pollStatus = "RESULT_API_ERROR"
                    $result.failureCode = $pollResponse.code
                    break
                }

                $payload = $pollResponse.data
                if ($null -eq $payload) {
                    Start-Sleep -Milliseconds 500
                    continue
                }

                if ($payload.finished -eq $true) {
                    $result.pollStatus = $payload.status
                    $result.failureCode = $payload.failureCode
                    $result.orderId = $payload.orderId
                    break
                }

                $sleepMillis = 500
                if ($null -ne $payload.nextPollIntervalMillis) {
                    $sleepMillis = [Math]::Max([int]$payload.nextPollIntervalMillis, 200)
                }
                Start-Sleep -Milliseconds $sleepMillis
            }

            if (-not $result.pollStatus) {
                $result.pollStatus = "TIMEOUT"
            }
        }
    } catch {
        $result.error = $_.Exception.Message
    }

    [pscustomobject]$result
}

$jobs = @()
foreach ($user in $loginSuccessUsers) {
    $jobs += Start-Job -ScriptBlock $jobScript -ArgumentList `
        $normalizedBaseUrl, `
        $MerchantId, `
        $ProductId, `
        $user.phone, `
        $user.token, `
        $Remark, `
        $PollResult.IsPresent, `
        $MaxWaitSeconds, `
        $RequestTimeoutSeconds
}

Wait-Job -Job $jobs | Out-Null
$requestResults = @(Receive-Job -Job $jobs)
$jobs | Remove-Job -Force | Out-Null

$summary = [ordered]@{
    generatedAt         = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    baseUrl             = $normalizedBaseUrl
    merchantId          = $MerchantId
    productId           = $ProductId
    totalUsers          = $users.Count
    loginSuccessCount   = $loginSuccessUsers.Count
    loginFailureCount   = $loginFailures.Count
    submitAcceptedCount = @($requestResults | Where-Object { $_.submitCode -eq 1 }).Count
    submitFailedCount   = @($requestResults | Where-Object { $_.submitCode -ne 1 }).Count
    requestErrorCount   = @($requestResults | Where-Object { -not [string]::IsNullOrWhiteSpace($_.error) }).Count
    submitCodeBreakdown = Get-GroupCount -Items $requestResults -PropertyName "submitCode"
}

if ($PollResult.IsPresent) {
    $summary["pollStatusBreakdown"] = Get-GroupCount -Items $requestResults -PropertyName "pollStatus"
    $summary["successOrderCount"] = @($requestResults | Where-Object { $_.pollStatus -eq "SUCCESS" }).Count
}

$report = [ordered]@{
    summary        = $summary
    loginFailures  = $loginFailures
    requestResults = $requestResults
}

$outputDirectory = Split-Path -Path $OutputPath -Parent
if (-not [string]::IsNullOrWhiteSpace($outputDirectory) -and -not (Test-Path -LiteralPath $outputDirectory)) {
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
}

$report | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $OutputPath -Encoding UTF8

Write-Host ""
Write-Host "Seckill pressure test finished."
Write-Host "Report file: $OutputPath"
Write-Host ""
Write-Host "Summary:"
$summary.GetEnumerator() | ForEach-Object {
    if ($_.Value -is [System.Collections.IDictionary]) {
        Write-Host ("- {0}: {1}" -f $_.Key, (($_.Value | ConvertTo-Json -Compress)))
    } else {
        Write-Host ("- {0}: {1}" -f $_.Key, $_.Value)
    }
}
