# Authorization Architecture

This version of ZAC uses an externalized authorization architecture based on the [AuthZEN](https://openid.github.io/authzen/) standard. Authorization decisions are made by a pluggable Policy Decision Point (PDP) selected at runtime via the `AUTHORIZATION_SERVICE_BACKEND` environment variable, with the PDP URL configured via `AUTHZEN_PDP_URL`.

This project includes six PDP backends, each demonstrating a different authorization paradigm. All backends evaluate the same 58 actions across 6 resource types (zaak, document, taak, zaakNotitie, application, werklijst) and pass the same integration test suite.

## Quick Start

```bash
# Run with the default in-process Kotlin backend (no external PDP needed)
./gradlew clean itest

# Run with an external PDP (e.g. Cerbos, OpenFTV)
AUTHORIZATION_SERVICE_BACKEND=cerbos ./gradlew clean itest
AUTHORIZATION_SERVICE_BACKEND=openftv ./gradlew clean itest
```

The itest config automatically maps the PDP name to the correct `AUTHORIZATION_SERVICE_BACKEND` value, docker-compose profile, and `AUTHZEN_PDP_URL`. For PDPs with a native AuthZEN API (Cerbos, OpenFTV), ZAC uses the generic `http` backend with endpoint discovery via `/.well-known/authzen-configuration`.

## Backend Overview

| Backend | Policy Language | Paradigm | Transport | Docker Profile |
|---------|----------------|----------|-----------|----------------|
| [Kotlin](#kotlin-default) | Kotlin | ABAC | In-process | _(none)_ |
| [Topaz](#topaz) | Rego | ABAC | Topaz `is` API | `topaz` |
| [Cerbos](#cerbos) | YAML + CEL | ABAC + derived roles | AuthZEN HTTP | `cerbos` |
| [SpiceDB](#spicedb) | ZED | ReBAC | Permissions API | `spicedb` |
| [AuthzForce](#authzforce) | XACML 3.0 | ABAC (classic) | XACML JSON + MDP | `authzforce` |
| [OpenFTV](#openftv) | Rego | ABAC | AuthZEN HTTP | `openftv` |

---

## Kotlin (default)

**What it is:** In-process Kotlin policy objects. No external PDP needed.

**Backend value:** `kotlin` (default)

**Policy files:**
- `src/main/kotlin/nl/info/zac/policy/service/ZaakPolicy.kt`
- `src/main/kotlin/nl/info/zac/policy/service/DocumentPolicy.kt`
- `src/main/kotlin/nl/info/zac/policy/service/TaakPolicy.kt`
- `src/main/kotlin/nl/info/zac/policy/service/SimplePolices.kt` (notitie, application, werklijst)

**How it works:** Each policy is a Kotlin singleton implementing a `Policy` interface with an `evaluate(action, subject, resourceProperties)` method. The `AccessServiceImpl` routes evaluations to the correct policy object based on resource type.

---

## Topaz

**What it is:** [Topaz](https://www.topaz.sh/) is an open-source authorizer by Aserto built on OPA (Open Policy Agent). It evaluates Rego policies via its native `is` API.

**Backend value:** `topaz`

**Policy files:** `topaz/policies/*.rego` (7 files, one per resource type + shared role definitions)

**Config:** `topaz/config.yaml` — configures the OPA instance, gRPC/HTTP gateway, and local policy bundle.

**How it works:** Policies are organized in Rego packages under `zac.*` (e.g., `package zac.zaak`). Each action is a separate Rego rule. The `authzen-topaz` adapter sends requests to the Topaz `is` API, which evaluates the matching Rego rule and returns the decision.

**Input structure:**
```
input.resource.subject.properties.rollen     # user roles
input.resource.subject.id                    # user ID
input.resource.resource.properties.*         # resource attributes
```

**Gotchas:**
- Resource type names are mapped: `zaakNotitie` -> `notitie`, `application` -> `overig` (matching Rego package names from the original OPA setup).
- The `rollen.rego` file defines shared role constants imported by all other policy files.
- The entrypoint script generates self-signed TLS certs and waits for the authorizer to be ready.

---

## Cerbos

**What it is:** [Cerbos](https://cerbos.dev/) is an open-source authorization engine that uses YAML policy definitions with [CEL](https://cel.dev/) (Common Expression Language) for conditions. Since version 0.51.0, Cerbos has a native AuthZEN API.

**Backend value:** `cerbos`

**Policy files:** `cerbos/policies/*.yaml` (6 resource policies + 1 derived roles file)

**Config:** `cerbos/.cerbos.yaml` — disk-based policy storage with watch-for-changes enabled.

**How it works:** Each resource type has a YAML resource policy. Rules map actions to derived roles with optional CEL conditions. Cerbos's native AuthZEN endpoint handles the translation between AuthZEN requests and its internal policy evaluation.

**Derived roles** (`cerbos/policies/_derived_roles.yaml`): Cerbos maps the `rollen` attribute from the AuthZEN subject to derived roles (behandelaar, raadpleger, coordinator, recordmanager, beheerder). Rules then reference these derived roles instead of raw attributes.

**Example rule:**
```yaml
rules:
  - actions: ["lezen"]
    effect: EFFECT_ALLOW
    derivedRoles: ["raadpleger"]
    condition:
      match:
        expr: V.zaaktype_allowed
```

**Gotchas:**
- Cerbos rejects empty context objects, so ZAC sends `{"source": "zac"}` as a workaround.
- Local variables (`V.*`) are used for reusable conditions like `zaaktype_allowed` and `open`.

---

## SpiceDB

**What it is:** [SpiceDB](https://authzed.com/spicedb) (by AuthZed) is a ReBAC (Relationship-Based Access Control) engine inspired by Google Zanzibar. Authorization is based on relationships between entities rather than attribute rules.

**Backend value:** `spicedb`

**Policy files:**
- `spicedb/schema.zed` — the relationship schema with caveats
- `spicedb/init.sh` — seeds the schema and initial relationships

**How it works:** Instead of policy rules, SpiceDB uses a relationship graph. The `authzen-spicedb` adapter translates AuthZEN requests into SpiceDB CheckPermission calls by dynamically provisioning relationships based on the subject's roles and resource properties.

**Caveats** (conditional relationships): SpiceDB caveats add attribute-based conditions to relationships. For example, `zaak_is_open` is a caveat that checks `open == true` — the relationship `behandelaar_when_open` only holds when the caveat evaluates to true. The adapter passes resource properties as caveat context.

**Gotchas:**
- The adapter has a **preCheck predicate** that denies all requests when `rollen` is empty (PABC zaaktype-level filtering). Without this, SpiceDB would need negative relationships.
- Resource type mapping: `zaakNotitie` -> `zaak_notitie` (SpiceDB doesn't allow camelCase in types).
- The `init.sh` script seeds both the schema and role-resource relationships. If the schema changes, the init script must also be updated.
- Role assignments are defined in `AccessServiceProducer.kt` (not in SpiceDB config), mapping AuthZEN roles to SpiceDB role-resource relationships with caveats.

---

## AuthzForce

**What it is:** [AuthzForce](https://authzforce.ow2.org/) is an open-source implementation of the OASIS XACML 3.0 standard. It represents the classic, standards-based approach to externalized authorization that predates modern policy engines.

**Backend value:** `authzforce`

**Policy files:** `authzforce/zac-policies.xml` (98 KB XACML 3.0 PolicySet)

**Init script:** `authzforce/init.sh` — creates a domain, enables MDP, and uploads the policy.

**How it works:** The `authzen-authzforce` adapter translates AuthZEN requests into XACML JSON requests. AuthzForce evaluates them against the uploaded XACML PolicySet and returns Permit/Deny decisions.

**MDP (Multiple Decision Profile):** AuthzForce's MDP support allows batch evaluation of multiple actions in a single request — this maps to the AuthZEN Evaluations endpoint. The init script enables the MDP request preprocessor.

**Gotchas:**
- The XACML policy file is large (98 KB) because XACML is inherently verbose. Each rule requires explicit attribute category/ID declarations, match functions, and combining algorithm specifications.
- The init script must run before the PDP is usable — it creates the AuthzForce domain, enables MDP, and uploads the policy. The domain ID is auto-discovered by the adapter.
- AuthzForce uses its own REST API (not AuthZEN). The `authzen-authzforce` adapter handles the translation between AuthZEN and XACML JSON.

---

## OpenFTV

**What it is:** [OpenFTV](https://gitlab.com/digilab.overheid.nl/ecosystem/ftv/open-ftv) is the open-source reference implementation of the Dutch government's Federatieve Toegangsverlening (FTV) standard for federated access management. It wraps multiple policy engines (Cedar, OPA, Cerbos, OpenFGA) behind a native AuthZEN API.

**Backend value:** `openftv`

**Policy files:** `openftv/policies/authz.rego` (single consolidated Rego file)

**How it works:** The PDP loads Rego policies from a local directory (`PDP_POLICIES_STORE=/policies`) and evaluates them via its embedded OPA engine. The AuthZEN endpoint is at `/authzen/v1/evaluation` (discovered automatically via `/.well-known/authzen-configuration`).

**Input structure** (different from Topaz — uses `principal` not `subject`):
```
input.principal.attributes.rollen      # user roles
input.principal.id                     # user ID (used for document lock checks)
input.resource.type                    # resource type
input.resource.attributes.*            # resource attributes
input.action.id                        # action name
```

**What makes OpenFTV special:**
- **Authorization Decision Log (ADL):** OpenFTV logs every authorization decision with full context (subject, action, resource, decision, timing). This provides an audit trail that can be sent to stdout, PostgreSQL, or OpenTelemetry (`PDP_ADL_TYPE`).
- **Register Toegangsbeleid:** OpenFTV is designed to support the upcoming [Register Toegangsbeleid](https://vng-realisatie.github.io/ftv/), a Dutch government initiative for a centralized access policy register. This register enables organizations to publish, discover, and share access policies in a federated manner — moving beyond per-application policy management to organization-wide policy governance.
- **Multi-engine support:** While we use OPA here, OpenFTV also supports Cedar, Cerbos, and OpenFGA as policy engines, selectable via `PDP_POLICIES_LANGUAGE`.

**Gotchas:**
- The `authz.rego` policy includes a permissive rule for `service` resource types (`allow if { res_type == "service" }`). This is needed because OpenFTV uses its own OPA engine for internal authorization (e.g., health checks, bundle retrieval) and the loaded policies must permit those internal requests.
- OpenFTV serves plain HTTP when `PDP_TLS_CERT`/`PDP_TLS_KEY` are not set, despite using port 8443.
- The OPA input uses `principal` (not `subject`) as the key for the subject entity, unlike Topaz which uses `input.resource.subject`.

---

## Wiring Architecture

All backends are wired through `AccessServiceProducer.kt`, a CDI producer that selects the `AccessService` implementation based on `AUTHORIZATION_SERVICE_BACKEND`:

```
AUTHORIZATION_SERVICE_BACKEND=kotlin     -> AccessServiceImpl (in-process)
AUTHORIZATION_SERVICE_BACKEND=topaz      -> TopazAccessService (authzen-topaz)
AUTHORIZATION_SERVICE_BACKEND=spicedb    -> SpiceDbAccessService (authzen-spicedb)
AUTHORIZATION_SERVICE_BACKEND=authzforce -> AuthzForceAccessService (authzen-authzforce)
AUTHORIZATION_SERVICE_BACKEND=http       -> AuthZenHttpAccessService (authzen-http)
AUTHORIZATION_SERVICE_BACKEND=grpc       -> AuthZenGrpcAccessService (authzen-grpc)
```

PDP-specific adapters (topaz, spicedb, authzforce) are loaded as `runtimeOnly` dependencies via reflection, so ZAC can be deployed without unnecessary backend libraries on the classpath.

PDPs with native AuthZEN APIs (Cerbos, OpenFTV) use the generic `http` backend. The `AuthZenHttpAccessService` discovers endpoint paths via `/.well-known/authzen-configuration` and falls back to `/access/v1/*` defaults, so it works with any AuthZEN-compliant PDP regardless of path conventions.
