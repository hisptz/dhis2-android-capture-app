package org.dhis2.mobile.omrocr.di

import org.dhis2.mobile.omrocr.data.HardCodedOmrFormConfigRepository
import org.dhis2.mobile.omrocr.domain.GetFormSections
import org.dhis2.mobile.omrocr.domain.OmrFormConfigRepository
import org.dhis2.mobile.omrocr.domain.ReadScannedForm
import org.dhis2.mobile.omrocr.domain.SaveScannedValues
import org.dhis2.mobile.omrocr.ui.viewmodel.FormScanViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal expect val formScanPlatformModule: Module

val formScanModule =
    module {
        includes(formScanPlatformModule)

        single<OmrFormConfigRepository> { HardCodedOmrFormConfigRepository() }
        factory { GetFormSections(repository = get()) }
        factory { ReadScannedForm(configRepository = get(), formImageProcessor = get()) }
        factory { SaveScannedValues(repository = get()) }

        viewModel { params ->
            FormScanViewModel(
                parameters = params.get(),
                getFormSections = get(),
                readScannedForm = get(),
                saveScannedValues = get(),
            )
        }
    }
