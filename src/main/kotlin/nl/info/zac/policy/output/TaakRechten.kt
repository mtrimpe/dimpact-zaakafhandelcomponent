/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

data class TaakRechten(
    val lezen: Boolean,
    val wijzigen: Boolean,
    val toekennen: Boolean,
    val creerenDocument: Boolean,
    val toevoegenDocument: Boolean
) {
    companion object {
        fun fromEvaluations(decisions: Map<String, Boolean>) = TaakRechten(
            lezen = decisions["lezen"] ?: false,
            wijzigen = decisions["wijzigen"] ?: false,
            toekennen = decisions["toekennen"] ?: false,
            creerenDocument = decisions["creeren_document"] ?: false,
            toevoegenDocument = decisions["toevoegen_document"] ?: false
        )
    }
}
