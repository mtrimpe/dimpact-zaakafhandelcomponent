#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Topaz per-decision policy for zaak permissions.
# Each rule name corresponds to a decision name in the Topaz 'is' API.
# Input comes via resourceContext from the AuthZEN subject/resource.
#
package zac.zaak

import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.raadpleger
import data.net.atos.zac.rol.recordmanager

user := input.resource.subject.properties
zaak := input.resource.resource.properties

default zaaktype_allowed := false
zaaktype_allowed if {
    not user.zaaktypen
}
zaaktype_allowed if {
    zaak.zaaktype in user.zaaktypen
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
    zaak.open
}
wijzigen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default toekennen := false
toekennen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
toekennen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default behandelen := false
behandelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default afbreken := false
afbreken if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default heropenen := false
heropenen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default bekijken_zaakdata := false
bekijken_zaakdata if {
    beheerder.rol in user.rollen
}

default wijzigen_doorlooptijd := false
wijzigen_doorlooptijd if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

default verlengen := false
verlengen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
    not zaak.heropend
    not zaak.opgeschort
    not zaak.verlengd
}

default opschorten := false
opschorten if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
    not zaak.heropend
    not zaak.opgeschort
}

default hervatten := false
hervatten if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default creeren_document := false
creeren_document if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

default toevoegen_document := false
toevoegen_document if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
toevoegen_document if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default koppelen := false
koppelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
koppelen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default versturen_email := false
versturen_email if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

default versturen_ontvangstbevestiging := false
versturen_ontvangstbevestiging if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

default toevoegen_initiator_persoon := false
toevoegen_initiator_persoon if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
toevoegen_initiator_persoon if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default toevoegen_initiator_bedrijf := false
toevoegen_initiator_bedrijf if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
toevoegen_initiator_bedrijf if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default verwijderen_initiator := false
verwijderen_initiator if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
verwijderen_initiator if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default toevoegen_betrokkene_persoon := false
toevoegen_betrokkene_persoon if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
toevoegen_betrokkene_persoon if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default toevoegen_betrokkene_bedrijf := false
toevoegen_betrokkene_bedrijf if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
toevoegen_betrokkene_bedrijf if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default verwijderen_betrokkene := false
verwijderen_betrokkene if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
verwijderen_betrokkene if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default toevoegen_bag_object := false
toevoegen_bag_object if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
toevoegen_bag_object if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default starten_taak := false
starten_taak if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

default vastleggen_besluit := false
vastleggen_besluit if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
    not zaak.intake
    zaak.besloten
}

default verlengen_doorlooptijd := false
verlengen_doorlooptijd if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

default wijzigen_locatie := false
wijzigen_locatie if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
wijzigen_locatie if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
