/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

data class WerklijstRechten(
    val inbox: Boolean,
    val ontkoppeldeDocumentenVerwijderen: Boolean,
    val inboxProductaanvragenVerwijderen: Boolean,
    val zakenTaken: Boolean,
    val zakenTakenVerdelen: Boolean,
    val zakenTakenExporteren: Boolean
) {
    companion object {
        fun fromEvaluations(decisions: Map<String, Boolean>) = WerklijstRechten(
            inbox = decisions["inbox"] ?: false,
            ontkoppeldeDocumentenVerwijderen = decisions["ontkoppelde_documenten_verwijderen"] ?: false,
            inboxProductaanvragenVerwijderen = decisions["inbox_productaanvragen_verwijderen"] ?: false,
            zakenTaken = decisions["zaken_taken"] ?: false,
            zakenTakenVerdelen = decisions["zaken_taken_verdelen"] ?: false,
            zakenTakenExporteren = decisions["zaken_taken_exporteren"] ?: false
        )
    }
}
