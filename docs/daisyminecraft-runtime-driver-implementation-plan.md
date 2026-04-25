# DaisyMinecraft Runtime Driver Implementation Plan

## 95-Point Implementation Plan
1. Define the runtime driver contract as the boundary between provider planning and node execution.
2. Keep provider `apply` deterministic and side-effect free except for planned resource state.
3. Treat runtime launch as an explicit node-agent action, never as an implicit page render.
4. Preserve the existing `MinecraftNodeAgentTask` idempotency key as the reconciliation key.
5. Persist runtime status separately from control-plane provisioning status.
6. Add a local runtime driver for developer and demo execution.
7. Add a Docker/container runtime driver as the production executor path.
8. Add a runtime driver selector that can choose local-process, Docker, or dry-run modes.
9. Expose driver selection through system properties and environment variables.
10. Default local demo mode to safe local-process execution with no image pull.
11. Require explicit server artifact configuration before launching a local Java process.
12. Keep Docker image pull and execution behind an explicit action path.
13. Materialize a per-service runtime root directory.
14. Materialize a per-service data volume directory.
15. Materialize startup files into the mounted data directory.
16. Reject unsafe service, volume, and file names.
17. Prevent path traversal in startup file paths.
18. Persist the resolved container spec next to the runtime workspace.
19. Persist runtime pid metadata for local-process execution.
20. Persist stdout and stderr logs in a deterministic log directory.
21. Persist the last runtime action result for the admin panel.
22. Resolve Java executable path from configuration, then `java` fallback.
23. Resolve server jar path from configuration, then runtime artifact directory fallback.
24. Copy the configured jar into the service data directory as `server.jar`.
25. Launch `java -jar server.jar nogui` from the service data directory.
26. Apply memory limits from `containerResourceLimits.memoryMb`.
27. Preserve `server.properties` as the source of the game port.
28. Detect an already-running pid before starting a duplicate process.
29. Detect port conflicts before launching a new process.
30. Stop the process by pid on delete.
31. Preserve data volume on delete unless a destructive action is explicitly requested.
32. Release local port reservations on stop.
33. Probe process liveness through `ProcessHandle`.
34. Probe TCP reachability for the configured game endpoint.
35. Probe console/log availability from the runtime log file.
36. Mark TPS health as warmup until telemetry exists.
37. Mark DaisyBase health from connector bootstrap status.
38. Return degraded health when the process is alive but the port is not yet reachable.
39. Return failed health when a required runtime artifact is missing.
40. Surface runtime status in provider attributes without overwriting control-plane evidence.
41. Add admin portal controls for explicit runtime start.
42. Add admin portal controls for explicit runtime stop.
43. Add admin portal controls for runtime refresh/reprobe.
44. Add admin portal evidence for pid, workspace, logs, and jar path.
45. Add admin portal warnings when only control-plane resources exist.
46. Add admin portal warnings when local runtime launch fails.
47. Add admin portal guidance for configuring the server jar.
48. Add portal API action `start-minecraft-runtime`.
49. Add portal API action `stop-minecraft-runtime`.
50. Add portal API action `refresh-minecraft-runtime`.
51. Add ResourceManager operation evidence for runtime action dispatch.
52. Create the managed DaisyBase control database before node-agent execution.
53. Execute generated DaisyBase bootstrap SQL before runtime start.
54. Preserve database bootstrap idempotency.
55. Persist runtime execution ids into resource attributes.
56. Persist runtime applied step evidence.
57. Persist runtime health signal evidence.
58. Persist runtime failure reason evidence.
59. Avoid replacing user changes in unrelated resources.
60. Keep the provider test fake driver for pure contract tests.
61. Add local driver unit tests for file materialization.
62. Add local driver unit tests for path safety.
63. Add local driver unit tests for missing jar failure.
64. Add local driver unit tests for process metadata.
65. Add local driver unit tests for stop semantics.
66. Add node-agent tests for failed runtime launch.
67. Add portal tests for runtime action failure display.
68. Add portal tests for runtime action success with a controlled fake jar.
69. Add portal tests that the admin page never claims running without a listener.
70. Add smoke tests for the live portal endpoint.
71. Add optional Docker integration tests gated by an environment flag.
72. Add Docker driver image pull tests with a mocked command runner.
73. Add Docker driver port binding tests.
74. Add Docker driver volume mapping tests.
75. Add Docker driver environment mapping tests.
76. Add Docker driver health inspection tests.
77. Add Docker driver stop/remove tests that preserve volumes.
78. Add restart policy mapping tests.
79. Add active instance switch tests.
80. Add rollback instance activation tests.
81. Add backup-before-start tests.
82. Add backup-before-destructive-change tests.
83. Add marketplace content mount tests.
84. Add plugin/mod dependency materialization tests.
85. Add content lock mismatch fail-fast tests.
86. Add malware scan gate tests.
87. Add EULA rejection tests at runtime dispatch.
88. Add RBAC tests for runtime operations.
89. Add audit trail writes for start, stop, restart, and console actions.
90. Add log tail truncation and redaction tests.
91. Add crash report detection tests.
92. Add Windows path handling tests.
93. Add Unix path handling tests.
94. Add documentation for configuring local `server.jar`.
95. Add release notes explaining that control-plane ready is not runtime running.

## Testing Matrix
| Area | Test Cases | Expected Coverage |
| --- | --- | --- |
| Provider planning | Plan, apply, observe, delete, import, export | Control-plane contract remains deterministic |
| Runtime dispatch | Reconcile, delete, unsupported action, idempotent retry | Node-agent action routing and failure handling |
| Local driver filesystem | Volume creation, startup file writes, spec metadata, log paths | Workspace materialization and path safety |
| Local driver process | Missing jar, configured jar, duplicate start, stop by pid | Honest launch semantics and lifecycle control |
| Port handling | Free port, occupied port, TCP reachable, TCP missing | No false running state |
| Health probes | Process, port, console, TPS warmup, DaisyBase | Degraded vs healthy vs failed states |
| DaisyBase bootstrap | Managed DB, external DB, disabled DB, duplicate DDL | First-class database integration |
| Admin portal | Runtime pending, failed launch, TCP reachable, action controls | Browser-visible operator evidence |
| Portal API | Start runtime, stop runtime, refresh runtime, action JSON | HTTP workflow and state persistence |
| Security | EULA required, RBAC, unsafe paths, unsafe names | Runtime cannot launch unsafe or unauthorized workloads |
| Container driver | Pull, create, start, inspect, stop, volume preservation | Production container semantics |
| Recovery | Restart, crash, stale pid, log tail, rollback instance | Operational hardening |
| Cross-platform | Windows local paths, Unix local paths, Java path with spaces | Developer and production portability |
| Integration gates | Unit tests, portal tests, optional Docker tests, live smoke | Full regression coverage without mandatory Docker |
