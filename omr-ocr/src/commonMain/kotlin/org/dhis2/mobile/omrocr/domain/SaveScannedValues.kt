package org.dhis2.mobile.omrocr.domain

import org.dhis2.mobile.commons.domain.UseCase
import org.dhis2.mobile.omrocr.model.FormScanParameters
import org.dhis2.mobile.omrocr.model.SaveScannedValuesSummary
import org.dhis2.mobile.omrocr.model.ScannedDataValue

class SaveScannedValues(
    private val repository: ScannedValuesRepository,
) : UseCase<SaveScannedValues.Input, SaveScannedValuesSummary> {
    data class Input(
        val parameters: FormScanParameters,
        val values: List<ScannedDataValue>,
    )

    override suspend fun invoke(input: Input): Result<SaveScannedValuesSummary> = repository.saveValues(input.parameters, input.values)
}
