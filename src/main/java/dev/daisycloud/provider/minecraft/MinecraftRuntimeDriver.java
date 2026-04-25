package dev.daisycloud.provider.minecraft;

import java.util.Map;

public interface MinecraftRuntimeDriver {
    void pullImage(String image);

    void ensureVolume(String volumeName, String mountPath);

    void writeStartupFile(String serviceName, String fileName, String content);

    void writeBinaryFile(String serviceName, String fileName, byte[] content);

    void bindPort(String serviceName, String containerPort, String hostPort);

    void startContainer(MinecraftRuntimeContainerSpec spec);

    void stopContainer(String serviceName);

    void releasePort(String serviceName, String containerPort, String hostPort);

    Map<String, String> probeHealth(String serviceName, Map<String, String> expectedChecks);
}
