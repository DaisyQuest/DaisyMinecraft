# DaisyMinecraft Server Container

This container is the managed Minecraft Java runtime used by DaisyCloud node agents. The control plane emits a container manifest; node agents launch an image built from this context or a user-supplied `serverType=custom` image.

## Required Runtime Inputs

- `EULA=TRUE` or `DAISY_MINECRAFT_EULA_ACCEPTED=true`
- `/data` mounted as the persistent server volume
- `DAISY_MINECRAFT_MEMORY_MB`, default `2048`
- `DAISY_MINECRAFT_PORT`, default `25565`
- `DAISY_MINECRAFT_INSTALL_BUNDLED_PLUGINS`, default `true`

## Server Jar Sources

The entrypoint starts from one of these sources:

- `DAISY_MINECRAFT_CUSTOM_SERVER_JAR_URL` plus `DAISY_MINECRAFT_CUSTOM_SERVER_JAR_SHA256`
- A baked `/opt/daisyminecraft/server/server.jar`
- An existing jar in `/data` named by `DAISY_MINECRAFT_SERVER_JAR_NAME`

The published DaisyMinecraft image bakes a SHA-256 verified Paper server jar into `/opt/daisyminecraft/server/server.jar` and copies bundled server-side Paper plugins from `/opt/daisyminecraft/bundled-plugins` into `/data/plugins` on startup. CI also uploads the same server jar file as the `minecraft-server-jar` workflow artifact. Use `scripts/prepare-minecraft-server-jar.ps1` to recreate the jar locally or upload it to Azure Blob Storage for custom URL deployments.

Custom commands are supported through `DAISY_MINECRAFT_CUSTOM_SERVER_COMMAND` for advanced runtimes such as Mohist, proxy stacks, or vendor-provided launchers.
