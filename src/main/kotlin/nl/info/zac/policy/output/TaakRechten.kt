/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult
import nl.info.zac.policy.input.Action

data class TaakRechten(
    val lezen: Boolean,
    val wijzigen: Boolean,
    val toekennen: Boolean,
    val creerenDocument: Boolean,
    val toevoegenDocument: Boolean
) : OpaRuleResult {
    companion object {
        @JsonbCreator
        @JvmStatic
        fun fromActionSearch(
            @JsonbProperty("results") results: List<Action>
        ): TaakRechten {
            val names = results.map { it.name }.toSet()
            return TaakRechten(
                lezen = "lezen" in names,
                wijzigen = "wijzigen" in names,
                toekennen = "toekennen" in names,
                creerenDocument = "creeren_document" in names,
                toevoegenDocument = "toevoegen_document" in names
            )
        }
    }
}
