package org.dhis2.mobile.omrocr.domain

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CancellationException
import org.dhis2.mobile.commons.domain.UseCase
import org.dhis2.mobile.omrocr.model.FormScanResult

class ReadScannedForm(
    private val configRepository: OmrFormConfigRepository,
    private val formImageProcessor: FormImageProcessor,
) : UseCase<ReadScannedForm.Input, FormScanResult> {
    data class Input(
        val dataSetUid: String,
        val sectionUid: String?,
        val image: ImageBitmap,
    )

    override suspend fun invoke(input: Input): Result<FormScanResult> =
        try {
            val config = configRepository.configFor(input.dataSetUid, input.sectionUid)
            if (config == null) {
                Result.failure(
                    IllegalStateException(
                        "No OMR configuration for data set ${input.dataSetUid} and section ${input.sectionUid}",
                    ),
                )
            } else {
                Result.success(formImageProcessor.process(input.image, config))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
