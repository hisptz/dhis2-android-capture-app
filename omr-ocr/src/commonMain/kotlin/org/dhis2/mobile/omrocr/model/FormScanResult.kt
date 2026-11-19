package org.dhis2.mobile.omrocr.model

import androidx.compose.ui.graphics.ImageBitmap

data class FormScanResult(
    val previewImage: ImageBitmap,
    val values: List<ScannedDataValue>,
)
