package org.dhis2.mobile.omrocr.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.dhis2.mobile.omrocr.resources.Res
import org.dhis2.mobile.omrocr.resources.form_scan_take_photo
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.SurfaceColor
import org.jetbrains.compose.resources.stringResource

const val SHUTTER_BUTTON_TAG = "SHUTTER_BUTTON"

internal val SHUTTER_BUTTON_SIZE = 76.dp
private val SHUTTER_RING_WIDTH = 4.dp
private val SHUTTER_RING_GAP = 6.dp
private const val PRESSED_SCALE = 0.88f
private const val DISABLED_ALPHA = 0.4f

/**
 * Round camera shutter. [progress] (0..1) fills the ring in green while the automatic capture
 * waits for the camera to be held still.
 */
@Composable
fun ShutterButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val innerScale by animateFloatAsState(if (isPressed) PRESSED_SCALE else 1f)
    val description = stringResource(Res.string.form_scan_take_photo)

    Box(
        modifier =
            modifier
                .size(SHUTTER_BUTTON_SIZE)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(CircleShape)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ).semantics { contentDescription = description }
                .testTag(SHUTTER_BUTTON_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringWidth = SHUTTER_RING_WIDTH.toPx()
            val inset = ringWidth / 2
            val arcSize = size.copy(width = size.width - ringWidth, height = size.height - ringWidth)
            val arcTopLeft = Offset(inset, inset)
            drawArc(
                color = Color.White,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringWidth),
            )
            if (progress > 0f) {
                drawArc(
                    color = SurfaceColor.CustomGreen,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = ringWidth, cap = StrokeCap.Round),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(SHUTTER_RING_WIDTH + SHUTTER_RING_GAP)
                    .scale(innerScale)
                    .background(Color.White, CircleShape),
        )
    }
}

@Preview
@Composable
private fun ShutterButtonPreview() {
    DHIS2Theme {
        Box(modifier = Modifier.background(Color.Black).padding(16.dp)) {
            ShutterButton(enabled = true, onClick = {}, progress = 0.6f)
        }
    }
}
