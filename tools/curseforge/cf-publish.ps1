<#
    CurseForge publishing helper - MultiHooker (project 1740006)

    Two modes:
      Upload a NEW file          -Jar <path to .jar>
      Update an EXISTING file    -FileID <id>

    The API token is read from a file OUTSIDE this git repo
    (default: C:\Users\Mikeg\Desktop\Modding Project\.curseforge-token.txt).
    It is never printed in full and never written into the repo.

    EXAMPLES

      # 1. see exactly what would be sent; nothing is uploaded
      .\cf-publish.ps1 -Jar ..\..\build\libs\multihooker-3.10.6.jar `
                       -ChangelogFile 'C:\Users\Mikeg\Desktop\cf-3.10.6.txt' -DryRun

      # 2. actually publish it
      .\cf-publish.ps1 -Jar ..\..\build\libs\multihooker-3.10.6.jar `
                       -ChangelogFile 'C:\Users\Mikeg\Desktop\cf-3.10.6.txt'

      # 3. fix the changelog / name of an already published file
      .\cf-publish.ps1 -FileID 9201284 -ChangelogFile 'C:\Users\Mikeg\Desktop\cf-3.10.5.txt'

    Game version IDs (Minecraft 1.21.1 / NeoForge):
      Client   = 9638    (typeId 75208)
      1.21.1   = 11779   (typeId 77784)
      NeoForge = 10150   (typeId 68441)
    These are the exact three the live 3.10.5 file uses.
#>
[CmdletBinding(DefaultParameterSetName = 'Publish')]
param(
    # --- upload a new file ---------------------------------------------------
    [Parameter(ParameterSetName = 'Publish', Mandatory = $true, Position = 0)]
    [string]$Jar,

    # --- update an existing file --------------------------------------------
    [Parameter(ParameterSetName = 'Update')]
    [switch]$Edit,

    [Parameter(ParameterSetName = 'Update', Mandatory = $true)]
    [int]$FileID,

    # --- common -------------------------------------------------------------
    [string]$DisplayName,
    [string]$ChangelogFile,
    [string]$ChangelogText,
    [ValidateSet('text', 'markdown', 'html')]
    [string]$ChangelogType = 'text',
    [ValidateSet('release', 'beta', 'alpha')]
    [string]$ReleaseType = 'release',

    # Client, 1.21.1, NeoForge
    [int[]]$GameVersions = @(9638, 11779, 10150),

    [int]$ProjectId = 1740006,

    # minecraft.curseforge.com returns Minecraft-scoped data; www is the fallback
    [string]$ApiHost = 'minecraft.curseforge.com',

    [string]$TokenPath = 'C:\Users\Mikeg\Desktop\Modding Project\.curseforge-token.txt',

    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'

function Get-CfToken {
    if ($env:CURSEFORGE_TOKEN) { return $env:CURSEFORGE_TOKEN.Trim() }
    if (-not (Test-Path -LiteralPath $TokenPath)) {
        throw "Token file not found: $TokenPath`nCreate it (outside the git repo) and paste the token in, or pass -TokenPath."
    }
    $t = ([IO.File]::ReadAllText($TokenPath) -replace '\s', '')
    if ([string]::IsNullOrWhiteSpace($t)) { throw "Token file is empty: $TokenPath" }
    if ($t.Length -lt 20) { Write-Warning "Token looks unusually short ($($token.Length) chars)." }
    return $t
}

function Invoke-CfMultipart {
    param(
        [string]$Url,
        [string]$Token,
        [string]$MetadataJson,
        [string]$FilePath
    )

    Add-Type -AssemblyName System.Net.Http

    $client = New-Object System.Net.Http.HttpClient
    $client.Timeout = [TimeSpan]::FromMinutes(15)
    [void]$client.DefaultRequestHeaders.Add('X-Api-Token', $Token)
    [void]$client.DefaultRequestHeaders.Add('User-Agent', 'multihooker-publish-script')

    $utf8 = New-Object System.Text.UTF8Encoding($false)
    $form = New-Object System.Net.Http.MultipartFormDataContent
    $stream = $null

    try {
        $meta = New-Object System.Net.Http.StringContent($MetadataJson, $utf8, 'application/json')
        $form.Add($meta, 'metadata')

        if ($FilePath) {
            $stream = [IO.File]::OpenRead($FilePath)
            $fileContent = New-Object System.Net.Http.StreamContent($stream)
            $fileContent.Headers.ContentType =
                [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('application/java-archive')
            $form.Add($fileContent, 'file', [IO.Path]::GetFileName($FilePath))
        }

        $resp = $client.PostAsync($Url, $form).GetAwaiter().GetResult()
        $body = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()

        [pscustomobject]@{
            Url     = $Url
            Status  = [int]$resp.StatusCode
            Ok      = $resp.IsSuccessStatusCode
            Body    = $body
            Reason  = [string]$resp.StatusCode
        }
    }
    catch {
        [pscustomobject]@{
            Url    = $Url
            Status = 0
            Ok     = $false
            Body   = $_.Exception.Message
            Reason = 'transport error'
        }
    }
    finally {
        if ($stream) { $stream.Dispose() }
        $form.Dispose()
        $client.Dispose()
    }
}

# ---------------------------------------------------------------------------
# Gather what we are going to send
# ---------------------------------------------------------------------------
$token = ''
if (-not $DryRun) {
    $token = ([IO.File]::ReadAllText($TokenPath)).Trim()
    if (-not $token) { throw "Token file is empty: $TokenPath" }
}

$changelog = $null
if ($ChangelogFile) {
    if (-not (Test-Path $ChangelogFile)) { throw "Changelog file not found: $ChangelogFile" }
    $raw = [IO.File]::ReadAllText($ChangelogFile)
    $raw = ($raw -replace "`r`n", "`n") -replace "`r", "`n"   # normalise
    $changelog = $raw -replace "`n", "`r`n"                    # store as CRLF
}
elseif ($ChangelogText) {
    $changelog = ($ChangelogText -replace "`r`n", "`n") -replace "`n", "`r`n"
}

$releaseTypeId = $null
if ($PSBoundParameters.ContainsKey('ReleaseType')) {
    $releaseTypeId = @{ release = 'release'; beta = 'beta'; alpha = 'alpha' }[$ReleaseType]
}
if (-not $PSBoundParameters.ContainsKey('ReleaseType')) {
    $releaseTypeName = $ReleaseType
}
else {
    $releaseTypeName = $ReleaseType
}

$metadata = [ordered]@{}
if ($PSCmdlet.ParameterSetName -eq 'Publish') {
    if (-not (Test-Path $Jar)) { throw "Jar not found: $Jar" }
    $jarItem = Get-Item $Jar
    if (-not $DisplayName) { $DisplayName = $jarItem.Name }
    $metadata['displayName']  = $DisplayName
    $metadata['gameVersions'] = $GameVersions
    $metadata['releaseType']  = $ReleaseType
    if ($changelog) {
        $metadata['changelog']     = $changelog
        $metadata['changelogType'] = $ChangelogType
    }
    $endpoint = 'upload-file'
}
else {
    $metadata['fileID'] = $FileID
    if ($DisplayName)  { $metadata['displayName']  = $DisplayName }
    if ($PSBoundParameters.ContainsKey('GameVersions')) { $metadata['gameVersions'] = $GameVersions }
    if ($PSBoundParameters.ContainsKey('ReleaseType')) { $metadata['releaseType'] = $ReleaseType }
    if ($changelog) {
        $metadata['changelog']     = $changelog
        $metadata['changelogType'] = $ChangelogType
    }
    if ($metadata.Count -le 1) {
        throw "Update mode needs at least one thing to change: -DisplayName, -ChangelogFile/-ChangelogText, -ReleaseType or -GameVersions."
    }
    $endpoint = 'update-file'
}

$json = $metadata | ConvertTo-Json -Depth 6 -Compress
$url  = "https://$ApiHost/api/projects/$ProjectId/$endpoint"

# ---------------------------------------------------------------------------
# Report / send
# ---------------------------------------------------------------------------
Write-Host ''
Write-Host ('Mode      : ' + $PSCmdlet.ParameterSetName) -ForegroundColor Cyan
Write-Host ('Endpoint  : ' + $url)
Write-Host ('Project   : ' + $ProjectId)
if ($PSCmdlet.ParameterSetName -eq 'Publish') {
    Write-Host ('Jar       : ' + (Get-Item $Jar).FullName + '  (' + (Get-Item $Jar).Length + ' bytes)')
}
Write-Host ('Name      : ' + $(if ($DisplayName) { $DisplayName } else { '(unchanged)' }))
$relStr = $ReleaseType
if ($PSBoundParameters.ContainsKey('ReleaseType')) { $relStr = $ReleaseType } else { $relStr = '(unchanged)' }
Write-Host ('Released  : ' + $relStr)
Write-Host ('Versions  : ' + ($GameVersions -join ', '))
if ($changelog) {
    Write-Host ('Changelog : ' + $changelog.Length + ' chars')
    Write-Host '---------- changelog preview (first 12 lines) ----------'
    ($changelog -split "`r`n" | Select-Object -First 12) | ForEach-Object { Write-Host ('  ' + $_) }
    Write-Host '-------------------------------------------------------'
}
else {
    Write-Host 'Changelog : (none)'
}
if ($token) {
    Write-Host ('Token     : ' + $token.Substring(0, [Math]::Min(8, $token.Length)) + '... (not shown in full)')
}
Write-Host ''

if ($DryRun) {
    Write-Host 'DRY RUN - nothing was sent.' -ForegroundColor Yellow
    Write-Host ''
    Write-Host 'metadata JSON that would be POSTed:'
    Write-Host $json
    return
}

Write-Host 'Sending...' -ForegroundColor Yellow
$result = Invoke-CfMultipart -Url $url -Token $token -MetadataJson $json -FilePath $(if ($PSCmdlet.ParameterSetName -eq 'Publish') { $Jar } else { $null })

# retry once on the other host if the endpoint was not found there
if (-not $result.Ok -and $result.Status -in @(404, 405, 0)) {
    $other = if ($ApiHost -eq 'minecraft.curseforge.com') { 'www.curseforge.com' } else { 'minecraft.curseforge.com' }
    Write-Host ("No luck on $ApiHost (" + $result.Status + "), retrying on $other ...") -ForegroundColor Yellow
    $url2 = "https://$other/api/projects/$ProjectId/$endpoint"
    $result = Invoke-CfMultipart -Url $url2 -Token $token -MetadataJson $json -FilePath $(if ($PSCmdlet.ParameterSetName -eq 'Publish') { $Jar } else { $null })
}

Write-Host ''
if ($result.Ok) {
    Write-Host ('SUCCESS  HTTP ' + $result.Status) -ForegroundColor Green
    Write-Host $result.Body
    try {
        $o = $result.Body | ConvertFrom-Json
        $id = $o.id
        if (-not $id) { $id = $o.data.id }
        if ($id) {
            Write-Host ''
            Write-Host ('File page : https://www.curseforge.com/minecraft/mc-mods/multihooker/files/' + $id) -ForegroundColor Green
        }
    }
    catch { }
}
else {
    Write-Host ('FAILED  HTTP ' + $result.Status + ' (' + $result.Reason + ')') -ForegroundColor Red
    Write-Host $result.Body
    exit 1
}
