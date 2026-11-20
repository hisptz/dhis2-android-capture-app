package org.dhis2.mobile.omrocr.ui.camera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

val FULL_FRAME = Rect(left = 0f, top = 0f, right = 1f, bottom = 1f)

private const val MIN_SECTION_AREA_FRACTION = 0.15f

private const val MAX_MARKER_SIZE_RATIO = 2.5f

private const val MAX_CORNER_COSINE = 0.26f

private const val MAX_CANDIDATES = 24

data class MarkerCandidate(
    val center: Offset,
    val size: Float,
)

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
