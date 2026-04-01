/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.service

import com.dataversation.authzen.model.Subject

/**
 * Authorization policy for document (enkelvoudig informatieobject) resources.
 */
object DocumentPolicy : Policy {

    override fun evaluate(action: String, subject: Subject?, resourceProperties: Map<String, Any?>?) =
        when (action) {
            "lezen" -> lezen(subject, resourceProperties)
            "wijzigen" -> wijzigen(subject, resourceProperties)
            "verwijderen" -> verwijderen(subject, resourceProperties)
            "vergrendelen" -> vergrendelen(subject, resourceProperties)
            "ontgrendelen" -> ontgrendelen(subject, resourceProperties)
            "ondertekenen" -> ondertekenen(subject, resourceProperties)
            "toevoegen_nieuwe_versie" -> toevoegenNieuweVersie(subject, resourceProperties)
            "verplaatsen" -> verplaatsen(subject, resourceProperties)
            "ontkoppelen" -> ontkoppelen(subject, resourceProperties)
            "downloaden" -> downloaden(subject, resourceProperties)
            "converteren" -> converteren(subject, resourceProperties)
            else -> false
        }

    /** True if document is unlocked, or locked by the current user. */
    private fun unlockedOrLockedByUser(subject: Subject?, doc: Map<String, Any?>?): Boolean {
        if (doc.prop("vergrendeld") != true) return true
        return doc?.get("vergrendeld_door") == subject?.id
    }

    private fun zaaktypeAllowedForDoc(subject: Subject?, doc: Map<String, Any?>?): Boolean {
        val docZaaktype = doc?.get("zaaktype") ?: return true
        val userZaaktypen = subject?.properties?.get("zaaktypen") as? Collection<*> ?: return true
        return docZaaktype in userZaaktypen
    }

    private fun lezen(subject: Subject?, doc: Map<String, Any?>?) =
        subject.hasRole("raadpleger") && zaaktypeAllowedForDoc(subject, doc)

    private fun wijzigen(subject: Subject?, doc: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowedForDoc(subject, doc) &&
            doc.prop("zaak_open") && !doc.prop("definitief") && unlockedOrLockedByUser(subject, doc)) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowedForDoc(subject, doc))

    private fun verwijderen(subject: Subject?, doc: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowedForDoc(subject, doc) &&
            doc.prop("zaak_open") && !doc.prop("definitief") && !doc.prop("vergrendeld")) ||
            (subject.hasRole("recordmanager") && !doc.prop("vergrendeld"))

    private fun vergrendelen(subject: Subject?, doc: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowedForDoc(subject, doc) && doc.prop("zaak_open")

    private fun ontgrendelen(subject: Subject?, doc: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowedForDoc(subject, doc) && doc?.get("vergrendeld_door") == subject?.id) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowedForDoc(subject, doc))

    private fun ondertekenen(subject: Subject?, doc: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && zaaktypeAllowedForDoc(subject, doc) &&
            doc.prop("zaak_open") && unlockedOrLockedByUser(subject, doc)

    private fun toevoegenNieuweVersie(subject: Subject?, doc: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowedForDoc(subject, doc) &&
            doc.prop("zaak_open") && !doc.prop("definitief") && unlockedOrLockedByUser(subject, doc)) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowedForDoc(subject, doc))

    private fun verplaatsen(subject: Subject?, doc: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowedForDoc(subject, doc) &&
            doc.prop("zaak_open") && !doc.prop("definitief") && unlockedOrLockedByUser(subject, doc)) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowedForDoc(subject, doc))

    private fun ontkoppelen(subject: Subject?, doc: Map<String, Any?>?) =
        (subject.hasRole("behandelaar") && zaaktypeAllowedForDoc(subject, doc) &&
            doc.prop("zaak_open") && !doc.prop("definitief") && unlockedOrLockedByUser(subject, doc)) ||
            (subject.hasRole("recordmanager") && zaaktypeAllowedForDoc(subject, doc))

    private fun downloaden(subject: Subject?, doc: Map<String, Any?>?) =
        subject.hasRole("raadpleger") && zaaktypeAllowedForDoc(subject, doc)

    private fun converteren(subject: Subject?, doc: Map<String, Any?>?) =
        subject.hasRole("behandelaar") && doc.prop("definitief") && zaaktypeAllowedForDoc(subject, doc)
}
