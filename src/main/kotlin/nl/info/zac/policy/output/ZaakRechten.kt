/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult
import nl.info.zac.policy.input.Action

data class ZaakRechten(
    val lezen: Boolean,
    val wijzigen: Boolean,
    val toekennen: Boolean,
    val behandelen: Boolean,
    val afbreken: Boolean,
    val heropenen: Boolean,
    val bekijkenZaakdata: Boolean,
    val wijzigenDoorlooptijd: Boolean,
    val verlengen: Boolean,
    val opschorten: Boolean,
    val hervatten: Boolean,
    val creerenDocument: Boolean,
    val toevoegenDocument: Boolean,
    val koppelen: Boolean,
    val versturenEmail: Boolean,
    val versturenOntvangstbevestiging: Boolean,
    val toevoegenInitiatorPersoon: Boolean,
    val toevoegenInitiatorBedrijf: Boolean,
    val verwijderenInitiator: Boolean,
    val toevoegenBetrokkenePersoon: Boolean,
    val toevoegenBetrokkeneBedrijf: Boolean,
    val verwijderenBetrokkene: Boolean,
    val toevoegenBagObject: Boolean,
    val startenTaak: Boolean,
    val vastleggenBesluit: Boolean,
    val verlengenDoorlooptijd: Boolean,
    val wijzigenLocatie: Boolean
) : OpaRuleResult {
    companion object {
        @JsonbCreator
        @JvmStatic
        fun fromActionSearch(
            @JsonbProperty("results") results: List<Action>
        ): ZaakRechten {
            val names = results.map { it.name }.toSet()
            return ZaakRechten(
                lezen = "lezen" in names,
                wijzigen = "wijzigen" in names,
                toekennen = "toekennen" in names,
                behandelen = "behandelen" in names,
                afbreken = "afbreken" in names,
                heropenen = "heropenen" in names,
                bekijkenZaakdata = "bekijken_zaakdata" in names,
                wijzigenDoorlooptijd = "wijzigen_doorlooptijd" in names,
                verlengen = "verlengen" in names,
                opschorten = "opschorten" in names,
                hervatten = "hervatten" in names,
                creerenDocument = "creeren_document" in names,
                toevoegenDocument = "toevoegen_document" in names,
                koppelen = "koppelen" in names,
                versturenEmail = "versturen_email" in names,
                versturenOntvangstbevestiging = "versturen_ontvangstbevestiging" in names,
                toevoegenInitiatorPersoon = "toevoegen_initiator_persoon" in names,
                toevoegenInitiatorBedrijf = "toevoegen_initiator_bedrijf" in names,
                verwijderenInitiator = "verwijderen_initiator" in names,
                toevoegenBetrokkenePersoon = "toevoegen_betrokkene_persoon" in names,
                toevoegenBetrokkeneBedrijf = "toevoegen_betrokkene_bedrijf" in names,
                verwijderenBetrokkene = "verwijderen_betrokkene" in names,
                toevoegenBagObject = "toevoegen_bag_object" in names,
                startenTaak = "starten_taak" in names,
                vastleggenBesluit = "vastleggen_besluit" in names,
                verlengenDoorlooptijd = "verlengen_doorlooptijd" in names,
                wijzigenLocatie = "wijzigen_locatie" in names
            )
        }
    }
}
