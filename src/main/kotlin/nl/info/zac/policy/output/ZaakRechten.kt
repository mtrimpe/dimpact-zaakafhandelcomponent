/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

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
) {
    companion object {
        fun fromEvaluations(decisions: Map<String, Boolean>) = ZaakRechten(
            lezen = decisions["lezen"] ?: false,
            wijzigen = decisions["wijzigen"] ?: false,
            toekennen = decisions["toekennen"] ?: false,
            behandelen = decisions["behandelen"] ?: false,
            afbreken = decisions["afbreken"] ?: false,
            heropenen = decisions["heropenen"] ?: false,
            bekijkenZaakdata = decisions["bekijken_zaakdata"] ?: false,
            wijzigenDoorlooptijd = decisions["wijzigen_doorlooptijd"] ?: false,
            verlengen = decisions["verlengen"] ?: false,
            opschorten = decisions["opschorten"] ?: false,
            hervatten = decisions["hervatten"] ?: false,
            creerenDocument = decisions["creeren_document"] ?: false,
            toevoegenDocument = decisions["toevoegen_document"] ?: false,
            koppelen = decisions["koppelen"] ?: false,
            versturenEmail = decisions["versturen_email"] ?: false,
            versturenOntvangstbevestiging = decisions["versturen_ontvangstbevestiging"] ?: false,
            toevoegenInitiatorPersoon = decisions["toevoegen_initiator_persoon"] ?: false,
            toevoegenInitiatorBedrijf = decisions["toevoegen_initiator_bedrijf"] ?: false,
            verwijderenInitiator = decisions["verwijderen_initiator"] ?: false,
            toevoegenBetrokkenePersoon = decisions["toevoegen_betrokkene_persoon"] ?: false,
            toevoegenBetrokkeneBedrijf = decisions["toevoegen_betrokkene_bedrijf"] ?: false,
            verwijderenBetrokkene = decisions["verwijderen_betrokkene"] ?: false,
            toevoegenBagObject = decisions["toevoegen_bag_object"] ?: false,
            startenTaak = decisions["starten_taak"] ?: false,
            vastleggenBesluit = decisions["vastleggen_besluit"] ?: false,
            verlengenDoorlooptijd = decisions["verlengen_doorlooptijd"] ?: false,
            wijzigenLocatie = decisions["wijzigen_locatie"] ?: false
        )
    }
}
