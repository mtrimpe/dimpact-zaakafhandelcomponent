#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.taak

import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.raadpleger
# AuthZEN aliases: map subject/resource to local names used by policy rules
user := input.subject.properties
taak := input.resource.properties

_all_taak_rechten := {
    "lezen": lezen,
    "wijzigen": wijzigen,
    "toekennen": toekennen,
    "creeren_document": creeren_document,
    "toevoegen_document": toevoegen_document
}

taak_rechten := {"results": [action | some name; _all_taak_rechten[name] == true; action := {"name": name}]}

default zaaktype_allowed := false
zaaktype_allowed if {
    not user.zaaktypen
}
zaaktype_allowed if {
    taak.zaaktype in user.zaaktypen
}

default lezen := false
lezen if {
    raadpleger.rol in user.rollen
    zaaktype_allowed
}

default wijzigen := false
wijzigen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default toekennen := false
toekennen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default creeren_document := false
creeren_document if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    taak.open
}

default toevoegen_document := false
toevoegen_document if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    taak.open
}
