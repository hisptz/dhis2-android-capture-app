package org.dhis2.mobile.omrocr.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.dhis2.mobile.commons.coroutine.Dispatcher
import org.dhis2.mobile.commons.error.DomainErrorMapper
import org.dhis2.mobile.commons.error.withDomainErrors
import org.dhis2.mobile.omrocr.domain.ScannedValuesRepository
import org.dhis2.mobile.omrocr.model.FormScanParameters
import org.dhis2.mobile.omrocr.model.SaveScannedValuesSummary
import org.dhis2.mobile.omrocr.model.ScannedDataValue
import org.hisp.dhis.android.core.D2

private const val OMR_COMMENT_PREFIX = "OMR: "

internal class ScannedValuesRepositoryImpl(
    private val d2: D2,
    private val domainErrorMapper: DomainErrorMapper,
    private val dispatcher: Dispatcher,
) : ScannedValuesRepository {
    override suspend fun saveValues(
        parameters: FormScanParameters,
        values: List<ScannedDataValue>,
    ): Result<SaveScannedValuesSummary> =
        try {
            withContext(dispatcher.io) {
                domainErrorMapper.withDomainErrors {
                    var savedCount = 0
                    var skippedCount = 0
                    values.forEach { scanned ->
                        if (scanned.value.isNotBlank() && saveValue(parameters, scanned)) {
                            savedCount++
                        } else {
                            skippedCount++
                        }
                    }
                    Result.success(SaveScannedValuesSummary(savedCount = savedCount, skippedCount = skippedCount))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun saveValue(
        parameters: FormScanParameters,
        scanned: ScannedDataValue,
    ): Boolean =
        try {
            val valueType =
                d2
                    .dataElementModule()
                    .dataElements()
                    .uid(scanned.dataElementUid)
                    .blockingGet()
                    ?.valueType()
            val validValue = valueType?.validator?.validate(scanned.value)?.getOrThrow() ?: scanned.value

            d2
                .dataValueModule()
                .dataValues()
                .value(
                    period = parameters.periodId,
                    organisationUnit = parameters.organisationUnitUid,
                    dataElement = scanned.dataElementUid,
                    categoryOptionCombo = scanned.categoryOptionComboUid,
                    attributeOptionCombo = parameters.attributeOptionComboUid,
                    sourceDataSet = parameters.dataSetUid,
                ).apply {
                    blockingSet(validValue)
                    setComment("$OMR_COMMENT_PREFIX$validValue")
                }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
}
