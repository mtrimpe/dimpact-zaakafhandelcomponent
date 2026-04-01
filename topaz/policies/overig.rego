#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Topaz per-decision policy for application ('overige') permissions.
# Each rule name corresponds to a decision name in the Topaz 'is' API.
# Input comes via resourceContext from the AuthZEN subject/resource.
#
package zac.overig

import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.raadpleger

user := input.resource.subject.properties

default starten_zaak := false
starten_zaak if { behandelaar.rol in user.rollen }

default beheren := false
beheren if { beheerder.rol in user.rollen }

default zoeken := false
zoeken if { raadpleger.rol in user.rollen }
