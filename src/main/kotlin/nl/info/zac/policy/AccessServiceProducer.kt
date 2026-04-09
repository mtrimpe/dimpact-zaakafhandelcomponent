/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy

import com.dataversation.authzen.AccessService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import nl.info.zac.policy.service.AccessServiceImpl
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import okhttp3.OkHttpClient
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Optional
import java.util.logging.Logger

/**
 * CDI producer that selects the [AccessService] implementation based on configuration.
 *
 * Each backend demonstrates a different integration pattern:
 * - `kotlin` (default): in-process Kotlin policies — no external PDP needed
 * - `topaz`: Topaz Rego engine via native `is` API
 * - `spicedb`: SpiceDB via Permissions API
 * - `authzforce`: AuthzForce XACML 3.0 PDP via XACML JSON + MDP
 * - `http`: generic HTTP AuthZEN PDP (works with Cerbos, OpenFTV, or any AuthZEN-compliant PDP)
 * - `grpc`: generic gRPC AuthZEN PDP
 */
@ApplicationScoped
@AllOpen
@NoArgConstructor
class AccessServiceProducer @Inject constructor(
    @ConfigProperty(name = "AUTHORIZATION_SERVICE_BACKEND", defaultValue = "kotlin")
    private val backend: String,
    @ConfigProperty(name = "AUTHZEN_PDP_URL")
    private val authZenPdpUrl: Optional<String>
) {
    @Produces
    @ApplicationScoped
    fun produce(): AccessService = when (backend.lowercase()) {
        "kotlin" -> AccessServiceImpl()
        "topaz" -> createTopazTransport()
        "spicedb" -> createSpiceDbTransport()
        "authzforce" -> createAuthzForceTransport()
        "http" -> createAuthZenHttpTransport()
        "grpc" -> createAuthZenGrpcTransport()
        else -> throw IllegalArgumentException(
            "Unknown authorization backend: '$backend'. " +
                "Must be 'kotlin', 'topaz', 'spicedb', 'authzforce', 'http', or 'grpc'."
        )
    }

    private fun requirePdpUrl(): String = authZenPdpUrl.orElseThrow {
        IllegalStateException("AUTHZEN_PDP_URL must be set when AUTHORIZATION_SERVICE_BACKEND=$backend")
    }

    private fun createTopazTransport(): AccessService = try {
        val clazz = Class.forName("com.dataversation.authzen.topaz.TopazAccessService")
        val url = requirePdpUrl()
        // ZAC-specific: Rego package prefix and resource type name overrides
        val policyPackage = "zac"
        val resourceTypeMap = mapOf(
            "zaakNotitie" to "notitie",
            "application" to "overig"
        )
        clazz.getConstructor(String::class.java, String::class.java, OkHttpClient::class.java, Map::class.java)
            .newInstance(url, policyPackage, OkHttpClient(), resourceTypeMap) as AccessService
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("Backend 'topaz' requires authzen-topaz on the classpath.", e)
    }

    @Suppress("UNCHECKED_CAST")
    private fun createSpiceDbTransport(): AccessService = try {
        val clazz = Class.forName("com.dataversation.authzen.spicedb.SpiceDbAccessService")
        val roleSpecClass = Class.forName("com.dataversation.authzen.spicedb.RoleSpec")
        val roleSpecCtor = roleSpecClass.getConstructor(String::class.java, String::class.java)
        fun role(name: String, caveat: String? = null) = roleSpecCtor.newInstance(name, caveat)

        val url = requirePdpUrl()
        val spicedbToken = System.getenv("SPICEDB_TOKEN") ?: "test"
        // ZAC-specific: map AuthZEN resource type names to SpiceDB schema type names
        val resourceTypeMap = mapOf("zaakNotitie" to "zaak_notitie")
        // ZAC-specific: role-resource relationship assignments for auto-provisioning
        val roleAssignments = mapOf(
            "zaak" to mapOf(
                "raadpleger" to listOf(role("raadpleger")),
                "behandelaar" to listOf(role("behandelaar")),
                "coordinator" to listOf(role("coordinator")),
                "recordmanager" to listOf(role("recordmanager")),
                "beheerder" to listOf(role("beheerder")),
                "behandelaar_when_open" to listOf(role("behandelaar", "zaak_is_open")),
                "behandelaar_can_verlengen" to listOf(role("behandelaar", "zaak_can_verlengen")),
                "behandelaar_can_opschorten" to listOf(role("behandelaar", "zaak_can_opschorten")),
                "behandelaar_can_vastleggen_besluit" to listOf(role("behandelaar", "zaak_can_vastleggen_besluit")),
            ),
            "taak" to mapOf(
                "raadpleger" to listOf(role("raadpleger")),
                "behandelaar" to listOf(role("behandelaar")),
                "behandelaar_when_open" to listOf(role("behandelaar", "taak_is_open")),
            ),
            "document" to mapOf(
                "raadpleger" to listOf(role("raadpleger")),
                "behandelaar" to listOf(role("behandelaar")),
                "recordmanager" to listOf(role("recordmanager")),
                "behandelaar_zaak_open" to listOf(role("behandelaar", "doc_zaak_open")),
                "behandelaar_can_edit" to listOf(role("behandelaar", "doc_can_edit")),
                "behandelaar_can_delete" to listOf(role("behandelaar", "doc_can_delete")),
                "rm_not_locked" to listOf(role("recordmanager", "doc_rm_can_delete")),
                "behandelaar_unlock_or_own" to listOf(role("behandelaar", "doc_unlocked_or_own_lock")),
                "behandelaar_own_lock" to listOf(role("behandelaar", "doc_own_lock")),
                "behandelaar_definitief" to listOf(role("behandelaar", "doc_is_definitief")),
            ),
            "zaak_notitie" to mapOf(
                "raadpleger" to listOf(role("raadpleger")),
                "behandelaar" to listOf(role("behandelaar")),
            ),
            "application" to mapOf(
                "behandelaar" to listOf(role("behandelaar")),
                "beheerder" to listOf(role("beheerder")),
                "raadpleger" to listOf(role("raadpleger")),
            ),
            "werklijst" to mapOf(
                "raadpleger" to listOf(role("raadpleger")),
                "coordinator" to listOf(role("coordinator")),
                "recordmanager" to listOf(role("recordmanager")),
                "beheerder" to listOf(role("beheerder")),
            ),
        )
        // ZAC-specific: deny all when rollen is empty (PABC zaaktype-level filtering)
        val preCheck = java.util.function.Predicate<Any> { request ->
            val rollen = try {
                val subject = request::class.java.getMethod("getSubject").invoke(request)
                val props = subject?.let { it::class.java.getMethod("getProperties").invoke(it) } as? Map<*, *>
                props?.get("rollen")
            } catch (_: Exception) { null }
            rollen !is Collection<*> || rollen.isNotEmpty()
        }
        clazz.getConstructor(
            String::class.java, String::class.java, OkHttpClient::class.java,
            Map::class.java, Map::class.java, java.util.function.Predicate::class.java
        ).newInstance(
            url, spicedbToken, OkHttpClient(),
            resourceTypeMap, roleAssignments, preCheck
        ) as AccessService
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("Backend 'spicedb' requires authzen-spicedb on the classpath.", e)
    }

    private fun createAuthzForceTransport(): AccessService = try {
        val clazz = Class.forName("com.dataversation.authzen.authzforce.AuthzForceAccessService")
        val url = requirePdpUrl()
        clazz.getConstructor(String::class.java, OkHttpClient::class.java)
            .newInstance(url, OkHttpClient()) as AccessService
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("Backend 'authzforce' requires authzen-authzforce on the classpath.", e)
    }

    private fun createAuthZenHttpTransport(): AccessService = try {
        val clazz = Class.forName("com.dataversation.authzen.http.AuthZenHttpAccessService")
        val url = requirePdpUrl()
        clazz.getConstructor(String::class.java, OkHttpClient::class.java)
            .newInstance(url, OkHttpClient()) as AccessService
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException(
            "Backend '$backend' requires com.dataversation.authzen.http.AuthZenHttpAccessService " +
                "on the classpath. Add the corresponding Maven dependency.",
            e
        )
    }

    private fun createAuthZenGrpcTransport(): AccessService = try {
        val clazz = Class.forName("com.dataversation.authzen.grpc.AuthZenGrpcAccessService")
        val url = requirePdpUrl()
        val useTls = url.startsWith("https://")
        val target = url.removePrefix("https://").removePrefix("http://")
        clazz.getConstructor(
            String::class.java,
            Boolean::class.javaPrimitiveType,
            javax.net.ssl.SSLSocketFactory::class.java
        ).newInstance(target, useTls, null) as AccessService
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException(
            "Backend '$backend' requires com.dataversation.authzen.grpc.AuthZenGrpcAccessService " +
                "on the classpath. Add the corresponding Maven dependency.",
            e
        )
    }

    companion object {
        private val LOG = Logger.getLogger(AccessServiceProducer::class.java.name)
    }
}
