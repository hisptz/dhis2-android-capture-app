package org.dhis2.mobile.omrocr.model

data class ScannedDataValue(
    val dataElementUid: String,
    val categoryOptionComboUid: String,
    val value: String,
)
