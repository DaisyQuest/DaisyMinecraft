#!/usr/bin/env sh
set -eu

log() {
  printf '%s %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*" >&2
}

fail() {
  log "ERROR: $*"
  exit 1
}

lower() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]'
}

require_eula() {
  value="$(lower "${EULA:-${DAISY_MINECRAFT_EULA_ACCEPTED:-false}}")"
  case "$value" in
    true|1|yes) ;;
    *) fail "Minecraft EULA must be accepted with EULA=TRUE or DAISY_MINECRAFT_EULA_ACCEPTED=true" ;;
  esac
}

require_number() {
  name="$1"
  value="$2"
  case "$value" in
    ''|*[!0-9]*) fail "$name must be a positive integer" ;;
    0) fail "$name must be greater than zero" ;;
  esac
}

safe_jar_name() {
  value="$1"
  base="$(basename "$value")"
  if [ "$value" != "$base" ]; then
    fail "Jar name must not contain path separators: $value"
  fi
  case "$value" in
    *.jar) printf '%s' "$value" ;;
    *) fail "Jar name must end with .jar: $value" ;;
  esac
}

download_custom_jar() {
  url="${DAISY_MINECRAFT_CUSTOM_SERVER_JAR_URL:-}"
  [ -n "$url" ] || return 0

  sha="${DAISY_MINECRAFT_CUSTOM_SERVER_JAR_SHA256:-}"
  [ -n "$sha" ] || fail "DAISY_MINECRAFT_CUSTOM_SERVER_JAR_SHA256 is required when a custom server jar URL is set"

  jar_name="$(safe_jar_name "${DAISY_MINECRAFT_CUSTOM_SERVER_JAR_NAME:-${DAISY_MINECRAFT_SERVER_JAR_NAME:-server.jar}}")"
  tmp="${jar_name}.download"

  if [ -s "$jar_name" ]; then
    current="$(sha256sum "$jar_name" | awk '{print $1}')"
    if [ "$current" = "$sha" ]; then
      log "Custom server jar already present and verified: $jar_name"
      return 0
    fi
  fi

  log "Downloading custom server jar: $url"
  rm -f "$tmp"
  curl --fail --location --show-error --retry 3 --retry-delay 2 --output "$tmp" "$url"
  printf '%s  %s\n' "$sha" "$tmp" | sha256sum -c -
  mv "$tmp" "$jar_name"
  chmod 0644 "$jar_name"
}

copy_bundled_server_jar() {
  [ -z "${DAISY_MINECRAFT_CUSTOM_SERVER_JAR_URL:-}" ] || return 0
  jar_name="$(safe_jar_name "${DAISY_MINECRAFT_SERVER_JAR_NAME:-server.jar}")"
  bundled="/opt/daisyminecraft/server/server.jar"
  if [ -s "$jar_name" ] || [ ! -s "$bundled" ]; then
    return 0
  fi
  log "Copying bundled server jar into data volume"
  cp "$bundled" "$jar_name"
}

require_eula

DATA_DIR="${DAISY_MINECRAFT_DATA_DIR:-/data}"
MEMORY_MB="${DAISY_MINECRAFT_MEMORY_MB:-2048}"
PORT="${DAISY_MINECRAFT_PORT:-25565}"
SERVER_JAR_NAME="$(safe_jar_name "${DAISY_MINECRAFT_CUSTOM_SERVER_JAR_NAME:-${DAISY_MINECRAFT_SERVER_JAR_NAME:-server.jar}}")"

require_number "DAISY_MINECRAFT_MEMORY_MB" "$MEMORY_MB"
require_number "DAISY_MINECRAFT_PORT" "$PORT"

mkdir -p "$DATA_DIR" "$DATA_DIR/plugins" "$DATA_DIR/mods" "$DATA_DIR/config" "$DATA_DIR/logs"
cd "$DATA_DIR"

printf 'eula=true\n' > eula.txt

download_custom_jar
copy_bundled_server_jar

if [ -n "${DAISY_MINECRAFT_CUSTOM_SERVER_COMMAND:-}" ]; then
  log "Starting custom server command"
  exec sh -c "$DAISY_MINECRAFT_CUSTOM_SERVER_COMMAND"
fi

if [ -n "${DAISY_MINECRAFT_SERVER_COMMAND:-}" ]; then
  log "Starting server command"
  exec sh -c "$DAISY_MINECRAFT_SERVER_COMMAND"
fi

if [ ! -s "$SERVER_JAR_NAME" ]; then
  fail "Server jar not found at $DATA_DIR/$SERVER_JAR_NAME. Provide DAISY_MINECRAFT_CUSTOM_SERVER_JAR_URL or bake /opt/daisyminecraft/server/server.jar into the image."
fi

JVM_ARGS="${DAISY_MINECRAFT_JVM_ARGS:-}"
log "Starting Minecraft Java server on port $PORT with ${MEMORY_MB}MiB memory"
if [ -n "$JVM_ARGS" ]; then
  exec sh -c "exec java $JVM_ARGS -Xms${MEMORY_MB}M -Xmx${MEMORY_MB}M -jar \"\$0\" nogui" "$SERVER_JAR_NAME"
fi

exec java "-Xms${MEMORY_MB}M" "-Xmx${MEMORY_MB}M" -jar "$SERVER_JAR_NAME" nogui
