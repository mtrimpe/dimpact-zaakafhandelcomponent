/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.policy

import com.dataversation.authzen.AccessService
import com.dataversation.authzen.model.EvaluationResponse
import com.dataversation.authzen.model.EvaluationsRequest
import com.dataversation.authzen.model.EvaluationsResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.Ondertekening
import nl.info.client.zgw.drc.model.generated.SoortEnum
import nl.info.client.zgw.model.createVerlenging
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.client.zgw.zrc.util.isVerlengd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.model.createEnkelvoudigInformatieObjectLock
import nl.info.zac.policy.output.createDocumentRechten
import nl.info.zac.policy.output.createOverigeRechten
import nl.info.zac.policy.output.createTaakRechten
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.createTaakZoekObject
import nl.info.zac.search.model.createZaakZoekObject
import java.net.URI
import java.time.LocalDate
import java.util.UUID

// Helpers for clean assertion access on EvaluationsRequest
private val EvaluationsRequest.subjectId get() = subject!!.id
private val EvaluationsRequest.subjectRollen get() = (subject!!.properties!!["rollen"] as Collection<*>).toSet()
private val EvaluationsRequest.subjectZaaktypen
    get() = subject!!.properties!!["zaaktypen"]?.let {
        (it as Collection<*>).toSet()
    }
private fun EvaluationsRequest.resourceProp(key: String) = resource!!.properties!![key]

/**
 * Convert a *Rechten fixture to an [EvaluationsResponse] by finding which boolean properties are true
 * and mapping them to evaluation decisions in the order defined by [ResourceActions].
 */
private fun toEvaluationsResponse(rechten: Any, resourceType: String): EvaluationsResponse {
    val actions = ResourceActions.BY_RESOURCE_TYPE[resourceType]!!
    val trueNames = rechten::class.members
        .filter { it.parameters.size == 1 && it.call(rechten) == true }
        .map { member ->
            member.name.replace(Regex("([a-z])([A-Z])")) {
                "${it.groupValues[1]}_${it.groupValues[2].lowercase()}"
            }
        }
        .toSet()
    return EvaluationsResponse(
        evaluations = actions.map { EvaluationResponse(decision = it in trueNames) }
    )
}

@Suppress("LargeClass")
class PolicyServiceTest : BehaviorSpec({
    val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val accessService = mockk<AccessService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val configurationService = mockk<ConfigurationService>()
    val loggedInUser = createLoggedInUser()
    val policyService = PolicyService(
        loggedInUserInstance,
        accessService,
        ztcClientService,
        enkelvoudigInformatieObjectLockService,
        zrcClientService,
        configurationService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Reading zaakrechten") {
        Given(
            """
            A logged-in with functional roles, application roles per zaaktype mappings,
            and a zaak, with PABC feature flag enabled
            """
        ) {
            val zaaktypeOmschrijving = "fakeZaaktype1"
            val applicationRolesForZaakType = setOf("fakeApplicationRole1", "fakeApplicationRole2")
            val zaak = createZaak(
                status = URI("https://example.com/status/${UUID.randomUUID()}")
            )
            val zaakType = createZaakType(
                omschrijving = zaaktypeOmschrijving
            )
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType()
            val expectedZaakRechten = createZaakRechten()
            val requestSlot = slot<EvaluationsRequest>()
            val loggedInUser = createLoggedInUser(
                roles = setOf("fakeRole1", "fakeRole2"),
                // obsolete and not used when PABC feature flag is enabled
                geautoriseerdeZaaktypen = null,
                applicationRolesPerZaaktype = setOf(
                    zaaktypeOmschrijving to applicationRolesForZaakType,
                    "fakeZaaktype2" to setOf("fakeApplicationRole3")
                ).toMap()
            )

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { accessService.evaluations(capture(requestSlot)) } returns toEvaluationsResponse(expectedZaakRechten, "zaak")
            every { configurationService.featureFlagPabcIntegration() } returns true

            When("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak)

                Then("the returned zaakrechten are correct") {
                    zaakRechten shouldBe expectedZaakRechten
                }

                And("the expected evaluation data is sent to the policy evaluation client") {
                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        resourceProp("open") shouldBe true
                        resourceProp("zaaktype") shouldBe zaakType.omschrijving
                        resourceProp("opgeschort") shouldBe zaak.isOpgeschort()
                        resourceProp("verlengd") shouldBe zaak.isVerlengd()
                        resourceProp("besloten") shouldBe false
                        resourceProp("intake") shouldBe false
                        resourceProp("heropend") shouldBe false
                    }
                    with(requestSlot.captured) {
                        subjectId shouldBe loggedInUser.id
                        subjectRollen shouldContainExactly applicationRolesForZaakType
                        subjectZaaktypen shouldBe setOf(zaakType.omschrijving)
                    }
                }
            }
        }

        Given("A logged-in user, a zaak and PABC feature flag disabled") {
            val zaak = createZaak(
                status = URI("https://example.com/status/${UUID.randomUUID()}")
            )
            val zaakType = createZaakType()
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType()
            val expectedZaakRechten = createZaakRechten()
            val requestSlot = slot<EvaluationsRequest>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { accessService.evaluations(capture(requestSlot)) } returns toEvaluationsResponse(expectedZaakRechten, "zaak")
            every { configurationService.featureFlagPabcIntegration() } returns false

            When("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak)

                Then("correct ZaakData is sent to OPA") {
                    zaakRechten shouldBe expectedZaakRechten
                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        resourceProp("open") shouldBe true
                        resourceProp("zaaktype") shouldBe zaakType.omschrijving
                        resourceProp("opgeschort") shouldBe zaak.isOpgeschort()
                        resourceProp("verlengd") shouldBe zaak.isVerlengd()
                        resourceProp("besloten") shouldBe false
                        resourceProp("intake") shouldBe false
                        resourceProp("heropend") shouldBe false
                    }
                }
            }
        }

        Given("locked zaak that has intake status and PABC feature flag enabled") {
            val zaak = createZaak(
                verlenging = createVerlenging(),
                status = URI("https://example.com/status/${UUID.randomUUID()}"),
            )
            val zaakType = createZaakType()
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType(omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_INTAKE)
            val expectedZaakRechten = createZaakRechten()
            val requestSlot = slot<EvaluationsRequest>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { accessService.evaluations(capture(requestSlot)) } returns toEvaluationsResponse(expectedZaakRechten, "zaak")
            every { configurationService.featureFlagPabcIntegration() } returns true

            When("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak)

                Then("correct ZaakData is sent to OPA") {
                    zaakRechten shouldBe expectedZaakRechten
                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        resourceProp("open") shouldBe true
                        resourceProp("zaaktype") shouldBe zaakType.omschrijving
                        resourceProp("opgeschort") shouldBe zaak.isOpgeschort()
                        resourceProp("verlengd") shouldBe zaak.isVerlengd()
                        resourceProp("besloten") shouldBe false
                        resourceProp("intake") shouldBe true
                        resourceProp("heropend") shouldBe false
                    }
                }
            }
        }

        Given("zaak with status that was reopened and PABC feature flag enabled") {
            val zaak = createZaak(
                verlenging = createVerlenging(),
                status = URI("https://example.com/${UUID.randomUUID()}")
            )
            val zaakType = createZaakType()
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType(omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_HEROPEND)
            val expectedZaakRechten = createZaakRechten()
            val requestSlot = slot<EvaluationsRequest>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { accessService.evaluations(capture(requestSlot)) } returns toEvaluationsResponse(expectedZaakRechten, "zaak")
            every { configurationService.featureFlagPabcIntegration() } returns true

            When("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak)

                Then("correct ZaakData is sent to OPA") {
                    zaakRechten shouldBe expectedZaakRechten
                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        resourceProp("open") shouldBe true
                        resourceProp("zaaktype") shouldBe zaakType.omschrijving
                        resourceProp("opgeschort") shouldBe zaak.isOpgeschort()
                        resourceProp("verlengd") shouldBe zaak.isVerlengd()
                        resourceProp("besloten") shouldBe false
                        resourceProp("intake") shouldBe false
                        resourceProp("heropend") shouldBe true
                    }
                }
            }
        }
    }

    Context("Reading zaakrechten for searching zaken") {
        Given("ZaakZoekObject and PABC feature flag enabled") {
            val zaakZoekObject = createZaakZoekObject().apply {
                this.setIndicatie(ZaakIndicatie.OPSCHORTING, true)
                this.setIndicatie(ZaakIndicatie.VERLENGD, true)
                this.setIndicatie(ZaakIndicatie.HEROPEND, true)
            }
            val expectedZaakRechten = createZaakRechten()
            val requestSlot = slot<EvaluationsRequest>()
            every { accessService.evaluations(capture(requestSlot)) } returns toEvaluationsResponse(expectedZaakRechten, "zaak")
            every { configurationService.featureFlagPabcIntegration() } returns true
            every { loggedInUserInstance.get() } returns createLoggedInUser()

            When("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)

                Then("correct ZaakData is sent to OPA") {
                    zaakRechten shouldBe expectedZaakRechten
                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        resourceProp("open") shouldBe true
                        resourceProp("zaaktype") shouldBe zaakZoekObject.zaaktypeOmschrijving
                        resourceProp("opgeschort") shouldBe true
                        resourceProp("verlengd") shouldBe true
                        resourceProp("heropend") shouldBe true
                        // We don't set these two
                        resourceProp("besloten") shouldBe null
                        resourceProp("intake") shouldBe null
                    }
                }
            }
        }
    }

    Context("Reading taakrechten") {
        Given(
            """
            An open CMMN task as part of a zaak and a logged in user with application roles for the zaaktype of the zaak
            and PABC feature flag enabled
            """
        ) {
            val zaakType = createZaakType()
            val testTask = createTestTask(
                caseVariables = mapOf("zaaktypeOmschrijving" to zaakType.omschrijving)
            )
            val userApplicationRolesForZaakType = setOf("fakeApplicationRole1", "fakeApplicationRole2")
            val loggedInUser = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    zaakType.omschrijving to userApplicationRolesForZaakType
                )
            )
            val expectedTaakRechten = createTaakRechten()
            val requestSlot = slot<EvaluationsRequest>()

            every { accessService.evaluations(capture(requestSlot)) } returns toEvaluationsResponse(expectedTaakRechten, "taak")
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configurationService.featureFlagPabcIntegration() } returns true

            When("task policy rights are requested for the task") {
                val taskPermissions = policyService.readTaakRechten(testTask)

                Then("the response contains the expected taakrechten") {
                    taskPermissions shouldBe expectedTaakRechten
                }
                And("the correct data is sent to the OPA evaluation client") {
                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        resourceProp("open") shouldBe true
                        resourceProp("zaaktype") shouldBe zaakType.omschrijving
                    }
                    with(requestSlot.captured) {
                        subjectId shouldBe loggedInUser.id
                        subjectRollen shouldContainExactlyInAnyOrder userApplicationRolesForZaakType
                        subjectZaaktypen shouldContainExactly listOf(zaakType.omschrijving)
                    }
                }
            }
        }
    }

    Context("Reading taakrechten for searching tasks") {
        Given(
            """
            An open CMMN task as part of a zaak and a logged in user with application roles for the zaaktype of the zaak
            and PABC feature flag enabled
            """
        ) {
            val zaakType = createZaakType()
            val taakZoekObject = createTaakZoekObject(
                zaaktypeOmschrijving = zaakType.omschrijving
            )
            val userApplicationRolesForZaakType = setOf("fakeApplicationRole1", "fakeApplicationRole2")
            val loggedInUser = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    zaakType.omschrijving to userApplicationRolesForZaakType
                )
            )
            val expectedTaakRechten = createTaakRechten()
            val requestSlot = slot<EvaluationsRequest>()

            every { accessService.evaluations(capture(requestSlot)) } returns toEvaluationsResponse(expectedTaakRechten, "taak")
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configurationService.featureFlagPabcIntegration() } returns true

            When("task policy rights are requested for the task search object") {
                val taskPermissions = policyService.readTaakRechten(taakZoekObject)

                Then("the response contains the expected taakrechten") {
                    taskPermissions shouldBe expectedTaakRechten
                }
                And("the correct data is sent to the OPA evaluation client") {
                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        // 'open' is always false for task search objects
                        resourceProp("open") shouldBe false
                        resourceProp("zaaktype") shouldBe zaakType.omschrijving
                    }
                    with(requestSlot.captured) {
                        subjectId shouldBe loggedInUser.id
                        subjectRollen shouldContainExactlyInAnyOrder userApplicationRolesForZaakType
                        subjectZaaktypen shouldContainExactly listOf(zaakType.omschrijving)
                    }
                }
            }
        }
    }

    Context("Reading werklijstrechten") {
        Given("A logged-in user with functional roles, roles mappings and PABC feature flag enabled") {
            val expectedWerklijstRechten = createWerklijstRechten()
            val requestSlot = slot<EvaluationsRequest>()
            val zaaktype1Omschrijving = "fakeZaaktype1"
            val zaaktype2Omschrijving = "fakeZaaktype2"
            val applicationRolesForZaakType1 = setOf("fakeApplicationRole1", "fakeApplicationRole2")
            val applicationRolesForZaakType2 = setOf("fakeApplicationRole3")
            val loggedInUser = createLoggedInUser(
                roles = setOf("fakeRole1", "fakeRole2"),
                // obsolete and not used when PABC feature flag is enabled
                geautoriseerdeZaaktypen = null,
                applicationRolesPerZaaktype = setOf(
                    zaaktype1Omschrijving to applicationRolesForZaakType1,
                    zaaktype2Omschrijving to applicationRolesForZaakType2
                ).toMap()
            )
            every {
                accessService.evaluations(capture(requestSlot))
            } returns toEvaluationsResponse(expectedWerklijstRechten, "werklijst")
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configurationService.featureFlagPabcIntegration() } returns true

            When("the werklijst rechten are requested") {
                val werklijstRechten = policyService.readWerklijstRechten()

                Then("the evaluation client is called with the correct arguments") {
                    werklijstRechten shouldBe expectedWerklijstRechten
                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        subjectId shouldBe loggedInUser.id
                        // this policy check is not zaaktype-specific,
                        // so the roles should be the union of all application roles for which at least one zaaktype is authorized
                        subjectRollen shouldContainExactly
                            applicationRolesForZaakType1 + applicationRolesForZaakType2
                        // this policy check is not zaaktype-specific, so zaaktypen should be null
                        subjectZaaktypen shouldBe null
                    }
                }
            }
        }

        Given("A logged-in user and PABC feature flag disabled") {
            val expectedWerklijstRechten = createWerklijstRechten()
            val requestSlot = slot<EvaluationsRequest>()
            every {
                accessService.evaluations(capture(requestSlot))
            } returns toEvaluationsResponse(expectedWerklijstRechten, "werklijst")
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configurationService.featureFlagPabcIntegration() } returns false

            When("the werklijst rechten are requested") {
                val werklijstRechten = policyService.readWerklijstRechten()

                Then("the evaluation client is called with the correct arguments") {
                    werklijstRechten shouldBe expectedWerklijstRechten
                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        subjectId shouldBe loggedInUser.id
                        subjectRollen shouldBe loggedInUser.roles
                        subjectZaaktypen shouldBe loggedInUser.geautoriseerdeZaaktypen
                    }
                }
            }
        }
    }

    Context("Reading documentrechten") {
        Given("Unsigned information object and PABC feature flag enabled") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val userApplicationRolesForZaakType = setOf("fakeApplicationRole1", "fakeApplicationRole2")
            val loggedInUser = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    zaakType.omschrijving to userApplicationRolesForZaakType
                )
            )
            val enkelvoudigInformatieobject = createEnkelvoudigInformatieObject()
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val expectedDocumentRights = createDocumentRechten()
            val requestSlot = slot<EvaluationsRequest>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { accessService.evaluations(capture(requestSlot)) } returns toEvaluationsResponse(expectedDocumentRights, "document")
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configurationService.featureFlagPabcIntegration() } returns true

            When("document policy rights are requested") {
                val documentRights = policyService.readDocumentRechten(
                    enkelvoudigInformatieobject,
                    enkelvoudigInformatieObjectLock,
                    zaak
                )

                Then("the correct data is sent to OPA") {
                    documentRights shouldBe expectedDocumentRights

                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        resourceProp("definitief") shouldBe false
                        resourceProp("vergrendeld") shouldBe false
                        resourceProp("ondertekend") shouldBe false
                        resourceProp("vergrendeld_door") shouldBe null
                        resourceProp("zaaktype") shouldBe zaakType.omschrijving
                        resourceProp("zaak_open") shouldBe true
                    }
                    with(requestSlot.captured) {
                        subjectId shouldBe loggedInUser.id
                        subjectRollen shouldContainExactlyInAnyOrder userApplicationRolesForZaakType
                        subjectZaaktypen shouldContainExactly listOf(zaakType.omschrijving)
                    }
                }
            }
        }

        Given("signed and locked information object and PABC feature flag enabled") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val userApplicationRolesForZaakType = setOf("fakeApplicationRole1")
            val loggedInUser = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    zaakType.omschrijving to userApplicationRolesForZaakType
                )
            )
            val enkelvoudigInformatieobject = createEnkelvoudigInformatieObject(locked = true).apply {
                ondertekening = Ondertekening().apply {
                    soort = SoortEnum.ANALOOG
                    datum = LocalDate.now()
                }
            }
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val expectedDocumentRights = createDocumentRechten()
            val requestSlot = slot<EvaluationsRequest>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every {
                accessService.evaluations(capture(requestSlot))
            } returns toEvaluationsResponse(expectedDocumentRights, "document")
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configurationService.featureFlagPabcIntegration() } returns true

            When("document policy rights are requested") {
                val documentRights = policyService.readDocumentRechten(
                    enkelvoudigInformatieobject,
                    enkelvoudigInformatieObjectLock,
                    zaak
                )

                Then("the correct data is sent to OPA") {
                    documentRights shouldBe expectedDocumentRights

                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        resourceProp("definitief") shouldBe false
                        resourceProp("vergrendeld") shouldBe true
                        resourceProp("ondertekend") shouldBe true
                        resourceProp("vergrendeld_door") shouldBe null
                        resourceProp("zaaktype") shouldBe zaakType.omschrijving
                        resourceProp("zaak_open") shouldBe true
                    }
                    with(requestSlot.captured) {
                        subjectId shouldBe loggedInUser.id
                        subjectRollen shouldContainExactlyInAnyOrder userApplicationRolesForZaakType
                        subjectZaaktypen shouldContainExactly listOf(zaakType.omschrijving)
                    }
                }
            }
        }

        Given("Unsigned information object and PABC feature flag disabled") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val enkelvoudigInformatieobject = createEnkelvoudigInformatieObject()
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val expectedDocumentRights = createDocumentRechten()
            val requestSlot = slot<EvaluationsRequest>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { accessService.evaluations(capture(requestSlot)) } returns toEvaluationsResponse(expectedDocumentRights, "document")
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configurationService.featureFlagPabcIntegration() } returns false

            When("document policy rights are requested") {
                val documentRights = policyService.readDocumentRechten(
                    enkelvoudigInformatieobject,
                    enkelvoudigInformatieObjectLock,
                    zaak
                )

                Then("the correct data is sent to OPA") {
                    documentRights shouldBe expectedDocumentRights

                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        resourceProp("definitief") shouldBe false
                        resourceProp("vergrendeld") shouldBe false
                        resourceProp("ondertekend") shouldBe false
                        resourceProp("vergrendeld_door") shouldBe null
                        resourceProp("zaaktype") shouldBe zaakType.omschrijving
                        resourceProp("zaak_open") shouldBe true
                    }
                    with(requestSlot.captured) {
                        subjectId shouldBe loggedInUser.id
                        subjectRollen shouldBe loggedInUser.roles
                        subjectZaaktypen shouldBe loggedInUser.geautoriseerdeZaaktypen
                    }
                }
            }
        }

        Given("signed and locked information object and PABC feature flag disabled") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val enkelvoudigInformatieobject = createEnkelvoudigInformatieObject(locked = true).apply {
                ondertekening = Ondertekening().apply {
                    soort = SoortEnum.ANALOOG
                    datum = LocalDate.now()
                }
            }
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val expectedDocumentRights = createDocumentRechten()
            val requestSlot = slot<EvaluationsRequest>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every {
                accessService.evaluations(capture(requestSlot))
            } returns toEvaluationsResponse(expectedDocumentRights, "document")
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configurationService.featureFlagPabcIntegration() } returns false

            When("document policy rights are requested") {
                val documentRights = policyService.readDocumentRechten(
                    enkelvoudigInformatieobject,
                    enkelvoudigInformatieObjectLock,
                    zaak
                )

                Then("the correct data is sent to OPA") {
                    documentRights shouldBe expectedDocumentRights

                    verify(exactly = 1) {
                        accessService.evaluations(any<EvaluationsRequest>())
                    }
                    with(requestSlot.captured) {
                        resourceProp("definitief") shouldBe false
                        resourceProp("vergrendeld") shouldBe true
                        resourceProp("ondertekend") shouldBe true
                        resourceProp("vergrendeld_door") shouldBe null
                        resourceProp("zaaktype") shouldBe zaakType.omschrijving
                        resourceProp("zaak_open") shouldBe true
                    }
                }
            }
        }
    }

    Context("Reading overige rechten") {
        val functionalRoles = setOf("fakeRole1", "fakeRole2")

        Given("A logged-in user with application roles per zaaktype with PABC integration enabled") {
            val zaaktype = "test-zaaktype"
            val pabcRolesForZaakType = setOf("applicationRole1", "applicationRole2")
            val loggedInUserWithMappings = LoggedInUser(
                id = "user1",
                firstName = "Given",
                lastName = "Family",
                displayName = "Full Name",
                email = "user@example.com",
                roles = functionalRoles,
                groupIds = emptySet(),
                geautoriseerdeZaaktypen = setOf("zaakType1", "zaakType2"),
                applicationRolesPerZaaktype = mapOf(zaaktype to pabcRolesForZaakType)
            )

            val requestSlot = slot<EvaluationsRequest>()
            val expected = createOverigeRechten()
            every { loggedInUserInstance.get() } returns loggedInUserWithMappings
            every { accessService.evaluations(capture(requestSlot)) } returns toEvaluationsResponse(expected, "application")
            every { configurationService.featureFlagPabcIntegration() } returns true

            When("calling readOverigeRechten with a zaaktype") {
                val actual = policyService.readOverigeRechten(zaaktype)

                Then("OPA receives rollen from PABC for that zaaktype and zaaktypen contains only that zaaktype") {
                    actual shouldBe expected

                    verify(exactly = 1) { accessService.evaluations(any()) }

                    with(requestSlot.captured) {
                        subjectId shouldBe loggedInUserWithMappings.id
                        subjectRollen shouldBe pabcRolesForZaakType
                        subjectZaaktypen shouldBe setOf(zaaktype)
                    }
                }
            }
        }

        Given("A logged-in user with authorized zaaktypes without PABC integration enabled") {
            val roles = setOf("fakeRole1", "fakeRole2")
            val authorizedZaaktypes = setOf("zaaktype1", "zaaktype2")
            val loggedInUserLegacy = LoggedInUser(
                id = "user1",
                firstName = null,
                lastName = null,
                displayName = null,
                email = null,
                roles = roles,
                groupIds = emptySet(),
                geautoriseerdeZaaktypen = authorizedZaaktypes,
                applicationRolesPerZaaktype = emptyMap()
            )

            val requestSlot = slot<EvaluationsRequest>()
            val expected = createOverigeRechten()
            every { loggedInUserInstance.get() } returns loggedInUserLegacy
            every { accessService.evaluations(capture(requestSlot)) } returns toEvaluationsResponse(expected, "application")
            every { configurationService.featureFlagPabcIntegration() } returns false

            When("calling readOverigeRechten without a zaaktype") {
                val actual = policyService.readOverigeRechten(null)

                Then("OPA receives functional roles and original geautoriseerde zaaktypen") {
                    actual shouldBe expected

                    verify(exactly = 1) { accessService.evaluations(any()) }

                    with(requestSlot.captured) {
                        subjectRollen shouldBe roles
                        subjectZaaktypen shouldBe authorizedZaaktypes
                    }
                }
            }

            When("calling readOverigeRechten with a zaaktype") {
                clearMocks(accessService, answers = false, recordedCalls = true, exclusionRules = false)
                val actual = policyService.readOverigeRechten("redundant-zaaktype")

                Then("OPA receives roles with the authorized zaaktypes") {
                    actual shouldBe expected

                    verify(exactly = 1) { accessService.evaluations(any()) }

                    with(requestSlot.captured) {
                        subjectRollen shouldBe roles
                        subjectZaaktypen shouldBe authorizedZaaktypes
                    }
                }
            }
        }
    }
})
