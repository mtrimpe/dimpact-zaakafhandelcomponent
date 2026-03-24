/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty

data class Resource<T>(
    @field:JsonbProperty("type")
    val type: String,

    @field:JsonbProperty("id")
    val id: String? = null,

    @field:JsonbProperty("properties")
    val properties: T? = null
)
