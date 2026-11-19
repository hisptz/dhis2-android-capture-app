package org.dhis2.mobile.omrocr.domain

import androidx.compose.ui.graphics.ImageBitmap
import org.dhis2.mobile.omrocr.model.FormScanResult
import org.dhis2.mobile.omrocr.model.OmrFormConfig

interface FormImageProcessor {
    suspend fun process(
        image: ImageBitmap,
        config: OmrFormConfig,
    ): FormScanResult
}
