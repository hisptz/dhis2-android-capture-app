package org.dhis2.mobile.omrocr.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.dhis2.mobile.omrocr.resources.Res
import org.dhis2.mobile.omrocr.resources.form_scan_cancel
import org.dhis2.mobile.omrocr.resources.form_scan_discard
import org.dhis2.mobile.omrocr.resources.form_scan_discard_description
import org.dhis2.mobile.omrocr.resources.form_scan_discard_title
import org.hisp.dhis.mobile.ui.designsystem.component.BottomSheetShell
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.ButtonStyle
import org.hisp.dhis.mobile.ui.designsystem.component.ColorStyle
import org.hisp.dhis.mobile.ui.designsystem.component.state.BottomSheetShellUIState
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.jetbrains.compose.resources.stringResource

@Composable
fun DiscardScanBottomSheet(
    onDiscardConfirmed: () -> Unit,
    onDiscardDismissed: () -> Unit,
) {
    BottomSheetShell(
        uiState =
            BottomSheetShellUIState(
                title = stringResource(Res.string.form_scan_discard_title),
                description = stringResource(Res.string.form_scan_discard_description),
            ),
        content = null,
        buttonBlock = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Spacing8)) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.FILLED,
                    colorStyle = ColorStyle.ERROR,
                    text = stringResource(Res.string.form_scan_discard),
                    onClick = onDiscardConfirmed,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.OUTLINED,
                    text = stringResource(Res.string.form_scan_cancel),
                    onClick = onDiscardDismissed,
                )
            }
        },
        onDismiss = onDiscardDismissed,
    )
}

@Preview
@Composable
private fun DiscardScanBottomSheetPreview() {
    DHIS2Theme {
        DiscardScanBottomSheet(onDiscardConfirmed = {}, onDiscardDismissed = {})
    }
}
