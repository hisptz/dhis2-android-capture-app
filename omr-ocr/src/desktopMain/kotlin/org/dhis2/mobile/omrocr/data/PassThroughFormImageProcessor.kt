package org.dhis2.mobile.omrocr.data

import androidx.compose.ui.graphics.ImageBitmap
import org.dhis2.mobile.omrocr.domain.FormImageProcessor
import org.dhis2.mobile.omrocr.model.FormScanResult
import org.dhis2.mobile.omrocr.model.OmrFormConfig

class PassThroughFormImageProcessor : FormImageProcessor {
    override suspend fun process(
        image: ImageBitmap,
        config: OmrFormConfig,
    ): FormScanResult = FormScanResult(previewImage = image, values = emptyList())
}
