/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.service

import com.dataversation.authzen.model.Subject

/**
 * Authorization policy for taak (task) resources.
 */
object TaakPolicy : Policy {

    override fun evaluate(action: String, subject: Subject?, resourceProperties: Map<String, Any?>?) =
        when (action) {
            "lezen" -> subject.hasRole("raadpleger") && zaaktypeAllowed(subject, resourceProperties)
            "wijzigen" -> subject.hasRole("behandelaar") && zaaktypeAllowed(subject, resourceProperties)
            "toekennen" -> subject.hasRole("behandelaar") && zaaktypeAllowed(subject, resourceProperties)
            "creeren_document" -> subject.hasRole("behandelaar") && zaaktypeAllowed(subject, resourceProperties) && resourceProperties.prop("open")
            "toevoegen_document" -> subject.hasRole("behandelaar") && zaaktypeAllowed(subject, resourceProperties) && resourceProperties.prop("open")
            else -> false
        }
}
