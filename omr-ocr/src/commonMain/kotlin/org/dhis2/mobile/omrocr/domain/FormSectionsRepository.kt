package org.dhis2.mobile.omrocr.domain

import org.dhis2.mobile.omrocr.model.FormSection

interface FormSectionsRepository {
    suspend fun sections(dataSetUid: String): List<FormSection>
}
