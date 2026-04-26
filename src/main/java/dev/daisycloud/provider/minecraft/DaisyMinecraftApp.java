package dev.daisycloud.provider.minecraft;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DaisyMinecraftApp {
    private static final String APP_NAME = "DaisyMinecraft";
    private static final String VERSION = "0.1.0-SNAPSHOT";
    private static final ExecutorService HTTP_EXECUTOR = Executors.newCachedThreadPool();

    private DaisyMinecraftApp() {
    }

    public static void main(String[] args) throws IOException {
        int port = configuredPort();
        HttpServer server = createServer(port);
        server.start();
        System.out.println(APP_NAME + " listening on 0.0.0.0:" + server.getAddress().getPort());
    }

    static HttpServer createServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(HTTP_EXECUTOR);
        server.createContext("/", DaisyMinecraftApp::handle);
        return server;
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("GET".equals(exchange.getRequestMethod()) && ("/".equals(path)
                || "/minecraft/survival-core.html".equals(path))) {
            respond(exchange, 200, "text/html; charset=utf-8", portalHtml());
            return;
        }
        if ("GET".equals(exchange.getRequestMethod()) && "/health".equals(path)) {
            respond(exchange, 200, "application/json; charset=utf-8", healthJson());
            return;
        }
        if ("GET".equals(exchange.getRequestMethod()) && "/api/status".equals(path)) {
            respond(exchange, 200, "application/json; charset=utf-8", statusJson());
            return;
        }
        if ("GET".equals(exchange.getRequestMethod()) && "/api/server-browser".equals(path)) {
            respond(exchange, 200, "application/json; charset=utf-8", serverBrowserJson());
            return;
        }
        respond(exchange, 404, "application/json; charset=utf-8",
                "{\"error\":\"not_found\",\"path\":\"" + json(path) + "\"}");
    }

    private static int configuredPort() {
        String configured = firstNonBlank(
                System.getProperty("server.port"),
                System.getenv("PORT"),
                System.getenv("WEBSITES_PORT"),
                "8080");
        try {
            int port = Integer.parseInt(configured);
            if (port < 0 || port > 65_535) {
                throw new IllegalArgumentException("server port must be between 0 and 65535");
            }
            return port;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Invalid server port: " + configured, error);
        }
    }

    private static void respond(HttpExchange exchange, int statusCode, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String healthJson() {
        return """
                {"status":"ok","service":"DaisyMinecraft","version":"0.1.0-SNAPSHOT"}
                """.trim();
    }

    private static String statusJson() {
        Map<String, String> values = new TreeMap<>();
        values.put("service", APP_NAME);
        values.put("version", VERSION);
        values.put("runtime", "azure-app-service-java");
        values.put("provider", "DaisyCloud.Minecraft");
        values.put("resourceType", "servers");
        values.put("adminPanel", "available");
        values.put("serverBrowser", "opt-in");
        values.put("companionPlugin", "Daisy");
        values.put("timestamp", Instant.now().toString());
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (json.length() > 1) {
                json.append(',');
            }
            json.append('"').append(json(entry.getKey())).append("\":\"").append(json(entry.getValue())).append('"');
        }
        return json.append('}').toString();
    }

    private static String serverBrowserJson() {
        return """
                {
                  "listingMode": "opt-in",
                  "publicEndpointPattern": "daisyquest.azurewebsites.net:${port}",
                  "features": [
                    "server-listings",
                    "vote-collection",
                    "admin-panel-health",
                    "marketplace-readiness",
                    "runtime-driver-readiness"
                  ],
                  "sampleListings": [
                    {
                      "name": "Survival Core",
                      "edition": "java",
                      "version": "1.21.11",
                      "status": "awaiting-runtime-binding",
                      "votes": 0
                    }
                  ]
                }
                """.replace("${port}", firstNonBlank(System.getenv("PORT"), "25565")).trim();
    }

    private static String portalHtml() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>DaisyMinecraft Control Plane</title>
                  <style>
                    :root {
                      color-scheme: dark;
                      --bg: #07110d;
                      --panel: rgba(14, 31, 23, 0.84);
                      --panel-strong: rgba(22, 52, 37, 0.92);
                      --line: rgba(125, 255, 181, 0.24);
                      --text: #e8fff2;
                      --muted: #9fcab2;
                      --accent: #7dffb5;
                      --amber: #ffd36a;
                      --cyan: #75e6ff;
                    }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      min-height: 100vh;
                      font-family: "Segoe UI", "Trebuchet MS", sans-serif;
                      color: var(--text);
                      background:
                        radial-gradient(circle at 20% 20%, rgba(117, 230, 255, 0.18), transparent 28rem),
                        radial-gradient(circle at 80% 10%, rgba(125, 255, 181, 0.18), transparent 24rem),
                        linear-gradient(135deg, #040806, var(--bg) 52%, #0b1711);
                    }
                    body::before {
                      content: "";
                      position: fixed;
                      inset: 0;
                      pointer-events: none;
                      background-image:
                        linear-gradient(rgba(125, 255, 181, 0.07) 1px, transparent 1px),
                        linear-gradient(90deg, rgba(125, 255, 181, 0.07) 1px, transparent 1px);
                      background-size: 48px 48px;
                      mask-image: linear-gradient(to bottom, black, transparent 80%);
                    }
                    main {
                      width: min(1180px, calc(100% - 32px));
                      margin: 0 auto;
                      padding: 44px 0;
                    }
                    .hero {
                      border: 1px solid var(--line);
                      border-radius: 32px;
                      padding: clamp(28px, 5vw, 56px);
                      background: linear-gradient(135deg, rgba(15, 35, 26, 0.92), rgba(5, 14, 10, 0.86));
                      box-shadow: 0 24px 80px rgba(0, 0, 0, 0.42), inset 0 1px rgba(255, 255, 255, 0.08);
                      position: relative;
                      overflow: hidden;
                    }
                    .hero::after {
                      content: "";
                      position: absolute;
                      inset: auto -15% -45% 45%;
                      height: 320px;
                      background: radial-gradient(circle, rgba(125, 255, 181, 0.22), transparent 65%);
                    }
                    .eyebrow {
                      color: var(--accent);
                      letter-spacing: 0.18em;
                      text-transform: uppercase;
                      font-size: 0.8rem;
                      font-weight: 700;
                    }
                    h1 {
                      max-width: 760px;
                      margin: 14px 0 18px;
                      font-size: clamp(2.4rem, 7vw, 5.8rem);
                      line-height: 0.92;
                      letter-spacing: -0.07em;
                    }
                    .lead {
                      max-width: 760px;
                      color: var(--muted);
                      font-size: clamp(1.05rem, 2vw, 1.32rem);
                      line-height: 1.7;
                    }
                    .grid {
                      display: grid;
                      grid-template-columns: repeat(3, minmax(0, 1fr));
                      gap: 18px;
                      margin-top: 24px;
                    }
                    .card {
                      border: 1px solid var(--line);
                      border-radius: 24px;
                      padding: 22px;
                      background: var(--panel);
                    }
                    .card strong {
                      display: block;
                      color: var(--accent);
                      font-size: 1.8rem;
                      margin-bottom: 8px;
                    }
                    .card span { color: var(--muted); line-height: 1.55; }
                    .status {
                      display: flex;
                      flex-wrap: wrap;
                      gap: 12px;
                      margin-top: 28px;
                    }
                    .pill {
                      border: 1px solid var(--line);
                      border-radius: 999px;
                      padding: 10px 14px;
                      background: var(--panel-strong);
                      color: var(--muted);
                    }
                    .pill b { color: var(--text); }
                    a { color: var(--cyan); }
                    @media (max-width: 760px) {
                      main { width: min(100% - 20px, 1180px); padding: 20px 0; }
                      .hero { border-radius: 24px; }
                      .grid { grid-template-columns: 1fr; }
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <section class="hero">
                      <div class="eyebrow">DaisyCloud Minecraft Provider</div>
                      <h1>Control plane online.</h1>
                      <p class="lead">
                        DaisyMinecraft is booted as an Azure Java App Service. This runtime surface exposes health,
                        provider status, and the opt-in public server browser contract while the container runtime
                        driver handles actual Minecraft server processes.
                      </p>
                      <div class="status">
                        <span class="pill"><b>Health</b> /health</span>
                        <span class="pill"><b>Status</b> /api/status</span>
                        <span class="pill"><b>Browser</b> /api/server-browser</span>
                        <span class="pill"><b>Companion</b> Daisy dog plugin bundled</span>
                      </div>
                    </section>
                    <section class="grid" aria-label="DaisyMinecraft capabilities">
                      <article class="card">
                        <strong>Provider</strong>
                        <span>Minecraft server planning, content locks, backup/network policies, and node-agent runtime handoff.</span>
                      </article>
                      <article class="card">
                        <strong>Portal</strong>
                        <span>Live admin UX contract for console, files, config, players, worlds, backups, and support bundles.</span>
                      </article>
                      <article class="card">
                        <strong>Browser</strong>
                        <span>Opt-in public listings and vote collection designed for DaisyCloud server owners.</span>
                      </article>
                    </section>
                  </main>
                </body>
                </html>
                """;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        throw new IllegalArgumentException("at least one value must be non-blank");
    }

    private static String json(String value) {
        String input = Objects.toString(value, "");
        StringBuilder escaped = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (Character.isISOControl(character)) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
