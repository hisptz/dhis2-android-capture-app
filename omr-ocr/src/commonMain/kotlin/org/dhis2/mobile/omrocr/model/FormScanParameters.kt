package org.dhis2.mobile.omrocr.model

data class FormScanParameters(
    val dataSetUid: String,
    val dataSetName: String,
    val organisationUnitUid: String,
    val periodId: String,
    val attributeOptionComboUid: String,
)
