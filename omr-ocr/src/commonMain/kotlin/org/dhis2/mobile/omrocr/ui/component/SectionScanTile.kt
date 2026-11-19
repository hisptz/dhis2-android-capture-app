package org.dhis2.mobile.omrocr.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.dhis2.mobile.omrocr.model.FormSection
import org.dhis2.mobile.omrocr.resources.Res
import org.dhis2.mobile.omrocr.resources.form_scan_hide_photo
import org.dhis2.mobile.omrocr.resources.form_scan_open_photo
import org.dhis2.mobile.omrocr.resources.form_scan_show_photo
import org.dhis2.mobile.omrocr.resources.form_scan_take_section_photo
import org.dhis2.mobile.omrocr.resources.form_scan_values_read
import org.dhis2.mobile.omrocr.ui.state.SectionScan
import org.dhis2.mobile.omrocr.ui.state.SectionScanUiModel
import org.hisp.dhis.mobile.ui.designsystem.component.ExpandShrinkAnimatedVisibility
import org.hisp.dhis.mobile.ui.designsystem.component.IconButton
import org.hisp.dhis.mobile.ui.designsystem.component.IconButtonStyle
import org.hisp.dhis.mobile.ui.designsystem.theme.Border
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.Outline
import org.hisp.dhis.mobile.ui.designsystem.theme.Shape
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.hisp.dhis.mobile.ui.designsystem.theme.SurfaceColor
import org.hisp.dhis.mobile.ui.designsystem.theme.TextColor
import org.jetbrains.compose.resources.stringResource

const val SECTION_TILE_TAG = "SECTION_TILE_"
const val SECTION_CAPTURE_TAG = "SECTION_CAPTURE_"
const val SECTION_TOGGLE_TAG = "SECTION_TOGGLE_"
const val SECTION_THUMBNAIL_TAG = "SECTION_THUMBNAIL_"

private val THUMBNAIL_HEIGHT = 160.dp

@Composable
fun SectionScanTile(
    model: SectionScanUiModel,
    onCaptureClick: () -> Unit,
    onToggleExpanded: () -> Unit,
    onThumbnailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sectionUid = model.section.uid
    val sectionName = model.section.displayName
    val scan = model.scan
    val tileClickLabel =
        when {
            scan == null -> stringResource(Res.string.form_scan_take_section_photo, sectionName)
            model.isExpanded -> stringResource(Res.string.form_scan_hide_photo)
            else -> stringResource(Res.string.form_scan_show_photo)
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shape.Small)
                .background(SurfaceColor.SurfaceBright)
                .border(Border.Thin, Outline.Light, Shape.Small)
                .clickable(
                    role = Role.Button,
                    onClickLabel = tileClickLabel,
                    onClick = if (scan == null) onCaptureClick else onToggleExpanded,
                ).padding(horizontal = Spacing.Spacing16, vertical = Spacing.Spacing8)
                .testTag(SECTION_TILE_TAG + sectionUid),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Spacing8),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sectionName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextColor.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (scan != null) {
                    Text(
                        text = stringResource(Res.string.form_scan_values_read, scan.values.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextColor.OnSurfaceVariant,
                    )
                }
            }
            if (scan == null) {
                IconButton(
                    modifier = Modifier.testTag(SECTION_CAPTURE_TAG + sectionUid),
                    style = IconButtonStyle.STANDARD,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = stringResource(Res.string.form_scan_take_section_photo, sectionName),
                        )
                    },
                    onClick = onCaptureClick,
                )
            } else {
                IconButton(
                    modifier = Modifier.testTag(SECTION_TOGGLE_TAG + sectionUid),
                    style = IconButtonStyle.STANDARD,
                    icon = {
                        Icon(
                            imageVector =
                                if (model.isExpanded) {
                                    Icons.Filled.KeyboardArrowUp
                                } else {
                                    Icons.Filled.KeyboardArrowDown
                                },
                            contentDescription =
                                stringResource(
                                    if (model.isExpanded) Res.string.form_scan_hide_photo else Res.string.form_scan_show_photo,
                                ),
                        )
                    },
                    onClick = onToggleExpanded,
                )
            }
        }
        ExpandShrinkAnimatedVisibility(expanded = model.isExpanded && scan != null) {
            if (scan != null) {
                Image(
                    bitmap = scan.previewImage,
                    contentDescription = stringResource(Res.string.form_scan_open_photo, sectionName),
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .padding(top = Spacing.Spacing12)
                            .fillMaxWidth()
                            .height(THUMBNAIL_HEIGHT)
                            .clip(Shape.Small)
                            .clickable(onClick = onThumbnailClick)
                            .testTag(SECTION_THUMBNAIL_TAG + sectionUid),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SectionScanTileNotScannedPreview() {
    DHIS2Theme {
        SectionScanTile(
            model = SectionScanUiModel(section = FormSection(uid = "section", displayName = "Antenatal care")),
            onCaptureClick = {},
            onToggleExpanded = {},
            onThumbnailClick = {},
        )
    }
}

@Preview
@Composable
private fun SectionScanTileScannedPreview() {
    DHIS2Theme {
        SectionScanTile(
            model =
                SectionScanUiModel(
                    section = FormSection(uid = "section", displayName = "Antenatal care"),
                    scan = SectionScan(previewImage = ImageBitmap(400, 300), values = emptyList()),
                    isExpanded = true,
                ),
            onCaptureClick = {},
            onToggleExpanded = {},
            onThumbnailClick = {},
        )
    }
}
