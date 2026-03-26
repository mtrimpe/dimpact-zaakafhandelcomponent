/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy

import com.dataversation.authzen.AccessService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import nl.info.zac.policy.opa.OpaAccessService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Optional

/**
 * CDI producer that selects the [AccessService] implementation based on configuration.
 *
 * - `opa` (default): uses [OpaAccessService] which routes to OPA Data API
 * - `authzen-http`: loads [com.dataversation.authzen.http.AuthZenHttpAccessService] if on classpath
 * - `authzen-grpc`: loads [com.dataversation.authzen.grpc.AuthZenGrpcAccessService] if on classpath
 *
 * The HTTP and gRPC transports are optional dependencies — they're only loaded
 * if the corresponding Maven artifact is on the classpath AND configured as the backend.
 */
@ApplicationScoped
@AllOpen
@NoArgConstructor
class AccessServiceProducer @Inject constructor(
    @ConfigProperty(name = "AUTHORIZATION_SERVICE_BACKEND", defaultValue = "opa")
    private val backend: String,
    @ConfigProperty(name = "AUTHZEN_PDP_URL")
    private val authZenPdpUrl: Optional<String>,
    private val opaAccessService: OpaAccessService
) {
    @Produces
    @ApplicationScoped
    fun produce(): AccessService = when (backend.lowercase()) {
        "opa" -> opaAccessService
        "authzen-http" -> createTransport("com.dataversation.authzen.http.AuthZenHttpAccessService")
        "authzen-grpc" -> createTransport("com.dataversation.authzen.grpc.AuthZenGrpcAccessService")
        else -> throw IllegalArgumentException(
            "Unknown authorization backend: '$backend'. Must be 'opa', 'authzen-http', or 'authzen-grpc'."
        )
    }

    private fun requirePdpUrl(): String = authZenPdpUrl.orElseThrow {
        IllegalStateException("AUTHZEN_PDP_URL must be set when AUTHORIZATION_SERVICE_BACKEND=$backend")
    }

    private fun createTransport(className: String): AccessService = try {
        val clazz = Class.forName(className)
        clazz.getConstructor(String::class.java).newInstance(requirePdpUrl()) as AccessService
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException(
            "Backend '$backend' requires $className on the classpath. " +
                "Add the corresponding Maven dependency.",
            e
        )
    }
}
