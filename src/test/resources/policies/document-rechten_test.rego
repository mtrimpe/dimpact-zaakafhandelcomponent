#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
package net.atos.zac.document

import rego.v1

import data.net.atos.zac.document.zaaktype_allowed
import data.net.atos.zac.document.onvergrendeld_of_vergrendeld_door_user
import data.net.atos.zac.document.lezen
import data.net.atos.zac.document.wijzigen
import data.net.atos.zac.document.verwijderen
import data.net.atos.zac.document.vergrendelen
import data.net.atos.zac.document.ontgrendelen
import data.net.atos.zac.document.ondertekenen
import data.net.atos.zac.document.toevoegen_nieuwe_versie
import data.net.atos.zac.document.verplaatsen
import data.net.atos.zac.document.ontkoppelen
import data.net.atos.zac.document.downloaden

##################
# zaaktype_allowed
##################
test_zaaktype_allowed_with_mising_doc_zaaktype if {
    zaaktype_allowed with input.resource.properties.key as "value"
    zaaktype_allowed with input.resource.properties.zaaktype as null
    zaaktype_allowed with input.resource.properties.zaaktype as ""
}

test_zaaktype_allowed_with_mising_user_zaaktypen_key if {
    zaaktype_allowed with input.subject.properties.key as "value"
    zaaktype_allowed with input.subject.properties.zaaktypen as null
    zaaktype_allowed with input.subject.properties.zaaktypen as ""
}

test_zaaktype_allowed_with_user_zaaktypen_and_missing_doc_zaaktype if {
    zaaktype_allowed with input.subject.properties.zaaktypen as ["type"]
}

test_zaaktype_allowed_with_doc_zaaktype_and_missing_user_zaaktypen if {
    zaaktype_allowed with input.resource.properties.zaaktype as ["type"]
}

test_zaaktype_allowed_with_doc_zaaktype_in_user_zaaktypen if {
    zaaktype_allowed
        with input.resource.properties.zaaktype as "type"
        with input.subject.properties.zaaktypen as ["firstType", "type"]
}

test_zaaktype_allowed_with_doc_zaaktype_not_in_user_zaaktypen_fails if {
    not zaaktype_allowed
        with input.resource.properties.zaaktype as "type"
        with input.subject.properties.zaaktypen as ["unknown type"]
}

########################################
# onvergrendeld_of_vergrendeld_door_user
########################################
test_onvergrendeld_of_vergrendeld_door_user_vergrendeld_false if {
    onvergrendeld_of_vergrendeld_door_user with input.resource.properties.vergrendeld as false
}

test_onvergrendeld_of_vergrendeld_door_user_missing_vergrendeld_fails if {
    not onvergrendeld_of_vergrendeld_door_user with input.resource.properties.key as "value"
}

test_onvergrendeld_of_vergrendeld_door_user_vergrendeld_true_fails if {
    not onvergrendeld_of_vergrendeld_door_user with input.resource.properties.vergrendeld as true
}

test_onvergrendeld_of_vergrendeld_door_user if {
    onvergrendeld_of_vergrendeld_door_user
        with input.resource.properties.vergrendeld as true
        with input.resource.properties.vergrendeld_door as "1"
        with input.subject.id as "1"
}

test_onvergrendeld_of_vergrendeld_door_user_vergrendeld_true_and_vergrendeld_door_not_eq_user_id_fails if {
    not onvergrendeld_of_vergrendeld_door_user
        with input.resource.properties.vergrendeld as true
        with input.resource.properties.vergrendeld_door as "1"
        with input.subject.id as "2"
}

test_onvergrendeld_of_vergrendeld_door_user_vergrendeld_true_and_vergrendeld_door_missing_fails if {
    not onvergrendeld_of_vergrendeld_door_user
        with input.resource.properties.vergrendeld as true
        with input.subject.id as "2"
    not onvergrendeld_of_vergrendeld_door_user
        with input.resource.properties.vergrendeld as true
        with input.resource.properties.vergrendeld_door as null
        with input.subject.id as "2"
    not onvergrendeld_of_vergrendeld_door_user
        with input.resource.properties.vergrendeld as true
        with input.resource.properties.vergrendeld_door as ""
        with input.subject.id as "2"
}

#######
# lezen
#######
test_lezen if {
    lezen
        with input.subject.properties.rollen as ["raadpleger"]
        with input.resource.properties.zaaktype as "type"
        with input.subject.properties.zaaktypen as ["firstType", "type"]
}

test_lezen_missing_role_fails if {
    not lezen
        with input.resource.properties.zaaktype as "type"
        with input.subject.properties.zaaktypen as ["firstType", "type"]
}

test_lezen_wrong_role_fails if {
    not lezen
        with input.subject.properties.rollen as ["fakeRole"]
        with input.resource.properties.zaaktype as "type"
        with input.subject.properties.zaaktypen as ["firstType", "type"]
}

test_lezen_zaaktype_not_allowed_fails if {
    not lezen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaaktype as "unknown"
        with input.subject.properties.zaaktypen as ["firstType", "type"]
}

test_lezen_wrong_role_zaaktype_not_allowed_fails if {
    not lezen
        with input.subject.properties.rollen as ["fakeRole"]
        with input.resource.properties.zaaktype as "unknown"
        with input.subject.properties.zaaktypen as ["firstType", "type"]
}

##########
# wijzigen
##########
test_wijzigen_behandelaar_unlocked if {
    wijzigen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.onvergrendeld_of_vergrendeld_door_user as true
        with input.resource.properties.vergrendeld as false
}

test_wijzigen_behandelaar_locked_by_user if {
    wijzigen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as true
        with input.resource.properties.vergrendeld_door as "1"
        with input.subject.id as "1"
}

test_wijzigen_behandelaar_missing_role_fails if {
    not wijzigen
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_wijzigen_wrong_role_fails if {
    not wijzigen
        with input.subject.properties.rollen as ["fakeRole"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_wijzigen_zaaktype_not_allowed_fails if {
    not wijzigen
        with input.subject.properties.rollen as ["fakeRole"]
        with input.resource.properties.zaaktype as "unknown"
        with input.subject.properties.zaaktypen as ["firstType", "type"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_wijzigen_behandelaar_zaak_closed_fails if {
    not wijzigen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as false
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_wijzigen_behandelaar_definitief_fails if {
    not wijzigen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as true
        with input.resource.properties.vergrendeld as false
}

test_wijzigen_behandelaar_not_onvergrendeld_of_vergrendeld_door_user_fails if {
    not wijzigen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as true
        with input.resource.properties.vergrendeld_door as "2"
        with input.subject.id as "1"
}

test_wijzigen_recordmanager if {
    wijzigen
        with input.subject.properties.rollen as ["recordmanager"]
}

test_wijzigen_recordmanager_zaaktype_not_allowed_fails if {
    not wijzigen
        with input.subject.properties.rollen as ["recordmanager"]
        with input.resource.properties.zaaktype as "type"
        with input.subject.properties.zaaktypen as ["unknown type"]
}

#############
# verwijderen
#############
test_verwijderen_behandelaar if {
    verwijderen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_verwijderen_behandelaar_locked_by_this_user_fails if {
    not verwijderen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as true
        with input.resource.properties.vergrendeld_door as "1"
        with input.subject.id as "1"
}

test_verwijderen_behandelaar_zaak_closed_fails if {
    not verwijderen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as false
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_verwijderen_behandelaar_definitief_fails if {
    not verwijderen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as true
        with input.resource.properties.vergrendeld as false
}

test_verwijderen_behandelaar_locked_by_other_user_fails if {
    not verwijderen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as true
        with input.resource.properties.vergrendeld as true
        with input.resource.properties.vergrendeld_door as "2"
        with input.subject.id as "1"
}

test_verwijderen_behandelaar_missing_role_fails if {
    not verwijderen
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as true
        with input.resource.properties.vergrendeld as false
}

test_verwijderen_recordmanager if {
    verwijderen
        with input.subject.properties.rollen as ["recordmanager"]
        with input.resource.properties.vergrendeld as false
}

test_verwijderen_recordmanager_locked_fails if {
    not verwijderen
        with input.subject.properties.rollen as ["recordmanager"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.vergrendeld as true
}

test_verwijderen_recordmanager_missing_role_fails if {
    not verwijderen
        with input.resource.properties.zaak_open as true
}

test_verwijderen_wrong_role_fails if {
    not verwijderen
        with input.subject.properties.rollen as ["fakeRole"]
        with input.resource.properties.zaak_open as false
}

##############
# vergrendelen
##############
test_vergrendelen if {
    vergrendelen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
}

test_vergrendelen_wrong_role_fails if {
    not vergrendelen
        with input.subject.properties.rollen as ["fakeRole"]
        with input.resource.properties.zaak_open as true
}

test_vergrendelen_zaak_closed_fails if {
    not vergrendelen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as false
}

test_vergrendelen_role_missing_fails if {
    not vergrendelen
        with input.resource.properties.zaak_open as false
}

##############
# ontgrendelen
##############
test_ontgrendelen_behandelaar if {
    ontgrendelen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.subject.id as "1"
        with input.resource.properties.vergrendeld_door as "1"
}

test_ontgrendelen_behandelaar_zaak_closed if {
    ontgrendelen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as false
        with input.subject.id as "1"
        with input.resource.properties.vergrendeld_door as "1"
}

test_ontgrendelen_behandelaar_locked_by_other_user_fails if {
    not ontgrendelen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as false
        with input.subject.id as "1"
        with input.resource.properties.vergrendeld_door as "2"
}

test_ontgrendelen_recordmanager if {
    ontgrendelen
        with input.subject.properties.rollen as ["recordmanager"]
        with input.resource.properties.zaak_open as true
}

test_ontgrendelen_recordmanager_zaak_closed if {
    ontgrendelen
        with input.subject.properties.rollen as ["recordmanager"]
        with input.resource.properties.zaak_open as false
}

test_ontgrendelen_wrong_role_fails if {
    not ontgrendelen
        with input.subject.properties.rollen as ["fakeRole"]
}

test_ontgrendelen_missing_role_fails if {
    not ontgrendelen
        with input.resource.properties.zaak_open as true
}

##############
# ondertekenen
##############
test_ondertekenen_behandelaar if  {
    ondertekenen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.vergrendeld as false
}

test_ondertekenen_behandelaar_locked_by_this_user if  {
    ondertekenen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.vergrendeld as true
        with input.subject.id as "1"
        with input.resource.properties.vergrendeld_door as "1"
}

test_ondertekenen_behandelaar_zaak_closed_fails if  {
    not ondertekenen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as false
}

test_ondertekenen_behandelaar_locked_by_another_user_fails if  {
    not ondertekenen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.vergrendeld as true
        with input.subject.id as "1"
        with input.resource.properties.vergrendeld_door as "2"
}

test_ondertekenen_wrong_role_fails if {
    not ondertekenen
        with input.subject.properties.rollen as ["fakeRole"]
}

test_ondertekenen_missing_role_fails if {
    not ondertekenen
        with input.resource.properties.zaak_open as true
}

#########################
# toevoegen_nieuwe_versie
#########################
test_toevoegen_nieuwe_versie_behandelaar if {
    toevoegen_nieuwe_versie
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_toevoegen_nieuwe_versie_behandelaar if {
    toevoegen_nieuwe_versie
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_toevoegen_nieuwe_versie_behandelaar_locked_by_current_user if {
    toevoegen_nieuwe_versie
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as true
        with input.subject.id as "1"
        with input.resource.properties.vergrendeld_door as "1"
}

test_toevoegen_nieuwe_versie_behandelaar_zaak_closed_fails if {
    not toevoegen_nieuwe_versie
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as false
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_toevoegen_nieuwe_versie_behandelaar_definitief_fails if {
    not toevoegen_nieuwe_versie
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as true
        with input.resource.properties.vergrendeld as false
}

test_toevoegen_nieuwe_versie_behandelaar_locked_by_other_user_fails if {
    not toevoegen_nieuwe_versie
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as true
        with input.subject.id as "1"
        with input.resource.properties.vergrendeld_door as "2"
}

test_toevoegen_nieuwe_versie_recordmanager if {
    toevoegen_nieuwe_versie
        with input.subject.properties.rollen as ["recordmanager"]
        with input.resource.properties.ondertekend as false
}

test_toevoegen_nieuwe_versie_recordmanager_ondertekend if {
    toevoegen_nieuwe_versie
        with input.subject.properties.rollen as ["recordmanager"]
        with input.resource.properties.ondertekend as true
}

test_toevoegen_nieuwe_versie_wrong_role_fails if {
    not toevoegen_nieuwe_versie
        with input.subject.properties.rollen as ["fakeRole"]
}

#############
# verplaatsen
#############
test_verplaatsen_behandelaar if {
    verplaatsen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_verplaatsen_behandelaar_locked_same_user if {
    verplaatsen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as true
        with input.subject.id as "1"
        with input.resource.properties.vergrendeld_door as "1"
}

test_verplaatsen_behandelaar_zaak_closed_fails if {
    not verplaatsen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as false
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_verplaatsen_behandelaar_definitief_fails if {
    not verplaatsen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as true
        with input.resource.properties.vergrendeld as false
}

test_verplaatsen_behandelaar_locked_other_user_fails if {
    not verplaatsen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as true
        with input.resource.properties.vergrendeld as true
        with input.subject.id as "1"
        with input.resource.properties.vergrendeld_door as "2"
}

test_verplaatsen_recordmanager if {
    verplaatsen
        with input.subject.properties.rollen as ["recordmanager"]
}

test_verplaatsen_behandelaar_wrong_role_fails if {
    not toevoegen_nieuwe_versie
        with input.subject.properties.rollen as ["fakeRole"]
}

#############
# ontkoppelen
#############
test_ontkoppelen_behandelaar if {
    ontkoppelen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_ontkoppelen_behandelaar_locked_by_current_user if {
    ontkoppelen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as true
        with input.subject.id as "1"
        with input.resource.properties.vergrendeld_door as "1"
}

test_ontkoppelen_behandelaar_zaak_closed_fails if {
    not ontkoppelen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as false
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as false
}

test_ontkoppelen_behandelaar_definitief_fails if {
    not ontkoppelen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as true
        with input.resource.properties.vergrendeld as false
}

test_ontkoppelen_behandelaar_locked_by_another_user_fails if {
    not ontkoppelen
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.zaak_open as true
        with input.resource.properties.definitief as false
        with input.resource.properties.vergrendeld as true
        with input.subject.id as "1"
        with input.resource.properties.vergrendeld_door as "2"
}

test_ontkoppelen_recordmanager if  {
    ontkoppelen
        with input.subject.properties.rollen as ["recordmanager"]
}

test_ontkoppelen_missing_role_fails if {
    not ontkoppelen
        with input.resource.properties.zaak_open as true
}

test_ontkoppelen_wrong_role_fails if {
    not ontkoppelen
        with input.subject.properties.rollen as ["fakeRole"]
}

############
# downloaden
############
test_downloaden if  {
    downloaden
        with input.subject.properties.rollen as ["raadpleger"]
}

test_ontkoppelen_missing_role_fails if {
    not downloaden
        with input.resource.properties.zaak_open as true
}

test_ontkoppelen_wrong_role_fails if {
    not downloaden
        with input.subject.properties.rollen as ["fakeRole"]
}

############
# converteren
############
test_converteren if  {
    converteren
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.definitief as true
}

test_converteren_wrong_role_fails if {
    not converteren
        with input.subject.properties.rollen as ["raadpleger"]
        with input.resource.properties.definitief as true
}

test_converteren_document_not_definitief_fails if {
    not converteren
        with input.subject.properties.rollen as ["behandelaar"]
        with input.resource.properties.definitief as false
}
