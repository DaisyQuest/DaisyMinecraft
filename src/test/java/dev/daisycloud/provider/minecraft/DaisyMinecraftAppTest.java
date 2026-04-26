package dev.daisycloud.provider.minecraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaisyMinecraftAppTest {
    @Test
    void serverBrowserDoesNotAdvertiseAzureWebAppPortAsMinecraftIngress() {
        String json = DaisyMinecraftApp.serverBrowserJson();

        assertTrue(json.contains("\"controlPlaneUrl\": \"https://"));
        assertTrue(json.contains("\"publicEndpointPattern\": null"));
        assertTrue(json.contains("\"minecraftIngressState\": \"not-deployed\""));
        assertTrue(json.contains("\"status\": \"not-connectable\""));
        assertTrue(json.contains("\"connectable\": false"));
        assertTrue(json.contains("\"connectivityBlocker\": \"minecraft-ingress-not-deployed\""));
        assertTrue(json.contains("\"host\": null"));
        assertFalse(json.contains("azurewebsites.net:80"));
        assertFalse(json.contains("daisyquest.azurewebsites.net"));
    }

    @Test
    void serverBrowserPublishesConfiguredMinecraftEndpointOnlyWhenExplicitlySet() {
        String previous = System.getProperty("daisyminecraft.minecraftEndpoint");
        System.setProperty("daisyminecraft.minecraftEndpoint", "minecraft.example.test:25565");
        try {
            String json = DaisyMinecraftApp.serverBrowserJson();

            assertTrue(json.contains("\"publicEndpointPattern\": \"minecraft.example.test:25565\""));
            assertTrue(json.contains("\"minecraftIngressState\": \"ready\""));
            assertTrue(json.contains("\"status\": \"online-endpoint-configured\""));
            assertTrue(json.contains("\"connectable\": true"));
            assertTrue(json.contains("\"host\": \"minecraft.example.test:25565\""));
            assertFalse(json.contains("minecraft-ingress-not-deployed"));
        } finally {
            if (previous == null) {
                System.clearProperty("daisyminecraft.minecraftEndpoint");
            } else {
                System.setProperty("daisyminecraft.minecraftEndpoint", previous);
            }
        }
    }
}
