#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.notitie

import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.raadpleger
# AuthZEN alias: map subject to local name used by policy rules
user := input.subject.properties

_all_notitie_rechten := {
    "lezen": lezen,
    "wijzigen": wijzigen
}

notitie_rechten := {"results": [action | some name; _all_notitie_rechten[name] == true; action := {"name": name}]}

default lezen := false
lezen if {
    raadpleger.rol in user.rollen
}

default wijzigen := false
wijzigen if {
    behandelaar.rol in user.rollen
}
