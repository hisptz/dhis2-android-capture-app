package org.dhis2.mobile.omrocr.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import org.dhis2.mobile.omrocr.model.FormSection
import org.dhis2.mobile.omrocr.resources.Res
import org.dhis2.mobile.omrocr.resources.form_scan_no_photo
import org.dhis2.mobile.omrocr.resources.form_scan_section_photo
import org.dhis2.mobile.omrocr.resources.form_scan_section_values
import org.dhis2.mobile.omrocr.resources.form_scan_take_photo
import org.dhis2.mobile.omrocr.resources.form_scan_view_photo
import org.dhis2.mobile.omrocr.ui.state.SectionScan
import org.dhis2.mobile.omrocr.ui.state.SectionScanUiModel
import org.hisp.dhis.mobile.ui.designsystem.component.AdditionalInfoItem
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.ButtonStyle
import org.hisp.dhis.mobile.ui.designsystem.component.ListCard
import org.hisp.dhis.mobile.ui.designsystem.component.ListCardDescriptionModel
import org.hisp.dhis.mobile.ui.designsystem.component.ListCardTitleModel
import org.hisp.dhis.mobile.ui.designsystem.component.MetadataAvatar
import org.hisp.dhis.mobile.ui.designsystem.component.MetadataAvatarSize
import org.hisp.dhis.mobile.ui.designsystem.component.state.rememberAdditionalInfoColumnState
import org.hisp.dhis.mobile.ui.designsystem.component.state.rememberListCardState
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.hisp.dhis.mobile.ui.designsystem.theme.SurfaceColor
import org.hisp.dhis.mobile.ui.designsystem.theme.TextColor
import org.jetbrains.compose.resources.stringResource

const val SECTION_TILE_TAG = "SECTION_TILE_"
const val SECTION_CAPTURE_TAG = "SECTION_CAPTURE_"
const val SECTION_VIEW_PHOTO_TAG = "SECTION_VIEW_PHOTO_"

@Composable
fun SectionScanTile(
    model: SectionScanUiModel,
    onCaptureClick: () -> Unit,
    onViewPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sectionUid = model.section.uid
    val sectionName = model.section.displayName
    val scan = model.scan
    val onClick = if (scan == null) onCaptureClick else onViewPhotoClick

    ListCard(
        modifier = modifier.fillMaxWidth().testTag(SECTION_TILE_TAG + sectionUid),
        listCardState =
            rememberListCardState(
                title = ListCardTitleModel(text = sectionName),
                description =
                    ListCardDescriptionModel(
                        text =
                            if (scan == null) {
                                stringResource(Res.string.form_scan_no_photo)
                            } else {
                                stringResource(Res.string.form_scan_section_values, scan.values.size)
                            },
                        color = TextColor.OnSurfaceLight,
                    ),
                additionalInfoColumnState =
                    rememberAdditionalInfoColumnState(
                        additionalInfoList = emptyList(),
                        syncProgressItem = AdditionalInfoItem(value = ""),
                    ),
            ),
        listAvatar = {
            SectionPhotoAvatar(scan = scan, sectionName = sectionName)
        },
        actionButton = {
            if (scan == null) {
                Button(
                    modifier = Modifier.fillMaxWidth().testTag(SECTION_CAPTURE_TAG + sectionUid),
                    style = ButtonStyle.TONAL,
                    text = stringResource(Res.string.form_scan_take_photo),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = TextColor.OnPrimaryContainer,
                        )
                    },
                    onClick = onCaptureClick,
                )
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth().testTag(SECTION_VIEW_PHOTO_TAG + sectionUid),
                    style = ButtonStyle.TONAL,
                    text = stringResource(Res.string.form_scan_view_photo),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = TextColor.OnPrimaryContainer,
                        )
                    },
                    onClick = onViewPhotoClick,
                )
            }
        },
        onCardClick = onClick,
    )
}

@Composable
private fun SectionPhotoAvatar(
    scan: SectionScan?,
    sectionName: String,
) {
    MetadataAvatar(
        size = MetadataAvatarSize.L(),
        iconTint = SurfaceColor.Primary,
        icon = { expectedSize ->
            if (scan == null) {
                Icon(
                    modifier = Modifier.size(Spacing.Spacing32),
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = null,
                )
            } else {
                Image(
                    modifier = Modifier.size(expectedSize),
                    bitmap = scan.previewImage,
                    contentDescription = stringResource(Res.string.form_scan_section_photo, sectionName),
                    contentScale = ContentScale.Crop,
                )
            }
        },
    )
}

@Preview
@Composable
private fun SectionScanTileNotScannedPreview() {
    DHIS2Theme {
        SectionScanTile(
            model = SectionScanUiModel(section = FormSection(uid = "section", displayName = "Antenatal care")),
            onCaptureClick = {},
            onViewPhotoClick = {},
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
                ),
            onCaptureClick = {},
            onViewPhotoClick = {},
        )
    }
}
