#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Topaz per-decision policy for werklijst permissions.
# Each rule name corresponds to a decision name in the Topaz 'is' API.
# Input comes via resourceContext from the AuthZEN subject/resource.
#
package zac.werklijst

import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.coordinator
import data.net.atos.zac.rol.raadpleger
import data.net.atos.zac.rol.recordmanager

user := input.resource.subject.properties

default inbox := false
inbox if { coordinator.rol in user.rollen }

default ontkoppelde_documenten_verwijderen := false
ontkoppelde_documenten_verwijderen if { recordmanager.rol in user.rollen }

default inbox_productaanvragen_verwijderen := false
inbox_productaanvragen_verwijderen if { recordmanager.rol in user.rollen }

default zaken_taken := false
zaken_taken if { raadpleger.rol in user.rollen }

default zaken_taken_verdelen := false
zaken_taken_verdelen if { coordinator.rol in user.rollen }

default zaken_taken_exporteren := false
zaken_taken_exporteren if { beheerder.rol in user.rollen }
