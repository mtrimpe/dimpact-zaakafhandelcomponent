# Devcontainer for ZAC development

## Overview

This devcontainer provides a sandboxed development environment with:
- JDK 21 (Eclipse Temurin) for Gradle builds
- Docker CLI + Compose plugin (via host Docker socket mount)
- Claude Code CLI
- Network firewall restricting outbound access to allowed domains
- Port forwarding so `localhost` reaches Docker-mapped container ports

## Running integration tests (itests)

```bash
./gradlew itest --info
```

### How itests work in the devcontainer

The itests use TestContainers to start a full docker-compose stack (ZAC, Keycloak, OPA,
OpenZaak, Solr, etc.). TestContainers manages the host's Docker daemon via the mounted
socket (`/var/run/docker.sock`).

**The networking challenge:** Docker containers started via the host socket map their
ports to the Docker host (the OrbStack VM on macOS), not to the devcontainer's localhost.
For example, Keycloak maps `8081:8080`, but `curl http://localhost:8081` from inside the
devcontainer fails because the devcontainer has its own network namespace.

**The solution:** `forward-docker-ports.sh` uses iptables DNAT rules to transparently
forward `localhost:PORT` traffic to the Docker gateway IP. This makes the devcontainer
behave identically to running on the host — no code changes needed.

### Prerequisites

- The devcontainer must start with `--cap-add=NET_ADMIN` and `--cap-add=NET_RAW`
  (configured in `devcontainer.json`)
- The `postStartCommand` runs `forward-docker-ports.sh` automatically
- UTF-8 locale is required (configured via `LANG=C.utf8` in `containerEnv`)
  because `src/itest/resources/fakeTestDocument.pdf` has a Unicode umlaut in the filename

### Selecting an authorization backend

```bash
# Default (OPA)
./gradlew itest --info

# Alternative backends
AUTHORIZATION_SERVICE_BACKEND=topaz ./gradlew itest --info
AUTHORIZATION_SERVICE_BACKEND=cerbos ./gradlew itest --info
AUTHORIZATION_SERVICE_BACKEND=spicedb ./gradlew itest --info
```

## Docker socket access

The host Docker socket is bind-mounted into the devcontainer. `fix-docker-gid.sh` runs
at startup to ensure the `node` user can access it:
- When the socket GID is 0 (common with OrbStack), it `chmod 666`s the socket
- Otherwise, it creates a matching group and adds `node` to it

## Network firewall

`init-firewall.sh` sets up iptables rules that restrict outbound traffic to an allowlist
of domains (npm, Maven Central, GitHub, Docker registries, Gradle, etc.). To add a new
domain, edit `init-firewall.sh` and rebuild the container.

## Rebuilding

```bash
.devcontainer/rebuild.sh
```

Or from VS Code: "Dev Containers: Rebuild Container" command palette action.

## Key files

| File | Purpose |
|------|---------|
| `Dockerfile` | Container image with JDK, Docker CLI, Claude Code |
| `devcontainer.json` | VS Code devcontainer config, env vars, mounts |
| `init-firewall.sh` | Network firewall allowlist |
| `fix-docker-gid.sh` | Docker socket permission fix |
| `forward-docker-ports.sh` | iptables port forwarding for itests |
| `rebuild.sh` | Convenience script to rebuild the container |
