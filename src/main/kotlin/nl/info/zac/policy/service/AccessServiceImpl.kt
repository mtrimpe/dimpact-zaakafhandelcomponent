/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.service

import com.dataversation.authzen.AccessService
import jakarta.enterprise.inject.Vetoed
import com.dataversation.authzen.model.ActionSearchRequest
import com.dataversation.authzen.model.ActionSearchResponse
import com.dataversation.authzen.model.EvaluationRequest
import com.dataversation.authzen.model.EvaluationResponse
import com.dataversation.authzen.model.EvaluationsRequest
import com.dataversation.authzen.model.EvaluationsResponse
import com.dataversation.authzen.model.ResourceSearchRequest
import com.dataversation.authzen.model.ResourceSearchResponse
import com.dataversation.authzen.model.Subject
import com.dataversation.authzen.model.SubjectSearchRequest
import com.dataversation.authzen.model.SubjectSearchResponse

/**
 * In-process Kotlin implementation of [AccessService].
 *
 * Demonstrates that authorization policies can be written in the application's
 * own language — no external PDP, no policy DSL required. As long as the
 * [AccessService] interface is respected, the rest of the application is
 * unaware of where the policy logic runs.
 *
 * Each resource type delegates to a dedicated policy object that contains
 * the authorization rules as plain Kotlin functions.
 */
@Vetoed
class AccessServiceImpl : AccessService {

    private val policies = mapOf(
        "zaak" to ZaakPolicy,
        "taak" to TaakPolicy,
        "document" to DocumentPolicy,
        "zaakNotitie" to NotitiePolicy,
        "application" to ApplicationPolicy,
        "werklijst" to WerklijstPolicy
    )

    override fun evaluation(request: EvaluationRequest): EvaluationResponse {
        val policy = policies[request.resource?.type]
            ?: return EvaluationResponse(decision = false)
        val actionName = request.action?.name ?: return EvaluationResponse(decision = false)
        return EvaluationResponse(
            decision = policy.evaluate(actionName, request.subject, request.resource?.properties)
        )
    }

    override fun evaluations(request: EvaluationsRequest): EvaluationsResponse {
        val policy = policies[request.resource?.type]
            ?: return EvaluationsResponse(
                evaluations = request.evaluations.map { EvaluationResponse(decision = false) }
            )
        return EvaluationsResponse(
            evaluations = request.evaluations.map { eval ->
                val actionName = eval.action?.name ?: ""
                EvaluationResponse(
                    decision = policy.evaluate(actionName, request.subject, request.resource?.properties)
                )
            }
        )
    }

    override fun searchActions(request: ActionSearchRequest): ActionSearchResponse =
        throw UnsupportedOperationException("Use evaluations() instead")

    override fun searchSubjects(request: SubjectSearchRequest): SubjectSearchResponse =
        throw UnsupportedOperationException("Not supported")

    override fun searchResources(request: ResourceSearchRequest): ResourceSearchResponse =
        throw UnsupportedOperationException("Not supported")
}

/**
 * Base interface for resource-type-specific policy objects.
 */
interface Policy {
    fun evaluate(action: String, subject: Subject?, resourceProperties: Map<String, Any?>?): Boolean
}

// ─── Shared helpers ─────────────────────────────────────────────────────────

/** Check if the subject has a specific role. */
fun Subject?.hasRole(role: String): Boolean {
    val rollen = this?.properties?.get("rollen") as? Collection<*> ?: return false
    return role in rollen
}

/** Check if the resource's zaaktype is in the subject's authorized zaaktypes. */
fun zaaktypeAllowed(subject: Subject?, resourceProperties: Map<String, Any?>?): Boolean {
    val userZaaktypen = subject?.properties?.get("zaaktypen") as? Collection<*> ?: return true
    val resourceZaaktype = resourceProperties?.get("zaaktype") ?: return true
    return resourceZaaktype in userZaaktypen
}

/** Read a boolean resource property, defaulting to false. */
fun Map<String, Any?>?.prop(key: String): Boolean =
    (this?.get(key) as? Boolean) ?: (this?.get(key)?.toString() == "true")
