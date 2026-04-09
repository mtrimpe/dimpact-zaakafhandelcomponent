#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# ZAC authorization policies for OpenFTV OPA engine.
#
# Input structure (AuthZEN via EntityToAttribute):
#   input.subject.type         - e.g. "user"
#   input.principal.id           - user identifier
#   input.principal.attributes   - {rollen: [...], zaaktypen: [...]}
#   input.action.type          - "name"
#   input.action.id            - action name, e.g. "lezen"
#   input.resource.type        - e.g. "zaak", "document", "taak"
#   input.resource.id          - resource identifier
#   input.resource.attributes  - resource-specific properties
#   input.context              - additional context
#
package authz

default allow := false

# Allow OpenFTV internal service requests (bundle retrieval, health checks, etc.)
allow if {
    res_type == "service"
}

# ─── Aliases ────────────────────────────────────────────────────────────────

user := input.principal.attributes
action := input.action.id
res_type := input.resource.type
res := input.resource.attributes

# ─── Shared helpers ─────────────────────────────────────────────────────────

has_role(role) if {
    role in user.rollen
}

zaaktype_allowed if {
    not user.zaaktypen
}
zaaktype_allowed if {
    not res.zaaktype
}
zaaktype_allowed if {
    res.zaaktype in user.zaaktypen
}

doc_unlocked_or_own_lock if {
    res.vergrendeld == false
}
doc_unlocked_or_own_lock if {
    res.vergrendeld == true
    res.vergrendeld_door == input.principal.id
}

# ═══════════════════════════════════════════════════════════════════════════
# ZAAK
# ═══════════════════════════════════════════════════════════════════════════

allow if {
    res_type == "zaak"
    action == "lezen"
    has_role("raadpleger")
    zaaktype_allowed
}

# wijzigen, wijzigen_locatie: behandelaar + open, or recordmanager
allow if {
    res_type == "zaak"
    action in {"wijzigen", "wijzigen_locatie"}
    has_role("behandelaar")
    zaaktype_allowed
    res.open
}
allow if {
    res_type == "zaak"
    action in {"wijzigen", "wijzigen_locatie"}
    has_role("recordmanager")
    zaaktype_allowed
}

# toekennen: behandelaar + open, or recordmanager
allow if {
    res_type == "zaak"
    action == "toekennen"
    has_role("behandelaar")
    zaaktype_allowed
    res.open
}
allow if {
    res_type == "zaak"
    action == "toekennen"
    has_role("recordmanager")
    zaaktype_allowed
}

allow if {
    res_type == "zaak"
    action == "behandelen"
    has_role("behandelaar")
    zaaktype_allowed
}

allow if {
    res_type == "zaak"
    action == "afbreken"
    has_role("behandelaar")
    zaaktype_allowed
}

allow if {
    res_type == "zaak"
    action == "heropenen"
    has_role("recordmanager")
    zaaktype_allowed
}

allow if {
    res_type == "zaak"
    action == "bekijken_zaakdata"
    has_role("beheerder")
}

allow if {
    res_type == "zaak"
    action == "wijzigen_doorlooptijd"
    has_role("behandelaar")
    zaaktype_allowed
    res.open
}

# verlengen: open + not heropend + not opgeschort + not verlengd
allow if {
    res_type == "zaak"
    action == "verlengen"
    has_role("behandelaar")
    zaaktype_allowed
    res.open
    not res.heropend
    not res.opgeschort
    not res.verlengd
}

# opschorten: open + not heropend + not opgeschort
allow if {
    res_type == "zaak"
    action == "opschorten"
    has_role("behandelaar")
    zaaktype_allowed
    res.open
    not res.heropend
    not res.opgeschort
}

allow if {
    res_type == "zaak"
    action == "hervatten"
    has_role("behandelaar")
    zaaktype_allowed
}

# behandelaar + open actions
allow if {
    res_type == "zaak"
    action in {
        "creeren_document", "versturen_email", "versturen_ontvangstbevestiging",
        "starten_taak", "verlengen_doorlooptijd"
    }
    has_role("behandelaar")
    zaaktype_allowed
    res.open
}

# toevoegen_document, koppelen: behandelaar + open, or recordmanager
allow if {
    res_type == "zaak"
    action in {"toevoegen_document", "koppelen"}
    has_role("behandelaar")
    zaaktype_allowed
    res.open
}
allow if {
    res_type == "zaak"
    action in {"toevoegen_document", "koppelen"}
    has_role("recordmanager")
    zaaktype_allowed
}

# initiator/betrokkene/bag: behandelaar + open, or recordmanager
allow if {
    res_type == "zaak"
    action in {
        "toevoegen_initiator_persoon", "toevoegen_initiator_bedrijf", "verwijderen_initiator",
        "toevoegen_betrokkene_persoon", "toevoegen_betrokkene_bedrijf", "verwijderen_betrokkene",
        "toevoegen_bag_object"
    }
    has_role("behandelaar")
    zaaktype_allowed
    res.open
}
allow if {
    res_type == "zaak"
    action in {
        "toevoegen_initiator_persoon", "toevoegen_initiator_bedrijf", "verwijderen_initiator",
        "toevoegen_betrokkene_persoon", "toevoegen_betrokkene_bedrijf", "verwijderen_betrokkene",
        "toevoegen_bag_object"
    }
    has_role("recordmanager")
    zaaktype_allowed
}

# vastleggen_besluit: open + not intake + besloten
allow if {
    res_type == "zaak"
    action == "vastleggen_besluit"
    has_role("behandelaar")
    zaaktype_allowed
    res.open
    not res.intake
    res.besloten
}

# ═══════════════════════════════════════════════════════════════════════════
# DOCUMENT
# ═══════════════════════════════════════════════════════════════════════════

allow if {
    res_type == "document"
    action == "lezen"
    has_role("raadpleger")
    zaaktype_allowed
}

allow if {
    res_type == "document"
    action == "wijzigen"
    has_role("behandelaar")
    zaaktype_allowed
    res.zaak_open == true
    res.definitief == false
    doc_unlocked_or_own_lock
}
allow if {
    res_type == "document"
    action == "wijzigen"
    has_role("recordmanager")
    zaaktype_allowed
}

allow if {
    res_type == "document"
    action == "verwijderen"
    has_role("behandelaar")
    zaaktype_allowed
    res.zaak_open == true
    res.definitief == false
    res.vergrendeld == false
}
allow if {
    res_type == "document"
    action == "verwijderen"
    has_role("recordmanager")
    res.vergrendeld == false
}

allow if {
    res_type == "document"
    action == "vergrendelen"
    has_role("behandelaar")
    zaaktype_allowed
    res.zaak_open == true
}

allow if {
    res_type == "document"
    action == "ontgrendelen"
    has_role("behandelaar")
    zaaktype_allowed
    res.vergrendeld_door == input.principal.id
}
allow if {
    res_type == "document"
    action == "ontgrendelen"
    has_role("recordmanager")
    zaaktype_allowed
}

allow if {
    res_type == "document"
    action == "ondertekenen"
    has_role("behandelaar")
    zaaktype_allowed
    res.zaak_open == true
    doc_unlocked_or_own_lock
}

# toevoegen_nieuwe_versie, verplaatsen, ontkoppelen: same pattern
allow if {
    res_type == "document"
    action in {"toevoegen_nieuwe_versie", "verplaatsen", "ontkoppelen"}
    has_role("behandelaar")
    zaaktype_allowed
    res.zaak_open == true
    res.definitief == false
    doc_unlocked_or_own_lock
}
allow if {
    res_type == "document"
    action in {"toevoegen_nieuwe_versie", "verplaatsen", "ontkoppelen"}
    has_role("recordmanager")
    zaaktype_allowed
}

allow if {
    res_type == "document"
    action == "downloaden"
    has_role("raadpleger")
    zaaktype_allowed
}

allow if {
    res_type == "document"
    action == "converteren"
    has_role("behandelaar")
    res.definitief == true
    zaaktype_allowed
}

# ═══════════════════════════════════════════════════════════════════════════
# TAAK
# ═══════════════════════════════════════════════════════════════════════════

allow if {
    res_type == "taak"
    action == "lezen"
    has_role("raadpleger")
    zaaktype_allowed
}

allow if {
    res_type == "taak"
    action in {"wijzigen", "toekennen"}
    has_role("behandelaar")
    zaaktype_allowed
}

allow if {
    res_type == "taak"
    action in {"creeren_document", "toevoegen_document"}
    has_role("behandelaar")
    zaaktype_allowed
    res.open
}

# ═══════════════════════════════════════════════════════════════════════════
# ZAAKNOTITIE
# ═══════════════════════════════════════════════════════════════════════════

allow if {
    res_type == "zaakNotitie"
    action == "lezen"
    has_role("raadpleger")
}

allow if {
    res_type == "zaakNotitie"
    action == "wijzigen"
    has_role("behandelaar")
}

# ═══════════════════════════════════════════════════════════════════════════
# APPLICATION
# ═══════════════════════════════════════════════════════════════════════════

allow if {
    res_type == "application"
    action == "starten_zaak"
    has_role("behandelaar")
}

allow if {
    res_type == "application"
    action == "beheren"
    has_role("beheerder")
}

allow if {
    res_type == "application"
    action == "zaaktype_inzien"
    has_role("behandelaar")
}
allow if {
    res_type == "application"
    action == "zaaktype_inzien"
    has_role("beheerder")
}

allow if {
    res_type == "application"
    action == "zoeken"
    has_role("raadpleger")
}

# ═══════════════════════════════════════════════════════════════════════════
# WERKLIJST
# ═══════════════════════════════════════════════════════════════════════════

allow if {
    res_type == "werklijst"
    action == "inbox"
    has_role("coordinator")
}

allow if {
    res_type == "werklijst"
    action == "ontkoppelde_documenten_verwijderen"
    has_role("recordmanager")
}

allow if {
    res_type == "werklijst"
    action == "inbox_productaanvragen_verwijderen"
    has_role("recordmanager")
}

allow if {
    res_type == "werklijst"
    action == "zaken_taken"
    has_role("raadpleger")
}

allow if {
    res_type == "werklijst"
    action == "zaken_taken_verdelen"
    has_role("coordinator")
}

allow if {
    res_type == "werklijst"
    action == "zaken_taken_exporteren"
    has_role("beheerder")
}
