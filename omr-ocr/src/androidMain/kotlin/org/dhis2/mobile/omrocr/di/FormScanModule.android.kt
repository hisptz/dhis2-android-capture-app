package org.dhis2.mobile.omrocr.di

import org.dhis2.mobile.omrocr.data.FormSectionsRepositoryImpl
import org.dhis2.mobile.omrocr.data.OpenCvFormImageProcessor
import org.dhis2.mobile.omrocr.data.ScannedValuesRepositoryImpl
import org.dhis2.mobile.omrocr.domain.FormImageProcessor
import org.dhis2.mobile.omrocr.domain.FormSectionsRepository
import org.dhis2.mobile.omrocr.domain.ScannedValuesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val formScanPlatformModule: Module =
    module {
        factory<FormImageProcessor> { OpenCvFormImageProcessor() }
        factory<FormSectionsRepository> { FormSectionsRepositoryImpl(d2 = get(), dispatcher = get()) }
        factory<ScannedValuesRepository> {
            ScannedValuesRepositoryImpl(d2 = get(), domainErrorMapper = get(), dispatcher = get())
        }
    }
