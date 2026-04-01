/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

data class NotitieRechten(
    val lezen: Boolean,
    val wijzigen: Boolean
) {
    companion object {
        fun fromEvaluations(decisions: Map<String, Boolean>) = NotitieRechten(
            lezen = decisions["lezen"] ?: false,
            wijzigen = decisions["wijzigen"] ?: false
        )
    }
}
