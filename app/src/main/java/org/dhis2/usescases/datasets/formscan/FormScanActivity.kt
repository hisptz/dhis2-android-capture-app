package org.dhis2.usescases.datasets.formscan

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.dhis2.mobile.aggregates.ui.constants.INTENT_EXTRA_ATTRIBUTE_OPTION_COMBO_UID
import org.dhis2.mobile.aggregates.ui.constants.INTENT_EXTRA_DATA_SET_UID
import org.dhis2.mobile.aggregates.ui.constants.INTENT_EXTRA_ORGANISATION_UNIT_UID
import org.dhis2.mobile.aggregates.ui.constants.INTENT_EXTRA_PERIOD_ID
import org.dhis2.mobile.omrocr.model.FormScanParameters
import org.dhis2.mobile.omrocr.ui.screen.FormScanScreen
import org.dhis2.usescases.datasets.dataSetTable.DataSetInstanceActivity
import org.dhis2.usescases.general.ActivityGlobalAbstract
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme

class FormScanActivity : ActivityGlobalAbstract() {
    override var handleEdgeToEdge = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT))

        val parameters =
            FormScanParameters(
                dataSetUid = requireNotNull(intent.getStringExtra(EXTRA_DATA_SET_UID)),
                dataSetName = intent.getStringExtra(EXTRA_DATA_SET_NAME).orEmpty(),
                organisationUnitUid = requireNotNull(intent.getStringExtra(EXTRA_ORGANISATION_UNIT_UID)),
                periodId = requireNotNull(intent.getStringExtra(EXTRA_PERIOD_ID)),
                attributeOptionComboUid = requireNotNull(intent.getStringExtra(EXTRA_ATTRIBUTE_OPTION_COMBO_UID)),
            )

        setContent {
            DHIS2Theme {
                FormScanScreen(
                    parameters = parameters,
                    onBackClick = ::finish,
                    onValuesSaved = { openDataSetTable(parameters) },
                )
            }
        }
    }

    private fun openDataSetTable(parameters: FormScanParameters) {
        startActivity(
            Intent(this, DataSetInstanceActivity::class.java).apply {
                putExtra(INTENT_EXTRA_DATA_SET_UID, parameters.dataSetUid)
                putExtra(INTENT_EXTRA_ORGANISATION_UNIT_UID, parameters.organisationUnitUid)
                putExtra(INTENT_EXTRA_PERIOD_ID, parameters.periodId)
                putExtra(INTENT_EXTRA_ATTRIBUTE_OPTION_COMBO_UID, parameters.attributeOptionComboUid)
            },
        )
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        private const val EXTRA_DATA_SET_UID = "FORM_SCAN_DATA_SET_UID"
        private const val EXTRA_DATA_SET_NAME = "FORM_SCAN_DATA_SET_NAME"
        private const val EXTRA_ORGANISATION_UNIT_UID = "FORM_SCAN_ORGANISATION_UNIT_UID"
        private const val EXTRA_PERIOD_ID = "FORM_SCAN_PERIOD_ID"
        private const val EXTRA_ATTRIBUTE_OPTION_COMBO_UID = "FORM_SCAN_ATTRIBUTE_OPTION_COMBO_UID"

        @JvmStatic
        fun getIntent(
            context: Context,
            dataSetUid: String,
            dataSetName: String,
            organisationUnitUid: String,
            periodId: String,
            attributeOptionComboUid: String,
        ): Intent =
            Intent(context, FormScanActivity::class.java).apply {
                putExtra(EXTRA_DATA_SET_UID, dataSetUid)
                putExtra(EXTRA_DATA_SET_NAME, dataSetName)
                putExtra(EXTRA_ORGANISATION_UNIT_UID, organisationUnitUid)
                putExtra(EXTRA_PERIOD_ID, periodId)
                putExtra(EXTRA_ATTRIBUTE_OPTION_COMBO_UID, attributeOptionComboUid)
            }
    }
}
