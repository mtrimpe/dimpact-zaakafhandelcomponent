# Design Decisions — Kotlin Policy Backend & Composite AccessService

## 1. Kotlin Policy Backend (replacing OPA)

### Decision: In-process Kotlin AccessService implementation
The Kotlin backend implements `AccessService` directly in Kotlin, running in-process (no external PDP).
This demonstrates that the AuthZEN abstraction works equally well with policies written in the
application's own language — no policy DSL or external engine required.

### Policy structure
Each resource type (zaak, taak, document, notitie, application, werklijst) gets its own policy
object with clear, readable rule methods. The policies use the same AuthZEN model types
(Subject, Resource, Action) as input — the same information model that external PDPs receive.

### Role checking
Roles are checked via `subject.properties["rollen"]`. The Kotlin policies use simple
`hasRole(subject, "behandelaar")` helper functions for clarity.

### Zaaktype authorization
When `subject.properties["zaaktypen"]` is present, the resource's `zaaktype` must be in the
list. When absent, all zaaktypes are allowed (same logic as OPA/Topaz policies).

### Backend name
The backend is named `"kotlin"` in `AUTHORIZATION_SERVICE_BACKEND` configuration.
It's created directly (not via reflection) since it runs in-process.

## 2. Composite AccessService (authzen-client module)

### Decision: `CompositeAccessService` with configurable merge strategy
A new `authzen-composite` module in authzen-client that wraps two `AccessService` instances
and combines their results using a configurable strategy.

### Merge strategies
- `UNANIMOUS` — both must allow (AND)
- `AFFIRMATIVE` — either can allow (OR)
- `OVERRIDE` — secondary overrides primary when secondary returns a non-default decision

### How OVERRIDE works
For `evaluations()`: the secondary service is called. If its response contains any `true`
decisions, those override the primary's decisions. Decisions that are `false` in the secondary
are taken from the primary (the secondary "didn't have an opinion").

This enables: application ships with built-in Kotlin policies, customer deploys an external
PDP that can selectively grant or deny additional permissions.

### Open questions
- Should OVERRIDE support deny-override as well? (secondary can revoke permissions the primary granted)
  Current decision: OVERRIDE means "if secondary allows, it wins; if secondary denies, primary
  wins". This is one-directional: the secondary can only grant additional access, not revoke it.
  To revoke, use UNANIMOUS strategy instead.

- The boolean-only AuthZEN response means we can't distinguish "explicitly denied" from "no opinion".
  A future enhancement could use an optional third state (allow/deny/abstain) for richer composition.

## 3. OPA Removal

### Decision: Delete OPA, keep as optional profile
OPA is replaced by the in-process Kotlin backend as the default. The OPA and opa-tests containers
are moved to a dedicated `opa` Docker Compose profile and no longer start by default.
The OPA REST client classes (`OpaAccessService`, `OpaAdminClient`, `PoliciesDeployer`) are deleted
since Topaz serves the same Rego-based evaluation use case.

### CDI note
`KotlinAccessService` needs `@Vetoed` to prevent CDI from auto-discovering it as a bean (which
would conflict with the `@Produces` method in `AccessServiceProducer`).
