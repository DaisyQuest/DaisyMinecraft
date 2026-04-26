# DaisyMinecraft

DaisyMinecraft is the Minecraft server hosting provider for DaisyCloud. It contains the provider model, runtime driver interfaces, local runtime driver, content/modpack profiles, backup/network/admin-panel profiles, and tests for the Minecraft hosting slice.

## Repository Layout

- `src/main/java/dev/daisycloud/provider/minecraft`: provider and runtime implementation.
- `src/main/container`: Docker context for the managed DaisyMinecraft Java server runtime image.
- `src/test/java/dev/daisycloud/provider/minecraft`: provider and runtime tests.
- `docs`: implementation plans and product notes copied from the DaisyCloud development branch.

## Build

By default, this repository uses the sibling `../DaisyCloud` checkout as a Gradle composite build for shared DaisyCloud modules:

```powershell
.\gradlew.bat test
```

Set `-Pdaisyminecraft.includeDaisyCloudComposite=false` to resolve DaisyCloud dependencies from configured package repositories instead.

## Minecraft Server Container

The managed server image lives in `src/main/container`. It enforces explicit Minecraft EULA acceptance, writes `eula.txt`, supports SHA-256 verified custom server jar downloads, supports custom launch commands, exposes `25565/tcp` and `25565/udp`, and includes a port/process healthcheck.

Package the Docker context for CI or registry build handoff with:

```powershell
.\gradlew.bat packageMinecraftContainer
```

On `main`, CI validates and publishes the image as `ghcr.io/daisyquest/daisyminecraft-server:latest`. The Azure App Service deployment is only the HTTP control plane; point `DAISYMINECRAFT_MINECRAFT_ENDPOINT` at a real TCP-capable deployment, such as Azure Container Apps TCP ingress, before showing a Minecraft client endpoint as connectable.

After `az login`, deploy the server runtime to Azure Container Apps and wire the control plane to the resulting TCP endpoint with:

```powershell
.\scripts\deploy-minecraft-containerapp.ps1 `
  -ResourceGroup <resource-group> `
  -Location <azure-region> `
  -EnvironmentName <container-apps-environment> `
  -ContainerAppName <minecraft-container-app>
```

## Relationship To DaisyCloud

This repo is intentionally split from the DaisyCloud monorepo as a provider/plugin project. DaisyCloud remains the platform; DaisyMinecraft owns the Minecraft-specific provider code and product docs.
