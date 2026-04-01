/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.service

import com.dataversation.authzen.model.Subject

/**
 * Authorization policy for notitie (note/memo) resources.
 */
object NotitiePolicy : Policy {
    override fun evaluate(action: String, subject: Subject?, resourceProperties: Map<String, Any?>?) =
        when (action) {
            "lezen" -> subject.hasRole("raadpleger")
            "wijzigen" -> subject.hasRole("behandelaar")
            else -> false
        }
}

/**
 * Authorization policy for application-level actions.
 */
object ApplicationPolicy : Policy {
    override fun evaluate(action: String, subject: Subject?, resourceProperties: Map<String, Any?>?) =
        when (action) {
            "starten_zaak" -> subject.hasRole("behandelaar")
            "beheren" -> subject.hasRole("beheerder")
            "zaaktype_inzien" -> subject.hasRole("behandelaar") || subject.hasRole("beheerder")
            "zoeken" -> subject.hasRole("raadpleger")
            else -> false
        }
}

/**
 * Authorization policy for werklijst (work list) actions.
 */
object WerklijstPolicy : Policy {
    override fun evaluate(action: String, subject: Subject?, resourceProperties: Map<String, Any?>?) =
        when (action) {
            "inbox" -> subject.hasRole("coordinator")
            "ontkoppelde_documenten_verwijderen" -> subject.hasRole("recordmanager")
            "inbox_productaanvragen_verwijderen" -> subject.hasRole("recordmanager")
            "zaken_taken" -> subject.hasRole("raadpleger")
            "zaken_taken_verdelen" -> subject.hasRole("coordinator")
            "zaken_taken_exporteren" -> subject.hasRole("beheerder")
            else -> false
        }
}
