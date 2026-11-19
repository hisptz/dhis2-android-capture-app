package org.dhis2.mobile.omrocr.model

/**
 * Describes where every value is printed on one photographed page (a section, or the whole form
 * when the data set has no sections), so the OMR reader knows which circles to look at and which
 * data value each one belongs to.
 *
 * PLACEHOLDER: the real layout will be shared later. Add the circle positions to [OmrField]
 * when it arrives; nothing outside the processor needs to change.
 */
data class OmrFormConfig(
    val dataSetUid: String,
    val sectionUid: String?,
    val fields: List<OmrField>,
)

data class OmrField(
    val dataElementUid: String,
    val categoryOptionComboUid: String,
    // TODO: add the circle positions/sizes and the value each circle stands for.
)
