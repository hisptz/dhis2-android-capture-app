package org.dhis2.mobile.omrocr.data

import kotlinx.coroutines.withContext
import org.dhis2.mobile.commons.coroutine.Dispatcher
import org.dhis2.mobile.omrocr.domain.FormSectionsRepository
import org.dhis2.mobile.omrocr.model.FormSection
import org.hisp.dhis.android.core.D2

internal class FormSectionsRepositoryImpl(
    private val d2: D2,
    private val dispatcher: Dispatcher,
) : FormSectionsRepository {
    override suspend fun sections(dataSetUid: String): List<FormSection> =
        withContext(dispatcher.io) {
            d2
                .dataSetModule()
                .sections()
                .byDataSetUid()
                .eq(dataSetUid)
                .blockingGet()
                .sortedBy { it.sortOrder() ?: Int.MAX_VALUE }
                .map { FormSection(uid = it.uid(), displayName = it.displayName() ?: it.uid()) }
        }
}
