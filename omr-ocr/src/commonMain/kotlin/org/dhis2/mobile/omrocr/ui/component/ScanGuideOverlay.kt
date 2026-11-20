package org.dhis2.mobile.omrocr.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.dhis2.mobile.omrocr.resources.Res
import org.dhis2.mobile.omrocr.resources.form_scan_align_section
import org.dhis2.mobile.omrocr.resources.form_scan_align_section_manual
import org.dhis2.mobile.omrocr.resources.form_scan_hold_still
import org.dhis2.mobile.omrocr.ui.camera.AutoCaptureState
import org.dhis2.mobile.omrocr.ui.camera.FillCenterMapping
import org.dhis2.mobile.omrocr.ui.camera.SectionCorners
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.hisp.dhis.mobile.ui.designsystem.theme.SurfaceColor
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

const val SCAN_GUIDE_MESSAGE_TAG = "SCAN_GUIDE_MESSAGE"

private val SCRIM_COLOR = Color.Black.copy(alpha = 0.45f)
private val MESSAGE_BACKGROUND = Color.Black.copy(alpha = 0.55f)
private val GUIDE_MARGIN = 16.dp
private val GUIDE_CORNER_RADIUS = 12.dp
private val GUIDE_BRACKET_LENGTH = 32.dp
private val GUIDE_BRACKET_WIDTH = 4.dp
private val DETECTED_OUTLINE_WIDTH = 3.dp
private val DETECTED_CORNER_RADIUS = 6.dp

@Composable
fun ScanGuideOverlay(
    state: AutoCaptureState,
    frameAspect: Float,
    bottomInset: Dp,
    onGuideChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val corners =
        when (state) {
            is AutoCaptureState.Holding -> state.corners
            is AutoCaptureState.Ready -> state.corners
            else -> null
        }
    val guideColor = if (corners != null) SurfaceColor.CustomGreen else Color.White
    val currentOnGuideChanged by rememberUpdatedState(onGuideChanged)
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val viewSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val guide =
            with(density) {
                val margin = GUIDE_MARGIN.toPx()
                Rect(
                    left = margin,
                    top = margin,
                    right = viewSize.width - margin,
                    bottom = (viewSize.height - bottomInset.toPx()).coerceAtLeast(margin * 2),
                )
            }
        val mapping = remember(viewSize, frameAspect) { FillCenterMapping(viewSize, frameAspect) }

        LaunchedEffect(guide, mapping) { currentOnGuideChanged(mapping.toFrame(guide)) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawScrim(guide)
            drawGuideBrackets(guide, guideColor)
            corners?.let { drawDetectedSection(it, mapping) }
        }
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(guide.left.roundToInt(), guide.top.roundToInt()) }
                    .size(with(density) { guide.width.toDp() }, with(density) { guide.height.toDp() }),
            contentAlignment = Alignment.Center,
        ) {
            ScanGuideMessage(
                state = state,
                modifier = Modifier.padding(horizontal = Spacing.Spacing24),
            )
        }
    }
}

@Composable
private fun ScanGuideMessage(
    state: AutoCaptureState,
    modifier: Modifier = Modifier,
) {
    val message =
        when (state) {
            AutoCaptureState.Unavailable -> Res.string.form_scan_align_section_manual
            AutoCaptureState.Searching -> Res.string.form_scan_align_section
            is AutoCaptureState.Holding, is AutoCaptureState.Ready -> Res.string.form_scan_hold_still
        }
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .background(MESSAGE_BACKGROUND, RoundedCornerShape(Spacing.Spacing16))
                .padding(horizontal = Spacing.Spacing16, vertical = Spacing.Spacing8)
                .testTag(SCAN_GUIDE_MESSAGE_TAG),
    )
}

private fun DrawScope.drawScrim(frame: Rect) {
    val radius = GUIDE_CORNER_RADIUS.toPx()
    val path =
        Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(RoundRect(frame, CornerRadius(radius)))
        }
    drawPath(path, SCRIM_COLOR)
}

private fun DrawScope.drawGuideBrackets(
    frame: Rect,
    color: Color,
) {
    val length = GUIDE_BRACKET_LENGTH.toPx()
    val radius = GUIDE_CORNER_RADIUS.toPx()
    val stroke = Stroke(width = GUIDE_BRACKET_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val brackets =
        listOf(
            Triple(Offset(frame.left, frame.top + length), Offset(frame.left, frame.top), Offset(frame.left + length, frame.top)),
            Triple(Offset(frame.right - length, frame.top), Offset(frame.right, frame.top), Offset(frame.right, frame.top + length)),
            Triple(
                Offset(frame.right, frame.bottom - length),
                Offset(frame.right, frame.bottom),
                Offset(frame.right - length, frame.bottom),
            ),
            Triple(Offset(frame.left + length, frame.bottom), Offset(frame.left, frame.bottom), Offset(frame.left, frame.bottom - length)),
        )
    brackets.forEach { (start, corner, end) ->
        val path =
            Path().apply {
                moveTo(start.x, start.y)
                arcToCorner(start, corner, end, radius)
                lineTo(end.x, end.y)
            }
        drawPath(path, color, style = stroke)
    }
}

private fun Path.arcToCorner(
    start: Offset,
    corner: Offset,
    end: Offset,
    radius: Float,
) {
    val beforeCorner = corner + (start - corner).normalized() * radius
    val afterCorner = corner + (end - corner).normalized() * radius
    lineTo(beforeCorner.x, beforeCorner.y)
    quadraticTo(corner.x, corner.y, afterCorner.x, afterCorner.y)
}

private fun Offset.normalized(): Offset = if (getDistance() == 0f) this else this / getDistance()

private fun DrawScope.drawDetectedSection(
    corners: SectionCorners,
    mapping: FillCenterMapping,
) {
    val points = corners.points.map(mapping::toView)
    val outline =
        Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
    drawPath(outline, SurfaceColor.CustomGreen, style = Stroke(width = DETECTED_OUTLINE_WIDTH.toPx(), join = StrokeJoin.Round))
    corners.markerPoints.forEach {
        drawCircle(SurfaceColor.CustomGreen, radius = DETECTED_CORNER_RADIUS.toPx(), center = mapping.toView(it))
    }
}

@Preview
@Composable
private fun ScanGuideOverlaySearchingPreview() {
    DHIS2Theme {
        ScanGuideOverlay(
            state = AutoCaptureState.Searching,
            frameAspect = 3f / 4f,
            bottomInset = 124.dp,
            onGuideChanged = {},
            modifier = Modifier.size(360.dp, 640.dp).background(Color.DarkGray),
        )
    }
}

@Preview
@Composable
private fun ScanGuideOverlayHoldingPreview() {
    DHIS2Theme {
        ScanGuideOverlay(
            state =
                AutoCaptureState.Holding(
                    corners =
                        SectionCorners(
                            topLeft = Offset(0.2f, 0.2f),
                            topRight = Offset(0.8f, 0.18f),
                            bottomRight = Offset(0.82f, 0.7f),
                            bottomLeft = Offset(0.22f, 0.72f),
                        ),
                    progress = 0.5f,
                ),
            frameAspect = 3f / 4f,
            bottomInset = 124.dp,
            onGuideChanged = {},
            modifier = Modifier.size(360.dp, 640.dp).background(Color.DarkGray),
        )
    }
}
