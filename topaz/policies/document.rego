#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Topaz per-decision policy for document permissions.
# Each rule name corresponds to a decision name in the Topaz 'is' API.
# Input comes via resourceContext from the AuthZEN subject/resource.
#
package zac.document

import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.raadpleger
import data.net.atos.zac.rol.recordmanager

user := input.resource.subject.properties
document := input.resource.resource.properties

default zaaktype_allowed := false
zaaktype_allowed if {
    not document.zaaktype
}
zaaktype_allowed if {
    not user.zaaktypen
}
zaaktype_allowed if {
    document.zaaktype in user.zaaktypen
}

default onvergrendeld_of_vergrendeld_door_user := false
onvergrendeld_of_vergrendeld_door_user if {
    document.vergrendeld == false
}
onvergrendeld_of_vergrendeld_door_user if {
    document.vergrendeld == true
    document.vergrendeld_door == input.resource.subject.id
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
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
wijzigen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default verwijderen := false
verwijderen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    document.vergrendeld == false
}
verwijderen if {
    recordmanager.rol in user.rollen
    document.vergrendeld == false
}

default vergrendelen := false
vergrendelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
}

default ontgrendelen := false
ontgrendelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.vergrendeld_door == input.resource.subject.id
}
ontgrendelen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default ondertekenen := false
ondertekenen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
}

default toevoegen_nieuwe_versie := false
toevoegen_nieuwe_versie if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
toevoegen_nieuwe_versie if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default verplaatsen := false
verplaatsen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
verplaatsen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default ontkoppelen := false
ontkoppelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
ontkoppelen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default downloaden := false
downloaden if {
    raadpleger.rol in user.rollen
    zaaktype_allowed
}

default converteren := false
converteren if {
    behandelaar.rol in user.rollen
    document.definitief == true
    zaaktype_allowed
}
