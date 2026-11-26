package org.dhis2.mobile.omrocr.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import org.dhis2.mobile.omrocr.resources.Res
import org.dhis2.mobile.omrocr.resources.form_scan_processed_image
import org.dhis2.mobile.omrocr.resources.form_scan_retake_photo
import org.dhis2.mobile.omrocr.resources.form_scan_values_read
import org.dhis2.mobile.omrocr.ui.state.SectionScan
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.ButtonStyle
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.hisp.dhis.mobile.ui.designsystem.theme.TextColor
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScanPreviewContent(
    sectionUid: String?,
    scan: SectionScan,
    canSave: Boolean,
    isSaving: Boolean,
    saveFailed: Boolean,
    onSaveClick: () -> Unit,
    onRetakeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSingleScan = sectionUid == null

    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.Spacing16),
        verticalArrangement = Arrangement.spacedBy(Spacing.Spacing16),
    ) {
        Image(
            bitmap = scan.previewImage,
            contentDescription = stringResource(Res.string.form_scan_processed_image),
            contentScale = ContentScale.Fit,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        Text(
            text = stringResource(Res.string.form_scan_values_read, scan.values.size),
            style = MaterialTheme.typography.bodyMedium,
            color = TextColor.OnSurface,
        )
        if (isSingleScan) {
            SaveValuesButton(
                enabled = canSave,
                onClick = onSaveClick,
            )
            if (saveFailed) {
                SaveFailedText()
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            style = if (isSingleScan) ButtonStyle.OUTLINED else ButtonStyle.FILLED,
            enabled = !isSaving,
            text = stringResource(Res.string.form_scan_retake_photo),
            icon = { Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null) },
            onClick = onRetakeClick,
        )
    }
}

@Preview
@Composable
private fun SingleScanPreviewContentPreview() {
    DHIS2Theme {
        ScanPreviewContent(
            sectionUid = null,
            scan = SectionScan(previewImage = ImageBitmap(300, 400), values = emptyList()),
            canSave = true,
            isSaving = false,
            saveFailed = false,
            onSaveClick = {},
            onRetakeClick = {},
        )
    }
}

@Preview
@Composable
private fun SectionScanPreviewContentPreview() {
    DHIS2Theme {
        ScanPreviewContent(
            sectionUid = "section",
            scan = SectionScan(previewImage = ImageBitmap(300, 400), values = emptyList()),
            canSave = true,
            isSaving = false,
            saveFailed = false,
            onSaveClick = {},
            onRetakeClick = {},
        )
    }
}
