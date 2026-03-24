/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult
import nl.info.zac.policy.input.Action

data class OverigeRechten(
    val startenZaak: Boolean,
    val beheren: Boolean,
    val zoeken: Boolean
) : OpaRuleResult {
    companion object {
        @JsonbCreator
        @JvmStatic
        fun fromActionSearch(
            @JsonbProperty("results") results: List<Action>
        ): OverigeRechten {
            val names = results.map { it.name }.toSet()
            return OverigeRechten(
                startenZaak = "starten_zaak" in names,
                beheren = "beheren" in names,
                zoeken = "zoeken" in names
            )
        }
    }
}
