package org.dhis2.mobile.omrocr.domain

import org.dhis2.mobile.omrocr.model.OmrFormConfig

interface OmrFormConfigRepository {
    fun configFor(
        dataSetUid: String,
        sectionUid: String?,
    ): OmrFormConfig?
}
