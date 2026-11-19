package org.dhis2.mobile.omrocr.ui.contracts

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import org.dhis2.mobile.omrocr.resources.Res
import org.dhis2.mobile.omrocr.resources.form_scan_camera_unavailable
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun CameraCapture(
    onImageCaptured: (ImageBitmap) -> Unit,
    onCaptureFailed: () -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = stringResource(Res.string.form_scan_camera_unavailable))
    }
}
