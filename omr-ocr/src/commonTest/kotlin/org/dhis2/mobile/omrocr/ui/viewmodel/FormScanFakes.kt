package org.dhis2.mobile.omrocr.ui.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import org.dhis2.mobile.omrocr.domain.FormImageProcessor
import org.dhis2.mobile.omrocr.domain.FormSectionsRepository
import org.dhis2.mobile.omrocr.domain.OmrFormConfigRepository
import org.dhis2.mobile.omrocr.domain.ScannedValuesRepository
import org.dhis2.mobile.omrocr.model.FormScanParameters
import org.dhis2.mobile.omrocr.model.FormScanResult
import org.dhis2.mobile.omrocr.model.FormSection
import org.dhis2.mobile.omrocr.model.OmrFormConfig
import org.dhis2.mobile.omrocr.model.SaveScannedValuesSummary
import org.dhis2.mobile.omrocr.model.ScannedDataValue

class FakeFormImageProcessor : FormImageProcessor {
    val results = ArrayDeque<FormScanResult>()
    var failure: Exception? = null

    override suspend fun process(
        image: ImageBitmap,
        config: OmrFormConfig,
    ): FormScanResult {
        failure?.let { throw it }
        return results.removeFirst()
    }
}

class FakeConfigRepository : OmrFormConfigRepository {
    var hasConfig = true

    override fun configFor(
        dataSetUid: String,
        sectionUid: String?,
    ): OmrFormConfig? =
        if (hasConfig) {
            OmrFormConfig(dataSetUid = dataSetUid, sectionUid = sectionUid, fields = emptyList())
        } else {
            null
        }
}

class FakeSectionsRepository : FormSectionsRepository {
    var sections: List<FormSection> = emptyList()
    var failure: Exception? = null

    override suspend fun sections(dataSetUid: String): List<FormSection> {
        failure?.let { throw it }
        return sections
    }
}

class FakeScannedValuesRepository : ScannedValuesRepository {
    val savedRequests = mutableListOf<Pair<FormScanParameters, List<ScannedDataValue>>>()
    var failure: Exception? = null

    override suspend fun saveValues(
        parameters: FormScanParameters,
        values: List<ScannedDataValue>,
    ): Result<SaveScannedValuesSummary> {
        savedRequests.add(parameters to values)
        return failure?.let { Result.failure(it) }
            ?: Result.success(SaveScannedValuesSummary(savedCount = values.size, skippedCount = 0))
    }
}
