# DaisyMinecraft

DaisyMinecraft is the Minecraft server hosting provider for DaisyCloud. It contains the provider model, runtime driver interfaces, local runtime driver, content/modpack profiles, backup/network/admin-panel profiles, and tests for the Minecraft hosting slice.

## Repository Layout

- `src/main/java/dev/daisycloud/provider/minecraft`: provider and runtime implementation.
- `src/test/java/dev/daisycloud/provider/minecraft`: provider and runtime tests.
- `docs`: implementation plans and product notes copied from the DaisyCloud development branch.

## Build

By default, this repository uses the sibling `../DaisyCloud` checkout as a Gradle composite build for shared DaisyCloud modules:

```powershell
.\gradlew.bat test
```

Set `-Pdaisyminecraft.includeDaisyCloudComposite=false` to resolve DaisyCloud dependencies from configured package repositories instead.

## Relationship To DaisyCloud

This repo is intentionally split from the DaisyCloud monorepo as a provider/plugin project. DaisyCloud remains the platform; DaisyMinecraft owns the Minecraft-specific provider code and product docs.
