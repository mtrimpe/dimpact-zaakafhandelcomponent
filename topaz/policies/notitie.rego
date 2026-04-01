#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Topaz per-decision policy for notitie permissions.
# Each rule name corresponds to a decision name in the Topaz 'is' API.
# Input comes via resourceContext from the AuthZEN subject/resource.
#
package zac.notitie

import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.raadpleger

user := input.resource.subject.properties

default lezen := false
lezen if { raadpleger.rol in user.rollen }

default wijzigen := false
wijzigen if { behandelaar.rol in user.rollen }
