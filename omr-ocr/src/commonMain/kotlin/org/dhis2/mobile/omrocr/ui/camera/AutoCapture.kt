package org.dhis2.mobile.omrocr.ui.camera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

val FULL_FRAME = Rect(left = 0f, top = 0f, right = 1f, bottom = 1f)

private const val MIN_SECTION_AREA_FRACTION = 0.1f

private const val MAX_MARKER_SIZE_RATIO = 2f

private const val MAX_CORNER_COSINE = 0.3f

private const val MAX_FOURTH_CORNER_OFFSET = 0.2f

private const val INSIDE_MARGIN = 1.05f

private const val MAX_SHIFT_FROM_EXPECTED = 0.1f

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

    internal fun maxShiftTo(other: SectionCorners): Float = points.zip(other.points).maxOf { (a, b) -> max(abs(a.x - b.x), abs(a.y - b.y)) }
}

data class SectionMarkers(
    val topLeft: MarkerCandidate,
    val topRight: MarkerCandidate,
    val bottomRight: MarkerCandidate,
    val bottomLeft: MarkerCandidate,
) {
    val all: List<MarkerCandidate> get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    val corners: SectionCorners
        get() =
            SectionCorners(
                topLeft = topLeft.center,
                topRight = topRight.center,
                bottomRight = bottomRight.center,
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
    expected: SectionCorners? = null,
): SectionMarkers? {
    val inside =
        candidates
            .filter { area.contains(it.center) && it.size > 0f }
            .sortedByDescending { it.size }
            .take(MAX_CANDIDATES)
    if (inside.size < 4) return null

    fun Offset.toPicture() = Offset(x * frameAspect, y)
    val minArea = area.width * frameAspect * area.height * MIN_SECTION_AREA_FRACTION
    val areaMiddle = area.center.toPicture()

    var best: SectionMarkers? = null
    var bestScore = Float.MAX_VALUE
    for (corner in inside) {
        for (i in inside.indices) {
            for (j in i + 1 until inside.size) {
                val first = inside[i]
                val second = inside[j]
                if (first === corner || second === corner) continue

                val toFirst = first.center.toPicture() - corner.center.toPicture()
                val toSecond = second.center.toPicture() - corner.center.toPicture()
                val shorterSide = min(toFirst.getDistance(), toSecond.getDistance())
                if (shorterSide == 0f) continue
                val cosine = (toFirst.x * toSecond.x + toFirst.y * toSecond.y) / (toFirst.getDistance() * toSecond.getDistance())
                if (abs(cosine) > MAX_CORNER_COSINE) continue
                if (abs(toFirst.x * toSecond.y - toFirst.y * toSecond.x) < minArea) continue

                val predicted = corner.center.toPicture() + toFirst + toSecond
                val fourth =
                    inside
                        .filter { it !== corner && it !== first && it !== second }
                        .minByOrNull { (it.center.toPicture() - predicted).getDistance() }
                        ?: continue
                if ((fourth.center.toPicture() - predicted).getDistance() > shorterSide * MAX_FOURTH_CORNER_OFFSET) continue

                val group = listOf(corner, first, second, fourth)
                if (group.maxOf { it.size } / group.minOf { it.size } > MAX_MARKER_SIZE_RATIO) continue

                val markers = group.toSectionMarkers(frameAspect)
                val outline = markers.all.map { it.center.toPicture() }.grownBy(INSIDE_MARGIN)
                val hasMarkerInside =
                    candidates.any { other ->
                        group.none { it === other } && outline.surrounds(other.center.toPicture())
                    }
                if (hasMarkerInside) continue

                val score =
                    if (expected != null) {
                        markers.corners.maxShiftTo(expected).takeIf { it <= MAX_SHIFT_FROM_EXPECTED } ?: continue
                    } else {
                        if (!outline.surrounds(areaMiddle)) continue
                        (outline.middle() - areaMiddle).getDistance()
                    }
                if (score < bestScore) {
                    best = markers
                    bestScore = score
                }
            }
        }
    }
    return best
}

private fun List<MarkerCandidate>.toSectionMarkers(frameAspect: Float): SectionMarkers {
    fun MarkerCandidate.picture() = Offset(center.x * frameAspect, center.y)
    val middle = map { it.picture() }.middle()
    val clockwise = sortedBy { atan2(it.picture().y - middle.y, it.picture().x - middle.x) }
    val start = clockwise.indices.minBy { (clockwise[it].picture().x - middle.x) + (clockwise[it].picture().y - middle.y) }
    val ordered = List(4) { clockwise[(start + it) % 4] }
    return SectionMarkers(topLeft = ordered[0], topRight = ordered[1], bottomRight = ordered[2], bottomLeft = ordered[3])
}

private fun List<Offset>.middle(): Offset = Offset(sumOf { it.x.toDouble() }.toFloat() / size, sumOf { it.y.toDouble() }.toFloat() / size)

private fun List<Offset>.grownBy(factor: Float): List<Offset> {
    val middle = middle()
    return map { middle + (it - middle) * factor }
}

private fun List<Offset>.surrounds(point: Offset): Boolean =
    indices.all { index ->
        val start = this[index]
        val end = this[(index + 1) % size]
        (end.x - start.x) * (point.y - start.y) - (end.y - start.y) * (point.x - start.x) >= 0f
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
