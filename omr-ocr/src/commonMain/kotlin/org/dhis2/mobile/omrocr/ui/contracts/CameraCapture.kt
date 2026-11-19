package org.dhis2.mobile.omrocr.ui.contracts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

@Composable
expect fun CameraCapture(
    onImageCaptured: (ImageBitmap) -> Unit,
    onCaptureFailed: () -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier,
)
