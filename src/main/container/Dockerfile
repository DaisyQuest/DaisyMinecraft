FROM eclipse-temurin:21-jre-jammy

LABEL org.opencontainers.image.title="DaisyMinecraft Server Runtime"
LABEL org.opencontainers.image.description="Managed Minecraft Java server container for DaisyCloud."
LABEL org.opencontainers.image.source="https://github.com/DaisyQuest/DaisyMinecraft"

ENV DAISY_MINECRAFT_DATA_DIR=/data
ENV DAISY_MINECRAFT_PORT=25565
ENV DAISY_MINECRAFT_MEMORY_MB=2048
ENV DAISY_MINECRAFT_SERVER_JAR_NAME=server.jar

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl netcat-openbsd procps tini \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 25565 minecraft \
    && useradd --system --uid 25565 --gid minecraft --home-dir /data --shell /usr/sbin/nologin minecraft \
    && mkdir -p /data /opt/daisyminecraft/server \
    && chown -R minecraft:minecraft /data /opt/daisyminecraft

COPY daisyminecraft-entrypoint.sh /usr/local/bin/daisyminecraft-entrypoint
COPY daisyminecraft-healthcheck.sh /usr/local/bin/daisyminecraft-healthcheck

RUN chmod 0755 /usr/local/bin/daisyminecraft-entrypoint /usr/local/bin/daisyminecraft-healthcheck

USER minecraft:minecraft
WORKDIR /data
VOLUME ["/data"]

EXPOSE 25565/tcp 25565/udp

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD ["/usr/local/bin/daisyminecraft-healthcheck"]

ENTRYPOINT ["/usr/bin/tini", "--", "/usr/local/bin/daisyminecraft-entrypoint"]
