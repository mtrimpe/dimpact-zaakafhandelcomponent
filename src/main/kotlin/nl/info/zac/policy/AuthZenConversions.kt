/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy

import com.dataversation.authzen.model.EvaluationRequest
import com.dataversation.authzen.model.EvaluationsRequest
import com.dataversation.authzen.model.EvaluationsResponse
import nl.info.zac.policy.input.UserData
import nl.info.zac.policy.input.UserInput
import com.dataversation.authzen.model.Action as AuthZenAction
import com.dataversation.authzen.model.Resource as AuthZenResource
import com.dataversation.authzen.model.Subject as AuthZenSubject

/**
 * Convert a ZAC [UserData] (AuthZEN subject shape) to an AuthZEN [AuthZenSubject].
 */
fun UserData.toAuthZenSubject() = AuthZenSubject(
    type = type,
    id = id,
    properties = properties.let { props ->
        buildMap<String, Any?> {
            put("rollen", props.rollen)
            props.zaaktypen?.let { put("zaaktypen", it) }
        }
    }
)

/**
 * Convert a ZAC [nl.info.zac.policy.input.Resource] to an AuthZEN [AuthZenResource].
 * Serializes the domain-specific properties to a generic map.
 */
fun <T> nl.info.zac.policy.input.Resource<T>.toAuthZenResource(): AuthZenResource {
    val propsMap = properties?.let { props ->
        when (props) {
            is Map<*, *> ->
                @Suppress("UNCHECKED_CAST")
                (props as Map<String, Any?>)
            else -> props.toPropertyMap()
        }
    }
    return AuthZenResource(
        type = type,
        id = id ?: "_",
        properties = propsMap
    )
}

/**
 * Convert a typed properties object to a generic map via reflection.
 * This handles ZaakData, TaakData, DocumentData, etc.
 */
private fun Any.toPropertyMap(): Map<String, Any?> = buildMap {
    this@toPropertyMap::class.members
        .filter { it.parameters.size == 1 } // property getters only
        .forEach { member ->
            try {
                val value = member.call(this@toPropertyMap)
                // Convert camelCase to snake_case for JSON compatibility
                val key = member.name.replace(Regex("([a-z])([A-Z])")) {
                    "${it.groupValues[1]}_${it.groupValues[2].lowercase()}"
                }
                put(key, value)
            } catch (_: Exception) {
                // Skip inaccessible properties
            }
        }
}

/**
 * Extract the AuthZEN [AuthZenResource] from any [UserInput] subtype that has a resource field.
 */
fun UserInput.extractAuthZenResource(): AuthZenResource = when (this) {
    is nl.info.zac.policy.input.ZaakInput -> this.resource.toAuthZenResource()
    is nl.info.zac.policy.input.TaakInput -> this.resource.toAuthZenResource()
    is nl.info.zac.policy.input.DocumentInput -> this.resource.toAuthZenResource()
    is nl.info.zac.policy.input.NotitieInput -> this.resource.toAuthZenResource()
    is nl.info.zac.policy.input.OverigeInput -> this.resource.toAuthZenResource()
    is nl.info.zac.policy.input.WerklijstInput -> this.resource.toAuthZenResource()
    else -> throw IllegalArgumentException("Unknown input type: ${this::class}")
}

/**
 * Build an [EvaluationsRequest] from any ZAC [UserInput] subtype.
 * Uses shared subject/resource at the top level, with one [EvaluationRequest] per candidate action.
 */
fun UserInput.toEvaluationsRequest(): EvaluationsRequest {
    val resource = this.extractAuthZenResource()
    val actions = ResourceActions.BY_RESOURCE_TYPE[resource.type]
        ?: throw IllegalArgumentException("Unknown resource type: ${resource.type}")
    return EvaluationsRequest(
        subject = this.subject.toAuthZenSubject(),
        resource = resource,
        // Non-empty context required because Cerbos rejects empty context objects
        context = mapOf("source" to "zac"),
        evaluations = actions.map { actionName ->
            EvaluationRequest(action = AuthZenAction(name = actionName), context = mapOf("source" to "zac"))
        }
    )
}

/**
 * Convert an [EvaluationsResponse] to a map of action name → decision boolean.
 * The response evaluations are zipped with the candidate actions in order.
 */
fun EvaluationsResponse.toDecisions(actions: List<String>): Map<String, Boolean> =
    actions.zip(evaluations) { name, eval -> name to eval.decision }.toMap()
