param(
    [switch]$Rebuild,
    [int]$DockerTimeoutSeconds = 180,
    [int]$ReadyTimeoutSeconds = 60
)

$ErrorActionPreference = 'Stop'
function Get-Sha256([string]$path) {
    $stream=[IO.File]::OpenRead($path)
    $algorithm=[Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-','') }
    finally { $stream.Dispose();$algorithm.Dispose() }
}
$dockerCommand = Get-Command docker.exe -ErrorAction SilentlyContinue
if (-not $dockerCommand) {
    $dockerCandidates = @(
        (Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\resources\bin\docker.exe'),
        (Join-Path $env:ProgramFiles 'Docker\Docker\resources\bin\docker.exe')
    )
    $dockerPath = $dockerCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $dockerPath) { throw 'Docker Desktop was not found. Install Docker Desktop with the WSL2 Linux engine first.' }
} else { $dockerPath = $dockerCommand.Source }

function Test-DockerReady {
    # Windows PowerShell 5 treats native stderr as an error record when Docker is still starting.
    $ErrorActionPreference = 'Continue'
    $null = & $dockerPath info --format '{{.OSType}}' 2>$null
    return $LASTEXITCODE -eq 0
}

if (-not (Test-DockerReady)) {
    $desktopCandidates = @(
        (Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\Docker Desktop.exe'),
        (Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'),
        (Join-Path (Split-Path (Split-Path $dockerPath -Parent) -Parent) 'Docker Desktop.exe')
    )
    $desktopPath = $desktopCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $desktopPath) { throw 'Docker is not ready and Docker Desktop was not found. Start the Linux Docker engine and retry.' }
    if (-not (Get-Process -Name 'Docker Desktop' -ErrorAction SilentlyContinue)) {
        Write-Host 'Starting Docker Desktop...'
        Start-Process -FilePath $desktopPath -WindowStyle Hidden | Out-Null
    }
    Write-Host "Waiting for Docker (up to $DockerTimeoutSeconds seconds)..."
    $deadline = [DateTime]::UtcNow.AddSeconds($DockerTimeoutSeconds)
    while (-not (Test-DockerReady)) {
        if ([DateTime]::UtcNow -ge $deadline) { throw 'Docker startup timed out. Check Docker Desktop / WSL2 and retry the one-click launcher.' }
        Start-Sleep -Seconds 2
    }
}
$engine = & $dockerPath info --format '{{.OSType}}'
if ($LASTEXITCODE -ne 0 -or "$engine".Trim() -ne 'linux') { throw 'The sample sandbox requires the Docker Linux engine. Switch Docker Desktop to Linux containers.' }

$previousToken=$env:JUDGE_TOKEN
Push-Location $PSScriptRoot
try {
    $tokenPath=Join-Path $PSScriptRoot '.judge-token'
    if (-not (Test-Path -LiteralPath $tokenPath)) {
        $bytes=New-Object byte[] 32
        $random=[Security.Cryptography.RandomNumberGenerator]::Create()
        try { $random.GetBytes($bytes) } finally { $random.Dispose() }
        [IO.File]::WriteAllText($tokenPath,(-join ($bytes | ForEach-Object { $_.ToString("x2") })))
    }
    $env:JUDGE_TOKEN=[IO.File]::ReadAllText($tokenPath).Trim()
    if ([string]::IsNullOrWhiteSpace($env:JUDGE_TOKEN)) { throw 'The sample sandbox token file is empty.' }
    $ErrorActionPreference = 'Continue'
    $null = & $dockerPath image inspect acmate-go-judge:1.13.3 --format '{{.Id}}' 2>$null
    $hasImage = $LASTEXITCODE -eq 0
    $ErrorActionPreference = 'Stop'
    $stampPath=Join-Path $PSScriptRoot '.judge-build.sha256'
    # Only image inputs require rebuilding; changes to the downloader itself do not.
    $buildSources=@('Dockerfile','csharp-compile.sh','.dockerignore')
    $buildFingerprint=($buildSources | ForEach-Object { Get-Sha256 (Join-Path $PSScriptRoot $_) }) -join ':'
    $previousFingerprint=if(Test-Path -LiteralPath $stampPath){[IO.File]::ReadAllText($stampPath)}else{''}
    $fingerprintMatches=$previousFingerprint -eq $buildFingerprint
    $previousParts=$previousFingerprint -split ':'
    $currentParts=$buildFingerprint -split ':'
    # Accept the former four-part stamp when its three image inputs still match.
    if(-not $fingerprintMatches -and $previousParts.Count -eq 4 -and $currentParts.Count -eq 3) {
        $fingerprintMatches=$previousParts[0] -eq $currentParts[0] -and $previousParts[1] -eq $currentParts[1] -and $previousParts[3] -eq $currentParts[2]
    }
    $needsBuild=$Rebuild -or -not $hasImage -or -not $fingerprintMatches
    if ($needsBuild) {
        Write-Host 'Building the sample sandbox image (new or changed language environment)...'
        & (Join-Path $PSScriptRoot 'prepare-dotnet.ps1')
        & $dockerPath compose up -d --build
    } else {
        Write-Host 'Starting the sample sandbox with the existing image...'
        & $dockerPath compose up -d --no-build
    }
    if ($LASTEXITCODE -ne 0) { throw 'Sample sandbox deployment failed' }
    if($needsBuild -or $previousFingerprint -ne $buildFingerprint){[IO.File]::WriteAllText($stampPath,$buildFingerprint)}
    $deadline = [DateTime]::UtcNow.AddSeconds($ReadyTimeoutSeconds)
    while ($true) {
        try {
            $null = Invoke-WebRequest -Uri 'http://127.0.0.1:5050/version' -UseBasicParsing -TimeoutSec 3 -Headers @{ Authorization = "Bearer $env:JUDGE_TOKEN" }
            break
        } catch {
            if ([DateTime]::UtcNow -ge $deadline) { throw 'Sample sandbox did not become ready on 127.0.0.1:5050. Check Docker Desktop and retry.' }
            Start-Sleep -Seconds 1
        }
    }
    Write-Host 'Sample sandbox is ready on http://127.0.0.1:5050.'
} finally {
    if ($null -eq $previousToken) { Remove-Item Env:JUDGE_TOKEN -ErrorAction SilentlyContinue } else { $env:JUDGE_TOKEN=$previousToken }
    Pop-Location
}
