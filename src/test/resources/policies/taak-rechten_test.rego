#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
package net.atos.zac.taak

import rego.v1

import data.net.atos.zac.taak.zaaktype_allowed
import data.net.atos.zac.taak.lezen
import data.net.atos.zac.taak.wijzigen
import data.net.atos.zac.taak.toekennen
import data.net.atos.zac.taak.creeren_document
import data.net.atos.zac.taak.toevoegen_document

##################
# zaaktype_allowed
##################
test_zaaktype_allowed if {
    zaaktype_allowed
        with input.resource.properties.zaaktype as "type"
        with input.subject.properties.zaaktypen as ["first", "type"]
}

test_zaaktype_allowed_missing_user_zaaktypen if {
    zaaktype_allowed
        with input.resource.properties.zaaktype as "type"
}

test_zaaktype_allowed_taak_zaaktype_not_in_user_zaaktypen_fails if {
    not zaaktype_allowed
        with input.resource.properties.zaaktype as "missing"
        with input.subject.properties.zaaktypen as ["first", "type"]
}

#######
# lezen
#######
test_lezen if {
    lezen with input.subject.properties.rollen as [ "raadpleger" ]
}

test_lezen_wrong_role_fails if {
    not lezen with input.subject.properties.rollen as [ "fakeRole" ]
}

test_lezen_missing_role_fails if {
    not lezen with input.subject.properties.key as "value"
}

##########
# wijzigen
##########
test_wijzigen if {
    wijzigen
        with input.subject.properties.rollen as [ "behandelaar" ]
        with input.resource.properties.open as true
}

test_wijzigen_taak_closed_fails if {
    not wijzigen
        with input.subject.properties.rollen as [ "behandelaar" ]
        with input.resource.properties.open as false
}

test_wijzigen_wrong_role_fails if {
    not wijzigen
        with input.subject.properties.rollen as [ "fakeRole" ]
        with input.resource.properties.open as true
}

test_wijzigen_missing_role_fails if {
    not wijzigen with input.subject.properties.key as "value"
}

###########
# toekennen
###########
test_toekennen if {
    toekennen
        with input.subject.properties.rollen as [ "behandelaar" ]
        with input.resource.properties.open as true
}

test_toekennen_taak_closed_fails if {
    not toekennen
        with input.subject.properties.rollen as [ "behandelaar" ]
        with input.resource.properties.open as false
}

test_toekennen_wrong_role_fails if {
    not toekennen
        with input.subject.properties.rollen as [ "fakeRole" ]
        with input.resource.properties.open as true
}

test_toekennen_missing_role_fails if {
    not toekennen with input.subject.properties.key as "value"
}

###################
# creeren_document
###################
test_creeren_document if {
    creeren_document
        with input.subject.properties.rollen as [ "behandelaar" ]
        with input.resource.properties.open as true
}

test_creeren_document_taak_closed_fails if {
    not creeren_document
        with input.subject.properties.rollen as [ "behandelaar" ]
        with input.resource.properties.open as false
}

test_creeren_document_wrong_role_fails if {
    not creeren_document with input.subject.properties.rollen as [ "fakeRole" ]
}

test_creeren_document_missing_role_fails if {
    not creeren_document with input.subject.properties.key as "value"
}

###################
# toevoegen_document
###################
test_toevoegen_document if {
    toevoegen_document
        with input.subject.properties.rollen as [ "behandelaar" ]
        with input.resource.properties.open as true
}

test_creeren_document_taak_closed_fails if {
    not toevoegen_document
        with input.subject.properties.rollen as [ "behandelaar" ]
        with input.resource.properties.open as false
}

test_toevoegen_document_wrong_role_fails if {
    not toevoegen_document with input.subject.properties.rollen as [ "fakeRole" ]
}

test_toevoegen_document_missing_role_fails if {
    not toevoegen_document with input.subject.properties.key as "value"
}
