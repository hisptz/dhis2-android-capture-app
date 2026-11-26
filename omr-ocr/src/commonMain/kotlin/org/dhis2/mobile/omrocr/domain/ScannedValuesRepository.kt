package org.dhis2.mobile.omrocr.domain

import org.dhis2.mobile.omrocr.model.FormScanParameters
import org.dhis2.mobile.omrocr.model.SaveScannedValuesSummary
import org.dhis2.mobile.omrocr.model.ScannedDataValue

interface ScannedValuesRepository {
    suspend fun saveValues(
        parameters: FormScanParameters,
        values: List<ScannedDataValue>,
    ): Result<SaveScannedValuesSummary>
}
