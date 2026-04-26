#!/usr/bin/env sh
set -eu

if [ "${DAISY_MINECRAFT_HEALTHCHECK:-enabled}" = "disabled" ]; then
  exit 0
fi

port="${DAISY_MINECRAFT_PORT:-25565}"
case "$port" in
  ''|*[!0-9]*|0) exit 1 ;;
esac

if nc -z -w 2 127.0.0.1 "$port" >/dev/null 2>&1; then
  exit 0
fi

if pgrep -f 'java .*\.jar' >/dev/null 2>&1; then
  exit 0
fi

exit 1
