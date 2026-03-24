/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult
import nl.info.zac.policy.input.Action

data class NotitieRechten(
    val lezen: Boolean,
    val wijzigen: Boolean
) : OpaRuleResult {
    companion object {
        @JsonbCreator
        @JvmStatic
        fun fromActionSearch(
            @JsonbProperty("results") results: List<Action>
        ): NotitieRechten {
            val names = results.map { it.name }.toSet()
            return NotitieRechten(
                lezen = "lezen" in names,
                wijzigen = "wijzigen" in names
            )
        }
    }
}
