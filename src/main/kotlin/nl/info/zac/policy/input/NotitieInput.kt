/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.authentication.LoggedInUser

class NotitieInput(
    loggedInUser: LoggedInUser,
    featureFlagPabcIntegration: Boolean = false
) : UserInput(
    loggedInUser = loggedInUser,
    featureFlagPabcIntegration = featureFlagPabcIntegration
) {
    @field:JsonbProperty("resource")
    val resource = Resource<Any>(type = "zaakNotitie")
}
