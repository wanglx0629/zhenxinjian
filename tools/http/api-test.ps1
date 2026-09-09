# API test helper (ASCII comments only, see tools/http/README.md)
# Why BodyFile: this shell passes quotes literally into powershell args,
# so inline -Body never survives. Always pass JSON via file.
param(
    [string]$Method = "Get",
    [string]$Path = "/",
    [string]$BodyFile = "",
    [string]$Token = "",
    [string]$BaseUrl = "http://127.0.0.1:8080/api"
)
$headers = @{}
if ($Token -ne "") { $headers["Authorization"] = "Bearer $Token" }
$uri = $BaseUrl + $Path
try {
    if ($BodyFile -ne "") {
        $body = Get-Content -Raw -Encoding UTF8 $BodyFile
        $r = Invoke-RestMethod -Uri $uri -Method $Method -ContentType "application/json; charset=utf-8" -Body $body -Headers $headers
    } else {
        $r = Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers
    }
    $r | ConvertTo-Json -Depth 8 -Compress
} catch {
    if ($_.ErrorDetails) {
        $_.ErrorDetails.Message
    } else {
        $resp = $_.Exception.Response
        if ($resp) {
            $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
            $sr.ReadToEnd()
        } else {
            $_.Exception.Message
        }
    }
}
