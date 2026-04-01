#!/bin/bash
# Fix Docker socket permissions so the non-root 'node' user can use Docker.
set -euo pipefail

SOCKET=/var/run/docker.sock
if [ ! -S "$SOCKET" ]; then
  echo "Docker socket not mounted at $SOCKET — skipping"
  exit 0
fi

HOST_GID=$(stat -c '%g' "$SOCKET")
echo "Docker socket GID: $HOST_GID"

if [ "$HOST_GID" = "0" ]; then
  # Socket owned by root (common with OrbStack) — make it world-accessible
  # Group membership via usermod doesn't take effect in already-running shells
  chmod 666 "$SOCKET"
else
  # Create/update a group matching the socket GID and add node to it
  groupmod -g "$HOST_GID" docker-host 2>/dev/null || groupadd -g "$HOST_GID" docker-host 2>/dev/null || true
  usermod -aG docker-host node
fi

echo "Docker socket access configured for node user"
