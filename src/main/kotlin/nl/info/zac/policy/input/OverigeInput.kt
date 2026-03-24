/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.authentication.LoggedInUser

class OverigeInput(
    loggedInUser: LoggedInUser,
    zaaktype: String? = null,
    featureFlagPabcIntegration: Boolean = false
) : UserInput(
    loggedInUser = loggedInUser,
    zaaktype = zaaktype,
    featureFlagPabcIntegration = featureFlagPabcIntegration
) {
    @field:JsonbProperty("resource")
    val resource = Resource<Any>(type = "application", id = "zac")
}
