package org.dhis2.mobile.omrocr.data

import org.dhis2.mobile.omrocr.model.OmrFormConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class HardCodedOmrFormConfigRepositoryTest {
    private val repository = HardCodedOmrFormConfigRepository()

    @Test
    fun `returns an empty layout for a section`() {
        assertEquals(
            OmrFormConfig(dataSetUid = "dataSet", sectionUid = "section", fields = emptyList()),
            repository.configFor("dataSet", "section"),
        )
    }

    @Test
    fun `returns an empty layout for a data set without sections`() {
        assertEquals(
            OmrFormConfig(dataSetUid = "dataSet", sectionUid = null, fields = emptyList()),
            repository.configFor("dataSet", null),
        )
    }
}
