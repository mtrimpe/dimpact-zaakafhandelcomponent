/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult
import nl.info.zac.policy.input.Action

data class WerklijstRechten(
    val inbox: Boolean,
    val ontkoppeldeDocumentenVerwijderen: Boolean,
    val inboxProductaanvragenVerwijderen: Boolean,
    val zakenTaken: Boolean,
    val zakenTakenVerdelen: Boolean,
    val zakenTakenExporteren: Boolean
) : OpaRuleResult {
    companion object {
        @JsonbCreator
        @JvmStatic
        fun fromActionSearch(
            @JsonbProperty("results") results: List<Action>
        ): WerklijstRechten {
            val names = results.map { it.name }.toSet()
            return WerklijstRechten(
                inbox = "inbox" in names,
                ontkoppeldeDocumentenVerwijderen = "ontkoppelde_documenten_verwijderen" in names,
                inboxProductaanvragenVerwijderen = "inbox_productaanvragen_verwijderen" in names,
                zakenTaken = "zaken_taken" in names,
                zakenTakenVerdelen = "zaken_taken_verdelen" in names,
                zakenTakenExporteren = "zaken_taken_exporteren" in names
            )
        }
    }
}
