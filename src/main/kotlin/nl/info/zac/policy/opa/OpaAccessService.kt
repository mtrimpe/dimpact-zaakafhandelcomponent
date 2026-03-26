/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.opa

import com.dataversation.authzen.AccessService
import com.dataversation.authzen.model.ActionSearchRequest
import com.dataversation.authzen.model.ActionSearchResponse
import com.dataversation.authzen.model.EvaluationRequest
import com.dataversation.authzen.model.EvaluationResponse
import com.dataversation.authzen.model.EvaluationsRequest
import com.dataversation.authzen.model.EvaluationsResponse
import com.dataversation.authzen.model.ResourceSearchRequest
import com.dataversation.authzen.model.ResourceSearchResponse
import com.dataversation.authzen.model.SubjectSearchRequest
import com.dataversation.authzen.model.SubjectSearchResponse
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.eclipse.microprofile.rest.client.inject.RestClient

/**
 * OPA implementation of [AccessService].
 *
 * Only supports [searchActions] — routes to the appropriate OPA Data API path
 * based on the resource type, sending the AuthZEN subject/resource as OPA input.
 * All other AuthZEN operations throw [UnsupportedOperationException].
 */
@ApplicationScoped
@NoArgConstructor
@AllOpen
class OpaAccessService @Inject constructor(
    @RestClient private val opaDataClient: OpaDataClient
) : AccessService {

    override fun searchActions(request: ActionSearchRequest): ActionSearchResponse {
        val resource = requireNotNull(request.resource) { "Resource is required for action search" }
        val opaPath = RESOURCE_TYPE_TO_OPA_PATH[resource.type]
            ?: throw IllegalArgumentException("Unknown resource type: ${resource.type}")
        val opaInput = OpaRuleInput(
            input = mapOf(
                "subject" to request.subject,
                "resource" to request.resource
            )
        )
        return opaDataClient.query(opaPath, opaInput).result
    }

    override fun evaluation(request: EvaluationRequest): EvaluationResponse =
        throw UnsupportedOperationException("OPA backend only supports action search")

    override fun evaluations(request: EvaluationsRequest): EvaluationsResponse =
        throw UnsupportedOperationException("OPA backend only supports action search")

    override fun searchSubjects(request: SubjectSearchRequest): SubjectSearchResponse =
        throw UnsupportedOperationException("OPA backend only supports action search")

    override fun searchResources(request: ResourceSearchRequest): ResourceSearchResponse =
        throw UnsupportedOperationException("OPA backend only supports action search")

    companion object {
        private val RESOURCE_TYPE_TO_OPA_PATH = mapOf(
            "zaak" to "zaak/zaak_rechten",
            "taak" to "taak/taak_rechten",
            "document" to "document/document_rechten",
            "zaakNotitie" to "notitie/notitie_rechten",
            "application" to "overig/overige_rechten",
            "werklijst" to "werklijst/werklijst_rechten"
        )
    }
}

/**
 * Generic OPA Data API client that posts to any policy path under `v1/data/net/atos/zac/`.
 */
@RegisterRestClient(configKey = "OPA-Api-Client")
@Path("v1/data/net/atos/zac")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
interface OpaDataClient {
    @POST
    @Path("{path: .+}")
    fun query(@PathParam("path") path: String, input: OpaRuleInput): OpaRuleResponse
}

/**
 * OPA Data API request envelope: `{"input": {...}}`.
 */
data class OpaRuleInput(
    @field:JsonbProperty("input")
    val input: Map<String, Any?>
)

/**
 * OPA Data API response envelope: `{"result": {"results": [...]}}`.
 */
data class OpaRuleResponse @JsonbCreator constructor(
    @param:JsonbProperty("result")
    val result: ActionSearchResponse
)
