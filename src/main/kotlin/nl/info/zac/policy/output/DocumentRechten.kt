/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

data class DocumentRechten(
    val lezen: Boolean,
    val wijzigen: Boolean,
    val verwijderen: Boolean,
    val vergrendelen: Boolean,
    val ontgrendelen: Boolean,
    val ondertekenen: Boolean,
    val toevoegenNieuweVersie: Boolean,
    val verplaatsen: Boolean,
    val ontkoppelen: Boolean,
    val downloaden: Boolean,
    val converteren: Boolean
) {
    companion object {
        fun fromEvaluations(decisions: Map<String, Boolean>) = DocumentRechten(
            lezen = decisions["lezen"] ?: false,
            wijzigen = decisions["wijzigen"] ?: false,
            verwijderen = decisions["verwijderen"] ?: false,
            vergrendelen = decisions["vergrendelen"] ?: false,
            ontgrendelen = decisions["ontgrendelen"] ?: false,
            ondertekenen = decisions["ondertekenen"] ?: false,
            toevoegenNieuweVersie = decisions["toevoegen_nieuwe_versie"] ?: false,
            verplaatsen = decisions["verplaatsen"] ?: false,
            ontkoppelen = decisions["ontkoppelen"] ?: false,
            downloaden = decisions["downloaden"] ?: false,
            converteren = decisions["converteren"] ?: false
        )
    }
}
