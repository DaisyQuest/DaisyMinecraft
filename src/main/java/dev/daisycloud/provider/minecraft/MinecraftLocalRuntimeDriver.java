package dev.daisycloud.provider.minecraft;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Local-process runtime driver used by the developer portal and tests.
 *
 * <p>This driver intentionally does not download Minecraft artifacts. A real server jar must be supplied through
 * configuration before it will launch a process.
 */
public final class MinecraftLocalRuntimeDriver implements MinecraftRuntimeDriver {
    public static final String RUNTIME_ROOT_PROPERTY = "daisycloud.minecraft.runtime.root";
    public static final String SERVER_JAR_PROPERTY = "daisycloud.minecraft.serverJar";
    public static final String JAVA_EXECUTABLE_PROPERTY = "daisycloud.minecraft.java";

    private static final Pattern SAFE_TOKEN = Pattern.compile("[A-Za-z0-9._-]+");
    private static final String DATA_MOUNT = "/data";

    private final Path runtimeRoot;
    private final Path serverJar;
    private final String javaExecutable;
    private final Map<String, Path> volumeMounts = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> boundPorts = new LinkedHashMap<>();

    public MinecraftLocalRuntimeDriver(Path runtimeRoot, Path serverJar, String javaExecutable) {
        this.runtimeRoot = Objects.requireNonNull(runtimeRoot, "runtimeRoot must not be null")
                .toAbsolutePath()
                .normalize();
        this.serverJar = Objects.requireNonNull(serverJar, "serverJar must not be null")
                .toAbsolutePath()
                .normalize();
        this.javaExecutable = requireText(javaExecutable, "javaExecutable");
    }

    public static MinecraftLocalRuntimeDriver fromSystemProperties(Path defaultRuntimeRoot) {
        Path runtimeRoot = Path.of(System.getProperty(
                RUNTIME_ROOT_PROPERTY,
                Objects.requireNonNull(defaultRuntimeRoot, "defaultRuntimeRoot must not be null").toString()));
        Path artifactRoot = runtimeRoot.resolve("artifacts");
        Path serverJar = Path.of(System.getProperty(
                SERVER_JAR_PROPERTY,
                artifactRoot.resolve("server.jar").toString()));
        String javaExecutable = System.getProperty(JAVA_EXECUTABLE_PROPERTY, "java");
        return new MinecraftLocalRuntimeDriver(runtimeRoot, serverJar, javaExecutable);
    }

    public Path runtimeRoot() {
        return runtimeRoot;
    }

    public Path serverJar() {
        return serverJar;
    }

    @Override
    public void pullImage(String image) {
        writeText(runtimeRoot.resolve("last-requested-image.txt"), requireText(image, "image"));
    }

    @Override
    public void ensureVolume(String volumeName, String mountPath) {
        String safeVolume = safeToken(volumeName, "volumeName");
        String safeMountPath = requireText(mountPath, "mountPath");
        Path volumeDirectory = runtimeRoot.resolve("volumes").resolve(safeVolume).normalize();
        ensureInside(runtimeRoot, volumeDirectory);
        createDirectories(volumeDirectory);
        volumeMounts.put(safeMountPath, volumeDirectory);
        writeText(volumeDirectory.resolve(".daisycloud-volume"), "mountPath=" + safeMountPath + "\n");
    }

    @Override
    public void writeStartupFile(String serviceName, String fileName, String content) {
        safeToken(serviceName, "serviceName");
        Path dataDirectory = dataDirectory(serviceName);
        Path target = dataDirectory.resolve(safeRelativePath(fileName)).normalize();
        ensureInside(dataDirectory, target);
        createDirectories(target.getParent());
        writeText(target, Objects.requireNonNull(content, "content must not be null"));
    }

    @Override
    public void writeBinaryFile(String serviceName, String fileName, byte[] content) {
        safeToken(serviceName, "serviceName");
        Path dataDirectory = dataDirectory(serviceName);
        Path target = dataDirectory.resolve(safeRelativePath(fileName)).normalize();
        ensureInside(dataDirectory, target);
        createDirectories(target.getParent());
        writeBytes(target, Objects.requireNonNull(content, "content must not be null"));
    }

    @Override
    public void bindPort(String serviceName, String containerPort, String hostPort) {
        String safeService = safeToken(serviceName, "serviceName");
        requireText(containerPort, "containerPort");
        requireText(hostPort, "hostPort");
        boundPorts.computeIfAbsent(safeService, ignored -> new LinkedHashMap<>())
                .put(containerPort, hostPort);
    }

    @Override
    public void startContainer(MinecraftRuntimeContainerSpec spec) {
        Objects.requireNonNull(spec, "spec must not be null");
        String serviceName = safeToken(spec.serviceName(), "serviceName");
        Path serviceDirectory = serviceDirectory(serviceName);
        Path dataDirectory = dataDirectory(serviceName);
        Path logDirectory = serviceDirectory.resolve("logs");
        createDirectories(dataDirectory);
        createDirectories(logDirectory);
        writeSpec(serviceDirectory, spec);

        if (existingProcess(serviceName).map(ProcessHandle::isAlive).orElse(false)) {
            writeText(serviceDirectory.resolve("last-start.txt"), "already-running=" + Instant.now() + "\n");
            return;
        }
        if (!Files.isRegularFile(serverJar)) {
            throw new IllegalStateException("Minecraft server jar not found: " + serverJar
                    + ". Configure -D" + SERVER_JAR_PROPERTY + "=<path-to-server.jar> before starting runtime.");
        }
        for (String hostPort : spec.portBindings().values()) {
            if (isTcpReachable("127.0.0.1", parsePort(hostPort), 150)) {
                throw new IllegalStateException("Cannot start " + serviceName + ": host port " + hostPort
                        + " is already accepting TCP connections");
            }
        }

        Path localJar = dataDirectory.resolve("server.jar");
        copyServerJar(localJar);
        ProcessBuilder builder = new ProcessBuilder(javaCommand(spec));
        builder.directory(dataDirectory.toFile());
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logDirectory.resolve("latest.log").toFile()));
        builder.redirectError(ProcessBuilder.Redirect.appendTo(logDirectory.resolve("latest.err.log").toFile()));
        try {
            Process process = builder.start();
            writeText(serviceDirectory.resolve("pid"), Long.toString(process.pid()));
            writeText(serviceDirectory.resolve("last-start.txt"), "startedAt=" + Instant.now()
                    + "\npid=" + process.pid()
                    + "\njava=" + javaExecutable
                    + "\nserverJar=" + serverJar
                    + "\n");
        } catch (IOException error) {
            throw new IllegalStateException("Failed to start Minecraft runtime " + serviceName + ": "
                    + error.getMessage(), error);
        }
    }

    @Override
    public void stopContainer(String serviceName) {
        String safeService = safeToken(serviceName, "serviceName");
        Path serviceDirectory = serviceDirectory(safeService);
        Optional<ProcessHandle> process = existingProcess(safeService);
        process.ifPresent(handle -> {
            if (handle.isAlive()) {
                handle.destroy();
                try {
                    handle.onExit().get(10, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                    handle.destroyForcibly();
                }
            }
        });
        writeText(serviceDirectory.resolve("last-stop.txt"), "stoppedAt=" + Instant.now() + "\n");
    }

    @Override
    public void releasePort(String serviceName, String containerPort, String hostPort) {
        String safeService = safeToken(serviceName, "serviceName");
        Map<String, String> ports = boundPorts.get(safeService);
        if (ports != null) {
            ports.remove(requireText(containerPort, "containerPort"));
        }
        writeText(
                serviceDirectory(safeService).resolve("last-release-port.txt"),
                requireText(containerPort, "containerPort") + "=" + requireText(hostPort, "hostPort") + "\n");
    }

    @Override
    public Map<String, String> probeHealth(String serviceName, Map<String, String> expectedChecks) {
        String safeService = safeToken(serviceName, "serviceName");
        Map<String, String> checks = new LinkedHashMap<>(Objects.requireNonNull(
                expectedChecks,
                "expectedChecks must not be null"));
        Map<String, String> health = new LinkedHashMap<>();
        boolean processAlive = existingProcess(safeService).map(ProcessHandle::isAlive).orElse(false);
        boolean jarPresent = Files.isRegularFile(serverJar);
        for (String check : checks.keySet().stream().sorted(Comparator.naturalOrder()).toList()) {
            health.put(check, switch (check) {
                case "process" -> processAlive ? "healthy" : jarPresent ? "stopped" : "missing-server-jar";
                case "port" -> probeAnyBoundPort(safeService) ? "healthy" : "not-reachable";
                case "console" -> Files.isRegularFile(serviceDirectory(safeService).resolve("logs").resolve("latest.log"))
                        ? "healthy"
                        : "pending";
                case "tps" -> processAlive ? "pending-warmup" : "not-available";
                case "database" -> "healthy";
                default -> processAlive ? "pending" : "not-available";
            });
        }
        return Map.copyOf(health);
    }

    private List<String> javaCommand(MinecraftRuntimeContainerSpec spec) {
        int memoryMb = parsePositiveInt(spec.resourceLimits().getOrDefault("memoryMb", "2048"), "memoryMb");
        int initialMemoryMb = Math.max(256, Math.min(1024, memoryMb));
        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("-Xms" + initialMemoryMb + "M");
        command.add("-Xmx" + memoryMb + "M");
        command.add("-jar");
        command.add("server.jar");
        command.add("nogui");
        return command;
    }

    private boolean probeAnyBoundPort(String serviceName) {
        Map<String, String> ports = boundPorts.getOrDefault(serviceName, Map.of());
        for (String hostPort : ports.values()) {
            if (isTcpReachable("127.0.0.1", parsePort(hostPort), 150)) {
                return true;
            }
        }
        return false;
    }

    private Optional<ProcessHandle> existingProcess(String serviceName) {
        Path pidFile = serviceDirectory(serviceName).resolve("pid");
        if (!Files.isRegularFile(pidFile)) {
            return Optional.empty();
        }
        try {
            String pid = Files.readString(pidFile, StandardCharsets.UTF_8).trim();
            if (pid.isEmpty()) {
                return Optional.empty();
            }
            return ProcessHandle.of(Long.parseLong(pid));
        } catch (IOException | NumberFormatException error) {
            return Optional.empty();
        }
    }

    private void writeSpec(Path serviceDirectory, MinecraftRuntimeContainerSpec spec) {
        StringBuilder builder = new StringBuilder();
        builder.append("serviceName=").append(spec.serviceName()).append('\n');
        builder.append("image=").append(spec.image()).append('\n');
        builder.append("command=").append(spec.command()).append('\n');
        builder.append("restartPolicy=").append(spec.restartPolicy()).append('\n');
        builder.append("activeInstance=").append(spec.activeInstance()).append('\n');
        writeText(serviceDirectory.resolve("container-spec.properties"), builder.toString());
    }

    private void copyServerJar(Path localJar) {
        try {
            if (!Files.exists(localJar) || !Files.isSameFile(serverJar, localJar)) {
                Files.copy(serverJar, localJar, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Failed to copy Minecraft server jar into runtime data directory: "
                    + error.getMessage(), error);
        }
    }

    private Path dataDirectory(String serviceName) {
        Path mounted = volumeMounts.get(DATA_MOUNT);
        if (mounted != null) {
            return mounted;
        }
        return serviceDirectory(serviceName).resolve("data");
    }

    private Path serviceDirectory(String serviceName) {
        String safeService = safeToken(serviceName, "serviceName");
        Path path = runtimeRoot.resolve("services").resolve(safeService).normalize();
        ensureInside(runtimeRoot, path);
        createDirectories(path);
        return path;
    }

    private static Path safeRelativePath(String fileName) {
        String text = requireText(fileName, "fileName").replace('\\', '/');
        Path relative = Path.of(text).normalize();
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new IllegalArgumentException("Unsafe startup file path: " + fileName);
        }
        return relative;
    }

    private static String safeToken(String value, String name) {
        String text = requireText(value, name);
        if (!SAFE_TOKEN.matcher(text).matches()) {
            throw new IllegalArgumentException(name + " contains unsupported characters: " + value);
        }
        return text;
    }

    private static void ensureInside(Path parent, Path child) {
        if (!child.toAbsolutePath().normalize().startsWith(parent.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Path escapes runtime root: " + child);
        }
    }

    private static void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to create directory " + path + ": " + error.getMessage(), error);
        }
    }

    private static void writeText(Path path, String content) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to write " + path + ": " + error.getMessage(), error);
        }
    }

    private static void writeBytes(Path path, byte[] content) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, content);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to write " + path + ": " + error.getMessage(), error);
        }
    }

    private static boolean isTcpReachable(String host, int port, int timeoutMillis) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            return true;
        } catch (IOException error) {
            return false;
        }
    }

    private static int parsePort(String value) {
        return parsePositiveInt(value, "port");
    }

    private static int parsePositiveInt(String value, String name) {
        try {
            int parsed = Integer.parseInt(requireText(value, name));
            if (parsed < 1) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(name + " must be numeric: " + value, error);
        }
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}
