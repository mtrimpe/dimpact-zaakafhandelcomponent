#!/bin/bash
# Forward localhost ports to the Docker gateway so that tests running inside the
# devcontainer can reach Docker-mapped ports on localhost, just like on the host.
#
# Uses socat for userspace port forwarding (no kernel sysctl changes needed).
set -euo pipefail

GATEWAY=$(ip route show default | awk '{print $3}')
if [ -z "$GATEWAY" ]; then
  echo "Could not detect Docker gateway — skipping port forwarding"
  exit 0
fi

echo "Docker gateway: $GATEWAY"

# Install socat if not available
if ! command -v socat &> /dev/null; then
  echo "Installing socat..."
  apt-get update -qq && apt-get install -y -qq socat > /dev/null 2>&1
fi

# Forward common itest ports from localhost to the Docker gateway.
# These match the port mappings in docker-compose.yaml.
PORTS=(
  8080   # ZAC
  8081   # Keycloak HTTP
  9001   # Keycloak management/health
  9990   # ZAC management
  8001   # OpenZaak
  8010   # Objecten API
  8083   # Office converter
  8983   # Solr
  8006   # PABC API
  18080  # SmartDocuments wiremock
  18081  # KVK wiremock
  18083  # GreenMail API
  18084  # BRP wiremock
  5010   # BRP mock
)

# Kill any existing socat forwarders from a previous run
pkill -f "socat.*TCP-LISTEN.*fork.*TCP:$GATEWAY" 2>/dev/null || true

for PORT in "${PORTS[@]}"; do
  # Only start forwarder if port isn't already in use
  if ! ss -tlnp 2>/dev/null | grep -q ":$PORT "; then
    socat TCP-LISTEN:"$PORT",fork,reuseaddr TCP:"$GATEWAY":"$PORT" &
  fi
done

echo "Port forwarding configured: localhost:PORT → $GATEWAY:PORT for ${#PORTS[@]} ports (socat)"
