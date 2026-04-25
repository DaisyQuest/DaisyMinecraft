# Minecraft Server Hosting Provider Research, Feature Plan, and Test Matrix

Research date: April 25, 2026.

This document translates current Minecraft hosting market expectations into a DaisyCloud provider roadmap. The implemented first slice is `cloud-provider-minecraft`, provider namespace `DaisyCloud.Minecraft`, resource type `servers`.

## Source Basis
- [Apex Hosting modded hosting](https://apexminecrafthosting.com/guides/minecraft/mods/modded-minecraft-server-hosting/) - one-click modpacks, DDoS protection, automated offsite backups, FTP custom mods, 24/7 support, instant setup.
- [Shockbyte Minecraft hosting](https://shockbyte.com/games/minecraft-server-hosting) - instant setup, uptime/support claims, DDoS, modpack/plugin installer, real-time console, server instances, config manager, backups, Java/Bedrock support.
- [BisectHosting](https://www.bisecthosting.com/) - 2,300+ modpacks, 21 locations, advanced DDoS, support, custom control panel, game swapping, instance manager.
- [Nodecraft Minecraft hosting](https://nodecraft.com/games/minecraft-server-hosting) - cloud backups, game swapping, static IP, tasks, file manager, SFTP, MySQL, console, worldwide locations.
- [PebbleHost modded hosting](https://pebblehost.com/modded-minecraft-server-hosting/) and [modpack hosting](https://pebblehost.com/minecraft-server-with-mod-packs/) - NVMe/DDR5 tiers, DDoS, backups, tasks, permissions, FTP, MySQL, subdomains, safe rollbacks.
- [Hostinger Minecraft VPS hosting](https://www.hostinger.com/vps/minecraft-hosting) - mod support, DDoS, root access, one-click installation, AI/MCP agent, EPYC, NVMe, worldwide datacenters, backups and snapshots.
- [ScalaCube](https://scalacube.com/) - game-server hosting positioning and Minecraft modpack support baseline.
- [GGServers](https://www.ggservers.com/) - Java/Bedrock support, one-click modpacks/plugins, custom Pterodactyl panel, storage, FTP/MySQL, DDoS, Geyser setup, whitelist editor, importer, log/crash scan.
- [Modrinth API docs](https://docs.modrinth.com/api/) - project search, project type facets, loader and game-version metadata used for compatibility filtering.
- [CurseForge API docs](https://docs.curseforge.com/rest-api/) - mod distribution, API-backed launcher/UI integrations, moderation, and dependency metadata path.
- [PaperMC Downloads Service](https://docs.papermc.io/misc/downloads-service/) - stable build discovery/download API and production-safe user-agent guidance.

## DaisyCloud Implementation Target
- Provider namespace: `DaisyCloud.Minecraft`.
- Resource type: `servers`.
- Current implemented schema slice: `serverName`, `edition`, `minecraftVersion`, `serverType`, `memoryMb`, `vcpu`, `storageGb`, `region`, `adminPanel`, `panelAccess`, `selectedMods`, `selectedPlugins`, `modpackId`, `backupSchedule`, `backupRetentionDays`, `networkMode`, `ddosProtection`, `eulaAccepted`, `gamemode`, `difficulty`, `viewDistance`, `simulationDistance`, `motd`, `pvp`, `enableCommandBlock`, `databaseMode`, `databaseResourceId`, `databaseEndpoint`, `marketplaceMode`, `marketplaceSources`, `marketplaceInstallPolicy`, `marketplaceMalwareScan`, `marketplaceRollbackPolicy`, `instanceManager`, `activeInstance`, and `maxInstances`.
- Current lifecycle coverage: validate, plan, apply, observe, delete, import, export, backup, restore, and diagnose are provider-specific. Apply/delete currently emit deterministic node-agent task handoffs, container manifests, startup file sets, content locks, admin panel profiles, admin UX capability profiles, network policy profiles, backup policy profiles, DaisyBase control-plane contracts, live marketplace contracts, instance-manager contracts, reconcile digests, and idempotency keys. `MinecraftNodeAgentExecutor` now provides the first executable adapter seam: it bootstraps DaisyBase SQL, invokes a runtime driver for image/volume/startup-file/port/container operations, probes health, and returns applied runtime state. Import/export emit migration and portability contracts for future portal/API workflows.
- Key interface files: `cloud-provider-spi/src/main/java/dev/daisycloud/provider/spi/ResourceProvider.java`, `cloud-resource-manager/src/main/java/dev/daisycloud/resource/manager/ResourceManager.java`, and `cloud-provider-minecraft/src/main/java/dev/daisycloud/provider/minecraft/MinecraftServerProvider.java`.

## 100 Important Industry Features
| ID | Category | Feature | Why it matters |
|---|---|---|---|
| F001 | Provisioning and lifecycle | Instant server provisioning | Create a playable server from order or API request with minimal operator intervention. |
| F002 | Provisioning and lifecycle | Start stop restart kill lifecycle controls | Expose safe lifecycle operations for normal and emergency server control. |
| F003 | Provisioning and lifecycle | Always-on hosting mode | Keep servers online around the clock for communities and public networks. |
| F004 | Provisioning and lifecycle | Hibernation and wake-on-demand mode | Suspend idle servers and resume them through a wake link or API call. |
| F005 | Provisioning and lifecycle | Server templates and cloning | Create repeatable servers from saved plans, worlds, and configuration baselines. |
| F006 | Provisioning and lifecycle | Resource sizing plans | Select RAM, CPU, storage, player count, and tier before provisioning. |
| F007 | Provisioning and lifecycle | Region and datacenter selection | Place servers near players using latency and availability evidence. |
| F008 | Provisioning and lifecycle | Dedicated IP and default port support | Support static endpoints, default port 25565, and custom port policy. |
| F009 | Provisioning and lifecycle | Subdomain and custom DNS | Give each server a shareable hostname and optional customer domain. |
| F010 | Provisioning and lifecycle | Multi-server network grouping | Manage related lobby, survival, minigame, proxy, and database resources together. |
| F011 | Runtime versions and software | Java Edition support | Run Minecraft Java servers as the primary mod-capable hosting path. |
| F012 | Runtime versions and software | Bedrock Edition support | Run Bedrock or PocketMine servers for mobile, console, and Windows players. |
| F013 | Runtime versions and software | Server software selection | Choose Vanilla, Paper, Spigot, Purpur, Forge, Fabric, Quilt, NeoForge, Bedrock, or PocketMine. |
| F014 | Runtime versions and software | Minecraft version pinning | Pin exact Minecraft versions or choose latest and stable channels. |
| F015 | Runtime versions and software | Upgrade and downgrade workflow | Plan version changes with backup, compatibility, and rollback gates. |
| F016 | Runtime versions and software | Java runtime selection | Select supported Java runtimes that match Minecraft and loader requirements. |
| F017 | Runtime versions and software | JVM flags and GC presets | Expose safe memory and garbage-collection tuning without forcing shell access. |
| F018 | Runtime versions and software | Custom server JAR upload | Allow expert users to upload custom server builds with validation. |
| F019 | Runtime versions and software | EULA acceptance and compliance record | Require explicit Minecraft EULA acceptance before provisioning. |
| F020 | Runtime versions and software | Container image and reproducible runtime | Build deterministic server containers with pinned image, runtime, and content lock metadata. |
| F021 | Mods plugins and content | One-click modpack installer | Install popular modpacks from the panel without manual file work. |
| F022 | Mods plugins and content | Searchable mod repository catalog | Search mods and modpacks by name, category, loader, version, and source. |
| F023 | Mods plugins and content | Mod loader compatibility filters | Prevent selecting Fabric mods for Forge servers or plugins for vanilla runtimes. |
| F024 | Mods plugins and content | Mod dependency resolution | Resolve required libraries and transitive mod dependencies before install. |
| F025 | Mods plugins and content | Saved modpack profiles and instances | Save different worlds and modpack setups for fast switching. |
| F026 | Mods plugins and content | Custom mod upload | Upload custom mod files with validation, hashing, and rollback protection. |
| F027 | Mods plugins and content | Plugin installer | Install plugins for Paper, Spigot, and Purpur servers. |
| F028 | Mods plugins and content | Plugin config editor | Edit plugin configuration with schema hints and safe restart prompts. |
| F029 | Mods plugins and content | Mod conflict detection | Detect known incompatible mods, duplicate libraries, and loader mismatches. |
| F030 | Mods plugins and content | Modpack update and rollback planning | Stage content updates with backup, dry-run, and restore points. |
| F031 | Mods plugins and content | CurseForge integration | Search and resolve CurseForge mods, modpacks, files, and dependencies through an approved API path. |
| F032 | Mods plugins and content | Modrinth integration | Search and resolve Modrinth projects, versions, loaders, and game-version filters. |
| F033 | Mods plugins and content | FTB Technic ATLauncher importers | Import established modpack ecosystems that remain common in hosted Minecraft. |
| F034 | Mods plugins and content | Pack-specific RAM guidance | Recommend memory based on selected pack, mod count, player count, and telemetry. |
| F035 | Mods plugins and content | Curated and safe marketplace | Promote reviewed content and mark risky, abandoned, or incompatible packages. |
| F036 | Admin panel | Web admin dashboard | Provide the primary browser UI for server status, configuration, content, and operations. |
| F037 | Admin panel | Real-time console | Stream server console output and accept authorized commands. |
| F038 | Admin panel | Command execution with RBAC and audit | Gate every command by role, scope, and correlation ID. |
| F039 | Admin panel | Log viewer search and sharing | Search logs, highlight errors, and create redacted share links. |
| F040 | Admin panel | Web file manager | Upload, edit, move, delete, and archive server files from the panel. |
| F041 | Admin panel | SFTP or FTP access | Expose power-user file access with scoped credentials and auditability. |
| F042 | Admin panel | Structured config editor | Edit server.properties, YAML, JSON, TOML, and loader configs safely. |
| F043 | Admin panel | Whitelist management | Add, remove, import, export, and enforce whitelist entries. |
| F044 | Admin panel | OP staff and role management | Manage operators and staff roles without sharing owner credentials. |
| F045 | Admin panel | Banlist kick and player actions | Operate active players with controlled kick, ban, pardon, teleport, and message actions. |
| F046 | Admin panel | World settings editor | Change seed-visible metadata, game mode, difficulty, hardcore, spawn, and rules. |
| F047 | Admin panel | MOTD and server icon editor | Edit branding details shown in clients and server lists. |
| F048 | Admin panel | Scheduled tasks | Run restarts, broadcasts, backups, commands, and maintenance windows on schedule. |
| F049 | Admin panel | Sub-user accounts and permissions | Delegate panel access by server, operation, file scope, and time window. |
| F050 | Admin panel | Two-factor authentication and session security | Protect public or privileged panel access with MFA, secure sessions, and device controls. |
| F051 | Data backups and migration | Automated backups | Capture worlds, configs, mods, plugins, and metadata on schedule. |
| F052 | Data backups and migration | Manual snapshots | Let operators create named restore points before risky changes. |
| F053 | Data backups and migration | Point-in-time restore | Restore a server to a selected backup with clear replacement semantics. |
| F054 | Data backups and migration | Backup retention policies | Configure how many backups are retained and when old points expire. |
| F055 | Data backups and migration | Offsite backup storage | Store backups outside the active node failure domain. |
| F056 | Data backups and migration | World import | Upload existing worlds from local machines or object storage. |
| F057 | Data backups and migration | Host-to-host migration importer | Pull files from another host or archive with guided validation. |
| F058 | Data backups and migration | Backup before destructive changes | Force backups before modpack switches, deletes, restores, and major upgrades. |
| F059 | Data backups and migration | Rollback between modpacks | Return to the previous pack, world, and config without manual reconstruction. |
| F060 | Data backups and migration | Disaster recovery drills and evidence | Regularly prove restore works and attach evidence to release gates. |
| F061 | Performance and reliability | High single-thread CPU allocation | Prioritize CPU profiles that match Minecraft tick-loop behavior. |
| F062 | Performance and reliability | NVMe SSD storage | Use fast storage for world chunks, region files, logs, and backups. |
| F063 | Performance and reliability | Crash auto-restart | Restart crashed servers with loop protection and incident evidence. |
| F064 | Performance and reliability | Health checks and watchdog | Observe process, port, console, tick, and heartbeat health. |
| F065 | Performance and reliability | Vertical upgrade workflow | Resize RAM CPU and disk safely when a server outgrows its plan. |
| F066 | Performance and reliability | CPU RAM TPS and MSPT metrics | Track resource usage and gameplay health in one view. |
| F067 | Performance and reliability | Lag diagnostics and profiling | Identify plugin, mod, entity, chunk, disk, and GC causes of lag. |
| F068 | Performance and reliability | Chunk pre-generation | Generate world chunks ahead of play to reduce exploration lag. |
| F069 | Performance and reliability | View and simulation distance controls | Tune distance settings for performance and gameplay needs. |
| F070 | Performance and reliability | Load-aware scheduling and capacity guardrails | Avoid noisy-node overload and unsafe overcommitment. |
| F071 | Networking and security | DDoS protection | Keep game and panel endpoints available during network attacks. |
| F072 | Networking and security | Firewall rules | Control source ranges, ports, and protocol exposure. |
| F073 | Networking and security | Public private and internal access modes | Decide whether game and panel endpoints are internet-facing or private. |
| F074 | Networking and security | Additional port allocation | Expose extra ports for voice, maps, query, RCON, and proxies. |
| F075 | Networking and security | Velocity and Bungee proxy support | Run proxy networks with lobby and backend server routing. |
| F076 | Networking and security | Geyser and Floodgate bridge setup | Bridge Java and Bedrock players where compatible. |
| F077 | Networking and security | Panel TLS and reverse proxy | Serve admin surfaces over authenticated TLS routes. |
| F078 | Networking and security | Secrets management | Store RCON, SFTP, API, database, and token secrets outside visible config. |
| F079 | Networking and security | Abuse detection and rate limiting | Detect command spam, login storms, brute force, and suspicious upload behavior. |
| F080 | Networking and security | Upload malware scanning | Scan uploaded jars, zips, and scripts before activation. |
| F081 | Observability support and operations | Alerts and notifications | Notify owners about crashes, backup failures, high lag, quota, billing, and security events. |
| F082 | Observability support and operations | Status page and service health | Show node, region, panel, API, and server health to users and support. |
| F083 | Observability support and operations | Crash report analyzer | Summarize stack traces, mod conflicts, memory errors, and missing dependencies. |
| F084 | Observability support and operations | Support bundle and ticket handoff | Collect redacted diagnostics for human support without exposing secrets. |
| F085 | Observability support and operations | Live support integration | Connect panel context to chat or ticket workflows. |
| F086 | Observability support and operations | Tutorials and onboarding flows | Guide first-time owners through launch, OP setup, backups, mods, and invites. |
| F087 | Observability support and operations | Activity logs and correlation IDs | Record every operation across API, portal, provider, and node agents. |
| F088 | Observability support and operations | Node status and latency test | Expose location capacity and ping evidence before placement or migration. |
| F089 | Observability support and operations | API CLI and SDK coverage | Make server lifecycle and content operations scriptable outside the panel. |
| F090 | Observability support and operations | Webhooks and events | Emit lifecycle, backup, player, crash, and security events to external systems. |
| F091 | Governance billing and automation | Terraform and IaC templates | Create reproducible Minecraft hosting deployments from declarative templates. |
| F092 | Governance billing and automation | Cost estimates | Show expected monthly cost before create, resize, backup, or location changes. |
| F093 | Governance billing and automation | Usage metering | Track RAM CPU storage bandwidth backup and support usage. |
| F094 | Governance billing and automation | Quotas and budget alerts | Prevent runaway resource growth and notify before budget limits are hit. |
| F095 | Governance billing and automation | Tags and labels | Attach owner, environment, pack, community, cost center, and lifecycle metadata. |
| F096 | Governance billing and automation | Audit log export | Export immutable audit events to compliance or customer storage. |
| F097 | Governance billing and automation | Role model separation | Separate owner, admin, operator, developer, moderator, support, and reader powers. |
| F098 | Governance billing and automation | EULA and content policy enforcement | Prevent noncompliant provisioning, monetization, uploads, or unsafe marketplace content. |
| F099 | Governance billing and automation | Data residency and location policy | Restrict regions and backup storage according to customer or legal policy. |
| F100 | Governance billing and automation | SLA uptime and provider conformance evidence | Produce release evidence that the provider, panel, and recovery path are shippable. |

## 10-Point Implementation Plan For Each Feature

### F001 - Instant server provisioning
Category: Provisioning and lifecycle.
Importance: Create a playable server from order or API request with minimal operator intervention.
1. Add a DaisyCloud provider schema field or action for Instant server provisioning.
2. Validate create and update input for legal values, defaults, quota, and idempotency.
3. Map the request into a Minecraft container desired-state plan with resource ID, region, size, endpoint, and owner.
4. Persist planned attributes through ResourceManager so dry-run, validation-only, create, and update behave consistently.
5. Wire API CLI SDK and portal affordances without bypassing the provider SPI.
6. Enforce RBAC actions for read, write, lifecycle, emergency, and support operations.
7. Emit operation steps, activity logs, metrics, traces, and diagnostics for every transition.
8. Add retry, timeout, rollback, and partial-failure behavior for node-agent or container failures.
9. Add unit, provider conformance, resource-manager, portal, and negative authorization tests.
10. Document operator runbook, customer UX, known limits, and release evidence.

### F002 - Start stop restart kill lifecycle controls
Category: Provisioning and lifecycle.
Importance: Expose safe lifecycle operations for normal and emergency server control.
1. Add a DaisyCloud provider schema field or action for Start stop restart kill lifecycle controls.
2. Validate create and update input for legal values, defaults, quota, and idempotency.
3. Map the request into a Minecraft container desired-state plan with resource ID, region, size, endpoint, and owner.
4. Persist planned attributes through ResourceManager so dry-run, validation-only, create, and update behave consistently.
5. Wire API CLI SDK and portal affordances without bypassing the provider SPI.
6. Enforce RBAC actions for read, write, lifecycle, emergency, and support operations.
7. Emit operation steps, activity logs, metrics, traces, and diagnostics for every transition.
8. Add retry, timeout, rollback, and partial-failure behavior for node-agent or container failures.
9. Add unit, provider conformance, resource-manager, portal, and negative authorization tests.
10. Document operator runbook, customer UX, known limits, and release evidence.

### F003 - Always-on hosting mode
Category: Provisioning and lifecycle.
Importance: Keep servers online around the clock for communities and public networks.
1. Add a DaisyCloud provider schema field or action for Always-on hosting mode.
2. Validate create and update input for legal values, defaults, quota, and idempotency.
3. Map the request into a Minecraft container desired-state plan with resource ID, region, size, endpoint, and owner.
4. Persist planned attributes through ResourceManager so dry-run, validation-only, create, and update behave consistently.
5. Wire API CLI SDK and portal affordances without bypassing the provider SPI.
6. Enforce RBAC actions for read, write, lifecycle, emergency, and support operations.
7. Emit operation steps, activity logs, metrics, traces, and diagnostics for every transition.
8. Add retry, timeout, rollback, and partial-failure behavior for node-agent or container failures.
9. Add unit, provider conformance, resource-manager, portal, and negative authorization tests.
10. Document operator runbook, customer UX, known limits, and release evidence.

### F004 - Hibernation and wake-on-demand mode
Category: Provisioning and lifecycle.
Importance: Suspend idle servers and resume them through a wake link or API call.
1. Add a DaisyCloud provider schema field or action for Hibernation and wake-on-demand mode.
2. Validate create and update input for legal values, defaults, quota, and idempotency.
3. Map the request into a Minecraft container desired-state plan with resource ID, region, size, endpoint, and owner.
4. Persist planned attributes through ResourceManager so dry-run, validation-only, create, and update behave consistently.
5. Wire API CLI SDK and portal affordances without bypassing the provider SPI.
6. Enforce RBAC actions for read, write, lifecycle, emergency, and support operations.
7. Emit operation steps, activity logs, metrics, traces, and diagnostics for every transition.
8. Add retry, timeout, rollback, and partial-failure behavior for node-agent or container failures.
9. Add unit, provider conformance, resource-manager, portal, and negative authorization tests.
10. Document operator runbook, customer UX, known limits, and release evidence.

### F005 - Server templates and cloning
Category: Provisioning and lifecycle.
Importance: Create repeatable servers from saved plans, worlds, and configuration baselines.
1. Add a DaisyCloud provider schema field or action for Server templates and cloning.
2. Validate create and update input for legal values, defaults, quota, and idempotency.
3. Map the request into a Minecraft container desired-state plan with resource ID, region, size, endpoint, and owner.
4. Persist planned attributes through ResourceManager so dry-run, validation-only, create, and update behave consistently.
5. Wire API CLI SDK and portal affordances without bypassing the provider SPI.
6. Enforce RBAC actions for read, write, lifecycle, emergency, and support operations.
7. Emit operation steps, activity logs, metrics, traces, and diagnostics for every transition.
8. Add retry, timeout, rollback, and partial-failure behavior for node-agent or container failures.
9. Add unit, provider conformance, resource-manager, portal, and negative authorization tests.
10. Document operator runbook, customer UX, known limits, and release evidence.

### F006 - Resource sizing plans
Category: Provisioning and lifecycle.
Importance: Select RAM, CPU, storage, player count, and tier before provisioning.
1. Add a DaisyCloud provider schema field or action for Resource sizing plans.
2. Validate create and update input for legal values, defaults, quota, and idempotency.
3. Map the request into a Minecraft container desired-state plan with resource ID, region, size, endpoint, and owner.
4. Persist planned attributes through ResourceManager so dry-run, validation-only, create, and update behave consistently.
5. Wire API CLI SDK and portal affordances without bypassing the provider SPI.
6. Enforce RBAC actions for read, write, lifecycle, emergency, and support operations.
7. Emit operation steps, activity logs, metrics, traces, and diagnostics for every transition.
8. Add retry, timeout, rollback, and partial-failure behavior for node-agent or container failures.
9. Add unit, provider conformance, resource-manager, portal, and negative authorization tests.
10. Document operator runbook, customer UX, known limits, and release evidence.

### F007 - Region and datacenter selection
Category: Provisioning and lifecycle.
Importance: Place servers near players using latency and availability evidence.
1. Add a DaisyCloud provider schema field or action for Region and datacenter selection.
2. Validate create and update input for legal values, defaults, quota, and idempotency.
3. Map the request into a Minecraft container desired-state plan with resource ID, region, size, endpoint, and owner.
4. Persist planned attributes through ResourceManager so dry-run, validation-only, create, and update behave consistently.
5. Wire API CLI SDK and portal affordances without bypassing the provider SPI.
6. Enforce RBAC actions for read, write, lifecycle, emergency, and support operations.
7. Emit operation steps, activity logs, metrics, traces, and diagnostics for every transition.
8. Add retry, timeout, rollback, and partial-failure behavior for node-agent or container failures.
9. Add unit, provider conformance, resource-manager, portal, and negative authorization tests.
10. Document operator runbook, customer UX, known limits, and release evidence.

### F008 - Dedicated IP and default port support
Category: Provisioning and lifecycle.
Importance: Support static endpoints, default port 25565, and custom port policy.
1. Add a DaisyCloud provider schema field or action for Dedicated IP and default port support.
2. Validate create and update input for legal values, defaults, quota, and idempotency.
3. Map the request into a Minecraft container desired-state plan with resource ID, region, size, endpoint, and owner.
4. Persist planned attributes through ResourceManager so dry-run, validation-only, create, and update behave consistently.
5. Wire API CLI SDK and portal affordances without bypassing the provider SPI.
6. Enforce RBAC actions for read, write, lifecycle, emergency, and support operations.
7. Emit operation steps, activity logs, metrics, traces, and diagnostics for every transition.
8. Add retry, timeout, rollback, and partial-failure behavior for node-agent or container failures.
9. Add unit, provider conformance, resource-manager, portal, and negative authorization tests.
10. Document operator runbook, customer UX, known limits, and release evidence.

### F009 - Subdomain and custom DNS
Category: Provisioning and lifecycle.
Importance: Give each server a shareable hostname and optional customer domain.
1. Add a DaisyCloud provider schema field or action for Subdomain and custom DNS.
2. Validate create and update input for legal values, defaults, quota, and idempotency.
3. Map the request into a Minecraft container desired-state plan with resource ID, region, size, endpoint, and owner.
4. Persist planned attributes through ResourceManager so dry-run, validation-only, create, and update behave consistently.
5. Wire API CLI SDK and portal affordances without bypassing the provider SPI.
6. Enforce RBAC actions for read, write, lifecycle, emergency, and support operations.
7. Emit operation steps, activity logs, metrics, traces, and diagnostics for every transition.
8. Add retry, timeout, rollback, and partial-failure behavior for node-agent or container failures.
9. Add unit, provider conformance, resource-manager, portal, and negative authorization tests.
10. Document operator runbook, customer UX, known limits, and release evidence.

### F010 - Multi-server network grouping
Category: Provisioning and lifecycle.
Importance: Manage related lobby, survival, minigame, proxy, and database resources together.
1. Add a DaisyCloud provider schema field or action for Multi-server network grouping.
2. Validate create and update input for legal values, defaults, quota, and idempotency.
3. Map the request into a Minecraft container desired-state plan with resource ID, region, size, endpoint, and owner.
4. Persist planned attributes through ResourceManager so dry-run, validation-only, create, and update behave consistently.
5. Wire API CLI SDK and portal affordances without bypassing the provider SPI.
6. Enforce RBAC actions for read, write, lifecycle, emergency, and support operations.
7. Emit operation steps, activity logs, metrics, traces, and diagnostics for every transition.
8. Add retry, timeout, rollback, and partial-failure behavior for node-agent or container failures.
9. Add unit, provider conformance, resource-manager, portal, and negative authorization tests.
10. Document operator runbook, customer UX, known limits, and release evidence.

### F011 - Java Edition support
Category: Runtime versions and software.
Importance: Run Minecraft Java servers as the primary mod-capable hosting path.
1. Represent Java Edition support as runtime metadata in the Minecraft server resource schema.
2. Normalize versions, server types, Java runtimes, image tags, and custom artifact identifiers.
3. Validate edition/server-type compatibility before plan or apply.
4. Resolve container image and startup command deterministically.
5. Store runtime lock metadata for reproducible rebuilds and support bundles.
6. Protect risky changes with backup-before-change and rollback requirements.
7. Surface runtime controls in admin panel, CLI, SDK, and template paths.
8. Emit compatibility, startup, and crash diagnostics with correlation IDs.
9. Test supported and unsupported version/type/runtime combinations exhaustively.
10. Publish upgrade, downgrade, and custom-runtime runbooks.

### F012 - Bedrock Edition support
Category: Runtime versions and software.
Importance: Run Bedrock or PocketMine servers for mobile, console, and Windows players.
1. Represent Bedrock Edition support as runtime metadata in the Minecraft server resource schema.
2. Normalize versions, server types, Java runtimes, image tags, and custom artifact identifiers.
3. Validate edition/server-type compatibility before plan or apply.
4. Resolve container image and startup command deterministically.
5. Store runtime lock metadata for reproducible rebuilds and support bundles.
6. Protect risky changes with backup-before-change and rollback requirements.
7. Surface runtime controls in admin panel, CLI, SDK, and template paths.
8. Emit compatibility, startup, and crash diagnostics with correlation IDs.
9. Test supported and unsupported version/type/runtime combinations exhaustively.
10. Publish upgrade, downgrade, and custom-runtime runbooks.

### F013 - Server software selection
Category: Runtime versions and software.
Importance: Choose Vanilla, Paper, Spigot, Purpur, Forge, Fabric, Quilt, NeoForge, Bedrock, or PocketMine.
1. Represent Server software selection as runtime metadata in the Minecraft server resource schema.
2. Normalize versions, server types, Java runtimes, image tags, and custom artifact identifiers.
3. Validate edition/server-type compatibility before plan or apply.
4. Resolve container image and startup command deterministically.
5. Store runtime lock metadata for reproducible rebuilds and support bundles.
6. Protect risky changes with backup-before-change and rollback requirements.
7. Surface runtime controls in admin panel, CLI, SDK, and template paths.
8. Emit compatibility, startup, and crash diagnostics with correlation IDs.
9. Test supported and unsupported version/type/runtime combinations exhaustively.
10. Publish upgrade, downgrade, and custom-runtime runbooks.

### F014 - Minecraft version pinning
Category: Runtime versions and software.
Importance: Pin exact Minecraft versions or choose latest and stable channels.
1. Represent Minecraft version pinning as runtime metadata in the Minecraft server resource schema.
2. Normalize versions, server types, Java runtimes, image tags, and custom artifact identifiers.
3. Validate edition/server-type compatibility before plan or apply.
4. Resolve container image and startup command deterministically.
5. Store runtime lock metadata for reproducible rebuilds and support bundles.
6. Protect risky changes with backup-before-change and rollback requirements.
7. Surface runtime controls in admin panel, CLI, SDK, and template paths.
8. Emit compatibility, startup, and crash diagnostics with correlation IDs.
9. Test supported and unsupported version/type/runtime combinations exhaustively.
10. Publish upgrade, downgrade, and custom-runtime runbooks.

### F015 - Upgrade and downgrade workflow
Category: Runtime versions and software.
Importance: Plan version changes with backup, compatibility, and rollback gates.
1. Represent Upgrade and downgrade workflow as runtime metadata in the Minecraft server resource schema.
2. Normalize versions, server types, Java runtimes, image tags, and custom artifact identifiers.
3. Validate edition/server-type compatibility before plan or apply.
4. Resolve container image and startup command deterministically.
5. Store runtime lock metadata for reproducible rebuilds and support bundles.
6. Protect risky changes with backup-before-change and rollback requirements.
7. Surface runtime controls in admin panel, CLI, SDK, and template paths.
8. Emit compatibility, startup, and crash diagnostics with correlation IDs.
9. Test supported and unsupported version/type/runtime combinations exhaustively.
10. Publish upgrade, downgrade, and custom-runtime runbooks.

### F016 - Java runtime selection
Category: Runtime versions and software.
Importance: Select supported Java runtimes that match Minecraft and loader requirements.
1. Represent Java runtime selection as runtime metadata in the Minecraft server resource schema.
2. Normalize versions, server types, Java runtimes, image tags, and custom artifact identifiers.
3. Validate edition/server-type compatibility before plan or apply.
4. Resolve container image and startup command deterministically.
5. Store runtime lock metadata for reproducible rebuilds and support bundles.
6. Protect risky changes with backup-before-change and rollback requirements.
7. Surface runtime controls in admin panel, CLI, SDK, and template paths.
8. Emit compatibility, startup, and crash diagnostics with correlation IDs.
9. Test supported and unsupported version/type/runtime combinations exhaustively.
10. Publish upgrade, downgrade, and custom-runtime runbooks.

### F017 - JVM flags and GC presets
Category: Runtime versions and software.
Importance: Expose safe memory and garbage-collection tuning without forcing shell access.
1. Represent JVM flags and GC presets as runtime metadata in the Minecraft server resource schema.
2. Normalize versions, server types, Java runtimes, image tags, and custom artifact identifiers.
3. Validate edition/server-type compatibility before plan or apply.
4. Resolve container image and startup command deterministically.
5. Store runtime lock metadata for reproducible rebuilds and support bundles.
6. Protect risky changes with backup-before-change and rollback requirements.
7. Surface runtime controls in admin panel, CLI, SDK, and template paths.
8. Emit compatibility, startup, and crash diagnostics with correlation IDs.
9. Test supported and unsupported version/type/runtime combinations exhaustively.
10. Publish upgrade, downgrade, and custom-runtime runbooks.

### F018 - Custom server JAR upload
Category: Runtime versions and software.
Importance: Allow expert users to upload custom server builds with validation.
1. Represent Custom server JAR upload as runtime metadata in the Minecraft server resource schema.
2. Normalize versions, server types, Java runtimes, image tags, and custom artifact identifiers.
3. Validate edition/server-type compatibility before plan or apply.
4. Resolve container image and startup command deterministically.
5. Store runtime lock metadata for reproducible rebuilds and support bundles.
6. Protect risky changes with backup-before-change and rollback requirements.
7. Surface runtime controls in admin panel, CLI, SDK, and template paths.
8. Emit compatibility, startup, and crash diagnostics with correlation IDs.
9. Test supported and unsupported version/type/runtime combinations exhaustively.
10. Publish upgrade, downgrade, and custom-runtime runbooks.

### F019 - EULA acceptance and compliance record
Category: Runtime versions and software.
Importance: Require explicit Minecraft EULA acceptance before provisioning.
1. Represent EULA acceptance and compliance record as runtime metadata in the Minecraft server resource schema.
2. Normalize versions, server types, Java runtimes, image tags, and custom artifact identifiers.
3. Validate edition/server-type compatibility before plan or apply.
4. Resolve container image and startup command deterministically.
5. Store runtime lock metadata for reproducible rebuilds and support bundles.
6. Protect risky changes with backup-before-change and rollback requirements.
7. Surface runtime controls in admin panel, CLI, SDK, and template paths.
8. Emit compatibility, startup, and crash diagnostics with correlation IDs.
9. Test supported and unsupported version/type/runtime combinations exhaustively.
10. Publish upgrade, downgrade, and custom-runtime runbooks.

### F020 - Container image and reproducible runtime
Category: Runtime versions and software.
Importance: Build deterministic server containers with pinned image, runtime, and content lock metadata.
1. Represent Container image and reproducible runtime as runtime metadata in the Minecraft server resource schema.
2. Normalize versions, server types, Java runtimes, image tags, and custom artifact identifiers.
3. Validate edition/server-type compatibility before plan or apply.
4. Resolve container image and startup command deterministically.
5. Store runtime lock metadata for reproducible rebuilds and support bundles.
6. Protect risky changes with backup-before-change and rollback requirements.
7. Surface runtime controls in admin panel, CLI, SDK, and template paths.
8. Emit compatibility, startup, and crash diagnostics with correlation IDs.
9. Test supported and unsupported version/type/runtime combinations exhaustively.
10. Publish upgrade, downgrade, and custom-runtime runbooks.

### F021 - One-click modpack installer
Category: Mods plugins and content.
Importance: Install popular modpacks from the panel without manual file work.
1. Model One-click modpack installer as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F022 - Searchable mod repository catalog
Category: Mods plugins and content.
Importance: Search mods and modpacks by name, category, loader, version, and source.
1. Model Searchable mod repository catalog as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F023 - Mod loader compatibility filters
Category: Mods plugins and content.
Importance: Prevent selecting Fabric mods for Forge servers or plugins for vanilla runtimes.
1. Model Mod loader compatibility filters as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F024 - Mod dependency resolution
Category: Mods plugins and content.
Importance: Resolve required libraries and transitive mod dependencies before install.
1. Model Mod dependency resolution as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F025 - Saved modpack profiles and instances
Category: Mods plugins and content.
Importance: Save different worlds and modpack setups for fast switching.
1. Model Saved modpack profiles and instances as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F026 - Custom mod upload
Category: Mods plugins and content.
Importance: Upload custom mod files with validation, hashing, and rollback protection.
1. Model Custom mod upload as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F027 - Plugin installer
Category: Mods plugins and content.
Importance: Install plugins for Paper, Spigot, and Purpur servers.
1. Model Plugin installer as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F028 - Plugin config editor
Category: Mods plugins and content.
Importance: Edit plugin configuration with schema hints and safe restart prompts.
1. Model Plugin config editor as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F029 - Mod conflict detection
Category: Mods plugins and content.
Importance: Detect known incompatible mods, duplicate libraries, and loader mismatches.
1. Model Mod conflict detection as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F030 - Modpack update and rollback planning
Category: Mods plugins and content.
Importance: Stage content updates with backup, dry-run, and restore points.
1. Model Modpack update and rollback planning as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F031 - CurseForge integration
Category: Mods plugins and content.
Importance: Search and resolve CurseForge mods, modpacks, files, and dependencies through an approved API path.
1. Model CurseForge integration as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F032 - Modrinth integration
Category: Mods plugins and content.
Importance: Search and resolve Modrinth projects, versions, loaders, and game-version filters.
1. Model Modrinth integration as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F033 - FTB Technic ATLauncher importers
Category: Mods plugins and content.
Importance: Import established modpack ecosystems that remain common in hosted Minecraft.
1. Model FTB Technic ATLauncher importers as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F034 - Pack-specific RAM guidance
Category: Mods plugins and content.
Importance: Recommend memory based on selected pack, mod count, player count, and telemetry.
1. Model Pack-specific RAM guidance as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F035 - Curated and safe marketplace
Category: Mods plugins and content.
Importance: Promote reviewed content and mark risky, abandoned, or incompatible packages.
1. Model Curated and safe marketplace as a content selection, resolver, or content-management operation.
2. Add source adapters for Modrinth, CurseForge, custom upload, and pack ecosystems where applicable.
3. Resolve loader, Minecraft version, dependency, file, checksum, and license metadata before activation.
4. Generate a content lockfile and provider plan diff for install, update, remove, and swap.
5. Require backup and restore point creation before destructive content changes.
6. Expose content browsing and action controls in the admin panel with RBAC gates.
7. Scan uploads and reject unsafe, incompatible, duplicate, or policy-blocked content.
8. Emit mod-resolution telemetry, crash hints, and support-bundle manifests.
9. Test resolver success, missing dependencies, loader mismatch, checksum mismatch, rollback, and offline catalog paths.
10. Document content-source terms, moderation policy, and manual recovery steps.

### F036 - Web admin dashboard
Category: Admin panel.
Importance: Provide the primary browser UI for server status, configuration, content, and operations.
1. Add an admin-panel capability or view model for Web admin dashboard.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F037 - Real-time console
Category: Admin panel.
Importance: Stream server console output and accept authorized commands.
1. Add an admin-panel capability or view model for Real-time console.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F038 - Command execution with RBAC and audit
Category: Admin panel.
Importance: Gate every command by role, scope, and correlation ID.
1. Add an admin-panel capability or view model for Command execution with RBAC and audit.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F039 - Log viewer search and sharing
Category: Admin panel.
Importance: Search logs, highlight errors, and create redacted share links.
1. Add an admin-panel capability or view model for Log viewer search and sharing.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F040 - Web file manager
Category: Admin panel.
Importance: Upload, edit, move, delete, and archive server files from the panel.
1. Add an admin-panel capability or view model for Web file manager.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F041 - SFTP or FTP access
Category: Admin panel.
Importance: Expose power-user file access with scoped credentials and auditability.
1. Add an admin-panel capability or view model for SFTP or FTP access.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F042 - Structured config editor
Category: Admin panel.
Importance: Edit server.properties, YAML, JSON, TOML, and loader configs safely.
1. Add an admin-panel capability or view model for Structured config editor.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F043 - Whitelist management
Category: Admin panel.
Importance: Add, remove, import, export, and enforce whitelist entries.
1. Add an admin-panel capability or view model for Whitelist management.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F044 - OP staff and role management
Category: Admin panel.
Importance: Manage operators and staff roles without sharing owner credentials.
1. Add an admin-panel capability or view model for OP staff and role management.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F045 - Banlist kick and player actions
Category: Admin panel.
Importance: Operate active players with controlled kick, ban, pardon, teleport, and message actions.
1. Add an admin-panel capability or view model for Banlist kick and player actions.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F046 - World settings editor
Category: Admin panel.
Importance: Change seed-visible metadata, game mode, difficulty, hardcore, spawn, and rules.
1. Add an admin-panel capability or view model for World settings editor.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F047 - MOTD and server icon editor
Category: Admin panel.
Importance: Edit branding details shown in clients and server lists.
1. Add an admin-panel capability or view model for MOTD and server icon editor.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F048 - Scheduled tasks
Category: Admin panel.
Importance: Run restarts, broadcasts, backups, commands, and maintenance windows on schedule.
1. Add an admin-panel capability or view model for Scheduled tasks.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F049 - Sub-user accounts and permissions
Category: Admin panel.
Importance: Delegate panel access by server, operation, file scope, and time window.
1. Add an admin-panel capability or view model for Sub-user accounts and permissions.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F050 - Two-factor authentication and session security
Category: Admin panel.
Importance: Protect public or privileged panel access with MFA, secure sessions, and device controls.
1. Add an admin-panel capability or view model for Two-factor authentication and session security.
2. Define HTTP API, command equivalent, RBAC action, and audit event shape.
3. Validate inputs and block unsafe public or privileged operations by default.
4. Route panel requests through management services and provider actions rather than direct file or process access.
5. Persist any user-visible state with generation and stale-state indicators.
6. Provide mobile and keyboard-accessible UX with clear success and failure states.
7. Redact secrets and limit sensitive console, file, and log output.
8. Emit telemetry, activity log, support-bundle, and diagnostic evidence.
9. Test permissions, happy path, malformed inputs, concurrent edits, refresh, and browser flows.
10. Add contextual help and operator runbook links.

### F051 - Automated backups
Category: Data backups and migration.
Importance: Capture worlds, configs, mods, plugins, and metadata on schedule.
1. Define the backup, restore, import, export, or migration contract for Automated backups.
2. Include world, config, mods, plugins, metadata, lockfile, and provider schema version in scope.
3. Validate storage target, retention, encryption, region, quota, and compatibility before execution.
4. Implement lifecycle steps with durable operation records and safe stop/start sequencing.
5. Add integrity verification through hashes, manifest checks, and restore previews.
6. Enforce RBAC, legal hold, destructive-action confirmation, and audit logging.
7. Support partial failure recovery, retries, cancellation, and idempotent replays.
8. Expose status, progress, and evidence in portal CLI SDK and support bundle paths.
9. Test corrupt archives, missing files, quota exhaustion, interrupted restore, and successful drill recovery.
10. Publish RPO/RTO, retention, migration, and recovery runbooks.

### F052 - Manual snapshots
Category: Data backups and migration.
Importance: Let operators create named restore points before risky changes.
1. Define the backup, restore, import, export, or migration contract for Manual snapshots.
2. Include world, config, mods, plugins, metadata, lockfile, and provider schema version in scope.
3. Validate storage target, retention, encryption, region, quota, and compatibility before execution.
4. Implement lifecycle steps with durable operation records and safe stop/start sequencing.
5. Add integrity verification through hashes, manifest checks, and restore previews.
6. Enforce RBAC, legal hold, destructive-action confirmation, and audit logging.
7. Support partial failure recovery, retries, cancellation, and idempotent replays.
8. Expose status, progress, and evidence in portal CLI SDK and support bundle paths.
9. Test corrupt archives, missing files, quota exhaustion, interrupted restore, and successful drill recovery.
10. Publish RPO/RTO, retention, migration, and recovery runbooks.

### F053 - Point-in-time restore
Category: Data backups and migration.
Importance: Restore a server to a selected backup with clear replacement semantics.
1. Define the backup, restore, import, export, or migration contract for Point-in-time restore.
2. Include world, config, mods, plugins, metadata, lockfile, and provider schema version in scope.
3. Validate storage target, retention, encryption, region, quota, and compatibility before execution.
4. Implement lifecycle steps with durable operation records and safe stop/start sequencing.
5. Add integrity verification through hashes, manifest checks, and restore previews.
6. Enforce RBAC, legal hold, destructive-action confirmation, and audit logging.
7. Support partial failure recovery, retries, cancellation, and idempotent replays.
8. Expose status, progress, and evidence in portal CLI SDK and support bundle paths.
9. Test corrupt archives, missing files, quota exhaustion, interrupted restore, and successful drill recovery.
10. Publish RPO/RTO, retention, migration, and recovery runbooks.

### F054 - Backup retention policies
Category: Data backups and migration.
Importance: Configure how many backups are retained and when old points expire.
1. Define the backup, restore, import, export, or migration contract for Backup retention policies.
2. Include world, config, mods, plugins, metadata, lockfile, and provider schema version in scope.
3. Validate storage target, retention, encryption, region, quota, and compatibility before execution.
4. Implement lifecycle steps with durable operation records and safe stop/start sequencing.
5. Add integrity verification through hashes, manifest checks, and restore previews.
6. Enforce RBAC, legal hold, destructive-action confirmation, and audit logging.
7. Support partial failure recovery, retries, cancellation, and idempotent replays.
8. Expose status, progress, and evidence in portal CLI SDK and support bundle paths.
9. Test corrupt archives, missing files, quota exhaustion, interrupted restore, and successful drill recovery.
10. Publish RPO/RTO, retention, migration, and recovery runbooks.

### F055 - Offsite backup storage
Category: Data backups and migration.
Importance: Store backups outside the active node failure domain.
1. Define the backup, restore, import, export, or migration contract for Offsite backup storage.
2. Include world, config, mods, plugins, metadata, lockfile, and provider schema version in scope.
3. Validate storage target, retention, encryption, region, quota, and compatibility before execution.
4. Implement lifecycle steps with durable operation records and safe stop/start sequencing.
5. Add integrity verification through hashes, manifest checks, and restore previews.
6. Enforce RBAC, legal hold, destructive-action confirmation, and audit logging.
7. Support partial failure recovery, retries, cancellation, and idempotent replays.
8. Expose status, progress, and evidence in portal CLI SDK and support bundle paths.
9. Test corrupt archives, missing files, quota exhaustion, interrupted restore, and successful drill recovery.
10. Publish RPO/RTO, retention, migration, and recovery runbooks.

### F056 - World import
Category: Data backups and migration.
Importance: Upload existing worlds from local machines or object storage.
1. Define the backup, restore, import, export, or migration contract for World import.
2. Include world, config, mods, plugins, metadata, lockfile, and provider schema version in scope.
3. Validate storage target, retention, encryption, region, quota, and compatibility before execution.
4. Implement lifecycle steps with durable operation records and safe stop/start sequencing.
5. Add integrity verification through hashes, manifest checks, and restore previews.
6. Enforce RBAC, legal hold, destructive-action confirmation, and audit logging.
7. Support partial failure recovery, retries, cancellation, and idempotent replays.
8. Expose status, progress, and evidence in portal CLI SDK and support bundle paths.
9. Test corrupt archives, missing files, quota exhaustion, interrupted restore, and successful drill recovery.
10. Publish RPO/RTO, retention, migration, and recovery runbooks.

### F057 - Host-to-host migration importer
Category: Data backups and migration.
Importance: Pull files from another host or archive with guided validation.
1. Define the backup, restore, import, export, or migration contract for Host-to-host migration importer.
2. Include world, config, mods, plugins, metadata, lockfile, and provider schema version in scope.
3. Validate storage target, retention, encryption, region, quota, and compatibility before execution.
4. Implement lifecycle steps with durable operation records and safe stop/start sequencing.
5. Add integrity verification through hashes, manifest checks, and restore previews.
6. Enforce RBAC, legal hold, destructive-action confirmation, and audit logging.
7. Support partial failure recovery, retries, cancellation, and idempotent replays.
8. Expose status, progress, and evidence in portal CLI SDK and support bundle paths.
9. Test corrupt archives, missing files, quota exhaustion, interrupted restore, and successful drill recovery.
10. Publish RPO/RTO, retention, migration, and recovery runbooks.

### F058 - Backup before destructive changes
Category: Data backups and migration.
Importance: Force backups before modpack switches, deletes, restores, and major upgrades.
1. Define the backup, restore, import, export, or migration contract for Backup before destructive changes.
2. Include world, config, mods, plugins, metadata, lockfile, and provider schema version in scope.
3. Validate storage target, retention, encryption, region, quota, and compatibility before execution.
4. Implement lifecycle steps with durable operation records and safe stop/start sequencing.
5. Add integrity verification through hashes, manifest checks, and restore previews.
6. Enforce RBAC, legal hold, destructive-action confirmation, and audit logging.
7. Support partial failure recovery, retries, cancellation, and idempotent replays.
8. Expose status, progress, and evidence in portal CLI SDK and support bundle paths.
9. Test corrupt archives, missing files, quota exhaustion, interrupted restore, and successful drill recovery.
10. Publish RPO/RTO, retention, migration, and recovery runbooks.

### F059 - Rollback between modpacks
Category: Data backups and migration.
Importance: Return to the previous pack, world, and config without manual reconstruction.
1. Define the backup, restore, import, export, or migration contract for Rollback between modpacks.
2. Include world, config, mods, plugins, metadata, lockfile, and provider schema version in scope.
3. Validate storage target, retention, encryption, region, quota, and compatibility before execution.
4. Implement lifecycle steps with durable operation records and safe stop/start sequencing.
5. Add integrity verification through hashes, manifest checks, and restore previews.
6. Enforce RBAC, legal hold, destructive-action confirmation, and audit logging.
7. Support partial failure recovery, retries, cancellation, and idempotent replays.
8. Expose status, progress, and evidence in portal CLI SDK and support bundle paths.
9. Test corrupt archives, missing files, quota exhaustion, interrupted restore, and successful drill recovery.
10. Publish RPO/RTO, retention, migration, and recovery runbooks.

### F060 - Disaster recovery drills and evidence
Category: Data backups and migration.
Importance: Regularly prove restore works and attach evidence to release gates.
1. Define the backup, restore, import, export, or migration contract for Disaster recovery drills and evidence.
2. Include world, config, mods, plugins, metadata, lockfile, and provider schema version in scope.
3. Validate storage target, retention, encryption, region, quota, and compatibility before execution.
4. Implement lifecycle steps with durable operation records and safe stop/start sequencing.
5. Add integrity verification through hashes, manifest checks, and restore previews.
6. Enforce RBAC, legal hold, destructive-action confirmation, and audit logging.
7. Support partial failure recovery, retries, cancellation, and idempotent replays.
8. Expose status, progress, and evidence in portal CLI SDK and support bundle paths.
9. Test corrupt archives, missing files, quota exhaustion, interrupted restore, and successful drill recovery.
10. Publish RPO/RTO, retention, migration, and recovery runbooks.

### F061 - High single-thread CPU allocation
Category: Performance and reliability.
Importance: Prioritize CPU profiles that match Minecraft tick-loop behavior.
1. Define measurable SLO, metric, or capacity dimension for High single-thread CPU allocation.
2. Add provider schema, default values, and safe limits tied to Minecraft tick-loop behavior.
3. Collect node-agent telemetry for CPU, heap, disk, process, TPS, MSPT, startup, and crash signals.
4. Surface recommendations and warnings in plan output before changes apply.
5. Implement health checks, watchdog, restart policy, or resize workflow as appropriate.
6. Protect risky tuning with backups, drains, maintenance windows, and rollback points.
7. Alert on threshold breaches and attach correlation IDs to diagnostics.
8. Add capacity guardrails to avoid unsafe overcommitment and noisy-neighbor failures.
9. Test low, default, high, and limit values plus crash, slow startup, disk full, and lag scenarios.
10. Document tuning guidance and escalation thresholds.

### F062 - NVMe SSD storage
Category: Performance and reliability.
Importance: Use fast storage for world chunks, region files, logs, and backups.
1. Define measurable SLO, metric, or capacity dimension for NVMe SSD storage.
2. Add provider schema, default values, and safe limits tied to Minecraft tick-loop behavior.
3. Collect node-agent telemetry for CPU, heap, disk, process, TPS, MSPT, startup, and crash signals.
4. Surface recommendations and warnings in plan output before changes apply.
5. Implement health checks, watchdog, restart policy, or resize workflow as appropriate.
6. Protect risky tuning with backups, drains, maintenance windows, and rollback points.
7. Alert on threshold breaches and attach correlation IDs to diagnostics.
8. Add capacity guardrails to avoid unsafe overcommitment and noisy-neighbor failures.
9. Test low, default, high, and limit values plus crash, slow startup, disk full, and lag scenarios.
10. Document tuning guidance and escalation thresholds.

### F063 - Crash auto-restart
Category: Performance and reliability.
Importance: Restart crashed servers with loop protection and incident evidence.
1. Define measurable SLO, metric, or capacity dimension for Crash auto-restart.
2. Add provider schema, default values, and safe limits tied to Minecraft tick-loop behavior.
3. Collect node-agent telemetry for CPU, heap, disk, process, TPS, MSPT, startup, and crash signals.
4. Surface recommendations and warnings in plan output before changes apply.
5. Implement health checks, watchdog, restart policy, or resize workflow as appropriate.
6. Protect risky tuning with backups, drains, maintenance windows, and rollback points.
7. Alert on threshold breaches and attach correlation IDs to diagnostics.
8. Add capacity guardrails to avoid unsafe overcommitment and noisy-neighbor failures.
9. Test low, default, high, and limit values plus crash, slow startup, disk full, and lag scenarios.
10. Document tuning guidance and escalation thresholds.

### F064 - Health checks and watchdog
Category: Performance and reliability.
Importance: Observe process, port, console, tick, and heartbeat health.
1. Define measurable SLO, metric, or capacity dimension for Health checks and watchdog.
2. Add provider schema, default values, and safe limits tied to Minecraft tick-loop behavior.
3. Collect node-agent telemetry for CPU, heap, disk, process, TPS, MSPT, startup, and crash signals.
4. Surface recommendations and warnings in plan output before changes apply.
5. Implement health checks, watchdog, restart policy, or resize workflow as appropriate.
6. Protect risky tuning with backups, drains, maintenance windows, and rollback points.
7. Alert on threshold breaches and attach correlation IDs to diagnostics.
8. Add capacity guardrails to avoid unsafe overcommitment and noisy-neighbor failures.
9. Test low, default, high, and limit values plus crash, slow startup, disk full, and lag scenarios.
10. Document tuning guidance and escalation thresholds.

### F065 - Vertical upgrade workflow
Category: Performance and reliability.
Importance: Resize RAM CPU and disk safely when a server outgrows its plan.
1. Define measurable SLO, metric, or capacity dimension for Vertical upgrade workflow.
2. Add provider schema, default values, and safe limits tied to Minecraft tick-loop behavior.
3. Collect node-agent telemetry for CPU, heap, disk, process, TPS, MSPT, startup, and crash signals.
4. Surface recommendations and warnings in plan output before changes apply.
5. Implement health checks, watchdog, restart policy, or resize workflow as appropriate.
6. Protect risky tuning with backups, drains, maintenance windows, and rollback points.
7. Alert on threshold breaches and attach correlation IDs to diagnostics.
8. Add capacity guardrails to avoid unsafe overcommitment and noisy-neighbor failures.
9. Test low, default, high, and limit values plus crash, slow startup, disk full, and lag scenarios.
10. Document tuning guidance and escalation thresholds.

### F066 - CPU RAM TPS and MSPT metrics
Category: Performance and reliability.
Importance: Track resource usage and gameplay health in one view.
1. Define measurable SLO, metric, or capacity dimension for CPU RAM TPS and MSPT metrics.
2. Add provider schema, default values, and safe limits tied to Minecraft tick-loop behavior.
3. Collect node-agent telemetry for CPU, heap, disk, process, TPS, MSPT, startup, and crash signals.
4. Surface recommendations and warnings in plan output before changes apply.
5. Implement health checks, watchdog, restart policy, or resize workflow as appropriate.
6. Protect risky tuning with backups, drains, maintenance windows, and rollback points.
7. Alert on threshold breaches and attach correlation IDs to diagnostics.
8. Add capacity guardrails to avoid unsafe overcommitment and noisy-neighbor failures.
9. Test low, default, high, and limit values plus crash, slow startup, disk full, and lag scenarios.
10. Document tuning guidance and escalation thresholds.

### F067 - Lag diagnostics and profiling
Category: Performance and reliability.
Importance: Identify plugin, mod, entity, chunk, disk, and GC causes of lag.
1. Define measurable SLO, metric, or capacity dimension for Lag diagnostics and profiling.
2. Add provider schema, default values, and safe limits tied to Minecraft tick-loop behavior.
3. Collect node-agent telemetry for CPU, heap, disk, process, TPS, MSPT, startup, and crash signals.
4. Surface recommendations and warnings in plan output before changes apply.
5. Implement health checks, watchdog, restart policy, or resize workflow as appropriate.
6. Protect risky tuning with backups, drains, maintenance windows, and rollback points.
7. Alert on threshold breaches and attach correlation IDs to diagnostics.
8. Add capacity guardrails to avoid unsafe overcommitment and noisy-neighbor failures.
9. Test low, default, high, and limit values plus crash, slow startup, disk full, and lag scenarios.
10. Document tuning guidance and escalation thresholds.

### F068 - Chunk pre-generation
Category: Performance and reliability.
Importance: Generate world chunks ahead of play to reduce exploration lag.
1. Define measurable SLO, metric, or capacity dimension for Chunk pre-generation.
2. Add provider schema, default values, and safe limits tied to Minecraft tick-loop behavior.
3. Collect node-agent telemetry for CPU, heap, disk, process, TPS, MSPT, startup, and crash signals.
4. Surface recommendations and warnings in plan output before changes apply.
5. Implement health checks, watchdog, restart policy, or resize workflow as appropriate.
6. Protect risky tuning with backups, drains, maintenance windows, and rollback points.
7. Alert on threshold breaches and attach correlation IDs to diagnostics.
8. Add capacity guardrails to avoid unsafe overcommitment and noisy-neighbor failures.
9. Test low, default, high, and limit values plus crash, slow startup, disk full, and lag scenarios.
10. Document tuning guidance and escalation thresholds.

### F069 - View and simulation distance controls
Category: Performance and reliability.
Importance: Tune distance settings for performance and gameplay needs.
1. Define measurable SLO, metric, or capacity dimension for View and simulation distance controls.
2. Add provider schema, default values, and safe limits tied to Minecraft tick-loop behavior.
3. Collect node-agent telemetry for CPU, heap, disk, process, TPS, MSPT, startup, and crash signals.
4. Surface recommendations and warnings in plan output before changes apply.
5. Implement health checks, watchdog, restart policy, or resize workflow as appropriate.
6. Protect risky tuning with backups, drains, maintenance windows, and rollback points.
7. Alert on threshold breaches and attach correlation IDs to diagnostics.
8. Add capacity guardrails to avoid unsafe overcommitment and noisy-neighbor failures.
9. Test low, default, high, and limit values plus crash, slow startup, disk full, and lag scenarios.
10. Document tuning guidance and escalation thresholds.

### F070 - Load-aware scheduling and capacity guardrails
Category: Performance and reliability.
Importance: Avoid noisy-node overload and unsafe overcommitment.
1. Define measurable SLO, metric, or capacity dimension for Load-aware scheduling and capacity guardrails.
2. Add provider schema, default values, and safe limits tied to Minecraft tick-loop behavior.
3. Collect node-agent telemetry for CPU, heap, disk, process, TPS, MSPT, startup, and crash signals.
4. Surface recommendations and warnings in plan output before changes apply.
5. Implement health checks, watchdog, restart policy, or resize workflow as appropriate.
6. Protect risky tuning with backups, drains, maintenance windows, and rollback points.
7. Alert on threshold breaches and attach correlation IDs to diagnostics.
8. Add capacity guardrails to avoid unsafe overcommitment and noisy-neighbor failures.
9. Test low, default, high, and limit values plus crash, slow startup, disk full, and lag scenarios.
10. Document tuning guidance and escalation thresholds.

### F071 - DDoS protection
Category: Networking and security.
Importance: Keep game and panel endpoints available during network attacks.
1. Model DDoS protection as a network, endpoint, credential, or security policy in provider attributes.
2. Validate public exposure, port ranges, DNS labels, proxy compatibility, and default-deny behavior.
3. Apply policy through DaisyNetwork or node-agent adapters without leaking provider internals.
4. Require MFA, TLS, secret references, and audit for privileged or public admin access.
5. Generate firewall, route, DDoS, proxy, and endpoint evidence in plan output.
6. Add abuse, upload, and brute-force controls where user-controlled inputs cross trust boundaries.
7. Emit security telemetry, denied-action audit events, and diagnostic hints.
8. Add rollback for route or credential changes that make the server unreachable.
9. Test public/private/internal permutations, invalid ports, blocked access, secret redaction, and attack simulations.
10. Document threat model, secure defaults, and emergency lock-down procedures.

### F072 - Firewall rules
Category: Networking and security.
Importance: Control source ranges, ports, and protocol exposure.
1. Model Firewall rules as a network, endpoint, credential, or security policy in provider attributes.
2. Validate public exposure, port ranges, DNS labels, proxy compatibility, and default-deny behavior.
3. Apply policy through DaisyNetwork or node-agent adapters without leaking provider internals.
4. Require MFA, TLS, secret references, and audit for privileged or public admin access.
5. Generate firewall, route, DDoS, proxy, and endpoint evidence in plan output.
6. Add abuse, upload, and brute-force controls where user-controlled inputs cross trust boundaries.
7. Emit security telemetry, denied-action audit events, and diagnostic hints.
8. Add rollback for route or credential changes that make the server unreachable.
9. Test public/private/internal permutations, invalid ports, blocked access, secret redaction, and attack simulations.
10. Document threat model, secure defaults, and emergency lock-down procedures.

### F073 - Public private and internal access modes
Category: Networking and security.
Importance: Decide whether game and panel endpoints are internet-facing or private.
1. Model Public private and internal access modes as a network, endpoint, credential, or security policy in provider attributes.
2. Validate public exposure, port ranges, DNS labels, proxy compatibility, and default-deny behavior.
3. Apply policy through DaisyNetwork or node-agent adapters without leaking provider internals.
4. Require MFA, TLS, secret references, and audit for privileged or public admin access.
5. Generate firewall, route, DDoS, proxy, and endpoint evidence in plan output.
6. Add abuse, upload, and brute-force controls where user-controlled inputs cross trust boundaries.
7. Emit security telemetry, denied-action audit events, and diagnostic hints.
8. Add rollback for route or credential changes that make the server unreachable.
9. Test public/private/internal permutations, invalid ports, blocked access, secret redaction, and attack simulations.
10. Document threat model, secure defaults, and emergency lock-down procedures.

### F074 - Additional port allocation
Category: Networking and security.
Importance: Expose extra ports for voice, maps, query, RCON, and proxies.
1. Model Additional port allocation as a network, endpoint, credential, or security policy in provider attributes.
2. Validate public exposure, port ranges, DNS labels, proxy compatibility, and default-deny behavior.
3. Apply policy through DaisyNetwork or node-agent adapters without leaking provider internals.
4. Require MFA, TLS, secret references, and audit for privileged or public admin access.
5. Generate firewall, route, DDoS, proxy, and endpoint evidence in plan output.
6. Add abuse, upload, and brute-force controls where user-controlled inputs cross trust boundaries.
7. Emit security telemetry, denied-action audit events, and diagnostic hints.
8. Add rollback for route or credential changes that make the server unreachable.
9. Test public/private/internal permutations, invalid ports, blocked access, secret redaction, and attack simulations.
10. Document threat model, secure defaults, and emergency lock-down procedures.

### F075 - Velocity and Bungee proxy support
Category: Networking and security.
Importance: Run proxy networks with lobby and backend server routing.
1. Model Velocity and Bungee proxy support as a network, endpoint, credential, or security policy in provider attributes.
2. Validate public exposure, port ranges, DNS labels, proxy compatibility, and default-deny behavior.
3. Apply policy through DaisyNetwork or node-agent adapters without leaking provider internals.
4. Require MFA, TLS, secret references, and audit for privileged or public admin access.
5. Generate firewall, route, DDoS, proxy, and endpoint evidence in plan output.
6. Add abuse, upload, and brute-force controls where user-controlled inputs cross trust boundaries.
7. Emit security telemetry, denied-action audit events, and diagnostic hints.
8. Add rollback for route or credential changes that make the server unreachable.
9. Test public/private/internal permutations, invalid ports, blocked access, secret redaction, and attack simulations.
10. Document threat model, secure defaults, and emergency lock-down procedures.

### F076 - Geyser and Floodgate bridge setup
Category: Networking and security.
Importance: Bridge Java and Bedrock players where compatible.
1. Model Geyser and Floodgate bridge setup as a network, endpoint, credential, or security policy in provider attributes.
2. Validate public exposure, port ranges, DNS labels, proxy compatibility, and default-deny behavior.
3. Apply policy through DaisyNetwork or node-agent adapters without leaking provider internals.
4. Require MFA, TLS, secret references, and audit for privileged or public admin access.
5. Generate firewall, route, DDoS, proxy, and endpoint evidence in plan output.
6. Add abuse, upload, and brute-force controls where user-controlled inputs cross trust boundaries.
7. Emit security telemetry, denied-action audit events, and diagnostic hints.
8. Add rollback for route or credential changes that make the server unreachable.
9. Test public/private/internal permutations, invalid ports, blocked access, secret redaction, and attack simulations.
10. Document threat model, secure defaults, and emergency lock-down procedures.

### F077 - Panel TLS and reverse proxy
Category: Networking and security.
Importance: Serve admin surfaces over authenticated TLS routes.
1. Model Panel TLS and reverse proxy as a network, endpoint, credential, or security policy in provider attributes.
2. Validate public exposure, port ranges, DNS labels, proxy compatibility, and default-deny behavior.
3. Apply policy through DaisyNetwork or node-agent adapters without leaking provider internals.
4. Require MFA, TLS, secret references, and audit for privileged or public admin access.
5. Generate firewall, route, DDoS, proxy, and endpoint evidence in plan output.
6. Add abuse, upload, and brute-force controls where user-controlled inputs cross trust boundaries.
7. Emit security telemetry, denied-action audit events, and diagnostic hints.
8. Add rollback for route or credential changes that make the server unreachable.
9. Test public/private/internal permutations, invalid ports, blocked access, secret redaction, and attack simulations.
10. Document threat model, secure defaults, and emergency lock-down procedures.

### F078 - Secrets management
Category: Networking and security.
Importance: Store RCON, SFTP, API, database, and token secrets outside visible config.
1. Model Secrets management as a network, endpoint, credential, or security policy in provider attributes.
2. Validate public exposure, port ranges, DNS labels, proxy compatibility, and default-deny behavior.
3. Apply policy through DaisyNetwork or node-agent adapters without leaking provider internals.
4. Require MFA, TLS, secret references, and audit for privileged or public admin access.
5. Generate firewall, route, DDoS, proxy, and endpoint evidence in plan output.
6. Add abuse, upload, and brute-force controls where user-controlled inputs cross trust boundaries.
7. Emit security telemetry, denied-action audit events, and diagnostic hints.
8. Add rollback for route or credential changes that make the server unreachable.
9. Test public/private/internal permutations, invalid ports, blocked access, secret redaction, and attack simulations.
10. Document threat model, secure defaults, and emergency lock-down procedures.

### F079 - Abuse detection and rate limiting
Category: Networking and security.
Importance: Detect command spam, login storms, brute force, and suspicious upload behavior.
1. Model Abuse detection and rate limiting as a network, endpoint, credential, or security policy in provider attributes.
2. Validate public exposure, port ranges, DNS labels, proxy compatibility, and default-deny behavior.
3. Apply policy through DaisyNetwork or node-agent adapters without leaking provider internals.
4. Require MFA, TLS, secret references, and audit for privileged or public admin access.
5. Generate firewall, route, DDoS, proxy, and endpoint evidence in plan output.
6. Add abuse, upload, and brute-force controls where user-controlled inputs cross trust boundaries.
7. Emit security telemetry, denied-action audit events, and diagnostic hints.
8. Add rollback for route or credential changes that make the server unreachable.
9. Test public/private/internal permutations, invalid ports, blocked access, secret redaction, and attack simulations.
10. Document threat model, secure defaults, and emergency lock-down procedures.

### F080 - Upload malware scanning
Category: Networking and security.
Importance: Scan uploaded jars, zips, and scripts before activation.
1. Model Upload malware scanning as a network, endpoint, credential, or security policy in provider attributes.
2. Validate public exposure, port ranges, DNS labels, proxy compatibility, and default-deny behavior.
3. Apply policy through DaisyNetwork or node-agent adapters without leaking provider internals.
4. Require MFA, TLS, secret references, and audit for privileged or public admin access.
5. Generate firewall, route, DDoS, proxy, and endpoint evidence in plan output.
6. Add abuse, upload, and brute-force controls where user-controlled inputs cross trust boundaries.
7. Emit security telemetry, denied-action audit events, and diagnostic hints.
8. Add rollback for route or credential changes that make the server unreachable.
9. Test public/private/internal permutations, invalid ports, blocked access, secret redaction, and attack simulations.
10. Document threat model, secure defaults, and emergency lock-down procedures.

### F081 - Alerts and notifications
Category: Observability support and operations.
Importance: Notify owners about crashes, backup failures, high lag, quota, billing, and security events.
1. Define the signal, workflow, or support artifact for Alerts and notifications.
2. Add stable event, metric, trace, and diagnostic-bundle fields with redaction classifications.
3. Correlate server, provider, operation, panel, node, backup, and content resolver records.
4. Surface the information in portal, CLI, SDK, MCP, and support views.
5. Add filtering by subscription, resource group, server, node, region, severity, and time window.
6. Protect customer data with redaction, scoped access, retention controls, and export policy.
7. Integrate alert routing, ticket handoff, and known-issue runbook links.
8. Preserve evidence during crashes, provider outages, and telemetry sink failures.
9. Test event shape, redaction, correlation, failure delivery, UI rendering, and support-bundle completeness.
10. Publish observable evidence requirements for release gates and support operations.

### F082 - Status page and service health
Category: Observability support and operations.
Importance: Show node, region, panel, API, and server health to users and support.
1. Define the signal, workflow, or support artifact for Status page and service health.
2. Add stable event, metric, trace, and diagnostic-bundle fields with redaction classifications.
3. Correlate server, provider, operation, panel, node, backup, and content resolver records.
4. Surface the information in portal, CLI, SDK, MCP, and support views.
5. Add filtering by subscription, resource group, server, node, region, severity, and time window.
6. Protect customer data with redaction, scoped access, retention controls, and export policy.
7. Integrate alert routing, ticket handoff, and known-issue runbook links.
8. Preserve evidence during crashes, provider outages, and telemetry sink failures.
9. Test event shape, redaction, correlation, failure delivery, UI rendering, and support-bundle completeness.
10. Publish observable evidence requirements for release gates and support operations.

### F083 - Crash report analyzer
Category: Observability support and operations.
Importance: Summarize stack traces, mod conflicts, memory errors, and missing dependencies.
1. Define the signal, workflow, or support artifact for Crash report analyzer.
2. Add stable event, metric, trace, and diagnostic-bundle fields with redaction classifications.
3. Correlate server, provider, operation, panel, node, backup, and content resolver records.
4. Surface the information in portal, CLI, SDK, MCP, and support views.
5. Add filtering by subscription, resource group, server, node, region, severity, and time window.
6. Protect customer data with redaction, scoped access, retention controls, and export policy.
7. Integrate alert routing, ticket handoff, and known-issue runbook links.
8. Preserve evidence during crashes, provider outages, and telemetry sink failures.
9. Test event shape, redaction, correlation, failure delivery, UI rendering, and support-bundle completeness.
10. Publish observable evidence requirements for release gates and support operations.

### F084 - Support bundle and ticket handoff
Category: Observability support and operations.
Importance: Collect redacted diagnostics for human support without exposing secrets.
1. Define the signal, workflow, or support artifact for Support bundle and ticket handoff.
2. Add stable event, metric, trace, and diagnostic-bundle fields with redaction classifications.
3. Correlate server, provider, operation, panel, node, backup, and content resolver records.
4. Surface the information in portal, CLI, SDK, MCP, and support views.
5. Add filtering by subscription, resource group, server, node, region, severity, and time window.
6. Protect customer data with redaction, scoped access, retention controls, and export policy.
7. Integrate alert routing, ticket handoff, and known-issue runbook links.
8. Preserve evidence during crashes, provider outages, and telemetry sink failures.
9. Test event shape, redaction, correlation, failure delivery, UI rendering, and support-bundle completeness.
10. Publish observable evidence requirements for release gates and support operations.

### F085 - Live support integration
Category: Observability support and operations.
Importance: Connect panel context to chat or ticket workflows.
1. Define the signal, workflow, or support artifact for Live support integration.
2. Add stable event, metric, trace, and diagnostic-bundle fields with redaction classifications.
3. Correlate server, provider, operation, panel, node, backup, and content resolver records.
4. Surface the information in portal, CLI, SDK, MCP, and support views.
5. Add filtering by subscription, resource group, server, node, region, severity, and time window.
6. Protect customer data with redaction, scoped access, retention controls, and export policy.
7. Integrate alert routing, ticket handoff, and known-issue runbook links.
8. Preserve evidence during crashes, provider outages, and telemetry sink failures.
9. Test event shape, redaction, correlation, failure delivery, UI rendering, and support-bundle completeness.
10. Publish observable evidence requirements for release gates and support operations.

### F086 - Tutorials and onboarding flows
Category: Observability support and operations.
Importance: Guide first-time owners through launch, OP setup, backups, mods, and invites.
1. Define the signal, workflow, or support artifact for Tutorials and onboarding flows.
2. Add stable event, metric, trace, and diagnostic-bundle fields with redaction classifications.
3. Correlate server, provider, operation, panel, node, backup, and content resolver records.
4. Surface the information in portal, CLI, SDK, MCP, and support views.
5. Add filtering by subscription, resource group, server, node, region, severity, and time window.
6. Protect customer data with redaction, scoped access, retention controls, and export policy.
7. Integrate alert routing, ticket handoff, and known-issue runbook links.
8. Preserve evidence during crashes, provider outages, and telemetry sink failures.
9. Test event shape, redaction, correlation, failure delivery, UI rendering, and support-bundle completeness.
10. Publish observable evidence requirements for release gates and support operations.

### F087 - Activity logs and correlation IDs
Category: Observability support and operations.
Importance: Record every operation across API, portal, provider, and node agents.
1. Define the signal, workflow, or support artifact for Activity logs and correlation IDs.
2. Add stable event, metric, trace, and diagnostic-bundle fields with redaction classifications.
3. Correlate server, provider, operation, panel, node, backup, and content resolver records.
4. Surface the information in portal, CLI, SDK, MCP, and support views.
5. Add filtering by subscription, resource group, server, node, region, severity, and time window.
6. Protect customer data with redaction, scoped access, retention controls, and export policy.
7. Integrate alert routing, ticket handoff, and known-issue runbook links.
8. Preserve evidence during crashes, provider outages, and telemetry sink failures.
9. Test event shape, redaction, correlation, failure delivery, UI rendering, and support-bundle completeness.
10. Publish observable evidence requirements for release gates and support operations.

### F088 - Node status and latency test
Category: Observability support and operations.
Importance: Expose location capacity and ping evidence before placement or migration.
1. Define the signal, workflow, or support artifact for Node status and latency test.
2. Add stable event, metric, trace, and diagnostic-bundle fields with redaction classifications.
3. Correlate server, provider, operation, panel, node, backup, and content resolver records.
4. Surface the information in portal, CLI, SDK, MCP, and support views.
5. Add filtering by subscription, resource group, server, node, region, severity, and time window.
6. Protect customer data with redaction, scoped access, retention controls, and export policy.
7. Integrate alert routing, ticket handoff, and known-issue runbook links.
8. Preserve evidence during crashes, provider outages, and telemetry sink failures.
9. Test event shape, redaction, correlation, failure delivery, UI rendering, and support-bundle completeness.
10. Publish observable evidence requirements for release gates and support operations.

### F089 - API CLI and SDK coverage
Category: Observability support and operations.
Importance: Make server lifecycle and content operations scriptable outside the panel.
1. Define the signal, workflow, or support artifact for API CLI and SDK coverage.
2. Add stable event, metric, trace, and diagnostic-bundle fields with redaction classifications.
3. Correlate server, provider, operation, panel, node, backup, and content resolver records.
4. Surface the information in portal, CLI, SDK, MCP, and support views.
5. Add filtering by subscription, resource group, server, node, region, severity, and time window.
6. Protect customer data with redaction, scoped access, retention controls, and export policy.
7. Integrate alert routing, ticket handoff, and known-issue runbook links.
8. Preserve evidence during crashes, provider outages, and telemetry sink failures.
9. Test event shape, redaction, correlation, failure delivery, UI rendering, and support-bundle completeness.
10. Publish observable evidence requirements for release gates and support operations.

### F090 - Webhooks and events
Category: Observability support and operations.
Importance: Emit lifecycle, backup, player, crash, and security events to external systems.
1. Define the signal, workflow, or support artifact for Webhooks and events.
2. Add stable event, metric, trace, and diagnostic-bundle fields with redaction classifications.
3. Correlate server, provider, operation, panel, node, backup, and content resolver records.
4. Surface the information in portal, CLI, SDK, MCP, and support views.
5. Add filtering by subscription, resource group, server, node, region, severity, and time window.
6. Protect customer data with redaction, scoped access, retention controls, and export policy.
7. Integrate alert routing, ticket handoff, and known-issue runbook links.
8. Preserve evidence during crashes, provider outages, and telemetry sink failures.
9. Test event shape, redaction, correlation, failure delivery, UI rendering, and support-bundle completeness.
10. Publish observable evidence requirements for release gates and support operations.

### F091 - Terraform and IaC templates
Category: Governance billing and automation.
Importance: Create reproducible Minecraft hosting deployments from declarative templates.
1. Model Terraform and IaC templates as a governance, billing, compliance, or automation contract.
2. Define schema, API, CLI, SDK, template, and portal behavior with stable IDs.
3. Validate policy, quota, budget, tag, location, role, and lifecycle constraints before provider mutation.
4. Persist decisions and evidence for replay, audit, export, and release review.
5. Enforce least privilege and separation of customer, support, billing, and provider-admin roles.
6. Integrate cost, usage, policy, and compliance state into plan and operation output.
7. Emit immutable activity and audit logs with correlation and actor context.
8. Add safe failure modes for policy engine, quota store, billing store, and automation runner outages.
9. Test API/CLI/SDK/template parity, denied operations, stale state, and export artifacts.
10. Document governance model, billing semantics, compliance boundaries, and release evidence.

### F092 - Cost estimates
Category: Governance billing and automation.
Importance: Show expected monthly cost before create, resize, backup, or location changes.
1. Model Cost estimates as a governance, billing, compliance, or automation contract.
2. Define schema, API, CLI, SDK, template, and portal behavior with stable IDs.
3. Validate policy, quota, budget, tag, location, role, and lifecycle constraints before provider mutation.
4. Persist decisions and evidence for replay, audit, export, and release review.
5. Enforce least privilege and separation of customer, support, billing, and provider-admin roles.
6. Integrate cost, usage, policy, and compliance state into plan and operation output.
7. Emit immutable activity and audit logs with correlation and actor context.
8. Add safe failure modes for policy engine, quota store, billing store, and automation runner outages.
9. Test API/CLI/SDK/template parity, denied operations, stale state, and export artifacts.
10. Document governance model, billing semantics, compliance boundaries, and release evidence.

### F093 - Usage metering
Category: Governance billing and automation.
Importance: Track RAM CPU storage bandwidth backup and support usage.
1. Model Usage metering as a governance, billing, compliance, or automation contract.
2. Define schema, API, CLI, SDK, template, and portal behavior with stable IDs.
3. Validate policy, quota, budget, tag, location, role, and lifecycle constraints before provider mutation.
4. Persist decisions and evidence for replay, audit, export, and release review.
5. Enforce least privilege and separation of customer, support, billing, and provider-admin roles.
6. Integrate cost, usage, policy, and compliance state into plan and operation output.
7. Emit immutable activity and audit logs with correlation and actor context.
8. Add safe failure modes for policy engine, quota store, billing store, and automation runner outages.
9. Test API/CLI/SDK/template parity, denied operations, stale state, and export artifacts.
10. Document governance model, billing semantics, compliance boundaries, and release evidence.

### F094 - Quotas and budget alerts
Category: Governance billing and automation.
Importance: Prevent runaway resource growth and notify before budget limits are hit.
1. Model Quotas and budget alerts as a governance, billing, compliance, or automation contract.
2. Define schema, API, CLI, SDK, template, and portal behavior with stable IDs.
3. Validate policy, quota, budget, tag, location, role, and lifecycle constraints before provider mutation.
4. Persist decisions and evidence for replay, audit, export, and release review.
5. Enforce least privilege and separation of customer, support, billing, and provider-admin roles.
6. Integrate cost, usage, policy, and compliance state into plan and operation output.
7. Emit immutable activity and audit logs with correlation and actor context.
8. Add safe failure modes for policy engine, quota store, billing store, and automation runner outages.
9. Test API/CLI/SDK/template parity, denied operations, stale state, and export artifacts.
10. Document governance model, billing semantics, compliance boundaries, and release evidence.

### F095 - Tags and labels
Category: Governance billing and automation.
Importance: Attach owner, environment, pack, community, cost center, and lifecycle metadata.
1. Model Tags and labels as a governance, billing, compliance, or automation contract.
2. Define schema, API, CLI, SDK, template, and portal behavior with stable IDs.
3. Validate policy, quota, budget, tag, location, role, and lifecycle constraints before provider mutation.
4. Persist decisions and evidence for replay, audit, export, and release review.
5. Enforce least privilege and separation of customer, support, billing, and provider-admin roles.
6. Integrate cost, usage, policy, and compliance state into plan and operation output.
7. Emit immutable activity and audit logs with correlation and actor context.
8. Add safe failure modes for policy engine, quota store, billing store, and automation runner outages.
9. Test API/CLI/SDK/template parity, denied operations, stale state, and export artifacts.
10. Document governance model, billing semantics, compliance boundaries, and release evidence.

### F096 - Audit log export
Category: Governance billing and automation.
Importance: Export immutable audit events to compliance or customer storage.
1. Model Audit log export as a governance, billing, compliance, or automation contract.
2. Define schema, API, CLI, SDK, template, and portal behavior with stable IDs.
3. Validate policy, quota, budget, tag, location, role, and lifecycle constraints before provider mutation.
4. Persist decisions and evidence for replay, audit, export, and release review.
5. Enforce least privilege and separation of customer, support, billing, and provider-admin roles.
6. Integrate cost, usage, policy, and compliance state into plan and operation output.
7. Emit immutable activity and audit logs with correlation and actor context.
8. Add safe failure modes for policy engine, quota store, billing store, and automation runner outages.
9. Test API/CLI/SDK/template parity, denied operations, stale state, and export artifacts.
10. Document governance model, billing semantics, compliance boundaries, and release evidence.

### F097 - Role model separation
Category: Governance billing and automation.
Importance: Separate owner, admin, operator, developer, moderator, support, and reader powers.
1. Model Role model separation as a governance, billing, compliance, or automation contract.
2. Define schema, API, CLI, SDK, template, and portal behavior with stable IDs.
3. Validate policy, quota, budget, tag, location, role, and lifecycle constraints before provider mutation.
4. Persist decisions and evidence for replay, audit, export, and release review.
5. Enforce least privilege and separation of customer, support, billing, and provider-admin roles.
6. Integrate cost, usage, policy, and compliance state into plan and operation output.
7. Emit immutable activity and audit logs with correlation and actor context.
8. Add safe failure modes for policy engine, quota store, billing store, and automation runner outages.
9. Test API/CLI/SDK/template parity, denied operations, stale state, and export artifacts.
10. Document governance model, billing semantics, compliance boundaries, and release evidence.

### F098 - EULA and content policy enforcement
Category: Governance billing and automation.
Importance: Prevent noncompliant provisioning, monetization, uploads, or unsafe marketplace content.
1. Model EULA and content policy enforcement as a governance, billing, compliance, or automation contract.
2. Define schema, API, CLI, SDK, template, and portal behavior with stable IDs.
3. Validate policy, quota, budget, tag, location, role, and lifecycle constraints before provider mutation.
4. Persist decisions and evidence for replay, audit, export, and release review.
5. Enforce least privilege and separation of customer, support, billing, and provider-admin roles.
6. Integrate cost, usage, policy, and compliance state into plan and operation output.
7. Emit immutable activity and audit logs with correlation and actor context.
8. Add safe failure modes for policy engine, quota store, billing store, and automation runner outages.
9. Test API/CLI/SDK/template parity, denied operations, stale state, and export artifacts.
10. Document governance model, billing semantics, compliance boundaries, and release evidence.

### F099 - Data residency and location policy
Category: Governance billing and automation.
Importance: Restrict regions and backup storage according to customer or legal policy.
1. Model Data residency and location policy as a governance, billing, compliance, or automation contract.
2. Define schema, API, CLI, SDK, template, and portal behavior with stable IDs.
3. Validate policy, quota, budget, tag, location, role, and lifecycle constraints before provider mutation.
4. Persist decisions and evidence for replay, audit, export, and release review.
5. Enforce least privilege and separation of customer, support, billing, and provider-admin roles.
6. Integrate cost, usage, policy, and compliance state into plan and operation output.
7. Emit immutable activity and audit logs with correlation and actor context.
8. Add safe failure modes for policy engine, quota store, billing store, and automation runner outages.
9. Test API/CLI/SDK/template parity, denied operations, stale state, and export artifacts.
10. Document governance model, billing semantics, compliance boundaries, and release evidence.

### F100 - SLA uptime and provider conformance evidence
Category: Governance billing and automation.
Importance: Produce release evidence that the provider, panel, and recovery path are shippable.
1. Model SLA uptime and provider conformance evidence as a governance, billing, compliance, or automation contract.
2. Define schema, API, CLI, SDK, template, and portal behavior with stable IDs.
3. Validate policy, quota, budget, tag, location, role, and lifecycle constraints before provider mutation.
4. Persist decisions and evidence for replay, audit, export, and release review.
5. Enforce least privilege and separation of customer, support, billing, and provider-admin roles.
6. Integrate cost, usage, policy, and compliance state into plan and operation output.
7. Emit immutable activity and audit logs with correlation and actor context.
8. Add safe failure modes for policy engine, quota store, billing store, and automation runner outages.
9. Test API/CLI/SDK/template parity, denied operations, stale state, and export artifacts.
10. Document governance model, billing semantics, compliance boundaries, and release evidence.

## Exhaustive Testing Matrix
The matrix below is feature-indexed. Full coverage requires applying each row across these dimensions where the feature is relevant:
- Editions: Java and Bedrock.
- Server types: Vanilla, Paper, Spigot, Purpur, Forge, Fabric, Quilt, NeoForge, Bedrock, PocketMine.
- Minecraft versions: exact current, exact older supported, latest, stable, unsupported, malformed.
- Content sources: none, Modrinth, CurseForge, custom upload, FTB, Technic, ATLauncher, unavailable source, checksum mismatch.
- Admin panel access: internal, public with MFA, public without MFA denied, disabled.
- Network modes: public with DDoS, public without DDoS denied, private, internal, disabled.
- Backup schedules: none, hourly, daily, weekly, retention 0 denied where scheduled, 1, 7, 30, 365.
- Resource sizes: minimum, default, recommended, high, maximum, above maximum denied.
- Operations: validate, plan, apply, observe, delete, import, export, backup, restore, diagnose.
- Surfaces: provider unit, ResourceManager integration, API, CLI, SDK, portal, MCP/read-only evidence where applicable.
- Faults: provider validation failure, node-agent outage, repository write failure, telemetry sink failure, content API outage, corrupt upload, port conflict, backup corruption, restore interruption, authorization denial.

| ID | Feature | Unit coverage | Negative coverage | Provider contract coverage | Integration and UI coverage | Resilience coverage | Evidence coverage |
|---|---|---|---|---|---|---|---|
| F001 | Instant server provisioning | Unit: schema/default/parser checks for Instant server provisioning. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F002 | Start stop restart kill lifecycle controls | Unit: schema/default/parser checks for Start stop restart kill lifecycle controls. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F003 | Always-on hosting mode | Unit: schema/default/parser checks for Always-on hosting mode. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F004 | Hibernation and wake-on-demand mode | Unit: schema/default/parser checks for Hibernation and wake-on-demand mode. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F005 | Server templates and cloning | Unit: schema/default/parser checks for Server templates and cloning. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F006 | Resource sizing plans | Unit: schema/default/parser checks for Resource sizing plans. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F007 | Region and datacenter selection | Unit: schema/default/parser checks for Region and datacenter selection. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F008 | Dedicated IP and default port support | Unit: schema/default/parser checks for Dedicated IP and default port support. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F009 | Subdomain and custom DNS | Unit: schema/default/parser checks for Subdomain and custom DNS. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F010 | Multi-server network grouping | Unit: schema/default/parser checks for Multi-server network grouping. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F011 | Java Edition support | Unit: schema/default/parser checks for Java Edition support. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F012 | Bedrock Edition support | Unit: schema/default/parser checks for Bedrock Edition support. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F013 | Server software selection | Unit: schema/default/parser checks for Server software selection. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F014 | Minecraft version pinning | Unit: schema/default/parser checks for Minecraft version pinning. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F015 | Upgrade and downgrade workflow | Unit: schema/default/parser checks for Upgrade and downgrade workflow. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F016 | Java runtime selection | Unit: schema/default/parser checks for Java runtime selection. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F017 | JVM flags and GC presets | Unit: schema/default/parser checks for JVM flags and GC presets. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F018 | Custom server JAR upload | Unit: schema/default/parser checks for Custom server JAR upload. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F019 | EULA acceptance and compliance record | Unit: schema/default/parser checks for EULA acceptance and compliance record. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F020 | Container image and reproducible runtime | Unit: schema/default/parser checks for Container image and reproducible runtime. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F021 | One-click modpack installer | Unit: schema/default/parser checks for One-click modpack installer. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F022 | Searchable mod repository catalog | Unit: schema/default/parser checks for Searchable mod repository catalog. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F023 | Mod loader compatibility filters | Unit: schema/default/parser checks for Mod loader compatibility filters. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F024 | Mod dependency resolution | Unit: schema/default/parser checks for Mod dependency resolution. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F025 | Saved modpack profiles and instances | Unit: schema/default/parser checks for Saved modpack profiles and instances. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F026 | Custom mod upload | Unit: schema/default/parser checks for Custom mod upload. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F027 | Plugin installer | Unit: schema/default/parser checks for Plugin installer. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F028 | Plugin config editor | Unit: schema/default/parser checks for Plugin config editor. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F029 | Mod conflict detection | Unit: schema/default/parser checks for Mod conflict detection. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F030 | Modpack update and rollback planning | Unit: schema/default/parser checks for Modpack update and rollback planning. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F031 | CurseForge integration | Unit: schema/default/parser checks for CurseForge integration. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F032 | Modrinth integration | Unit: schema/default/parser checks for Modrinth integration. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F033 | FTB Technic ATLauncher importers | Unit: schema/default/parser checks for FTB Technic ATLauncher importers. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F034 | Pack-specific RAM guidance | Unit: schema/default/parser checks for Pack-specific RAM guidance. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F035 | Curated and safe marketplace | Unit: schema/default/parser checks for Curated and safe marketplace. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F036 | Web admin dashboard | Unit: schema/default/parser checks for Web admin dashboard. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F037 | Real-time console | Unit: schema/default/parser checks for Real-time console. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F038 | Command execution with RBAC and audit | Unit: schema/default/parser checks for Command execution with RBAC and audit. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F039 | Log viewer search and sharing | Unit: schema/default/parser checks for Log viewer search and sharing. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F040 | Web file manager | Unit: schema/default/parser checks for Web file manager. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F041 | SFTP or FTP access | Unit: schema/default/parser checks for SFTP or FTP access. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F042 | Structured config editor | Unit: schema/default/parser checks for Structured config editor. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F043 | Whitelist management | Unit: schema/default/parser checks for Whitelist management. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F044 | OP staff and role management | Unit: schema/default/parser checks for OP staff and role management. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F045 | Banlist kick and player actions | Unit: schema/default/parser checks for Banlist kick and player actions. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F046 | World settings editor | Unit: schema/default/parser checks for World settings editor. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F047 | MOTD and server icon editor | Unit: schema/default/parser checks for MOTD and server icon editor. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F048 | Scheduled tasks | Unit: schema/default/parser checks for Scheduled tasks. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F049 | Sub-user accounts and permissions | Unit: schema/default/parser checks for Sub-user accounts and permissions. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F050 | Two-factor authentication and session security | Unit: schema/default/parser checks for Two-factor authentication and session security. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F051 | Automated backups | Unit: schema/default/parser checks for Automated backups. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F052 | Manual snapshots | Unit: schema/default/parser checks for Manual snapshots. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F053 | Point-in-time restore | Unit: schema/default/parser checks for Point-in-time restore. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F054 | Backup retention policies | Unit: schema/default/parser checks for Backup retention policies. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F055 | Offsite backup storage | Unit: schema/default/parser checks for Offsite backup storage. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F056 | World import | Unit: schema/default/parser checks for World import. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F057 | Host-to-host migration importer | Unit: schema/default/parser checks for Host-to-host migration importer. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F058 | Backup before destructive changes | Unit: schema/default/parser checks for Backup before destructive changes. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F059 | Rollback between modpacks | Unit: schema/default/parser checks for Rollback between modpacks. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F060 | Disaster recovery drills and evidence | Unit: schema/default/parser checks for Disaster recovery drills and evidence. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F061 | High single-thread CPU allocation | Unit: schema/default/parser checks for High single-thread CPU allocation. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F062 | NVMe SSD storage | Unit: schema/default/parser checks for NVMe SSD storage. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F063 | Crash auto-restart | Unit: schema/default/parser checks for Crash auto-restart. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F064 | Health checks and watchdog | Unit: schema/default/parser checks for Health checks and watchdog. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F065 | Vertical upgrade workflow | Unit: schema/default/parser checks for Vertical upgrade workflow. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F066 | CPU RAM TPS and MSPT metrics | Unit: schema/default/parser checks for CPU RAM TPS and MSPT metrics. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F067 | Lag diagnostics and profiling | Unit: schema/default/parser checks for Lag diagnostics and profiling. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F068 | Chunk pre-generation | Unit: schema/default/parser checks for Chunk pre-generation. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F069 | View and simulation distance controls | Unit: schema/default/parser checks for View and simulation distance controls. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F070 | Load-aware scheduling and capacity guardrails | Unit: schema/default/parser checks for Load-aware scheduling and capacity guardrails. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F071 | DDoS protection | Unit: schema/default/parser checks for DDoS protection. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F072 | Firewall rules | Unit: schema/default/parser checks for Firewall rules. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F073 | Public private and internal access modes | Unit: schema/default/parser checks for Public private and internal access modes. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F074 | Additional port allocation | Unit: schema/default/parser checks for Additional port allocation. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F075 | Velocity and Bungee proxy support | Unit: schema/default/parser checks for Velocity and Bungee proxy support. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F076 | Geyser and Floodgate bridge setup | Unit: schema/default/parser checks for Geyser and Floodgate bridge setup. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F077 | Panel TLS and reverse proxy | Unit: schema/default/parser checks for Panel TLS and reverse proxy. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F078 | Secrets management | Unit: schema/default/parser checks for Secrets management. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F079 | Abuse detection and rate limiting | Unit: schema/default/parser checks for Abuse detection and rate limiting. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F080 | Upload malware scanning | Unit: schema/default/parser checks for Upload malware scanning. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F081 | Alerts and notifications | Unit: schema/default/parser checks for Alerts and notifications. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F082 | Status page and service health | Unit: schema/default/parser checks for Status page and service health. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F083 | Crash report analyzer | Unit: schema/default/parser checks for Crash report analyzer. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F084 | Support bundle and ticket handoff | Unit: schema/default/parser checks for Support bundle and ticket handoff. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F085 | Live support integration | Unit: schema/default/parser checks for Live support integration. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F086 | Tutorials and onboarding flows | Unit: schema/default/parser checks for Tutorials and onboarding flows. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F087 | Activity logs and correlation IDs | Unit: schema/default/parser checks for Activity logs and correlation IDs. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F088 | Node status and latency test | Unit: schema/default/parser checks for Node status and latency test. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F089 | API CLI and SDK coverage | Unit: schema/default/parser checks for API CLI and SDK coverage. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F090 | Webhooks and events | Unit: schema/default/parser checks for Webhooks and events. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F091 | Terraform and IaC templates | Unit: schema/default/parser checks for Terraform and IaC templates. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F092 | Cost estimates | Unit: schema/default/parser checks for Cost estimates. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F093 | Usage metering | Unit: schema/default/parser checks for Usage metering. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F094 | Quotas and budget alerts | Unit: schema/default/parser checks for Quotas and budget alerts. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F095 | Tags and labels | Unit: schema/default/parser checks for Tags and labels. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F096 | Audit log export | Unit: schema/default/parser checks for Audit log export. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F097 | Role model separation | Unit: schema/default/parser checks for Role model separation. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F098 | EULA and content policy enforcement | Unit: schema/default/parser checks for EULA and content policy enforcement. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F099 | Data residency and location policy | Unit: schema/default/parser checks for Data residency and location policy. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |
| F100 | SLA uptime and provider conformance evidence | Unit: schema/default/parser checks for SLA uptime and provider conformance evidence. | Negative: malformed input, unsupported combo, denied RBAC, quota/policy failure. | Provider: validate/plan/apply/observe/delete/import/export/backup/restore/diagnose as applicable. | Integration: ResourceManager persistence, operation records, telemetry, diagnostics, portal/API/CLI parity. | Resilience: retry, timeout, partial failure, rollback, idempotency, and stale-state behavior. | Evidence: activity log, support bundle, release trace, and user-facing runbook. |

## Current First-Slice Test Assets
- `cloud-provider-minecraft/src/test/java/dev/daisycloud/provider/minecraft/MinecraftServerProviderTest.java` covers provider defaults, mod/plugin compatibility, EULA enforcement, public-panel MFA enforcement, DDoS enforcement, container manifest rendering, startup file rendering, structured content locks, offline content dependency expansion, stable content digests, node-agent handoff idempotency, reconcile digest changes, safe delete handoff, executable node-agent reconciliation through a runtime driver, executable delete handoff, admin panel route/permission/audit/security profiles, admin UX capability evidence, network/firewall/DDoS/TLS profiles, backup schedule/storage/compression/restore profiles, DaisyBase managed/external/disabled database profiles, generated DaisyBase bootstrap SQL compatibility, live marketplace source/policy validation, instance-manager validation, import/export migration contracts, backup, restore, observe, and diagnostics evidence.
- `cloud-testkit/src/test/java/dev/daisycloud/tests/product/ResourceProvidersTest.java` registers the Minecraft provider in the shared provider conformance suite.
- Next required tests before production: real node-agent/container execution tests, admin-panel browser and portal wiring tests, live content resolver fixtures, backup/restore execution drills, DaisyNetwork route binding tests, and API/CLI/SDK parity tests.

## Implementation Phasing
1. Foundation: provider schema, validation, planning, conformance, docs. This slice is complete.
2. Container execution contract: node-agent manifest, volume mount declaration, `eula.txt`, `server.properties`, content lockfile, runtime properties, process command, port bindings, and health probe declarations. This slice is complete.
3. Offline content lock and resolver catalog: structured modpack/mod/plugin items, source/id parsing, deterministic JSON, stable SHA-256 digest, offline dependency expansion, compatibility validation, and provider evidence. This slice is complete.
4. Node-agent execution handoff: typed task schema, resource limits, restart policy, startup file digests, reconcile digest, retry-safe idempotency key, and safe delete task preserving the world volume. This slice is complete.
5. Admin panel contract: console/log/file/config/player/content/support routes, owner/operator/viewer permission tiers, audit events, security policy, and support-bundle surfaces. This slice is complete.
6. Network policy contract: public/private/disabled exposure, game/panel endpoints, firewall rules, DDoS edge policy, DaisyNetwork route binding intent, and panel TLS posture. This slice is complete.
7. Backup policy contract: schedule, retention, storage class, compression, destructive-change backup guard, restore modes, RPO, and offsite evidence. This slice is complete.
8. DaisyBase, marketplace, admin UX, and instance-manager contracts: managed/external/disabled DaisyBase control-plane schema, bootstrap SQL, live/hybrid marketplace source intent, dependency/scan/rollback policy, realtime admin UX surfaces, and saved-instance switching evidence. This slice is complete at the provider-contract level.
9. Runtime execution adapter: `MinecraftNodeAgentExecutor` now executes provider-emitted handoffs against an injected runtime driver, performs DaisyBase bootstrap execution, materializes startup-file operations, starts/stops containers through the driver seam, probes health, and reports runtime state. Remaining work is a production Docker/OpenAppServiceContainer driver, live marketplace download/scan/install, DaisyNetwork route binding execution, durable instance switching, and backup job execution.
10. Admin panel UI/API wiring and product hardening: portal pages, browser-tested console/log/file/config/player/world flows, RBAC enforcement, audit persistence in DaisyBase, mobile layout, support bundle download, Modrinth/CurseForge/SpigotMC/FTB/Technic/ATLauncher live adapters, DR drills, billing, quotas, alerts, support workflows, IaC, CLI/SDK parity, load tests, and SLA evidence.
