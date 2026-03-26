/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy

import com.dataversation.authzen.model.ActionSearchRequest
import com.dataversation.authzen.model.ActionSearchResponse
import nl.info.zac.policy.input.UserData
import nl.info.zac.policy.input.UserInput
import com.dataversation.authzen.model.Resource as AuthZenResource
import com.dataversation.authzen.model.Subject as AuthZenSubject
import nl.info.zac.policy.input.Action as InputAction
import nl.info.zac.policy.input.Resource as InputResource

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
 * Convert a ZAC [InputResource] to an AuthZEN [AuthZenResource].
 * Serializes the domain-specific properties to a generic map.
 */
fun <T> InputResource<T>.toAuthZenResource(): AuthZenResource {
    val propsMap = properties?.let { props ->
        // Use Jakarta JSON-B to convert the typed properties to a Map
        // For simplicity, manually build the map based on known types
        when (props) {
            is Map<*, *> ->
                @Suppress("UNCHECKED_CAST")
                (props as Map<String, Any?>)
            else -> props.toPropertyMap()
        }
    }
    return AuthZenResource(
        type = type,
        id = id,
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
 * Build an [ActionSearchRequest] from any ZAC [UserInput] subtype.
 */
fun UserInput.toActionSearchRequest() = ActionSearchRequest(
    subject = this.subject.toAuthZenSubject(),
    resource = this.extractAuthZenResource()
)

/**
 * Convert AuthZEN [ActionSearchResponse] results to ZAC [InputAction] list for use with *Rechten.fromActionSearch().
 */
fun ActionSearchResponse.toInputActions(): List<InputAction> =
    results.map { InputAction(it.name) }
