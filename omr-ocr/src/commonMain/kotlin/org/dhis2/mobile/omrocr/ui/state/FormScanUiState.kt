package org.dhis2.mobile.omrocr.ui.state

import androidx.compose.ui.graphics.ImageBitmap
import org.dhis2.mobile.omrocr.model.FormSection
import org.dhis2.mobile.omrocr.model.ScannedDataValue

data class FormScanUiState(
    val dataSetName: String = "",
    val isLoadingSections: Boolean = true,
    val sections: List<SectionScanUiModel> = emptyList(),
    val singleScan: SectionScan? = null,
    val step: FormScanStep = FormScanStep.Loading,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val showDiscardConfirmation: Boolean = false,
) {
    val hasSections: Boolean get() = sections.isNotEmpty()
    val hasAnyScan: Boolean get() = singleScan != null || sections.any { it.scan != null }
    val canSave: Boolean get() = hasAnyScan && !isSaving
}

data class SectionScanUiModel(
    val section: FormSection,
    val scan: SectionScan? = null,
)

data class SectionScan(
    val previewImage: ImageBitmap,
    val values: List<ScannedDataValue>,
)

sealed interface FormScanStep {
    data object Loading : FormScanStep

    data object SectionList : FormScanStep

    data class Camera(
        val sectionUid: String?,
    ) : FormScanStep

    data class Processing(
        val sectionUid: String?,
    ) : FormScanStep

    data class Preview(
        val sectionUid: String?,
    ) : FormScanStep

    data class ScanFailed(
        val sectionUid: String?,
    ) : FormScanStep

    data class CameraPermissionDenied(
        val sectionUid: String?,
    ) : FormScanStep

    data object Saved : FormScanStep
}
