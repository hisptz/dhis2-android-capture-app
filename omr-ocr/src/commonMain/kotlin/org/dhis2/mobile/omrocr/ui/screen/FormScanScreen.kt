package org.dhis2.mobile.omrocr.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import org.dhis2.mobile.omrocr.model.FormScanParameters
import org.dhis2.mobile.omrocr.model.FormSection
import org.dhis2.mobile.omrocr.resources.Res
import org.dhis2.mobile.omrocr.resources.form_scan_back
import org.dhis2.mobile.omrocr.resources.form_scan_failed
import org.dhis2.mobile.omrocr.resources.form_scan_loading
import org.dhis2.mobile.omrocr.resources.form_scan_permission_denied
import org.dhis2.mobile.omrocr.resources.form_scan_photographing_section
import org.dhis2.mobile.omrocr.resources.form_scan_processing
import org.dhis2.mobile.omrocr.resources.form_scan_saving
import org.dhis2.mobile.omrocr.resources.form_scan_title
import org.dhis2.mobile.omrocr.resources.form_scan_try_again
import org.dhis2.mobile.omrocr.ui.component.DiscardScanBottomSheet
import org.dhis2.mobile.omrocr.ui.contracts.CameraCapture
import org.dhis2.mobile.omrocr.ui.contracts.PlatformBackHandler
import org.dhis2.mobile.omrocr.ui.state.FormScanStep
import org.dhis2.mobile.omrocr.ui.state.FormScanUiState
import org.dhis2.mobile.omrocr.ui.state.SectionScan
import org.dhis2.mobile.omrocr.ui.state.SectionScanUiModel
import org.dhis2.mobile.omrocr.ui.viewmodel.FormScanViewModel
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.ButtonStyle
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicator
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicatorType
import org.hisp.dhis.mobile.ui.designsystem.component.TopBar
import org.hisp.dhis.mobile.ui.designsystem.component.TopBarActionIcon
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.Shape
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.hisp.dhis.mobile.ui.designsystem.theme.SurfaceColor
import org.hisp.dhis.mobile.ui.designsystem.theme.TextColor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FormScanScreen(
    parameters: FormScanParameters,
    onBackClick: () -> Unit,
    onValuesSaved: () -> Unit,
) {
    val viewModel = koinViewModel<FormScanViewModel> { parametersOf(parameters) }
    val uiState by viewModel.uiState.collectAsState()
    val currentOnBackClick by rememberUpdatedState(onBackClick)
    val currentOnValuesSaved by rememberUpdatedState(onValuesSaved)

    PlatformBackHandler(enabled = true) { viewModel.onBack() }

    LaunchedEffect(Unit) {
        viewModel.exit.collect { currentOnBackClick() }
    }

    LaunchedEffect(uiState.step) {
        if (uiState.step == FormScanStep.Saved) currentOnValuesSaved()
    }

    FormScanContent(
        uiState = uiState,
        onBack = viewModel::onBack,
        onCaptureSectionClick = viewModel::onCaptureSectionClick,
        onImageCaptured = viewModel::onImageCaptured,
        onCaptureFailed = viewModel::onCaptureFailed,
        onCameraPermissionDenied = viewModel::onCameraPermissionDenied,
        onTryAgainClick = viewModel::onTryAgainClick,
        onToggleSectionExpanded = viewModel::onToggleSectionExpanded,
        onThumbnailClick = viewModel::onThumbnailClick,
        onRetakeClick = viewModel::onRetakeClick,
        onSaveClick = viewModel::onSaveClick,
        onDiscardConfirmed = viewModel::onDiscardConfirmed,
        onDiscardDismissed = viewModel::onDiscardDismissed,
    )
}

@Composable
fun FormScanContent(
    uiState: FormScanUiState,
    onBack: () -> Unit,
    onCaptureSectionClick: (String) -> Unit,
    onImageCaptured: (ImageBitmap) -> Unit,
    onCaptureFailed: () -> Unit,
    onCameraPermissionDenied: () -> Unit,
    onTryAgainClick: () -> Unit,
    onToggleSectionExpanded: (String) -> Unit,
    onThumbnailClick: (String) -> Unit,
    onRetakeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDiscardConfirmed: () -> Unit,
    onDiscardDismissed: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            TopBar(
                modifier = Modifier.fillMaxWidth(),
                navigationIcon = {
                    TopBarActionIcon(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        contentDescription = stringResource(Res.string.form_scan_back),
                        onClick = onBack,
                    )
                },
                actions = {},
                title = {
                    Text(
                        text = stringResource(Res.string.form_scan_title),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors().copy(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )
        },
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .clip(Shape.LargeTop)
                    .background(SurfaceColor.SurfaceBright)
                    .padding(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                        bottom = paddingValues.calculateBottomPadding(),
                    ),
        ) {
            when (val step = uiState.step) {
                FormScanStep.Loading -> StatusContent(Res.string.form_scan_loading)
                is FormScanStep.Processing -> StatusContent(Res.string.form_scan_processing)
                FormScanStep.Saved -> StatusContent(Res.string.form_scan_saving)
                is FormScanStep.Camera ->
                    CameraContent(
                        dataSetName = uiState.dataSetName,
                        sectionName = uiState.sectionName(step.sectionUid),
                        onImageCaptured = onImageCaptured,
                        onCaptureFailed = onCaptureFailed,
                        onCameraPermissionDenied = onCameraPermissionDenied,
                    )
                FormScanStep.SectionList ->
                    SectionListContent(
                        uiState = uiState,
                        onCaptureSectionClick = onCaptureSectionClick,
                        onToggleSectionExpanded = onToggleSectionExpanded,
                        onThumbnailClick = onThumbnailClick,
                        onSaveClick = onSaveClick,
                    )
                is FormScanStep.Preview ->
                    uiState.scanFor(step.sectionUid)?.let { scan ->
                        ScanPreviewContent(
                            sectionUid = step.sectionUid,
                            scan = scan,
                            canSave = uiState.canSave,
                            isSaving = uiState.isSaving,
                            saveFailed = uiState.saveFailed,
                            onSaveClick = onSaveClick,
                            onRetakeClick = onRetakeClick,
                        )
                    }
                is FormScanStep.ScanFailed ->
                    ErrorContent(
                        message = Res.string.form_scan_failed,
                        onTryAgainClick = onTryAgainClick,
                    )
                is FormScanStep.CameraPermissionDenied ->
                    ErrorContent(
                        message = Res.string.form_scan_permission_denied,
                        onTryAgainClick = onTryAgainClick,
                    )
            }
        }
    }

    if (uiState.showDiscardConfirmation) {
        DiscardScanBottomSheet(
            onDiscardConfirmed = onDiscardConfirmed,
            onDiscardDismissed = onDiscardDismissed,
        )
    }
}

private fun FormScanUiState.sectionName(sectionUid: String?): String? =
    sectionUid?.let { uid -> sections.firstOrNull { it.section.uid == uid }?.section?.displayName }

private fun FormScanUiState.scanFor(sectionUid: String?): SectionScan? =
    if (sectionUid == null) {
        singleScan
    } else {
        sections.firstOrNull { it.section.uid == sectionUid }?.scan
    }

@Composable
private fun StatusContent(message: StringResource) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.Spacing16),
        verticalArrangement = Arrangement.spacedBy(Spacing.Spacing16, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressIndicator(type = ProgressIndicatorType.CIRCULAR)
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = TextColor.OnSurface,
        )
    }
}

@Composable
private fun CameraContent(
    dataSetName: String,
    sectionName: String?,
    onImageCaptured: (ImageBitmap) -> Unit,
    onCaptureFailed: () -> Unit,
    onCameraPermissionDenied: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (sectionName != null) {
            Text(
                text = stringResource(Res.string.form_scan_photographing_section, dataSetName, sectionName),
                style = MaterialTheme.typography.bodyMedium,
                color = TextColor.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(SurfaceColor.Container)
                        .padding(horizontal = Spacing.Spacing16, vertical = Spacing.Spacing8),
            )
        }
        CameraCapture(
            onImageCaptured = onImageCaptured,
            onCaptureFailed = onCaptureFailed,
            onPermissionDenied = onCameraPermissionDenied,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun ErrorContent(
    message: StringResource,
    onTryAgainClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.Spacing16),
        verticalArrangement = Arrangement.spacedBy(Spacing.Spacing16, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.FILLED,
            text = stringResource(Res.string.form_scan_try_again),
            onClick = onTryAgainClick,
        )
    }
}

@Composable
private fun FormScanContentPreview(uiState: FormScanUiState) {
    DHIS2Theme {
        FormScanContent(
            uiState = uiState,
            onBack = {},
            onCaptureSectionClick = {},
            onImageCaptured = {},
            onCaptureFailed = {},
            onCameraPermissionDenied = {},
            onTryAgainClick = {},
            onToggleSectionExpanded = {},
            onThumbnailClick = {},
            onRetakeClick = {},
            onSaveClick = {},
            onDiscardConfirmed = {},
            onDiscardDismissed = {},
        )
    }
}

@Preview
@Composable
private fun FormScanSectionListPreview() {
    FormScanContentPreview(
        FormScanUiState(
            isLoadingSections = false,
            step = FormScanStep.SectionList,
            sections =
                listOf(
                    SectionScanUiModel(section = FormSection(uid = "a", displayName = "Antenatal care")),
                    SectionScanUiModel(section = FormSection(uid = "b", displayName = "Immunisation")),
                ),
        ),
    )
}

@Preview
@Composable
private fun FormScanProcessingPreview() {
    FormScanContentPreview(FormScanUiState(isLoadingSections = false, step = FormScanStep.Processing(null)))
}

@Preview
@Composable
private fun FormScanFailedPreview() {
    FormScanContentPreview(FormScanUiState(isLoadingSections = false, step = FormScanStep.ScanFailed(null)))
}
