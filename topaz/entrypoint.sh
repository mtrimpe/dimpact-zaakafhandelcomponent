#!/bin/sh
# Topaz entrypoint: start the server with per-action evaluation Rego policies.
set -e

CONFIG_FILE="${CONFIG_FILE:-/config/config.yaml}"
TOPAZ=/app/topaz
TOPAZD=/app/topazd

# Generate self-signed certs for gRPC/gateway TLS
mkdir -p /certs
$TOPAZ certs generate --certs-dir /certs 2>/dev/null || true

# Start topazd in the background
$TOPAZD run --config-file "$CONFIG_FILE" &
TOPAZ_PID=$!

# Wait for the authorizer to be ready (OPA policies need time to compile)
echo "Waiting for Topaz authorizer to be ready..."
for i in $(seq 1 30); do
  RESULT=$(wget -q -O - \
    --post-data='{"query":"x = data.zac.zaak.allow","input":"{\"subject\":{\"properties\":{\"rollen\":[\"raadpleger\"]}},\"action\":{\"name\":\"lezen\"},\"resource\":{\"properties\":{\"open\":true,\"zaaktype\":\"test\"}}}","identity_context":{"type":"IDENTITY_TYPE_NONE"},"policy_context":{"path":"","decisions":[]}}' \
    --header="Content-Type: application/json" \
    "http://localhost:8383/api/v2/authz/query" 2>&1) || true
  if echo "$RESULT" | grep -q '"result"'; then
    echo "Topaz authorizer is ready"
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "Topaz authorizer did not become ready in time"
    echo "Last response: $RESULT"
    exit 1
  fi
  sleep 1
done

# Wait for the server process
wait $TOPAZ_PID
