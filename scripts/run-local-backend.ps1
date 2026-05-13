[CmdletBinding()]
param(
  [ValidateRange(1, 65535)]
  [int] $PostgresPort = 0
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptRoot

function Test-NativePostgresOnDefaultPort {
  $listeners = Get-NetTCPConnection -LocalPort 5432 -State Listen -ErrorAction SilentlyContinue
  foreach ($listener in $listeners) {
    try {
      $process = Get-Process -Id $listener.OwningProcess -ErrorAction Stop
      if ($process.ProcessName -eq "postgres") {
        return $true
      }
    } catch {
      # Ignore processes that disappear while checking the port.
    }
  }

  return $false
}

if ($PostgresPort -eq 0) {
  if (Test-NativePostgresOnDefaultPort) {
    $PostgresPort = 55432
    Write-Warning "Native PostgreSQL is already listening on 5432. Using project Docker PostgreSQL on 55432."
  } else {
    $PostgresPort = 5432
  }
}

Push-Location $repoRoot
try {
  $env:APP_POSTGRES_PORT = "$PostgresPort"
  docker compose up -d postgres minio

  $env:SPRING_PROFILES_ACTIVE = "local"
  $env:APP_DB_URL = "jdbc:postgresql://localhost:$PostgresPort/activity_platform"
  if (-not $env:APP_DB_USERNAME) {
    $env:APP_DB_USERNAME = "activity_app"
  }
  if (-not $env:APP_DB_PASSWORD) {
    $env:APP_DB_PASSWORD = "activity_app"
  }

  Write-Host "Backend DB URL: $env:APP_DB_URL"
  Push-Location (Join-Path $repoRoot "backend")
  try {
    .\mvnw.cmd spring-boot:run
  } finally {
    Pop-Location
  }
} finally {
  Pop-Location
}
