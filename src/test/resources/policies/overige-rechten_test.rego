#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
package net.atos.zac.overig

import rego.v1

import data.net.atos.zac.overig.starten_zaak
import data.net.atos.zac.overig.beheren
import data.net.atos.zac.overig.zoeken

##############
# starten_zaak
##############
test_starten_zaak_with_behandelaar_role if {
    starten_zaak with input.subject.properties.rollen as [ "behandelaar" ]
}

test_starten_zaak_with_wrong_role_fails if {
    not starten_zaak with input.subject.properties.rollen as [ "fakeRole" ]
}

#########
# beheren
#########
test_beheren_with_beheerder_role if {
    beheren with input.subject.properties.rollen as [ "beheerder" ]
}

test_beheren_with_wrong_role if {
    not beheren with input.subject.properties.rollen as [ "fakeRole" ]
}

########
# zoeken
########
test_zoeken_with_behandelaar_role if {
    zoeken with input.subject.properties.rollen as [ "raadpleger" ]
}

test_zoeken_with_wrong_role if {
    not zoeken with input.subject.properties.rollen as [ "fakeRole" ]
}
