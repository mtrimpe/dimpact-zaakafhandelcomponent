/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.service

import com.dataversation.authzen.model.Subject

/**
 * Authorization policy for zaak resources.
 *
 * Rules are organized by action name. Each rule is a plain Kotlin function
 * that receives the subject and resource properties and returns a boolean.
 */
object ZaakPolicy : Policy {

    override fun evaluate(action: String, subject: Subject?, resourceProperties: Map<String, Any?>?) =
        when (action) {
            "lezen" -> lezen(subject, resourceProperties)
            "wijzigen" -> wijzigen(subject, resourceProperties)
            "toekennen" -> toekennen(subject, resourceProperties)
            "behandelen" -> behandelen(subject, resourceProperties)
            "afbreken" -> afbreken(subject, resourceProperties)
            "heropenen" -> heropenen(subject, resourceProperties)
            "bekijken_zaakdata" -> bekijkenZaakdata(subject)
            "wijzigen_doorlooptijd" -> wijzigenDoorlooptijd(subject, resourceProperties)
            "verlengen" -> verlengen(subject, resourceProperties)
            "opschorten" -> opschorten(subject, resourceProperties)
            "hervatten" -> hervatten(subject, resourceProperties)
            "creeren_document" -> creerenDocument(subject, resourceProperties)
            "toevoegen_document" -> toevoegenDocument(subject, resourceProperties)
            "koppelen" -> koppelen(subject, resourceProperties)
            "versturen_email" -> versturenEmail(subject, resourceProperties)
            "versturen_ontvangstbevestiging" -> versturenOntvangstbevestiging(subject, resourceProperties)
            "toevoegen_initiator_persoon" -> toevoegenInitiatorPersoon(subject, resourceProperties)
            "toevoegen_initiator_bedrijf" -> toevoegenInitiatorBedrijf(subject, resourceProperties)
            "verwijderen_initiator" -> verwijderenInitiator(subject, resourceProperties)
            "toevoegen_betrokkene_persoon" -> toevoegenBetrokkenePersoon(subject, resourceProperties)
            "toevoegen_betrokkene_bedrijf" -> toevoegenBetrokkeneBedrijf(subject, resourceProperties)
            "verwijderen_betrokkene" -> verwijderenBetrokkene(subject, resourceProperties)
            "toevoegen_bag_object" -> toevoegenBagObject(subject, resourceProperties)
            "starten_taak" -> startenTaak(subject, resourceProperties)
            "vastleggen_besluit" -> vastleggenBesluit(subject, resourceProperties)
            "verlengen_doorlooptijd" -> verlengenDoorlooptijd(subject, resourceProperties)
            "wijzigen_locatie" -> wijzigenLocatie(subject, resourceProperties)
            else -> false
        }

    // ─── Read ───

    /** Raadpleger or higher can read zaken for authorized zaaktypes. */
    private fun lezen(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("raadpleger") && zaaktypeAllowed(subject, zaak)

    // ─── Write (behandelaar when open, or recordmanager always) ───

    private fun wijzigen(subject: Subject?, zaak: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak))

    private fun toekennen(subject: Subject?, zaak: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak))

    private fun behandelen(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak)

    private fun afbreken(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak)

    private fun heropenen(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak)

    private fun bekijkenZaakdata(subject: Subject?) =
        subject.hasRole("beheerder")

    private fun wijzigenDoorlooptijd(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")

    /** Verlengen requires open zaak that is not heropend, opgeschort, or verlengd. */
    private fun verlengen(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) &&
            zaak.prop("open") && !zaak.prop("heropend") &&
            !zaak.prop("opgeschort") && !zaak.prop("verlengd")

    /** Opschorten requires open zaak that is not heropend or already opgeschort. */
    private fun opschorten(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) &&
            zaak.prop("open") && !zaak.prop("heropend") && !zaak.prop("opgeschort")

    private fun hervatten(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak)

    // ─── Documents & communication (behandelaar when open) ───

    private fun creerenDocument(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")

    private fun toevoegenDocument(subject: Subject?, zaak: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak))

    private fun koppelen(subject: Subject?, zaak: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak))

    private fun versturenEmail(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")

    private fun versturenOntvangstbevestiging(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")

    // ─── Initiator & betrokkene (behandelaar when open, or recordmanager) ───

    private fun toevoegenInitiatorPersoon(subject: Subject?, zaak: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak))

    private fun toevoegenInitiatorBedrijf(subject: Subject?, zaak: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak))

    private fun verwijderenInitiator(subject: Subject?, zaak: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak))

    private fun toevoegenBetrokkenePersoon(subject: Subject?, zaak: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak))

    private fun toevoegenBetrokkeneBedrijf(subject: Subject?, zaak: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak))

    private fun verwijderenBetrokkene(subject: Subject?, zaak: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak))

    private fun toevoegenBagObject(subject: Subject?, zaak: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowed(subject, zaak))

    private fun startenTaak(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")

    /** Vastleggen besluit requires open, non-intake zaak with besluittypen configured. */
    private fun vastleggenBesluit(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) &&
            zaak.prop("open") && !zaak.prop("intake") && zaak.prop("besloten")

    private fun verlengenDoorlooptijd(subject: Subject?, zaak: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowed(subject, zaak) && zaak.prop("open")

    /** Wijzigen locatie is allowed when wijzigen is allowed. */
    private fun wijzigenLocatie(subject: Subject?, zaak: Map<String, Any?>?) =
        wijzigen(subject, zaak)
}
