/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.authentication.LoggedInUser

data class ZaakInput(
    val loggedInUser: LoggedInUser,

    val zaakData: ZaakData,

    val featureFlagPabcIntegration: Boolean
) : UserInput(
    loggedInUser = loggedInUser,
    zaaktype = zaakData.zaaktype,
    featureFlagPabcIntegration = featureFlagPabcIntegration
) {
    @field:JsonbProperty("resource")
    val resource = Resource(type = "zaak", properties = zaakData)
}
