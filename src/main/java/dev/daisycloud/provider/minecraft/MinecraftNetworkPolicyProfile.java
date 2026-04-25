package dev.daisycloud.provider.minecraft;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record MinecraftNetworkPolicyProfile(
        String schemaVersion,
        String networkMode,
        String ddosProtection,
        String gameEndpoint,
        String panelEndpoint,
        Map<String, String> firewallRules,
        String daisyNetworkBinding,
        String tlsMode,
        String exposure,
        String evidence) {
    public MinecraftNetworkPolicyProfile {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        networkMode = requireText(networkMode, "networkMode");
        ddosProtection = requireText(ddosProtection, "ddosProtection");
        gameEndpoint = Objects.requireNonNull(gameEndpoint, "gameEndpoint must not be null").trim();
        panelEndpoint = Objects.requireNonNull(panelEndpoint, "panelEndpoint must not be null").trim();
        firewallRules = copyMap(firewallRules, "firewallRules");
        daisyNetworkBinding = requireText(daisyNetworkBinding, "daisyNetworkBinding");
        tlsMode = requireText(tlsMode, "tlsMode");
        exposure = requireText(exposure, "exposure");
        evidence = requireText(evidence, "evidence");
    }

    public static MinecraftNetworkPolicyProfile fromPlannedAttributes(Map<String, String> attributes) {
        Map<String, String> planned = Objects.requireNonNull(attributes, "attributes must not be null");
        String serverName = requirePlanned(planned, "serverName");
        String networkMode = requirePlanned(planned, "networkMode");
        String ddosProtection = requirePlanned(planned, "ddosProtection");
        String adminPanel = requirePlanned(planned, "adminPanel");
        String panelAccess = requirePlanned(planned, "panelAccess");
        String fileAccess = requirePlanned(planned, "fileAccess");
        String port = requirePlanned(planned, "port");
        String gameEndpoint = planned.getOrDefault("serverEndpoint", "");
        String panelEndpoint = planned.getOrDefault("panelUrl", "");

        Map<String, String> firewallRules = new LinkedHashMap<>();
        if (!"disabled".equals(networkMode)) {
            firewallRules.put("game-tcp", "allow tcp/" + port + " from " + gameSource(networkMode));
            firewallRules.put("game-udp", "allow udp/" + port + " from " + gameSource(networkMode));
        }
        if ("enabled".equals(adminPanel) && !"disabled".equals(panelAccess)) {
            firewallRules.put("panel-https", "allow tcp/443 from " + panelAccess);
        }
        if ("sftp".equals(fileAccess) || "both".equals(fileAccess)) {
            firewallRules.put("sftp", "allow tcp/22 from internal-admin-network");
        }

        return new MinecraftNetworkPolicyProfile(
                "daisyminecraft.network.v1",
                networkMode,
                ddosProtection,
                gameEndpoint,
                panelEndpoint,
                firewallRules,
                daisyNetworkBinding(serverName, networkMode, panelAccess),
                tlsMode(adminPanel, panelAccess),
                exposure(networkMode, panelAccess),
                evidence(networkMode, ddosProtection, panelAccess, firewallRules.size()));
    }

    public Map<String, String> toProviderAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("networkPolicySchema", schemaVersion);
        attributes.put("networkExposure", exposure);
        attributes.put("networkGameEndpoint", gameEndpoint);
        attributes.put("networkPanelEndpoint", panelEndpoint);
        attributes.put("networkFirewallRules", joinMap(firewallRules));
        attributes.put("networkDaisyNetworkBinding", daisyNetworkBinding);
        attributes.put("networkDdosPolicy", ddosProtection + ";edge-scrubbing=" + edgeScrubbing(ddosProtection));
        attributes.put("networkTlsMode", tlsMode);
        attributes.put("networkPolicyEvidence", evidence);
        return Map.copyOf(attributes);
    }

    private static String daisyNetworkBinding(String serverName, String networkMode, String panelAccess) {
        if ("disabled".equals(networkMode) && "disabled".equals(panelAccess)) {
            return "disabled";
        }
        if ("public".equals(networkMode)) {
            return "pending-route-binding:" + serverName + "-game";
        }
        if ("public".equals(panelAccess)) {
            return "pending-route-binding:" + serverName + "-panel";
        }
        return "internal-route-binding:" + serverName;
    }

    private static String tlsMode(String adminPanel, String panelAccess) {
        if (!"enabled".equals(adminPanel) || "disabled".equals(panelAccess)) {
            return "not-applicable";
        }
        return "panel-tls-required";
    }

    private static String exposure(String networkMode, String panelAccess) {
        if ("disabled".equals(networkMode) && "disabled".equals(panelAccess)) {
            return "fully-disabled";
        }
        return "game=" + networkMode + ";panel=" + panelAccess;
    }

    private static String gameSource(String networkMode) {
        return "public".equals(networkMode) ? "internet-with-ddos-edge" : "private-network";
    }

    private static String edgeScrubbing(String ddosProtection) {
        return switch (ddosProtection) {
            case "advanced" -> "always-on";
            case "basic" -> "threshold";
            case "none" -> "disabled";
            default -> "unknown";
        };
    }

    private static String evidence(
            String networkMode,
            String ddosProtection,
            String panelAccess,
            int firewallRuleCount) {
        return "networkMode=" + networkMode
                + ";ddosProtection=" + ddosProtection
                + ";panelAccess=" + panelAccess
                + ";firewallRules=" + firewallRuleCount;
    }

    private static Map<String, String> copyMap(Map<String, String> values, String name) {
        Map<String, String> copied = new LinkedHashMap<>(Objects.requireNonNull(values, name + " must not be null"));
        for (Map.Entry<String, String> entry : copied.entrySet()) {
            requireText(entry.getKey(), name + " key");
            Objects.requireNonNull(entry.getValue(), name + " value must not be null");
        }
        return Map.copyOf(copied);
    }

    private static String joinMap(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static String requirePlanned(Map<String, String> planned, String name) {
        return requireText(planned.get(name), name);
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}
