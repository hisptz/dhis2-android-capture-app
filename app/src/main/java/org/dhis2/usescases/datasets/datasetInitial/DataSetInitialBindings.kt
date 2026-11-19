package org.dhis2.usescases.datasets.datasetInitial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.databinding.BindingAdapter
import org.dhis2.R
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.ButtonStyle
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing

const val TYPE_VALUES_BUTTON_TAG = "TYPE_VALUES_BUTTON"
const val SCAN_FORM_BUTTON_TAG = "SCAN_FORM_BUTTON"

@BindingAdapter(value = ["dataSetInitialPresenter", "dataSetInitialModel"])
fun ComposeView.setDataSetInitialActions(
    presenter: DataSetInitialContract.Presenter?,
    dataSetModel: DataSetInitialModel?,
) {
    setContent {
        DHIS2Theme {
            DataSetInitialActions(
                onTypeValuesClick = {
                    dataSetModel?.let { presenter?.onActionButtonClick(it.periodType) }
                },
                onScanFormClick = {
                    dataSetModel?.let { presenter?.onScanButtonClick(it.periodType) }
                },
            )
        }
    }
}

@Composable
fun DataSetInitialActions(
    onTypeValuesClick: () -> Unit,
    onScanFormClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(Spacing.Spacing16),
        verticalArrangement = Arrangement.spacedBy(Spacing.Spacing8),
    ) {
        Button(
            modifier = Modifier.fillMaxWidth().testTag(TYPE_VALUES_BUTTON_TAG),
            style = ButtonStyle.OUTLINED,
            text = stringResource(R.string.type_values),
            icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = null) },
            onClick = onTypeValuesClick,
        )
        Button(
            modifier = Modifier.fillMaxWidth().testTag(SCAN_FORM_BUTTON_TAG),
            style = ButtonStyle.FILLED,
            text = stringResource(R.string.scan_values),
            icon = { Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null) },
            onClick = onScanFormClick,
        )
    }
}

@Preview(showBackground = false)
@Composable
private fun DataSetInitialActionsPreview() {
    DHIS2Theme {
        DataSetInitialActions(onTypeValuesClick = {}, onScanFormClick = {})
    }
}
