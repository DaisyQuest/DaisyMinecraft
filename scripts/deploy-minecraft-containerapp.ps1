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

    [string]$PersistentStorageAccountName,

    [string]$PersistentFileShareName,

    [string]$PersistentStorageMountName,

    [string]$PersistentMountPath = "/mnt/daisy-persist",

    [int]$PersistentFileShareQuotaGb = 256,

    [switch]$SkipPersistentStorage,

    [string]$RevisionSuffix,

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

function ConvertTo-YamlSingleQuoted([AllowNull()][string]$Value) {
    if ($null -eq $Value) {
        return "''"
    }
    return "'" + ($Value -replace "'", "''") + "'"
}

function New-SafeStorageAccountName([string]$Name) {
    $clean = $Name.ToLowerInvariant() -replace "[^a-z0-9]", ""
    $candidate = "${clean}data"
    if ($candidate.Length -gt 24) {
        $candidate = $candidate.Substring(0, 24)
    }
    if ($candidate.Length -lt 3) {
        throw "Could not derive a valid storage account name from '$Name'. Provide -PersistentStorageAccountName."
    }
    return $candidate
}

function New-SafeShareName([string]$Name) {
    $clean = $Name.ToLowerInvariant() -replace "[^a-z0-9-]", "-"
    $clean = $clean.Trim("-")
    if ($clean.Length -eq 0) {
        $clean = "minecraft"
    }
    $candidate = "${clean}-data"
    if ($candidate.Length -gt 63) {
        $candidate = $candidate.Substring(0, 63).Trim("-")
    }
    if ($candidate.Length -lt 3) {
        throw "Could not derive a valid Azure Files share name from '$Name'. Provide -PersistentFileShareName."
    }
    return $candidate
}

function New-SafeEnvironmentStorageName([string]$Name) {
    $clean = $Name.ToLowerInvariant() -replace "[^a-z0-9-]", "-"
    $clean = $clean.Trim("-")
    if ($clean.Length -eq 0) {
        $clean = "minecraft"
    }
    $candidate = "${clean}-persist"
    if ($candidate.Length -gt 63) {
        $candidate = $candidate.Substring(0, 63).Trim("-")
    }
    return $candidate
}

function Ensure-PersistentStorage() {
    if ($PersistentFileShareQuotaGb -lt 1) {
        throw "PersistentFileShareQuotaGb must be greater than zero."
    }
    if (-not $PersistentStorageAccountName) {
        $script:PersistentStorageAccountName = New-SafeStorageAccountName $ContainerAppName
    }
    if (-not $PersistentFileShareName) {
        $script:PersistentFileShareName = New-SafeShareName $ContainerAppName
    }
    if (-not $PersistentStorageMountName) {
        $script:PersistentStorageMountName = New-SafeEnvironmentStorageName $ContainerAppName
    }

    $storageExists = Test-AzCommand @(
        "storage", "account", "show",
        "--resource-group", $ResourceGroup,
        "--name", $PersistentStorageAccountName
    )
    if (-not $storageExists) {
        $availability = Az-Json @(
            "storage", "account", "check-name",
            "--name", $PersistentStorageAccountName
        )
        if ($availability -and $availability.nameAvailable -eq $false) {
            throw "Storage account name '$PersistentStorageAccountName' is unavailable. Rerun with a globally unique -PersistentStorageAccountName."
        }
        az storage account create `
            --resource-group $ResourceGroup `
            --name $PersistentStorageAccountName `
            --location $Location `
            --sku Standard_LRS `
            --kind StorageV2 `
            --min-tls-version TLS1_2 `
            --allow-blob-public-access false `
            --only-show-errors | Out-Null
    }

    $storageKey = Az-Tsv @(
        "storage", "account", "keys", "list",
        "--resource-group", $ResourceGroup,
        "--account-name", $PersistentStorageAccountName,
        "--query", "[0].value"
    )
    if (-not $storageKey) {
        throw "Could not resolve a storage account key for '$PersistentStorageAccountName'."
    }

    az storage share create `
        --account-name $PersistentStorageAccountName `
        --account-key $storageKey.Trim() `
        --name $PersistentFileShareName `
        --quota $PersistentFileShareQuotaGb `
        --only-show-errors | Out-Null

    az containerapp env storage set `
        --name $EnvironmentName `
        --resource-group $ResourceGroup `
        --storage-name $PersistentStorageMountName `
        --azure-file-account-name $PersistentStorageAccountName `
        --azure-file-account-key $storageKey.Trim() `
        --azure-file-share-name $PersistentFileShareName `
        --access-mode ReadWrite `
        --only-show-errors | Out-Null

    return [pscustomobject]@{
        storageAccount = $PersistentStorageAccountName
        fileShare = $PersistentFileShareName
        environmentStorageName = $PersistentStorageMountName
        mountPath = $PersistentMountPath
        quotaGb = $PersistentFileShareQuotaGb
    }
}

function Write-ContainerAppYaml([string]$Path, [string]$EnvironmentId, [array]$EnvVars, [object]$PersistentStorage) {
    $cpuValue = $Cpu.ToString([System.Globalization.CultureInfo]::InvariantCulture)
    if (-not $RevisionSuffix) {
        $script:RevisionSuffix = "deploy-" + (Get-Date -Format "yyyyMMddHHmmss")
    }

    $lines = @(
        "name: $ContainerAppName",
        "type: Microsoft.App/containerApps",
        "location: $Location",
        "properties:",
        "  environmentId: $EnvironmentId",
        "  configuration:",
        "    activeRevisionsMode: Single",
        "    ingress:",
        "      external: true",
        "      targetPort: $GamePort",
        "      exposedPort: $GamePort",
        "      transport: Tcp",
        "      traffic:",
        "      - latestRevision: true",
        "        weight: 100",
        "  template:",
        "    containers:",
        "    - name: $ContainerAppName",
        "      image: $(ConvertTo-YamlSingleQuoted $Image)",
        "      env:"
    )

    foreach ($entry in $EnvVars) {
        $separator = $entry.IndexOf("=")
        if ($separator -lt 1) {
            throw "Invalid environment variable entry '$entry'. Expected NAME=value."
        }
        $name = $entry.Substring(0, $separator)
        $value = $entry.Substring($separator + 1)
        $lines += "      - name: $name"
        $lines += "        value: $(ConvertTo-YamlSingleQuoted $value)"
    }

    $lines += @(
        "      resources:",
        "        cpu: $cpuValue",
        "        memory: $Memory"
    )

    if ($PersistentStorage) {
        $lines += @(
            "      volumeMounts:",
            "      - volumeName: minecraft-persist",
            "        mountPath: $(ConvertTo-YamlSingleQuoted $PersistentMountPath)"
        )
    }

    $lines += @(
        "      probes:",
        "      - type: Startup",
        "        tcpSocket:",
        "          port: $GamePort",
        "        initialDelaySeconds: 60",
        "        periodSeconds: 10",
        "        timeoutSeconds: 5",
        "        failureThreshold: 48",
        "      - type: Readiness",
        "        tcpSocket:",
        "          port: $GamePort",
        "        initialDelaySeconds: 60",
        "        periodSeconds: 10",
        "        timeoutSeconds: 5",
        "        failureThreshold: 48",
        "      - type: Liveness",
        "        tcpSocket:",
        "          port: $GamePort",
        "        initialDelaySeconds: 60",
        "        periodSeconds: 30",
        "        timeoutSeconds: 5",
        "        failureThreshold: 6",
        "    scale:",
        "      minReplicas: $MinReplicas",
        "      maxReplicas: $MaxReplicas",
        "    revisionSuffix: $RevisionSuffix"
    )

    if ($PersistentStorage) {
        $lines += @(
            "    volumes:",
            "    - name: minecraft-persist",
            "      storageName: $($PersistentStorage.environmentStorageName)",
            "      storageType: AzureFile"
        )
    }

    $directory = Split-Path -Parent $Path
    if ($directory) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }
    Set-Content -Path $Path -Value ($lines -join "`n") -Encoding utf8
}

function Ensure-ContainerAppsVnet() {
    az provider register --namespace Microsoft.App --only-show-errors | Out-Null
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
    "DAISY_MINECRAFT_SERVER_JAR_NAME=$ServerJarName",
    "DAISY_MINECRAFT_INSTALL_BUNDLED_PLUGINS=true"
)

$persistentStorage = $null
if (-not $SkipPersistentStorage) {
    $persistentStorage = Ensure-PersistentStorage
    $envVars += "DAISY_MINECRAFT_PERSIST_DIR=$PersistentMountPath"
}

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

if (-not $appExists) {
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

$environmentId = Az-Tsv @(
    "containerapp", "env", "show",
    "--name", $EnvironmentName,
    "--resource-group", $ResourceGroup,
    "--query", "id"
)
if (-not $environmentId) {
    throw "Could not resolve Container Apps environment ID for '$EnvironmentName'."
}

$deploymentYamlPath = Join-Path (Join-Path (Get-Location) "build") "azure-containerapp"
$deploymentYamlPath = Join-Path $deploymentYamlPath "$ContainerAppName.yaml"
Write-ContainerAppYaml `
    -Path $deploymentYamlPath `
    -EnvironmentId $environmentId.Trim() `
    -EnvVars $envVars `
    -PersistentStorage $persistentStorage

az containerapp update `
    --name $ContainerAppName `
    --resource-group $ResourceGroup `
    --yaml $deploymentYamlPath `
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
    $controlPlaneSettings = @(
        "DAISYMINECRAFT_MINECRAFT_ENDPOINT=$minecraftEndpoint",
        "DAISYMINECRAFT_AZURE_RESOURCE_GROUP=$ResourceGroup",
        "DAISYMINECRAFT_AZURE_CONTAINER_APP=$ContainerAppName",
        "DAISYMINECRAFT_AZURE_CONTAINER_APPS_ENVIRONMENT=$EnvironmentName"
    )
    if ($persistentStorage) {
        $controlPlaneSettings += @(
            "DAISYMINECRAFT_PERSISTENT_STORAGE_ACCOUNT=$($persistentStorage.storageAccount)",
            "DAISYMINECRAFT_PERSISTENT_FILE_SHARE=$($persistentStorage.fileShare)",
            "DAISYMINECRAFT_PERSISTENT_MOUNT_PATH=$($persistentStorage.mountPath)"
        )
    }

    az webapp config appsettings set `
        --name $ControlPlaneAppName `
        --resource-group $ResourceGroup `
        --settings @controlPlaneSettings `
        --only-show-errors | Out-Null
}

[pscustomobject]@{
    containerApp = $ContainerAppName
    image = $Image
    minecraftEndpoint = $minecraftEndpoint
    revisionSuffix = $RevisionSuffix
    persistentStorage = $persistentStorage
    deploymentYaml = $deploymentYamlPath
    controlPlaneAppSettingUpdated = (-not $SkipControlPlaneAppSetting)
} | ConvertTo-Json -Depth 5
