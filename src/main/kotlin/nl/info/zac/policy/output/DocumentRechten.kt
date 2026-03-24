/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult
import nl.info.zac.policy.input.Action

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
) : OpaRuleResult {
    companion object {
        @JsonbCreator
        @JvmStatic
        fun fromActionSearch(
            @JsonbProperty("results") results: List<Action>
        ): DocumentRechten {
            val names = results.map { it.name }.toSet()
            return DocumentRechten(
                lezen = "lezen" in names,
                wijzigen = "wijzigen" in names,
                verwijderen = "verwijderen" in names,
                vergrendelen = "vergrendelen" in names,
                ontgrendelen = "ontgrendelen" in names,
                ondertekenen = "ondertekenen" in names,
                toevoegenNieuweVersie = "toevoegen_nieuwe_versie" in names,
                verplaatsen = "verplaatsen" in names,
                ontkoppelen = "ontkoppelen" in names,
                downloaden = "downloaden" in names,
                converteren = "converteren" in names
            )
        }
    }
}
