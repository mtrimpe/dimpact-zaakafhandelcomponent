/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy

/**
 * Canonical action names per resource type, used to build AuthZEN Evaluations requests.
 */
object ResourceActions {
    val ZAAK = listOf(
        "lezen", "wijzigen", "toekennen", "behandelen", "afbreken", "heropenen",
        "bekijken_zaakdata", "wijzigen_doorlooptijd", "verlengen", "opschorten", "hervatten",
        "creeren_document", "toevoegen_document", "koppelen", "versturen_email",
        "versturen_ontvangstbevestiging", "toevoegen_initiator_persoon", "toevoegen_initiator_bedrijf",
        "verwijderen_initiator", "toevoegen_betrokkene_persoon", "toevoegen_betrokkene_bedrijf",
        "verwijderen_betrokkene", "toevoegen_bag_object", "starten_taak", "vastleggen_besluit",
        "verlengen_doorlooptijd", "wijzigen_locatie"
    )

    val DOCUMENT = listOf(
        "lezen", "wijzigen", "verwijderen", "vergrendelen", "ontgrendelen", "ondertekenen",
        "toevoegen_nieuwe_versie", "verplaatsen", "ontkoppelen", "downloaden", "converteren"
    )

    val TAAK = listOf(
        "lezen", "wijzigen", "toekennen", "creeren_document", "toevoegen_document"
    )

    val NOTITIE = listOf("lezen", "wijzigen")

    val APPLICATION = listOf("starten_zaak", "beheren", "zoeken")

    val WERKLIJST = listOf(
        "inbox", "ontkoppelde_documenten_verwijderen", "inbox_productaanvragen_verwijderen",
        "zaken_taken", "zaken_taken_verdelen", "zaken_taken_exporteren"
    )

    val BY_RESOURCE_TYPE = mapOf(
        "zaak" to ZAAK,
        "document" to DOCUMENT,
        "taak" to TAAK,
        "zaakNotitie" to NOTITIE,
        "application" to APPLICATION,
        "werklijst" to WERKLIJST
    )
}
