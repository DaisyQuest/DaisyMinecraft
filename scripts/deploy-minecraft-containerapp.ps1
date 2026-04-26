param(
    [Parameter(Mandatory = $true)]
    [string]$ResourceGroup,

    [Parameter(Mandatory = $true)]
    [string]$Location,

    [Parameter(Mandatory = $true)]
    [string]$EnvironmentName,

    [Parameter(Mandatory = $true)]
    [string]$ContainerAppName,

    [string]$Image = "ghcr.io/daisyquest/daisyminecraft-server:latest",

    [int]$GamePort = 25565,

    [int]$MemoryMb = 2048,

    [decimal]$Cpu = 1.0,

    [string]$Memory = "2Gi",

    [int]$MinReplicas = 1,

    [int]$MaxReplicas = 1,

    [string]$ControlPlaneAppName = "DaisyMinecraft",

    [switch]$SkipControlPlaneAppSetting
)

$ErrorActionPreference = "Stop"

function Require-Az() {
    if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
        throw "Azure CLI is required. Install az, run az login, then rerun this script."
    }
    $account = az account show --only-show-errors 2>$null
    if (-not $account) {
        throw "Azure CLI is not logged in. Run az login before deploying the Minecraft container app."
    }
}

function Az-Json([string[]]$Arguments) {
    $json = az @Arguments --only-show-errors -o json
    if (-not $json) {
        return $null
    }
    return $json | ConvertFrom-Json
}

if ($GamePort -lt 1 -or $GamePort -gt 65535 -or $GamePort -in @(80, 443)) {
    throw "GamePort must be between 1 and 65535 and cannot be 80 or 443 for TCP ingress."
}

Require-Az

az extension add --name containerapp --upgrade --only-show-errors | Out-Null
az group create --name $ResourceGroup --location $Location --only-show-errors | Out-Null

$environmentExists = $true
try {
    az containerapp env show --name $EnvironmentName --resource-group $ResourceGroup --only-show-errors | Out-Null
} catch {
    $environmentExists = $false
}

if (-not $environmentExists) {
    az containerapp env create `
        --name $EnvironmentName `
        --resource-group $ResourceGroup `
        --location $Location `
        --only-show-errors | Out-Null
}

$envVars = @(
    "EULA=TRUE",
    "DAISY_MINECRAFT_EULA_ACCEPTED=true",
    "DAISY_MINECRAFT_MEMORY_MB=$MemoryMb",
    "DAISY_MINECRAFT_PORT=$GamePort",
    "DAISY_MINECRAFT_SERVER_JAR_NAME=server.jar"
)

$appExists = $true
try {
    az containerapp show --name $ContainerAppName --resource-group $ResourceGroup --only-show-errors | Out-Null
} catch {
    $appExists = $false
}

if ($appExists) {
    az containerapp update `
        --name $ContainerAppName `
        --resource-group $ResourceGroup `
        --image $Image `
        --cpu $Cpu `
        --memory $Memory `
        --min-replicas $MinReplicas `
        --max-replicas $MaxReplicas `
        --set-env-vars @envVars `
        --only-show-errors | Out-Null
} else {
    az containerapp create `
        --name $ContainerAppName `
        --resource-group $ResourceGroup `
        --environment $EnvironmentName `
        --image $Image `
        --cpu $Cpu `
        --memory $Memory `
        --min-replicas $MinReplicas `
        --max-replicas $MaxReplicas `
        --env-vars @envVars `
        --ingress external `
        --transport tcp `
        --target-port $GamePort `
        --exposed-port $GamePort `
        --only-show-errors | Out-Null
}

az containerapp ingress enable `
    --name $ContainerAppName `
    --resource-group $ResourceGroup `
    --type external `
    --transport tcp `
    --target-port $GamePort `
    --exposed-port $GamePort `
    --only-show-errors | Out-Null

$fqdn = az containerapp show `
    --name $ContainerAppName `
    --resource-group $ResourceGroup `
    --query "properties.configuration.ingress.fqdn" `
    --only-show-errors `
    -o tsv

if (-not $fqdn) {
    throw "Container App was deployed, but Azure did not return an ingress FQDN."
}

$minecraftEndpoint = "${fqdn}:$GamePort"

if (-not $SkipControlPlaneAppSetting) {
    az webapp config appsettings set `
        --name $ControlPlaneAppName `
        --resource-group $ResourceGroup `
        --settings "DAISYMINECRAFT_MINECRAFT_ENDPOINT=$minecraftEndpoint" `
        --only-show-errors | Out-Null
}

[pscustomobject]@{
    containerApp = $ContainerAppName
    image = $Image
    minecraftEndpoint = $minecraftEndpoint
    controlPlaneAppSettingUpdated = (-not $SkipControlPlaneAppSetting)
} | ConvertTo-Json
