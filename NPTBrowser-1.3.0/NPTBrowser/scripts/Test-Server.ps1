param(
    [string]$WebBaseUrl = "http://NPT-SERVER",
    [string]$ApiBaseUrl = "http://NPT-SERVER:5001",
    [int]$TimeoutSeconds = 10,
    [int]$ConcurrentRequests = 10
)

$ErrorActionPreference = "Stop"

function Invoke-NptTest {
    param([string]$Name, [string]$Method, [string]$Url, [string]$Body = "")
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $parameters = @{
            Uri = $Url
            Method = $Method
            TimeoutSec = $TimeoutSeconds
            UseBasicParsing = $true
        }
        if ($Body) {
            $parameters.ContentType = "application/json"
            $parameters.Body = $Body
        }
        $response = Invoke-WebRequest @parameters
        $watch.Stop()
        [pscustomobject]@{
            Test = $Name
            Method = $Method
            Url = $Url
            Status = [int]$response.StatusCode
            ElapsedMs = $watch.ElapsedMilliseconds
            Result = "Da thuc hien"
        }
    } catch {
        $watch.Stop()
        $httpStatus = $null
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $httpStatus = [int]$_.Exception.Response.StatusCode
        }
        [pscustomobject]@{
            Test = $Name
            Method = $Method
            Url = $Url
            Status = $httpStatus
            ElapsedMs = $watch.ElapsedMilliseconds
            Result = "Loi: $($_.Exception.Message)"
        }
    }
}

$results = @(
    Invoke-NptTest "IIS home" "GET" "$WebBaseUrl/"
    Invoke-NptTest "Current time" "GET" "$ApiBaseUrl/api/time"
    Invoke-NptTest "Valid message" "POST" "$ApiBaseUrl/api/message" '{"message":"Xin chao NPT"}'
    Invoke-NptTest "Empty message" "POST" "$ApiBaseUrl/api/message" '{}'
    Invoke-NptTest "Students" "GET" "$ApiBaseUrl/api/students"
    Invoke-NptTest "Not found" "GET" "$ApiBaseUrl/abc"
)

$results | Format-Table -AutoSize

Write-Host "`nKiem tra $ConcurrentRequests request dong thoi toi /api/time..."
$jobs = 1..$ConcurrentRequests | ForEach-Object {
    Start-Job -ScriptBlock {
        param($Url, $Timeout)
        try {
            $response = Invoke-WebRequest -Uri $Url -TimeoutSec $Timeout -UseBasicParsing
            [int]$response.StatusCode
        } catch {
            if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
                [int]$_.Exception.Response.StatusCode
            } else {
                -1
            }
        }
    } -ArgumentList "$ApiBaseUrl/api/time", $TimeoutSeconds
}
$statuses = $jobs | Receive-Job -Wait
$jobs | Remove-Job -Force
$statuses | Group-Object | Select-Object @{Name="Status";Expression={$_.Name}}, Count | Format-Table -AutoSize
