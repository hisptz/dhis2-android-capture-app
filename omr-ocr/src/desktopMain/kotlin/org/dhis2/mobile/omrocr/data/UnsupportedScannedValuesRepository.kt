package org.dhis2.mobile.omrocr.data

import org.dhis2.mobile.omrocr.domain.ScannedValuesRepository
import org.dhis2.mobile.omrocr.model.FormScanParameters
import org.dhis2.mobile.omrocr.model.SaveScannedValuesSummary
import org.dhis2.mobile.omrocr.model.ScannedDataValue

class UnsupportedScannedValuesRepository : ScannedValuesRepository {
    override suspend fun saveValues(
        parameters: FormScanParameters,
        values: List<ScannedDataValue>,
    ): Result<SaveScannedValuesSummary> = Result.failure(UnsupportedOperationException("Saving scanned values is not available on desktop"))
}
