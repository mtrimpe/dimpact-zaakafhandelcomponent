#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Topaz per-decision policy for taak permissions.
# Each rule name corresponds to a decision name in the Topaz 'is' API.
# Input comes via resourceContext from the AuthZEN subject/resource.
#
package zac.taak

import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.raadpleger

user := input.resource.subject.properties
taak := input.resource.resource.properties

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
    taak.open
}

default toekennen := false
toekennen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    taak.open
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
