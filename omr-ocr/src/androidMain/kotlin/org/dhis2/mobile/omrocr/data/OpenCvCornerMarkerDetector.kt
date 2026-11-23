package org.dhis2.mobile.omrocr.data

import androidx.compose.ui.geometry.Offset
import org.dhis2.mobile.omrocr.ui.camera.MarkerCandidate
import org.dhis2.mobile.omrocr.ui.camera.rotateNormalized
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.RotatedRect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val DARK_SHARE_OF_LOCAL_BRIGHTNESS = 0.6

private const val MIN_CENTER_AREA_FRACTION = 0.00003
private const val MAX_CENTER_AREA_FRACTION = 0.01

private const val MIN_SQUARE_ASPECT = 0.5
private const val MAX_SQUARE_ASPECT = 2.0
private const val MIN_SQUARE_FILL = 0.75
private const val MAX_HOLE_AREA = 0.1

private const val MAX_CENTER_EDGE = 1.4
private const val MIN_RING_START = 1.3
private const val MAX_RING_START = 2.6
private const val MIN_RING_WIDTH = 0.2
private const val MAX_RING_WIDTH = 1.2
private const val MAX_RAY_LENGTH = 4.5
private const val RAY_STEP_PIXELS = 0.5
private const val MIN_MATCHING_RAYS = 12
private const val MAX_RING_START_SPREAD = 0.25

private const val OUTER_TO_GAP_SIZE = 1.4

private val RAY_OFFSETS = listOf(-0.75, -0.25, 0.25, 0.75)
private val RAY_TARGETS =
    RAY_OFFSETS.flatMap { offset -> listOf(-1.0 to offset, 1.0 to offset, offset to -1.0, offset to 1.0) }

internal data class DetectedMarker(
    val center: Point,
    val size: Double,
    val corners: List<Point>,
)

internal object OpenCvCornerMarkerDetector {
    fun findMarkers(gray: Mat): List<DetectedMarker> {
        val blurred = Mat()
        val threshold = Mat()
        val binary = Mat()
        val hierarchy = Mat()
        val contours = mutableListOf<MatOfPoint>()
        try {
            Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.0)
            val blockSize = (max(min(gray.cols(), gray.rows()) / 8, 3) or 1).toDouble()
            Imgproc.blur(blurred, threshold, Size(blockSize, blockSize))
            Core.multiply(threshold, Scalar(DARK_SHARE_OF_LOCAL_BRIGHTNESS), threshold)
            Core.compare(blurred, threshold, binary, Core.CMP_LT)
            val ink = InkMap(binary)
            Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
            if (contours.isEmpty()) return emptyList()

            val links = IntArray(contours.size * 4).also { hierarchy.get(0, 0, it) }
            val frameArea = gray.cols().toDouble() * gray.rows()
            val maxSide = max(gray.cols(), gray.rows()).toDouble()
            return contours.indices.mapNotNull { index ->
                markerAt(contours, links, index, ink, frameArea, maxSide)
            }
        } finally {
            blurred.release()
            threshold.release()
            binary.release()
            hierarchy.release()
            contours.forEach { it.release() }
        }
    }

    private fun markerAt(
        contours: List<MatOfPoint>,
        links: IntArray,
        index: Int,
        ink: InkMap,
        frameArea: Double,
        maxSide: Double,
    ): DetectedMarker? {
        val area = Imgproc.contourArea(contours[index])
        if (area < frameArea * MIN_CENTER_AREA_FRACTION || area > frameArea * MAX_CENTER_AREA_FRACTION) return null
        val square = contours[index].solidSquare(area) ?: return null
        if (!ink.isDark(square.center.x, square.center.y)) return null

        var holeArea = 0.0
        var hole = links[index * 4 + 2]
        while (hole >= 0) {
            holeArea += Imgproc.contourArea(contours[hole])
            hole = links[hole * 4]
        }
        if (holeArea > area * MAX_HOLE_AREA) return null

        val gapScale = ink.ringStartAround(square) ?: return null
        val gapWidth = square.size.width * gapScale
        val gapHeight = square.size.height * gapScale
        return DetectedMarker(
            center = square.center,
            size = sqrt(gapWidth * gapHeight) / maxSide,
            corners = square.cornersScaledBy(gapScale * OUTER_TO_GAP_SIZE),
        )
    }

    private fun MatOfPoint.solidSquare(area: Double): RotatedRect? {
        val points = MatOfPoint2f(*toArray())
        val rect =
            try {
                Imgproc.minAreaRect(points)
            } finally {
                points.release()
            }
        val width = rect.size.width
        val height = rect.size.height
        if (width <= 0 || height <= 0) return null
        if (width / height !in MIN_SQUARE_ASPECT..MAX_SQUARE_ASPECT) return null
        if (area / (width * height) < MIN_SQUARE_FILL) return null
        return rect
    }

    private fun RotatedRect.cornersScaledBy(factor: Double): List<Point> {
        val corners = arrayOfNulls<Point>(4)
        points(corners)
        return corners.filterNotNull().map { Point(center.x + (it.x - center.x) * factor, center.y + (it.y - center.y) * factor) }
    }

    private class InkMap(
        binary: Mat,
    ) {
        private val width = binary.cols()
        private val height = binary.rows()
        private val pixels = ByteArray(width * height).also { binary.get(0, 0, it) }

        fun isDark(
            x: Double,
            y: Double,
        ): Boolean {
            val column = x.roundToInt()
            val row = y.roundToInt()
            return column in 0 until width && row in 0 until height && pixels[row * width + column].toInt() != 0
        }

        fun ringStartAround(square: RotatedRect): Double? {
            val angle = Math.toRadians(square.angle)
            val halfWidth = square.size.width / 2
            val halfHeight = square.size.height / 2
            val step = RAY_STEP_PIXELS / min(halfWidth, halfHeight)
            val starts =
                RAY_TARGETS.mapNotNull { (across, along) ->
                    val dx = across * halfWidth * cos(angle) - along * halfHeight * sin(angle)
                    val dy = across * halfWidth * sin(angle) + along * halfHeight * cos(angle)
                    ringStartAlong(square.center, dx, dy, step)
                }
            if (starts.size < MIN_MATCHING_RAYS) return null
            val median = starts.sorted()[starts.size / 2]
            if (starts.count { abs(it - median) <= median * MAX_RING_START_SPREAD } < MIN_MATCHING_RAYS) return null
            return median
        }

        private fun ringStartAlong(
            center: Point,
            dx: Double,
            dy: Double,
            step: Double,
        ): Double? {
            var reachedGap = false
            var ringStart: Double? = null
            var distance = 1.0
            while (distance < MAX_RAY_LENGTH) {
                val dark = isDark(center.x + distance * dx, center.y + distance * dy)
                val start = ringStart
                when {
                    !reachedGap ->
                        if (!dark) {
                            reachedGap = true
                        } else if (distance > MAX_CENTER_EDGE) {
                            return null
                        }
                    start == null ->
                        if (dark) {
                            ringStart = distance
                        } else if (distance > MAX_RING_START) {
                            return null
                        }
                    !dark -> return start.takeIf { it >= MIN_RING_START && distance - it in MIN_RING_WIDTH..MAX_RING_WIDTH }
                }
                distance += step
            }
            return null
        }
    }
}

internal fun DetectedMarker.toCandidate(
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int = 0,
): MarkerCandidate =
    MarkerCandidate(
        center =
            rotateNormalized(
                Offset((center.x / imageWidth).toFloat(), (center.y / imageHeight).toFloat()),
                rotationDegrees,
            ),
        size = size.toFloat(),
    )
