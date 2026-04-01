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
import jakarta.enterprise.inject.Typed
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
 * Implements [evaluations] by making a single OPA Data API call per resource type
 * (which returns all allowed actions efficiently), then mapping the results to
 * per-action boolean decisions matching the requested evaluations.
 */
@ApplicationScoped
@Typed(OpaAccessService::class)
@NoArgConstructor
@AllOpen
class OpaAccessService @Inject constructor(
    @RestClient private val opaDataClient: OpaDataClient
) : AccessService {

    /**
     * Evaluate all actions in a single OPA Data API call.
     *
     * **Limitations:** Only varying `action.name` across evaluations is supported.
     * Per-evaluation subject/resource/context overrides, action properties, shared
     * top-level action, and top-level context are not supported.
     */
    override fun evaluations(request: EvaluationsRequest): EvaluationsResponse {
        val resource = requireNotNull(request.resource) { "Resource is required" }
        val opaPath = RESOURCE_TYPE_TO_OPA_PATH[resource.type]
            ?: throw IllegalArgumentException("Unknown resource type: ${resource.type}")

        // Single OPA call returns all allowed actions for this subject+resource.
        // Explicitly convert to maps so JSONB serializes nested AuthZen objects correctly
        // (the library classes may lack @JsonbProperty annotations needed by the WildFly JSONB provider).
        val opaInput = OpaRuleInput(
            input = mapOf(
                "subject" to request.subject?.let {
                    buildMap<String, Any?> {
                        put("type", it.type)
                        put("id", it.id)
                        it.properties?.let { props -> put("properties", props) }
                    }
                },
                "resource" to request.resource?.let {
                    buildMap<String, Any?> {
                        put("type", it.type)
                        put("id", it.id)
                        it.properties?.let { props -> put("properties", props) }
                    }
                }
            )
        )
        val opaResponse = opaDataClient.query(opaPath, opaInput)
        val allowedActions = opaResponse.results.map { it["name"] as String }.toSet()

        return EvaluationsResponse(
            evaluations = request.evaluations.map { eval ->
                EvaluationResponse(decision = eval.action?.name in allowedActions)
            }
        )
    }

    override fun evaluation(request: EvaluationRequest): EvaluationResponse =
        throw UnsupportedOperationException("Use evaluations() for batch evaluation")

    override fun searchActions(request: ActionSearchRequest): ActionSearchResponse =
        throw UnsupportedOperationException("Use evaluations() instead")

    override fun searchSubjects(request: SubjectSearchRequest): SubjectSearchResponse =
        throw UnsupportedOperationException("Use evaluations() instead")

    override fun searchResources(request: ResourceSearchRequest): ResourceSearchResponse =
        throw UnsupportedOperationException("Use evaluations() instead")

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
 * OPA Data API response envelope: `{"result": {"results": [{"name": "action1"}, ...]}}`.
 */
data class OpaRuleResponse @JsonbCreator constructor(
    @param:JsonbProperty("result")
    val result: Map<String, Any?>
) {
    val results: List<Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        get() = (result["results"] as? List<Map<String, Any?>>) ?: emptyList()
}
