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

    [int]$MemoryMb = 1536,

    [decimal]$Cpu = 1.0,

    [string]$Memory = "2Gi",

    [int]$MinReplicas = 1,

    [int]$MaxReplicas = 1,

    [string]$ControlPlaneAppName = "DaisyMinecraft",

    [string]$VnetName = "daisyminecraft-vnet",

    [string]$VnetAddressPrefix = "10.42.0.0/16",

    [string]$InfrastructureSubnetName = "containerapps-infra",

    [string]$InfrastructureSubnetPrefix = "10.42.0.0/23",

    [string]$ServerJarUrl,

    [string]$ServerJarSha256,

    [string]$ServerJarName = "server.jar",

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

function Az-Tsv([string[]]$Arguments) {
    $value = az @Arguments --only-show-errors -o tsv
    if ($LASTEXITCODE -ne 0) {
        return $null
    }
    return $value
}

function Test-AzCommand([string[]]$Arguments) {
    az @Arguments --only-show-errors 1>$null 2>$null
    return $LASTEXITCODE -eq 0
}

function Ensure-ContainerAppsVnet() {
    az provider register --namespace Microsoft.ContainerService --only-show-errors | Out-Null

    $vnetExists = Test-AzCommand @(
        "network", "vnet", "show",
        "--resource-group", $ResourceGroup,
        "--name", $VnetName
    )
    if (-not $vnetExists) {
        az network vnet create `
            --resource-group $ResourceGroup `
            --name $VnetName `
            --location $Location `
            --address-prefix $VnetAddressPrefix `
            --only-show-errors | Out-Null
    }

    $subnetExists = Test-AzCommand @(
        "network", "vnet", "subnet", "show",
        "--resource-group", $ResourceGroup,
        "--vnet-name", $VnetName,
        "--name", $InfrastructureSubnetName
    )
    if (-not $subnetExists) {
        az network vnet subnet create `
            --resource-group $ResourceGroup `
            --vnet-name $VnetName `
            --name $InfrastructureSubnetName `
            --address-prefixes $InfrastructureSubnetPrefix `
            --only-show-errors | Out-Null
    }

    az network vnet subnet update `
        --resource-group $ResourceGroup `
        --vnet-name $VnetName `
        --name $InfrastructureSubnetName `
        --delegations Microsoft.App/environments `
        --only-show-errors | Out-Null

    $subnetId = Az-Tsv @(
        "network", "vnet", "subnet", "show",
        "--resource-group", $ResourceGroup,
        "--vnet-name", $VnetName,
        "--name", $InfrastructureSubnetName,
        "--query", "id"
    )
    if (-not $subnetId) {
        throw "Could not resolve infrastructure subnet ID for $VnetName/$InfrastructureSubnetName."
    }
    return $subnetId.Trim()
}

if ($GamePort -lt 1 -or $GamePort -gt 65535 -or $GamePort -in @(80, 443)) {
    throw "GamePort must be between 1 and 65535 and cannot be 80 or 443 for TCP ingress."
}

Require-Az

az extension add --name containerapp --upgrade --only-show-errors | Out-Null
az group create --name $ResourceGroup --location $Location --only-show-errors | Out-Null

$environmentExists = Test-AzCommand @(
    "containerapp", "env", "show",
    "--name", $EnvironmentName,
    "--resource-group", $ResourceGroup
)

if (-not $environmentExists) {
    $infrastructureSubnetId = Ensure-ContainerAppsVnet
    az containerapp env create `
        --name $EnvironmentName `
        --resource-group $ResourceGroup `
        --location $Location `
        --infrastructure-subnet-resource-id $infrastructureSubnetId `
        --only-show-errors | Out-Null
} else {
    $environmentVnet = Az-Tsv @(
        "containerapp", "env", "show",
        "--name", $EnvironmentName,
        "--resource-group", $ResourceGroup,
        "--query", "properties.vnetConfiguration.infrastructureSubnetId"
    )
    if (-not $environmentVnet) {
        throw "Container Apps environment $EnvironmentName exists without a custom VNet. External TCP ingress requires a custom VNet-backed environment. Delete or recreate the environment, then rerun this script."
    }
}

$envVars = @(
    "EULA=TRUE",
    "DAISY_MINECRAFT_EULA_ACCEPTED=true",
    "DAISY_MINECRAFT_MEMORY_MB=$MemoryMb",
    "DAISY_MINECRAFT_PORT=$GamePort",
    "DAISY_MINECRAFT_SERVER_JAR_NAME=$ServerJarName"
)

if ($ServerJarUrl -or $ServerJarSha256) {
    if (-not $ServerJarUrl -or -not $ServerJarSha256) {
        throw "ServerJarUrl and ServerJarSha256 must be provided together."
    }
    $envVars += @(
        "DAISY_MINECRAFT_CUSTOM_SERVER_JAR_URL=$ServerJarUrl",
        "DAISY_MINECRAFT_CUSTOM_SERVER_JAR_SHA256=$ServerJarSha256",
        "DAISY_MINECRAFT_CUSTOM_SERVER_JAR_NAME=$ServerJarName"
    )
}

$appExists = Test-AzCommand @(
    "containerapp", "show",
    "--name", $ContainerAppName,
    "--resource-group", $ResourceGroup
)

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
