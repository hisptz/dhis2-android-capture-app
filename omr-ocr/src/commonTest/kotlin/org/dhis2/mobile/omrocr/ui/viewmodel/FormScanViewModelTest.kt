package org.dhis2.mobile.omrocr.ui.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.dhis2.mobile.omrocr.domain.GetFormSections
import org.dhis2.mobile.omrocr.domain.ReadScannedForm
import org.dhis2.mobile.omrocr.domain.SaveScannedValues
import org.dhis2.mobile.omrocr.model.FormScanParameters
import org.dhis2.mobile.omrocr.model.FormScanResult
import org.dhis2.mobile.omrocr.model.FormSection
import org.dhis2.mobile.omrocr.model.ScannedDataValue
import org.dhis2.mobile.omrocr.ui.state.FormScanStep
import org.mockito.kotlin.mock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FormScanViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val processor = FakeFormImageProcessor()
    private val configRepository = FakeConfigRepository()
    private val sectionsRepository = FakeSectionsRepository()
    private val valuesRepository = FakeScannedValuesRepository()

    private val parameters =
        FormScanParameters(
            dataSetUid = "dataSetUid",
            dataSetName = "Data set",
            organisationUnitUid = "orgUnitUid",
            periodId = "202611",
            attributeOptionComboUid = "aocUid",
        )

    private val sectionA = FormSection(uid = "sectionA", displayName = "Section A")
    private val sectionB = FormSection(uid = "sectionB", displayName = "Section B")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        FormScanViewModel(
            parameters = parameters,
            getFormSections = GetFormSections(repository = sectionsRepository),
            readScannedForm = ReadScannedForm(configRepository = configRepository, formImageProcessor = processor),
            saveScannedValues = SaveScannedValues(repository = valuesRepository),
        )

    private fun value(
        dataElementUid: String,
        value: String,
    ) = ScannedDataValue(dataElementUid = dataElementUid, categoryOptionComboUid = "cocUid", value = value)

    private fun result(vararg values: ScannedDataValue) = FormScanResult(previewImage = mock<ImageBitmap>(), values = values.toList())

    private fun TestScope.sectionViewModel(): FormScanViewModel {
        sectionsRepository.sections = listOf(sectionA, sectionB)
        val viewModel = createViewModel()
        advanceUntilIdle()
        return viewModel
    }

    private fun TestScope.scanSection(
        viewModel: FormScanViewModel,
        sectionUid: String,
        scanResult: FormScanResult,
    ) {
        processor.results.addLast(scanResult)
        viewModel.onCaptureSectionClick(sectionUid)
        viewModel.onImageCaptured(mock())
        advanceUntilIdle()
    }

    private fun TestScope.collectExit(viewModel: FormScanViewModel): List<Unit> {
        val exits = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.exit.collect { exits.add(it) } }
        return exits
    }

    @Test
    fun `data set without sections starts the single photo flow`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(FormScanStep.Camera(null), state.step)
            assertEquals("Data set", state.dataSetName)
            assertFalse(state.isLoadingSections)
            assertFalse(state.hasSections)
        }

    @Test
    fun `sections repository failure falls back to the single photo flow`() =
        runTest(testDispatcher) {
            sectionsRepository.failure = IllegalStateException("boom")

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(FormScanStep.Camera(null), viewModel.uiState.value.step)
            assertFalse(viewModel.uiState.value.isLoadingSections)
        }

    @Test
    fun `data set with sections shows the section list in order without scans`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()

            val state = viewModel.uiState.value
            assertEquals(FormScanStep.SectionList, state.step)
            assertEquals(listOf(sectionA, sectionB), state.sections.map { it.section })
            assertTrue(state.sections.all { it.scan == null && !it.isExpanded })
        }

    @Test
    fun `single photo capture is processed and shown in the preview`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val scanResult = result(value("de1", "3"))
            processor.results.addLast(scanResult)

            viewModel.onImageCaptured(mock())

            assertEquals(FormScanStep.Processing(null), viewModel.uiState.value.step)
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertEquals(FormScanStep.Preview(null), state.step)
            assertSame(scanResult.previewImage, state.singleScan?.previewImage)
            assertEquals(scanResult.values, state.singleScan?.values)
        }

    @Test
    fun `section capture returns to the list with that tile scanned and expanded`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()
            val scanResult = result(value("de1", "5"))
            processor.results.addLast(scanResult)

            viewModel.onCaptureSectionClick(sectionA.uid)
            assertEquals(FormScanStep.Camera(sectionA.uid), viewModel.uiState.value.step)
            viewModel.onImageCaptured(mock())
            assertEquals(FormScanStep.Processing(sectionA.uid), viewModel.uiState.value.step)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(FormScanStep.SectionList, state.step)
            val tileA = state.sections.first { it.section.uid == sectionA.uid }
            val tileB = state.sections.first { it.section.uid == sectionB.uid }
            assertEquals(scanResult.values, tileA.scan?.values)
            assertTrue(tileA.isExpanded)
            assertNull(tileB.scan)
            assertFalse(tileB.isExpanded)
        }

    @Test
    fun `section retake replaces the previous scan`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()
            scanSection(viewModel, sectionA.uid, result(value("de1", "1")))

            viewModel.onThumbnailClick(sectionA.uid)
            assertEquals(FormScanStep.Preview(sectionA.uid), viewModel.uiState.value.step)
            viewModel.onRetakeClick()
            assertEquals(FormScanStep.Camera(sectionA.uid), viewModel.uiState.value.step)
            val retake = result(value("de1", "2"))
            processor.results.addLast(retake)
            viewModel.onImageCaptured(mock())
            advanceUntilIdle()

            val tileA =
                viewModel.uiState.value.sections
                    .first { it.section.uid == sectionA.uid }
            assertEquals(retake.values, tileA.scan?.values)
            assertSame(retake.previewImage, tileA.scan?.previewImage)
        }

    @Test
    fun `processor failure shows the scan failed step and try again reopens the camera`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()
            processor.failure = IllegalStateException("unreadable")

            viewModel.onCaptureSectionClick(sectionA.uid)
            viewModel.onImageCaptured(mock())
            advanceUntilIdle()

            assertEquals(FormScanStep.ScanFailed(sectionA.uid), viewModel.uiState.value.step)
            viewModel.onTryAgainClick()
            assertEquals(FormScanStep.Camera(sectionA.uid), viewModel.uiState.value.step)
        }

    @Test
    fun `missing configuration shows the scan failed step`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()
            configRepository.hasConfig = false

            viewModel.onCaptureSectionClick(sectionB.uid)
            viewModel.onImageCaptured(mock())
            advanceUntilIdle()

            assertEquals(FormScanStep.ScanFailed(sectionB.uid), viewModel.uiState.value.step)
        }

    @Test
    fun `camera permission denied shows the permission step`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()

            viewModel.onCaptureSectionClick(sectionA.uid)
            viewModel.onCameraPermissionDenied()

            assertEquals(FormScanStep.CameraPermissionDenied(sectionA.uid), viewModel.uiState.value.step)
            viewModel.onTryAgainClick()
            assertEquals(FormScanStep.Camera(sectionA.uid), viewModel.uiState.value.step)
        }

    @Test
    fun `can save only after at least one section is scanned`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()
            assertFalse(viewModel.uiState.value.canSave)

            scanSection(viewModel, sectionA.uid, result(value("de1", "1")))

            assertTrue(viewModel.uiState.value.canSave)
        }

    @Test
    fun `save merges the values of all scanned sections and releases the images`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()
            val valueA = value("de1", "1")
            val valueB = value("de2", "2")
            scanSection(viewModel, sectionA.uid, result(valueA))
            scanSection(viewModel, sectionB.uid, result(valueB))

            viewModel.onSaveClick()
            assertTrue(viewModel.uiState.value.isSaving)
            advanceUntilIdle()

            assertEquals(listOf(parameters to listOf(valueA, valueB)), valuesRepository.savedRequests)
            val state = viewModel.uiState.value
            assertEquals(FormScanStep.Saved, state.step)
            assertFalse(state.isSaving)
            assertNull(state.singleScan)
            assertTrue(state.sections.all { it.scan == null })
        }

    @Test
    fun `save failure keeps the scans and reports the error`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()
            scanSection(viewModel, sectionA.uid, result(value("de1", "1")))
            valuesRepository.failure = IllegalStateException("db error")

            viewModel.onSaveClick()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.saveFailed)
            assertFalse(state.isSaving)
            assertEquals(FormScanStep.SectionList, state.step)
            assertNotNull(state.sections.first { it.section.uid == sectionA.uid }.scan)
        }

    @Test
    fun `back from section camera or section preview returns to the list`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()
            val exits = collectExit(viewModel)

            viewModel.onCaptureSectionClick(sectionA.uid)
            viewModel.onBack()
            assertEquals(FormScanStep.SectionList, viewModel.uiState.value.step)

            scanSection(viewModel, sectionA.uid, result(value("de1", "1")))
            viewModel.onThumbnailClick(sectionA.uid)
            viewModel.onBack()
            advanceUntilIdle()

            assertEquals(FormScanStep.SectionList, viewModel.uiState.value.step)
            assertFalse(viewModel.uiState.value.showDiscardConfirmation)
            assertTrue(exits.isEmpty())
        }

    @Test
    fun `back on the list with scans asks to discard them`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()
            val exits = collectExit(viewModel)
            scanSection(viewModel, sectionA.uid, result(value("de1", "1")))

            viewModel.onBack()
            assertTrue(viewModel.uiState.value.showDiscardConfirmation)
            viewModel.onDiscardDismissed()
            assertFalse(viewModel.uiState.value.showDiscardConfirmation)
            advanceUntilIdle()
            assertTrue(exits.isEmpty())

            viewModel.onBack()
            viewModel.onDiscardConfirmed()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showDiscardConfirmation)
            assertTrue(state.sections.all { it.scan == null })
            assertFalse(state.hasAnyScan)
            assertEquals(1, exits.size)
            assertTrue(valuesRepository.savedRequests.isEmpty())
            assertFalse(state.step == FormScanStep.Saved)
        }

    @Test
    fun `back on the list without scans exits`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()
            val exits = collectExit(viewModel)

            viewModel.onBack()
            advanceUntilIdle()

            assertEquals(1, exits.size)
            assertFalse(viewModel.uiState.value.showDiscardConfirmation)
        }

    @Test
    fun `toggling a scanned section flips its expanded state`() =
        runTest(testDispatcher) {
            val viewModel = sectionViewModel()
            scanSection(viewModel, sectionA.uid, result(value("de1", "1")))
            assertTrue(
                viewModel.uiState.value.sections
                    .first()
                    .isExpanded,
            )

            viewModel.onToggleSectionExpanded(sectionA.uid)
            assertFalse(
                viewModel.uiState.value.sections
                    .first()
                    .isExpanded,
            )

            viewModel.onToggleSectionExpanded(sectionA.uid)
            assertTrue(
                viewModel.uiState.value.sections
                    .first()
                    .isExpanded,
            )
        }
}
