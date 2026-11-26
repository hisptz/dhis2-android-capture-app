package org.dhis2.mobile.omrocr.ui.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import org.dhis2.mobile.commons.extensions.launchUseCase
import org.dhis2.mobile.omrocr.domain.GetFormSections
import org.dhis2.mobile.omrocr.domain.ReadScannedForm
import org.dhis2.mobile.omrocr.domain.SaveScannedValues
import org.dhis2.mobile.omrocr.model.FormScanParameters
import org.dhis2.mobile.omrocr.ui.state.FormScanStep
import org.dhis2.mobile.omrocr.ui.state.FormScanUiState
import org.dhis2.mobile.omrocr.ui.state.SectionScan
import org.dhis2.mobile.omrocr.ui.state.SectionScanUiModel

class FormScanViewModel(
    private val parameters: FormScanParameters,
    private val getFormSections: GetFormSections,
    private val readScannedForm: ReadScannedForm,
    private val saveScannedValues: SaveScannedValues,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FormScanUiState(dataSetName = parameters.dataSetName))
    val uiState: StateFlow<FormScanUiState> = _uiState.asStateFlow()

    private val _exit = Channel<Unit>(Channel.BUFFERED)
    val exit: Flow<Unit> = _exit.receiveAsFlow()

    init {
        launchUseCase {
            val sections = getFormSections(parameters.dataSetUid).getOrNull().orEmpty()
            _uiState.update { state ->
                if (sections.isEmpty()) {
                    state.copy(
                        isLoadingSections = false,
                        step = FormScanStep.Camera(null),
                    )
                } else {
                    state.copy(
                        isLoadingSections = false,
                        sections = sections.map { SectionScanUiModel(section = it) },
                        step = FormScanStep.SectionList,
                    )
                }
            }
        }
    }

    fun onCaptureSectionClick(sectionUid: String) {
        _uiState.update { it.copy(step = FormScanStep.Camera(sectionUid)) }
    }

    fun onImageCaptured(image: ImageBitmap) {
        val step = _uiState.value.step as? FormScanStep.Camera ?: return
        val sectionUid = step.sectionUid
        _uiState.update { it.copy(step = FormScanStep.Processing(sectionUid)) }
        launchUseCase {
            readScannedForm(
                ReadScannedForm.Input(
                    dataSetUid = parameters.dataSetUid,
                    sectionUid = sectionUid,
                    image = image,
                ),
            ).fold(
                onSuccess = { result ->
                    val scan = SectionScan(previewImage = result.previewImage, values = result.values)
                    _uiState.update { state ->
                        if (sectionUid == null) {
                            state.copy(
                                singleScan = scan,
                                step = FormScanStep.Preview(null),
                            )
                        } else {
                            state.copy(
                                sections =
                                    state.sections.map { model ->
                                        if (model.section.uid == sectionUid) {
                                            model.copy(scan = scan)
                                        } else {
                                            model
                                        }
                                    },
                                step = FormScanStep.SectionList,
                            )
                        }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(step = FormScanStep.ScanFailed(sectionUid)) }
                },
            )
        }
    }

    fun onCaptureFailed() {
        val step = _uiState.value.step as? FormScanStep.Camera ?: return
        _uiState.update { it.copy(step = FormScanStep.ScanFailed(step.sectionUid)) }
    }

    fun onCameraPermissionDenied() {
        val step = _uiState.value.step as? FormScanStep.Camera ?: return
        _uiState.update { it.copy(step = FormScanStep.CameraPermissionDenied(step.sectionUid)) }
    }

    fun onTryAgainClick() {
        val sectionUid =
            when (val step = _uiState.value.step) {
                is FormScanStep.ScanFailed -> step.sectionUid
                is FormScanStep.CameraPermissionDenied -> step.sectionUid
                else -> return
            }
        _uiState.update { it.copy(step = FormScanStep.Camera(sectionUid)) }
    }

    fun onViewPhotoClick(sectionUid: String) {
        _uiState.update { it.copy(step = FormScanStep.Preview(sectionUid)) }
    }

    fun onRetakeClick() {
        val step = _uiState.value.step as? FormScanStep.Preview ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(step = FormScanStep.Camera(step.sectionUid)) }
    }

    fun onBack() {
        val state = _uiState.value
        if (state.isSaving) return
        when (val step = state.step) {
            FormScanStep.Loading -> exit()
            FormScanStep.SectionList ->
                if (state.hasAnyScan) showDiscardConfirmation() else exit()
            is FormScanStep.Camera -> returnToListOrExit(step.sectionUid)
            is FormScanStep.ScanFailed -> returnToListOrExit(step.sectionUid)
            is FormScanStep.CameraPermissionDenied -> returnToListOrExit(step.sectionUid)
            is FormScanStep.Preview ->
                if (step.sectionUid != null) {
                    _uiState.update { it.copy(step = FormScanStep.SectionList) }
                } else {
                    showDiscardConfirmation()
                }
            is FormScanStep.Processing, FormScanStep.Saved -> Unit
        }
    }

    fun onDiscardConfirmed() {
        _uiState.update { state ->
            state.copy(
                sections = state.sections.map { it.copy(scan = null) },
                singleScan = null,
                showDiscardConfirmation = false,
            )
        }
        exit()
    }

    fun onDiscardDismissed() {
        _uiState.update { it.copy(showDiscardConfirmation = false) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (!state.canSave) return
        val values =
            state.singleScan?.values
                ?: state.sections.mapNotNull { it.scan }.flatMap { it.values }
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        launchUseCase {
            saveScannedValues(SaveScannedValues.Input(parameters = parameters, values = values)).fold(
                onSuccess = {
                    _uiState.update { current ->
                        current.copy(
                            sections = current.sections.map { model -> model.copy(scan = null) },
                            singleScan = null,
                            isSaving = false,
                            step = FormScanStep.Saved,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isSaving = false, saveFailed = true) }
                },
            )
        }
    }

    private fun returnToListOrExit(sectionUid: String?) {
        if (sectionUid != null) {
            _uiState.update { it.copy(step = FormScanStep.SectionList) }
        } else {
            exit()
        }
    }

    private fun showDiscardConfirmation() {
        _uiState.update { it.copy(showDiscardConfirmation = true) }
    }

    private fun exit() {
        _exit.trySend(Unit)
    }
}
