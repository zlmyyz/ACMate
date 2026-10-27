$ErrorActionPreference='Stop'
function Get-Sha512([string]$path) {
    $stream=[IO.File]::OpenRead($path)
    $algorithm=[Security.Cryptography.SHA512]::Create()
    try { return [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-','') }
    finally { $stream.Dispose();$algorithm.Dispose() }
}
$mutex=New-Object System.Threading.Mutex($false,'Local\ACMate_Judge_Dotnet_SDK_10_0_403')
$held=$false
try {
    try{$held=$mutex.WaitOne(900000)}catch [System.Threading.AbandonedMutexException]{$held=$true}
    if(-not $held){throw 'Another C# SDK preparation is still running. Retry the launcher later.'}
$cacheDirectory=Join-Path $PSScriptRoot '.sdk-cache'
$archive=Join-Path $cacheDirectory 'dotnet-sdk-10.0.403-linux-x64.tar.gz'
$expectedHash='b7b3d8461cf98b07d2552ce9199702fa3ad010ac2671a5f8cd5136f7534e50f6855ba7bbb02b11494563f1d04156276a31cda1103b900e5eecd0d9473dc7b9c5'
if(Test-Path -LiteralPath $archive) {
    if((Get-Sha512 $archive) -eq $expectedHash){return}
    Remove-Item -LiteralPath $archive
}
New-Item -ItemType Directory -Path $cacheDirectory -Force | Out-Null
$partial=$archive+'.download'
$url='https://dotnetcli.blob.core.windows.net/dotnet/Sdk/10.0.403/dotnet-sdk-10.0.403-linux-x64.tar.gz'
Write-Host 'Downloading the C# SDK from Microsoft (resumable, first build only)...'
$curl=Get-Command curl.exe -ErrorAction SilentlyContinue
if($curl) {
    & $curl.Source --fail --location --retry 5 --retry-all-errors --connect-timeout 15 --speed-time 60 --speed-limit 1024 --continue-at - --output $partial $url
    if($LASTEXITCODE -eq 33) {
        # Some network proxies ignore Range. Retry a clean download instead of keeping a corrupt partial file.
        Remove-Item -LiteralPath $partial -ErrorAction SilentlyContinue
        & $curl.Source --fail --location --retry 5 --retry-all-errors --connect-timeout 15 --output $partial $url
    }
    if($LASTEXITCODE -ne 0){throw 'C# SDK download failed. Run the one-click launcher again to resume.'}
}else {
    Invoke-WebRequest -Uri $url -OutFile $partial -UseBasicParsing -TimeoutSec 600
}
if((Get-Sha512 $partial) -ne $expectedHash) {
    Remove-Item -LiteralPath $partial
    throw 'C# SDK checksum verification failed. No unverified SDK will be installed; retry the launcher.'
}
Move-Item -LiteralPath $partial -Destination $archive
Write-Host 'Microsoft C# SDK checksum verified.'

} finally {if($held){$mutex.ReleaseMutex()};$mutex.Dispose()}
