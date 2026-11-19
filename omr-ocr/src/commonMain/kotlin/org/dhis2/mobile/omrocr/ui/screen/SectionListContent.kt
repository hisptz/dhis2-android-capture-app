package org.dhis2.mobile.omrocr.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import org.dhis2.mobile.omrocr.model.FormSection
import org.dhis2.mobile.omrocr.resources.Res
import org.dhis2.mobile.omrocr.resources.form_scan_save_failed
import org.dhis2.mobile.omrocr.resources.form_scan_save_values
import org.dhis2.mobile.omrocr.resources.form_scan_sections_hint
import org.dhis2.mobile.omrocr.ui.component.SectionScanTile
import org.dhis2.mobile.omrocr.ui.state.FormScanStep
import org.dhis2.mobile.omrocr.ui.state.FormScanUiState
import org.dhis2.mobile.omrocr.ui.state.SectionScan
import org.dhis2.mobile.omrocr.ui.state.SectionScanUiModel
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.ButtonStyle
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.hisp.dhis.mobile.ui.designsystem.theme.TextColor
import org.jetbrains.compose.resources.stringResource

const val SAVE_SCANNED_VALUES_BUTTON_TAG = "SAVE_SCANNED_VALUES_BUTTON"

@Composable
fun SectionListContent(
    uiState: FormScanUiState,
    onCaptureSectionClick: (String) -> Unit,
    onViewPhotoClick: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(Spacing.Spacing16),
            verticalArrangement = Arrangement.spacedBy(Spacing.Spacing16),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.form_scan_sections_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextColor.OnSurfaceVariant,
                )
            }
            items(items = uiState.sections, key = { it.section.uid }) { model ->
                SectionScanTile(
                    model = model,
                    onCaptureClick = { onCaptureSectionClick(model.section.uid) },
                    onViewPhotoClick = { onViewPhotoClick(model.section.uid) },
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.Spacing16),
            verticalArrangement = Arrangement.spacedBy(Spacing.Spacing8),
        ) {
            SaveValuesButton(
                enabled = uiState.canSave,
                onClick = onSaveClick,
            )
            if (uiState.saveFailed) {
                SaveFailedText()
            }
        }
    }
}

@Composable
internal fun SaveValuesButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier.fillMaxWidth().testTag(SAVE_SCANNED_VALUES_BUTTON_TAG),
        style = ButtonStyle.FILLED,
        enabled = enabled,
        text = stringResource(Res.string.form_scan_save_values),
        icon = { Icon(imageVector = Icons.Filled.Save, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
internal fun SaveFailedText() {
    Text(
        text = stringResource(Res.string.form_scan_save_failed),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Preview
@Composable
private fun SectionListContentPreview() {
    DHIS2Theme {
        SectionListContent(
            uiState =
                FormScanUiState(
                    isLoadingSections = false,
                    step = FormScanStep.SectionList,
                    sections =
                        listOf(
                            SectionScanUiModel(
                                section = FormSection(uid = "a", displayName = "Antenatal care"),
                                scan = SectionScan(previewImage = ImageBitmap(400, 300), values = emptyList()),
                            ),
                            SectionScanUiModel(section = FormSection(uid = "b", displayName = "Immunisation")),
                        ),
                ),
            onCaptureSectionClick = {},
            onViewPhotoClick = {},
            onSaveClick = {},
        )
    }
}
