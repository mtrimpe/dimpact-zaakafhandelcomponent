/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

data class OverigeRechten(
    val startenZaak: Boolean,
    val beheren: Boolean,
    val zoeken: Boolean
) {
    companion object {
        fun fromEvaluations(decisions: Map<String, Boolean>) = OverigeRechten(
            startenZaak = decisions["starten_zaak"] ?: false,
            beheren = decisions["beheren"] ?: false,
            zoeken = decisions["zoeken"] ?: false
        )
    }
}
