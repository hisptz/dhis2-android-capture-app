package org.dhis2.mobile.omrocr.data

import org.dhis2.mobile.omrocr.domain.OmrFormConfigRepository
import org.dhis2.mobile.omrocr.model.OmrFormConfig

/**
 * PLACEHOLDER for the hard-coded paper form layouts.
 *
 * For now every data set / section gets a layout with no fields, so the whole flow (photo → read →
 * save → data set table) can be tried end to end. When the real configuration is shared, put it
 * in [configs] keyed by [ConfigKey] and return `configs[ConfigKey(dataSetUid, sectionUid)]`.
 */
class HardCodedOmrFormConfigRepository : OmrFormConfigRepository {
    private data class ConfigKey(
        val dataSetUid: String,
        val sectionUid: String?,
    )

    private val configs: Map<ConfigKey, OmrFormConfig> = emptyMap()

    override fun configFor(
        dataSetUid: String,
        sectionUid: String?,
    ): OmrFormConfig? =
        configs[ConfigKey(dataSetUid, sectionUid)]
            ?: OmrFormConfig(dataSetUid = dataSetUid, sectionUid = sectionUid, fields = emptyList())
}
