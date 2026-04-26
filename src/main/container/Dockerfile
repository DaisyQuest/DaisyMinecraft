FROM eclipse-temurin:21-jre-jammy

LABEL org.opencontainers.image.title="DaisyMinecraft Server Runtime"
LABEL org.opencontainers.image.description="Managed Minecraft Java server container for DaisyCloud."
LABEL org.opencontainers.image.source="https://github.com/DaisyQuest/DaisyMinecraft"

ARG DAISY_MINECRAFT_BUNDLED_SERVER_JAR_URL="https://api.papermc.io/v2/projects/paper/versions/1.21.11/builds/69/downloads/paper-1.21.11-69.jar"
ARG DAISY_MINECRAFT_BUNDLED_SERVER_JAR_SHA256="cf374f2af9d71dfcc75343f37b722a7abcb091c574131b95e3b13c6fc2cb8fae"

ENV DAISY_MINECRAFT_DATA_DIR=/data
ENV DAISY_MINECRAFT_PORT=25565
ENV DAISY_MINECRAFT_MEMORY_MB=2048
ENV DAISY_MINECRAFT_SERVER_JAR_NAME=server.jar
ENV DAISY_MINECRAFT_INSTALL_BUNDLED_PLUGINS=true

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl netcat-openbsd procps tini \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 25565 minecraft \
    && useradd --system --uid 25565 --gid minecraft --home-dir /data --shell /usr/sbin/nologin minecraft \
    && mkdir -p /data /opt/daisyminecraft/server \
    && if [ -n "$DAISY_MINECRAFT_BUNDLED_SERVER_JAR_URL" ]; then \
        curl --fail --location --show-error --retry 3 --retry-delay 2 --output /tmp/daisyminecraft-server.jar "$DAISY_MINECRAFT_BUNDLED_SERVER_JAR_URL"; \
        printf '%s  %s\n' "$DAISY_MINECRAFT_BUNDLED_SERVER_JAR_SHA256" /tmp/daisyminecraft-server.jar | sha256sum -c -; \
        mv /tmp/daisyminecraft-server.jar /opt/daisyminecraft/server/server.jar; \
        chmod 0644 /opt/daisyminecraft/server/server.jar; \
    fi \
    && chown -R minecraft:minecraft /data /opt/daisyminecraft

COPY daisyminecraft-entrypoint.sh /usr/local/bin/daisyminecraft-entrypoint
COPY daisyminecraft-healthcheck.sh /usr/local/bin/daisyminecraft-healthcheck
COPY bundled-plugins/ /opt/daisyminecraft/bundled-plugins/

RUN chmod 0755 /usr/local/bin/daisyminecraft-entrypoint /usr/local/bin/daisyminecraft-healthcheck \
    && find /opt/daisyminecraft/bundled-plugins -type f -name '*.jar' -exec chmod 0644 {} \; \
    && chown -R minecraft:minecraft /opt/daisyminecraft/bundled-plugins

USER minecraft:minecraft
WORKDIR /data
VOLUME ["/data"]

EXPOSE 25565/tcp 25565/udp

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD ["/usr/local/bin/daisyminecraft-healthcheck"]

ENTRYPOINT ["/usr/bin/tini", "--", "/usr/local/bin/daisyminecraft-entrypoint"]
