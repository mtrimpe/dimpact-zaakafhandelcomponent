#!/bin/bash
# Run Claude Code in the ZAC sandbox container with full permissions.
# Mounts the project at the same path as the host so Claude Code
# finds existing sessions and memory.
#
# Usage: .devcontainer/run.sh [--clean] [--rebuild]
#   --clean    Remove cached image to force a full rebuild
#   --rebuild  Rebuild image even if it already exists
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
IMAGE_NAME="zac-claude-sandbox"
CLAUDE_DIR="${HOME}/.claude"
FORCE_BUILD=false

# Parse flags
for arg in "$@"; do
  case "$arg" in
    --clean)
      echo "Removing cached images..."
      docker rmi -f "$IMAGE_NAME" 2>/dev/null || true
      docker images --format '{{.Repository}}:{{.Tag}}' | grep "vsc-$(basename "$PROJECT_DIR")" | xargs -r docker rmi -f 2>/dev/null || true
      FORCE_BUILD=true
      ;;
    --rebuild)
      FORCE_BUILD=true
      ;;
  esac
done

# Build if needed or forced
if $FORCE_BUILD || ! docker image inspect "$IMAGE_NAME" >/dev/null 2>&1; then
  echo "Building $IMAGE_NAME..."
  docker build -t "$IMAGE_NAME" "$SCRIPT_DIR"
fi

# Extract OAuth credentials from macOS Keychain into .credentials.json
# (Claude Code on macOS stores tokens in Keychain only, not on disk.
#  The Linux container has no Keychain, so we need the file.)
CREDS_FILE="${CLAUDE_DIR}/.credentials.json"
KEYCHAIN_CREDS=$(security find-generic-password -a "$USER" -w -s "Claude Code-credentials" 2>/dev/null || true)
if [ -n "$KEYCHAIN_CREDS" ]; then
  echo "$KEYCHAIN_CREDS" > "$CREDS_FILE"
  chmod 600 "$CREDS_FILE"
  echo "Wrote credentials from Keychain to $CREDS_FILE"
else
  echo "Warning: Could not read credentials from Keychain. Run 'claude auth login' on the host first."
fi

# Mount project at same host path so Claude Code shares sessions/memory with host
# Drops into a shell — run `claude --dangerously-skip-permissions` to start Claude
exec docker run -it --rm \
  --cap-add=NET_ADMIN --cap-add=NET_RAW \
  -v "${PROJECT_DIR}:${PROJECT_DIR}" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "${CLAUDE_DIR}:/home/node/.claude" \
  -v claude-zac-history:/commandhistory \
  -v "$(dirname "${PROJECT_DIR}")/authzen-client:/authzen-client" \
  -e CLAUDE_CONFIG_DIR="/home/node/.claude" \
  -w "${PROJECT_DIR}" \
  "$IMAGE_NAME" \
  bash -c 'sudo /usr/local/bin/fix-docker-gid.sh && sudo /usr/local/bin/forward-docker-ports.sh && export HISTFILE=/commandhistory/.bash_history && echo "claude --dangerously-skip-permissions" >> $HISTFILE && echo "claude --dangerously-skip-permissions --continue" >> $HISTFILE && echo "" && echo "=== ZAC Claude Sandbox ===" && echo "  Press ↑ for suggested commands" && echo "" && exec bash'
