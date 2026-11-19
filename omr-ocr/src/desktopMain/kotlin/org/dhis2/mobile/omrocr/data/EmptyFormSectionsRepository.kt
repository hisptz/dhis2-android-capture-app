package org.dhis2.mobile.omrocr.data

import org.dhis2.mobile.omrocr.domain.FormSectionsRepository
import org.dhis2.mobile.omrocr.model.FormSection

class EmptyFormSectionsRepository : FormSectionsRepository {
    override suspend fun sections(dataSetUid: String): List<FormSection> = emptyList()
}
