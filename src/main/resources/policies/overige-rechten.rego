#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.overig

import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.raadpleger
# AuthZEN alias: map subject to local name used by policy rules
user := input.subject.properties

_all_overige_rechten := {
    "starten_zaak": starten_zaak,
    "beheren": beheren,
    "zoeken": zoeken
}

overige_rechten := {"results": [action | some name; _all_overige_rechten[name] == true; action := {"name": name}]}

default starten_zaak := false
starten_zaak if {
    behandelaar.rol in user.rollen
}

default beheren := false
beheren if {
    beheerder.rol in user.rollen
}

default zoeken := false
zoeken if {
    raadpleger.rol in user.rollen
}
