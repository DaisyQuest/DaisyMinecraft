[CmdletBinding()]
param(
    [ValidateSet("paper")]
    [string]$Provider = "paper",

    [string]$Version = "latest",

    [string]$OutputDirectory = (Join-Path (Get-Location) "build/minecraft-server-jars"),

    [string]$OutputName = "server.jar",

    [string]$SourceUrl,

    [string]$SourceSha256,

    [switch]$Upload,

    [string]$StorageAccountName,

    [string]$ResourceGroup,

    [string]$Location,

    [string]$StorageContainerName = "minecraft-artifacts",

    [string]$BlobName,

    [int]$SasExpiryDays = 365,

    [switch]$PublicBlob,

    [string]$GitHubEnvPath
)

$ErrorActionPreference = "Stop"

function Assert-SafeJarName([string]$Name) {
    if ([string]::IsNullOrWhiteSpace($Name)) {
        throw "Jar output name cannot be empty."
    }
    if ($Name -ne [System.IO.Path]::GetFileName($Name)) {
        throw "Jar output name must not contain path separators: $Name"
    }
    if (-not $Name.EndsWith(".jar", [StringComparison]::OrdinalIgnoreCase)) {
        throw "Jar output name must end with .jar: $Name"
    }
}

function ConvertTo-VersionKey([string]$Value) {
    $parts = $Value.Split(".") | ForEach-Object { [int]$_ }
    while ($parts.Count -lt 4) {
        $parts += 0
    }
    [version]::new($parts[0], $parts[1], $parts[2], $parts[3])
}

function Resolve-LatestPaperVersion() {
    $project = Invoke-RestMethod -Uri "https://api.papermc.io/v2/projects/paper"
    $stableVersions = @($project.versions | Where-Object { $_ -match '^\d+\.\d+(\.\d+)?$' })
    if ($stableVersions.Count -eq 0) {
        throw "PaperMC did not return any stable Minecraft versions."
    }
    $stableVersions | Sort-Object { ConvertTo-VersionKey $_ } | Select-Object -Last 1
}

function Resolve-PaperServerJar([string]$RequestedVersion) {
    $resolvedVersion = $RequestedVersion
    if ($RequestedVersion -eq "latest") {
        $resolvedVersion = Resolve-LatestPaperVersion
    }

    $versionMetadata = Invoke-RestMethod -Uri "https://api.papermc.io/v2/projects/paper/versions/$resolvedVersion"
    $latestBuild = @($versionMetadata.builds | Sort-Object { [int]$_ } | Select-Object -Last 1)[0]
    if (-not $latestBuild) {
        throw "PaperMC did not return builds for version $resolvedVersion."
    }

    $buildMetadata = Invoke-RestMethod -Uri "https://api.papermc.io/v2/projects/paper/versions/$resolvedVersion/builds/$latestBuild"
    $jarName = $buildMetadata.downloads.application.name
    $sha256 = $buildMetadata.downloads.application.sha256
    if (-not $jarName -or -not $sha256) {
        throw "PaperMC build $resolvedVersion/$latestBuild did not include application jar metadata."
    }

    [pscustomobject]@{
        provider = "paper"
        version = $resolvedVersion
        build = [int]$latestBuild
        jarName = $jarName
        url = "https://api.papermc.io/v2/projects/paper/versions/$resolvedVersion/builds/$latestBuild/downloads/$jarName"
        expectedSha256 = $sha256.ToLowerInvariant()
    }
}

function Require-AzLogin() {
    if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
        throw "Azure CLI is required for upload. Install az, run az login, then rerun this script."
    }
    $account = az account show --only-show-errors 2>$null
    if (-not $account) {
        throw "Azure CLI is not logged in. Run az login before uploading the Minecraft server jar."
    }
}

function Invoke-AzJson([string[]]$Arguments) {
    $json = az @Arguments --only-show-errors -o json
    if (-not $json) {
        return $null
    }
    $json | ConvertFrom-Json
}

function Upload-ServerJarToAzureBlob(
    [string]$FilePath,
    [string]$AccountName,
    [string]$ContainerName,
    [string]$Name,
    [string]$Group,
    [string]$AzureLocation,
    [bool]$MakePublic,
    [int]$ExpiryDays
) {
    Require-AzLogin

    if ([string]::IsNullOrWhiteSpace($AccountName)) {
        throw "StorageAccountName is required when uploading the server jar."
    }
    if ([string]::IsNullOrWhiteSpace($Name)) {
        throw "BlobName is required when uploading the server jar."
    }

    $accountResourceGroup = $Group
    $storageAccountJson = az storage account show --name $AccountName --only-show-errors -o json 2>$null
    $storageExists = $LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($storageAccountJson)
    if ($storageExists) {
        $storageAccount = $storageAccountJson | ConvertFrom-Json
        if ($storageAccount -and $storageAccount.resourceGroup) {
            $accountResourceGroup = $storageAccount.resourceGroup
        }
    }

    if (-not $storageExists) {
        if ([string]::IsNullOrWhiteSpace($Group) -or [string]::IsNullOrWhiteSpace($AzureLocation)) {
            throw "Storage account $AccountName does not exist. Provide ResourceGroup and Location to create it, or create it first in Azure."
        }
        $allowPublicBlobAccess = $MakePublic.ToString().ToLowerInvariant()
        az group create --name $Group --location $AzureLocation --only-show-errors | Out-Null
        az storage account create `
            --name $AccountName `
            --resource-group $Group `
            --location $AzureLocation `
            --sku Standard_LRS `
            --kind StorageV2 `
            --allow-blob-public-access $allowPublicBlobAccess `
            --only-show-errors | Out-Null
        $accountResourceGroup = $Group
    }

    if ([string]::IsNullOrWhiteSpace($accountResourceGroup)) {
        throw "Could not determine resource group for storage account $AccountName."
    }

    $accountKey = az storage account keys list `
        --account-name $AccountName `
        --resource-group $accountResourceGroup `
        --query "[0].value" `
        --only-show-errors `
        -o tsv
    if ([string]::IsNullOrWhiteSpace($accountKey)) {
        throw "Could not retrieve an access key for storage account $AccountName."
    }

    $publicAccess = if ($MakePublic) { "blob" } else { "off" }
    az storage container create `
        --account-name $AccountName `
        --account-key $accountKey `
        --name $ContainerName `
        --public-access $publicAccess `
        --only-show-errors | Out-Null

    az storage blob upload `
        --account-name $AccountName `
        --account-key $accountKey `
        --container-name $ContainerName `
        --name $Name `
        --file $FilePath `
        --overwrite true `
        --only-show-errors | Out-Null

    $baseUrl = az storage blob url `
        --account-name $AccountName `
        --account-key $accountKey `
        --container-name $ContainerName `
        --name $Name `
        --only-show-errors `
        -o tsv

    if ($MakePublic) {
        return $baseUrl
    }

    $expiry = (Get-Date).ToUniversalTime().AddDays($ExpiryDays).ToString("yyyy-MM-ddTHH:mmZ")
    $sas = az storage blob generate-sas `
        --account-name $AccountName `
        --account-key $accountKey `
        --container-name $ContainerName `
        --name $Name `
        --permissions r `
        --expiry $expiry `
        --https-only `
        --only-show-errors `
        -o tsv

    if ([string]::IsNullOrWhiteSpace($sas)) {
        throw "Azure did not return a SAS token for $AccountName/$ContainerName/$Name."
    }

    "$baseUrl`?$($sas.TrimStart('?'))"
}

Assert-SafeJarName $OutputName

if ($Provider -ne "paper") {
    throw "Unsupported provider: $Provider"
}

$resolved = if ($SourceUrl) {
    [pscustomobject]@{
        provider = "custom"
        version = $Version
        build = $null
        jarName = [System.IO.Path]::GetFileName(([uri]$SourceUrl).AbsolutePath)
        url = $SourceUrl
        expectedSha256 = if ($SourceSha256) { $SourceSha256.ToLowerInvariant() } else { $null }
    }
} else {
    Resolve-PaperServerJar $Version
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$jarPath = Join-Path $OutputDirectory $OutputName

Write-Host "Downloading $($resolved.provider) server jar: $($resolved.url)"
Invoke-WebRequest -Uri $resolved.url -OutFile $jarPath

$actualSha256 = (Get-FileHash -Algorithm SHA256 $jarPath).Hash.ToLowerInvariant()
if ($resolved.expectedSha256 -and $actualSha256 -ne $resolved.expectedSha256) {
    Remove-Item -LiteralPath $jarPath -Force
    throw "Downloaded jar SHA256 mismatch. Expected $($resolved.expectedSha256), got $actualSha256."
}

$effectiveSha256 = if ($resolved.expectedSha256) { $resolved.expectedSha256 } else { $actualSha256 }
$effectiveBlobName = if ($BlobName) { $BlobName } else { $OutputName }
$uploadedUrl = $null

if ($Upload -or $StorageAccountName) {
    $uploadedUrl = Upload-ServerJarToAzureBlob `
        -FilePath $jarPath `
        -AccountName $StorageAccountName `
        -ContainerName $StorageContainerName `
        -Name $effectiveBlobName `
        -Group $ResourceGroup `
        -AzureLocation $Location `
        -MakePublic ([bool]$PublicBlob) `
        -ExpiryDays $SasExpiryDays
}

$manifest = [pscustomobject]@{
    provider = $resolved.provider
    version = $resolved.version
    build = $resolved.build
    sourceJarName = $resolved.jarName
    sourceUrl = $resolved.url
    localPath = (Resolve-Path $jarPath).Path
    outputName = $OutputName
    sha256 = $effectiveSha256
    sizeBytes = (Get-Item -LiteralPath $jarPath).Length
    uploadedUrl = $uploadedUrl
    env = [ordered]@{
        DAISY_MINECRAFT_CUSTOM_SERVER_JAR_URL = $uploadedUrl
        DAISY_MINECRAFT_CUSTOM_SERVER_JAR_SHA256 = $effectiveSha256
        DAISY_MINECRAFT_CUSTOM_SERVER_JAR_NAME = $OutputName
    }
    dockerBuildArgs = [ordered]@{
        DAISY_MINECRAFT_BUNDLED_SERVER_JAR_URL = $resolved.url
        DAISY_MINECRAFT_BUNDLED_SERVER_JAR_SHA256 = $effectiveSha256
    }
}

$manifestPath = Join-Path $OutputDirectory "$OutputName.manifest.json"
$manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $manifestPath -Encoding UTF8

if ($GitHubEnvPath) {
    @(
        "PAPER_VERSION=$($resolved.version)",
        "PAPER_BUILD=$($resolved.build)",
        "PAPER_JAR_NAME=$($resolved.jarName)",
        "PAPER_JAR_URL=$($resolved.url)",
        "PAPER_JAR_SHA256=$effectiveSha256",
        "DAISY_MINECRAFT_SERVER_JAR_PATH=$((Resolve-Path $jarPath).Path)"
    ) | Add-Content -LiteralPath $GitHubEnvPath -Encoding UTF8
}

$manifest | ConvertTo-Json -Depth 6
