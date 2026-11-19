package org.dhis2.mobile.omrocr.ui.camera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

val FULL_FRAME = Rect(left = 0f, top = 0f, right = 1f, bottom = 1f)

/** Smaller sections would be too far away to read. */
private const val MIN_SECTION_AREA_FRACTION = 0.15f

private const val MAX_MARKER_SIZE_RATIO = 2.5f

/**
 * Largest cosine of the angle at the top-left square. The section is a rectangle, so the angle is 90
 * degrees on paper; 0.26 still accepts about 75 to 105 degrees when the phone is held at an angle.
 */
private const val MAX_CORNER_COSINE = 0.26f

private const val MAX_CANDIDATES = 24

/**
 * A solid dark square found in an image.
 * @param center position in normalized coordinates of the upright image (0..1 on each axis).
 * @param size side length relative to the image, only used to compare squares with each other.
 */
data class MarkerCandidate(
    val center: Offset,
    val size: Float,
)

/**
 * The corners of a section as printed on the form, in normalized coordinates of the upright image.
 * Only [topLeft], [topRight] and [bottomLeft] have a square; [bottomRight] is worked out from them.
 * When the paper is upside down in the photo, [topLeft] is still the form's top-left corner.
 */
data class SectionCorners(
    val topLeft: Offset,
    val topRight: Offset,
    val bottomRight: Offset,
    val bottomLeft: Offset,
) {
    val points: List<Offset> get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    val markerPoints: List<Offset> get() = listOf(topLeft, topRight, bottomLeft)

    internal fun maxShiftTo(other: SectionCorners): Float = points.zip(other.points).maxOf { (a, b) -> max(abs(a.x - b.x), abs(a.y - b.y)) }
}

data class SectionMarkers(
    val topLeft: MarkerCandidate,
    val topRight: MarkerCandidate,
    val bottomLeft: MarkerCandidate,
) {
    val corners: SectionCorners
        get() =
            SectionCorners(
                topLeft = topLeft.center,
                topRight = topRight.center,
                bottomRight = topRight.center + bottomLeft.center - topLeft.center,
                bottomLeft = bottomLeft.center,
            )
}

sealed interface AutoCaptureState {
    data object Unavailable : AutoCaptureState

    data object Searching : AutoCaptureState

    data class Holding(
        val corners: SectionCorners,
        val progress: Float,
    ) : AutoCaptureState

    data class Ready(
        val corners: SectionCorners,
    ) : AutoCaptureState
}

/**
 * Finds the three corner squares of a section among the [candidates] inside [area].
 *
 * Every section has squares on its top-left, top-right and bottom-left corners only. The top-left
 * one is where the other two meet at a right angle, and going from top-right to bottom-left turns
 * the same way whatever way up the paper is, so the corners are named after the form even when the
 * photo is rotated or upside down. When several sets fit, the one covering the largest area wins.
 *
 * @param frameAspect width / height of the upright image, so angles are measured on the real
 * picture rather than on the normalized coordinates.
 */
fun findSectionMarkers(
    candidates: List<MarkerCandidate>,
    frameAspect: Float,
    area: Rect = FULL_FRAME,
): SectionMarkers? {
    val inside =
        candidates
            .filter { area.contains(it.center) && it.size > 0f }
            .sortedByDescending { it.size }
            .take(MAX_CANDIDATES)
    if (inside.size < 3) return null

    fun Offset.toPicture() = Offset(x * frameAspect, y)
    val minArea = area.width * frameAspect * area.height * MIN_SECTION_AREA_FRACTION

    var best: SectionMarkers? = null
    var bestArea = 0f
    for (corner in inside) {
        for (i in inside.indices) {
            for (j in i + 1 until inside.size) {
                val first = inside[i]
                val second = inside[j]
                if (first === corner || second === corner) continue
                val sizes = listOf(corner.size, first.size, second.size)
                if (sizes.max() / sizes.min() > MAX_MARKER_SIZE_RATIO) continue

                val toFirst = first.center.toPicture() - corner.center.toPicture()
                val toSecond = second.center.toPicture() - corner.center.toPicture()
                val lengths = toFirst.getDistance() * toSecond.getDistance()
                if (lengths == 0f) continue
                val cosine = (toFirst.x * toSecond.x + toFirst.y * toSecond.y) / lengths
                if (abs(cosine) > MAX_CORNER_COSINE) continue

                val cross = toFirst.x * toSecond.y - toFirst.y * toSecond.x
                val sectionArea = abs(cross)
                if (sectionArea < minArea || sectionArea <= bestArea) continue

                val markers =
                    if (cross > 0f) {
                        SectionMarkers(topLeft = corner, topRight = first, bottomLeft = second)
                    } else {
                        SectionMarkers(topLeft = corner, topRight = second, bottomLeft = first)
                    }
                if (!area.contains(markers.corners.bottomRight)) continue
                best = markers
                bestArea = sectionArea
            }
        }
    }
    return best
}

/**
 * Turns a point of a camera frame, normalized to that frame, into upright coordinates by applying
 * the clockwise [rotationDegrees] the frame needs to be displayed upright.
 */
fun rotateNormalized(
    point: Offset,
    rotationDegrees: Int,
): Offset =
    when (rotationDegrees) {
        90 -> Offset(1f - point.y, point.x)
        180 -> Offset(1f - point.x, 1f - point.y)
        270 -> Offset(point.y, 1f - point.x)
        else -> point
    }

fun markerSize(
    width: Float,
    height: Float,
    imageWidth: Float,
    imageHeight: Float,
): Float = sqrt(width * height) / max(imageWidth, imageHeight)

/**
 * Decides when to take the photo: the corners have to stay within [maxCornerShift] of where they
 * were first seen for [holdMillis]. Up to [maxMissedFrames] frames in a row without a detection are
 * tolerated, so a single blurry frame does not restart the countdown.
 */
class AutoCaptureTracker(
    private val holdMillis: Long = 800,
    private val maxCornerShift: Float = 0.02f,
    private val maxMissedFrames: Int = 3,
) {
    private var anchor: SectionCorners? = null
    private var holdStartMillis = 0L
    private var missedFrames = 0

    fun update(
        corners: SectionCorners?,
        timeMillis: Long,
    ): AutoCaptureState {
        val current = anchor
        if (corners == null) {
            if (current == null) return AutoCaptureState.Searching
            missedFrames++
            if (missedFrames > maxMissedFrames) {
                anchor = null
                return AutoCaptureState.Searching
            }
            return AutoCaptureState.Holding(current, progress(timeMillis).coerceAtMost(HOLD_PROGRESS_CAP))
        }

        missedFrames = 0
        if (current == null || current.maxShiftTo(corners) > maxCornerShift) {
            anchor = corners
            holdStartMillis = timeMillis
        }
        val progress = progress(timeMillis)
        return if (progress >= 1f) {
            AutoCaptureState.Ready(corners)
        } else {
            AutoCaptureState.Holding(corners, progress)
        }
    }

    private fun progress(timeMillis: Long): Float = ((timeMillis - holdStartMillis).toFloat() / holdMillis).coerceIn(0f, 1f)

    private companion object {
        const val HOLD_PROGRESS_CAP = 0.99f
    }
}
