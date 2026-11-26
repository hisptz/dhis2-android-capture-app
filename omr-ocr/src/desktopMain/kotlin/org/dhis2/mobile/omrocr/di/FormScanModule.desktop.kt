package org.dhis2.mobile.omrocr.di

import org.dhis2.mobile.omrocr.data.EmptyFormSectionsRepository
import org.dhis2.mobile.omrocr.data.PassThroughFormImageProcessor
import org.dhis2.mobile.omrocr.data.UnsupportedScannedValuesRepository
import org.dhis2.mobile.omrocr.domain.FormImageProcessor
import org.dhis2.mobile.omrocr.domain.FormSectionsRepository
import org.dhis2.mobile.omrocr.domain.ScannedValuesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val formScanPlatformModule: Module =
    module {
        factory<FormImageProcessor> { PassThroughFormImageProcessor() }
        factory<FormSectionsRepository> { EmptyFormSectionsRepository() }
        factory<ScannedValuesRepository> { UnsupportedScannedValuesRepository() }
    }
