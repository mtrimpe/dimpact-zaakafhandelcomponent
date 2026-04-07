/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy

import com.dataversation.authzen.AccessService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.util.TaskUtil
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isHeropend
import nl.info.client.zgw.zrc.util.isIntake
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.client.zgw.zrc.util.isVerlengd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import nl.info.zac.enkelvoudiginformatieobject.util.isSigned
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.input.DocumentData
import nl.info.zac.policy.input.DocumentInput
import nl.info.zac.policy.input.NotitieInput
import nl.info.zac.policy.input.OverigeInput
import nl.info.zac.policy.input.TaakData
import nl.info.zac.policy.input.TaakInput
import nl.info.zac.policy.input.UserInput
import nl.info.zac.policy.input.WerklijstInput
import nl.info.zac.policy.input.ZaakData
import nl.info.zac.policy.input.ZaakInput
import nl.info.zac.policy.output.DocumentRechten
import nl.info.zac.policy.output.NotitieRechten
import nl.info.zac.policy.output.OverigeRechten
import nl.info.zac.policy.output.TaakRechten
import nl.info.zac.policy.output.WerklijstRechten
import nl.info.zac.policy.output.ZaakRechten
import nl.info.zac.search.model.DocumentIndicatie
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.zoekobject.DocumentZoekObject
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.task.api.TaskInfo

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class PolicyService @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val accessService: AccessService,
    private val ztcClientService: ZtcClientService,
    private val lockService: EnkelvoudigInformatieObjectLockService,
    private val zrcClientService: ZrcClientService,
    private val configurationService: ConfigurationService
) {
    /**
     * Read 'overige' permissions.
     *
     * @param zaaktypeDescription Optional zaaktype description to include in the input. In the legacy
     * non-PABC IAM architecture it is not used but in the new PABC-based IAM architecture it is,
     * but only for those 'overige rechten' permissions that are zaaktype-specific.
     */
    fun readOverigeRechten(zaaktypeDescription: String? = null) =
        OverigeInput(
            loggedInUser = loggedInUserInstance.get(),
            zaaktype = zaaktypeDescription,
            featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
        ).evaluate<OverigeRechten>()

    fun readZaakRechten(zaak: Zaak): ZaakRechten {
        val zaakType = ztcClientService.readZaaktype(zaak.zaaktype)
        return readZaakRechten(zaak, zaakType)
    }

    fun readZaakRechten(zaak: Zaak, zaaktype: ZaakType): ZaakRechten {
        val statusType = zaak.status?.let {
            zrcClientService.readStatus(it).statustype
                .let(ztcClientService::readStatustype)
        }
        val zaakData = ZaakData(
            open = zaak.isOpen(),
            zaaktype = zaaktype.getOmschrijving(),
            opgeschort = zaak.isOpgeschort(),
            verlengd = zaak.isVerlengd(),
            besloten = zaaktype.getBesluittypen()?.isNotEmpty() == true,
            intake = statusType?.isIntake(),
            heropend = statusType?.isHeropend()
        )
        return ZaakInput(
            loggedInUser = loggedInUserInstance.get(),
            zaakData = zaakData,
            featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
        ).evaluate()
    }

    fun readZaakRechtenForZaakZoekObject(zaakZoekObject: ZaakZoekObject): ZaakRechten {
        val zaakData = ZaakData(
            open = !zaakZoekObject.isAfgehandeld,
            zaaktype = zaakZoekObject.zaaktypeOmschrijving,
            opgeschort = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.OPSCHORTING),
            verlengd = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.VERLENGD),
            heropend = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.HEROPEND),
            // not taken into account when searching for a zaak
            intake = null,
            // not taken into account when searching for a zaak
            besloten = null
        )
        return ZaakInput(
            loggedInUser = loggedInUserInstance.get(),
            zaakData = zaakData,
            featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
        ).evaluate()
    }

    fun readDocumentRechten(enkelvoudigInformatieobject: EnkelvoudigInformatieObject, zaak: Zaak? = null) =
        readDocumentRechten(
            enkelvoudigInformatieobject = enkelvoudigInformatieobject,
            lock = lockService.findLock(enkelvoudigInformatieobject.getUrl().extractUuid()),
            zaak = zaak
        )

    fun readDocumentRechten(
        enkelvoudigInformatieobject: EnkelvoudigInformatieObject,
        lock: EnkelvoudigInformatieObjectLock?,
        zaak: Zaak?
    ): DocumentRechten {
        val documentData = DocumentData(
            definitief = enkelvoudigInformatieobject.getStatus() == StatusEnum.DEFINITIEF,
            vergrendeld = enkelvoudigInformatieobject.getLocked(),
            vergrendeldDoor = lock?.userId,
            ondertekend = enkelvoudigInformatieobject.isSigned(),
            zaakOpen = zaak?.isOpen() ?: false,
            zaaktype = zaak?.let { ztcClientService.readZaaktype(it.getZaaktype()).getOmschrijving() }
        )
        return DocumentInput(
            loggedInUser = loggedInUserInstance.get(),
            documentData = documentData,
            featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
        ).evaluate()
    }

    fun readDocumentRechten(enkelvoudigInformatieobject: DocumentZoekObject): DocumentRechten {
        val documentData = DocumentData(
            definitief = StatusEnum.DEFINITIEF == enkelvoudigInformatieobject.getStatus(),
            vergrendeld = enkelvoudigInformatieobject.isIndicatie(DocumentIndicatie.VERGRENDELD),
            vergrendeldDoor = enkelvoudigInformatieobject.vergrendeldDoorGebruikersnaam,
            zaakOpen = !enkelvoudigInformatieobject.isZaakAfgehandeld,
            zaaktype = enkelvoudigInformatieobject.zaaktypeOmschrijving,
            ondertekend = enkelvoudigInformatieobject.ondertekeningDatum != null
        )
        return DocumentInput(
            loggedInUser = loggedInUserInstance.get(),
            documentData = documentData,
            featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
        ).evaluate()
    }

    fun readTaakRechten(taskInfo: TaskInfo): TaakRechten {
        val zaaktypeOmschrijving = TaakVariabelenService.readZaaktypeOmschrijving(taskInfo)
        return readTaakRechten(taskInfo, zaaktypeOmschrijving)
    }

    fun readTaakRechten(
        taskInfo: TaskInfo,
        zaaktypeOmschrijving: String
    ): TaakRechten {
        val taakData = TaakData(
            open = TaskUtil.isOpen(taskInfo),
            zaaktype = zaaktypeOmschrijving
        )
        return TaakInput(
            loggedInUser = loggedInUserInstance.get(),
            taakData = taakData,
            featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
        ).evaluate()
    }

    fun readTaakRechten(taakZoekObject: TaakZoekObject): TaakRechten {
        val taakData = TaakData(
            zaaktype = taakZoekObject.zaaktypeOmschrijving
        )
        return TaakInput(
            loggedInUser = loggedInUserInstance.get(),
            taakData = taakData,
            featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
        ).evaluate()
    }

    fun readNotitieRechten(): NotitieRechten =
        NotitieInput(
            loggedInUser = loggedInUserInstance.get(),
            featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
        ).evaluate()

    fun readWerklijstRechten(): WerklijstRechten =
        WerklijstInput(
            loggedInUser = loggedInUserInstance.get(),
            featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
        ).evaluate()

    @Deprecated(
        "In PABC-based authorisation, the concept of being authorised for a zaaktype is meaningless, " +
            "since a user is always authorised for a zaaktype _for specific application roles_."
    )
    fun isAuthorisedForZaaktype(zaakTypeOmschrijving: String) =
        if (configurationService.featureFlagPabcIntegration()) {
            true
        } else {
            loggedInUserInstance.get().isAuthorisedForZaaktype(zaakTypeOmschrijving)
        }

    // ─── Single-action enforcement ───

    fun assertOverigeActionAllowed(actionName: String) = assertOverigeActionAllowed(actionName, null)

    fun assertOverigeActionAllowed(actionName: String, zaaktypeDescription: String?) =
        assertPolicy(
            OverigeInput(
                loggedInUser = loggedInUserInstance.get(),
                zaaktype = zaaktypeDescription,
                featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
            ).checkAllowed(actionName)
        )

    fun assertZaakActionAllowed(actionName: String, zaak: Zaak, zaakType: ZaakType) {
        val statusType = zaak.status?.let {
            zrcClientService.readStatus(it).statustype.let(ztcClientService::readStatustype)
        }
        assertPolicy(
            ZaakInput(
                loggedInUser = loggedInUserInstance.get(),
                zaakData = ZaakData(
                    open = zaak.isOpen(),
                    zaaktype = zaakType.getOmschrijving(),
                    opgeschort = zaak.isOpgeschort(),
                    verlengd = zaak.isVerlengd(),
                    besloten = zaakType.getBesluittypen()?.isNotEmpty() == true,
                    intake = statusType?.isIntake(),
                    heropend = statusType?.isHeropend()
                ),
                featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
            ).checkAllowed(actionName)
        )
    }

    fun assertZaakActionAllowed(actionName: String, zaak: Zaak) {
        val zaakType = ztcClientService.readZaaktype(zaak.zaaktype)
        assertZaakActionAllowed(actionName, zaak, zaakType)
    }

    fun assertDocumentActionAllowed(
        actionName: String,
        enkelvoudigInformatieobject: EnkelvoudigInformatieObject,
        zaak: Zaak? = null
    ) {
        val lock = lockService.findLock(enkelvoudigInformatieobject.getUrl().extractUuid())
        assertPolicy(
            DocumentInput(
                loggedInUser = loggedInUserInstance.get(),
                documentData = DocumentData(
                    definitief = enkelvoudigInformatieobject.getStatus() == StatusEnum.DEFINITIEF,
                    vergrendeld = enkelvoudigInformatieobject.getLocked(),
                    vergrendeldDoor = lock?.userId,
                    ondertekend = enkelvoudigInformatieobject.isSigned(),
                    zaakOpen = zaak?.isOpen() ?: false,
                    zaaktype = zaak?.let { ztcClientService.readZaaktype(it.getZaaktype()).getOmschrijving() }
                ),
                featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
            ).checkAllowed(actionName)
        )
    }

    fun assertTaakActionAllowed(actionName: String, taskInfo: TaskInfo) {
        val zaaktypeOmschrijving = TaakVariabelenService.readZaaktypeOmschrijving(taskInfo)
        assertPolicy(
            TaakInput(
                loggedInUser = loggedInUserInstance.get(),
                taakData = TaakData(
                    open = TaskUtil.isOpen(taskInfo),
                    zaaktype = zaaktypeOmschrijving
                ),
                featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
            ).checkAllowed(actionName)
        )
    }

    fun assertWerklijstActionAllowed(actionName: String) =
        assertPolicy(
            WerklijstInput(
                loggedInUser = loggedInUserInstance.get(),
                featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
            ).checkAllowed(actionName)
        )

    fun assertNotitieActionAllowed(actionName: String) =
        assertPolicy(
            NotitieInput(
                loggedInUser = loggedInUserInstance.get(),
                featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()
            ).checkAllowed(actionName)
        )

    /**
     * Evaluate a single action for this input. Used for server-side enforcement
     * where only one permission needs to be checked.
     */
    private fun UserInput.checkAllowed(actionName: String): Boolean {
        val request = this.toEvaluationRequest(actionName)
        return accessService.evaluation(request).decision
    }

    private inline fun <reified T> UserInput.evaluate(): T {
        val request = this.toEvaluationsRequest()
        val response = accessService.evaluations(request)
        val resourceType = this.extractAuthZenResource().type
        val actions = ResourceActions.BY_RESOURCE_TYPE[resourceType]
            ?: throw IllegalArgumentException("Unknown resource type: $resourceType")
        val decisions = response.toDecisions(actions)
        return rechtenFromEvaluations(decisions)
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> rechtenFromEvaluations(
        decisions: Map<String, Boolean>
    ): T = when (T::class) {
        ZaakRechten::class -> ZaakRechten.fromEvaluations(decisions)
        TaakRechten::class -> TaakRechten.fromEvaluations(decisions)
        DocumentRechten::class -> DocumentRechten.fromEvaluations(decisions)
        NotitieRechten::class -> NotitieRechten.fromEvaluations(decisions)
        OverigeRechten::class -> OverigeRechten.fromEvaluations(decisions)
        WerklijstRechten::class -> WerklijstRechten.fromEvaluations(decisions)
        else -> throw IllegalArgumentException("Unknown rechten type: ${T::class}")
    } as T
}

/**
 * Assert that the given policy is true.
 * If it is not, throw a [PolicyException].
 */
fun assertPolicy(policy: Boolean) {
    if (!policy) {
        throw PolicyException()
    }
}
