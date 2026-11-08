# Pruebas de caos para Windows (PowerShell 5.1 o 7+).
#   .\chaos\chaos.ps1 db-down ticket
#   .\chaos\chaos.ps1 db-up ticket
#   .\chaos\chaos.ps1 db-down all
#   .\chaos\chaos.ps1 redis-down | redis-up
#   .\chaos\chaos.ps1 service-down user | service-up user
#   .\chaos\chaos.ps1 status
#   .\chaos\chaos.ps1 scenario      # demo automática de caída y recuperación de la BD de tickets
param(
    [Parameter(Position = 0)][string]$Action = "help",
    [Parameter(Position = 1)][string]$Target = ""
)
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
$Base = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }

function Get-Dbs([string]$t) {
    switch ($t) {
        { $_ -in "auth", "user", "ticket" } { return @("mysql-$t") }
        "all" { return @("mysql-auth", "mysql-user", "mysql-ticket") }
        default { throw "Uso: chaos.ps1 db-down|db-up auth|user|ticket|all" }
    }
}

function Invoke-Api([string]$Method, [string]$Path, [string]$Token = "", [string]$Body = "") {
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{ Uri = "$Base$Path"; Method = $Method; Headers = $headers; UseBasicParsing = $true; TimeoutSec = 15 }
    if ($Body) { $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($Body) }
    try {
        $r = Invoke-WebRequest @params
        return [pscustomobject]@{ Status = [int]$r.StatusCode; Cache = $r.Headers["X-Cache"]; Body = $r.Content }
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { return [pscustomobject]@{ Status = 0; Cache = "-"; Body = $_.Exception.Message } }
        $content = ""
        try { $content = (New-Object System.IO.StreamReader($resp.GetResponseStream())).ReadToEnd() } catch { $content = $_.ErrorDetails.Message }
        return [pscustomobject]@{ Status = [int]$resp.StatusCode; Cache = "-"; Body = $content }
    }
}

function Write-Result([string]$Label, $r) {
    $body = if ($r.Body) { $r.Body.Substring(0, [Math]::Min(110, $r.Body.Length)) } else { "" }
    $cache = if ($r.Cache) { $r.Cache } else { "-" }
    "[{0}] {1,-52} -> HTTP {2}  X-Cache:{3,-6} {4}" -f (Get-Date -Format "HH:mm:ss"), $Label, $r.Status, $cache, $body
}

function Wait-Healthy([string]$Container) {
    Write-Host "Esperando a que $Container esté healthy..."
    while ((docker inspect -f "{{.State.Health.Status}}" $Container) -ne "healthy") { Start-Sleep -Seconds 2 }
    Write-Host "$Container healthy"
}

function Start-Scenario {
    Write-Host "=== Escenario: caída y recuperación de la BD de tickets (mysql-ticket) ==="
    $login = Invoke-RestMethod -Method Post -Uri "$Base/auth/login" -ContentType "application/json" `
        -Body '{"email":"cgonzalez@techsupport.pe","password":"Cliente123!"}'
    $token = $login.accessToken
    $critical = '{"title":"Caída total del ERP","description":"El ERP no conecta en producción","category":"BASES_DATOS"}'

    $r = Invoke-Api POST "/tickets" $token $critical; Write-Result "1. Crear ticket crítico (BD arriba)" $r
    $ticket = if ($r.Status -eq 201) { ($r.Body | ConvertFrom-Json).ticketId } else { ((Invoke-Api GET "/tickets/client/USR-C0001" $token).Body | ConvertFrom-Json)[0].ticketId }

    Write-Result "2. GET /tickets/$ticket (llena la caché)" (Invoke-Api GET "/tickets/$ticket" $token)
    Write-Result "3. GET /tickets/$ticket (desde Redis)" (Invoke-Api GET "/tickets/$ticket" $token)

    Write-Host "`n>>> docker compose stop mysql-ticket"; docker compose stop mysql-ticket | Out-Null; Start-Sleep 2
    1..3 | ForEach-Object { Write-Result "4.$_ GET /tickets/$ticket (BD caída)" (Invoke-Api GET "/tickets/$ticket" $token) }
    Write-Result "5. GET /tickets/client/USR-C0001 (BD caída)" (Invoke-Api GET "/tickets/client/USR-C0001" $token)
    $body = '{"title":"Prueba","description":"Con BD caída","category":"SISTEMAS_SOFTWARE"}'
    1..5 | ForEach-Object { Write-Result "6.$_ POST /tickets (BD caída -> 503 rápido)" (Invoke-Api POST "/tickets" $token $body) }
    Write-Result "7. GET /users/clients/USR-C0001 (otra BD)" (Invoke-Api GET "/users/clients/USR-C0001" $token)

    Write-Host "`n>>> docker compose start mysql-ticket"; docker compose start mysql-ticket | Out-Null
    Wait-Healthy "mysql-ticket"; Write-Host "Esperando al circuit breaker (12 s)..."; Start-Sleep 12
    $body2 = '{"title":"Post recuperación","description":"La BD volvió","category":"SISTEMAS_SOFTWARE"}'
    Write-Result "8. POST /tickets (BD recuperada)" (Invoke-Api POST "/tickets" $token $body2)
    Write-Result "9. GET /tickets/$ticket (datos frescos)" (Invoke-Api GET "/tickets/$ticket" $token)
    Write-Host "`nRevise Grafana -> '3 · Resiliencia y caché' (http://localhost:3000)"
}

switch ($Action) {
    "db-down" { docker compose stop (Get-Dbs $Target) }
    "db-up" { docker compose start (Get-Dbs $Target); Get-Dbs $Target | ForEach-Object { Wait-Healthy $_ } }
    "redis-down" { docker compose stop redis }
    "redis-up" { docker compose start redis }
    "service-down" { docker compose stop "$Target-service" }
    "service-up" { docker compose start "$Target-service" }
    "status" { docker compose ps --format "table {{.Name}}\t{{.Status}}" }
    "scenario" { Start-Scenario }
    default { Get-Content $PSCommandPath -TotalCount 8 }
}
