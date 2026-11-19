package org.dhis2.mobile.omrocr.ui.camera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.max

/**
 * Maps between normalized coordinates of the upright camera frame and pixels of a preview that
 * fills [viewSize] by scaling the frame up and cropping what does not fit (CameraX FILL_CENTER).
 *
 * @param frameAspect width / height of the upright camera frame.
 */
class FillCenterMapping(
    viewSize: Size,
    frameAspect: Float,
) {
    private val shownHeight = max(viewSize.height, viewSize.width / frameAspect)
    private val shownWidth = shownHeight * frameAspect
    private val origin = Offset((viewSize.width - shownWidth) / 2f, (viewSize.height - shownHeight) / 2f)

    fun toView(point: Offset): Offset = Offset(origin.x + point.x * shownWidth, origin.y + point.y * shownHeight)

    fun toFrame(point: Offset): Offset = Offset((point.x - origin.x) / shownWidth, (point.y - origin.y) / shownHeight)

    fun toFrame(rect: Rect): Rect = Rect(toFrame(rect.topLeft), toFrame(rect.bottomRight))
}
