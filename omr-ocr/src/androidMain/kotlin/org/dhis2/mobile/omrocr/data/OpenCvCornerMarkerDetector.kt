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
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val DARK_SHARE_OF_LOCAL_BRIGHTNESS = 0.6

private const val MIN_CENTER_AREA_FRACTION = 0.00003
private const val MAX_CENTER_AREA_FRACTION = 0.01

private const val MIN_SQUARE_ASPECT = 0.5
private const val MAX_SQUARE_ASPECT = 2.0
private const val MIN_SQUARE_FILL = 0.75

private const val MIN_GAP_TO_CENTER_AREA = 1.8
private const val MAX_GAP_TO_CENTER_AREA = 4.5
private const val MIN_OUTSIDE_TO_GAP_AREA = 1.4
private const val MAX_CENTER_OFFSET = 0.15
private const val MAX_STRAY_AREA_IN_GAP = 0.1

private const val OUTER_TO_GAP_SIZE = 1.4

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
            Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
            if (contours.isEmpty()) return emptyList()

            val links = IntArray(contours.size * 4).also { hierarchy.get(0, 0, it) }
            val tree = ContourTree(contours, links)
            val frameArea = gray.cols().toDouble() * gray.rows()
            val maxSide = max(gray.cols(), gray.rows()).toDouble()
            return contours.indices.mapNotNull { index -> tree.markerAt(index, frameArea, maxSide) }
        } finally {
            blurred.release()
            threshold.release()
            binary.release()
            hierarchy.release()
            contours.forEach { it.release() }
        }
    }

    private class ContourTree(
        private val contours: List<MatOfPoint>,
        private val links: IntArray,
    ) {
        private val areas = HashMap<Int, Double>()

        private fun next(index: Int) = links[index * 4]

        private fun firstChild(index: Int) = links[index * 4 + 2]

        private fun parent(index: Int) = links[index * 4 + 3]

        private fun area(index: Int) = areas.getOrPut(index) { Imgproc.contourArea(contours[index]) }

        fun markerAt(
            index: Int,
            frameArea: Double,
            maxSide: Double,
        ): DetectedMarker? {
            val gap = parent(index).takeIf { it >= 0 } ?: return null
            val outside = parent(gap).takeIf { it >= 0 } ?: return null

            val centerArea = area(index)
            if (centerArea < frameArea * MIN_CENTER_AREA_FRACTION || centerArea > frameArea * MAX_CENTER_AREA_FRACTION) return null
            val centerSquare = contours[index].solidSquare(centerArea) ?: return null

            val gapArea = area(gap)
            if (gapArea / centerArea !in MIN_GAP_TO_CENTER_AREA..MAX_GAP_TO_CENTER_AREA) return null
            if (area(outside) < gapArea * MIN_OUTSIDE_TO_GAP_AREA) return null
            val gapSquare = contours[gap].solidSquare(gapArea) ?: return null

            val gapSide = max(gapSquare.size.width, gapSquare.size.height)
            if (distance(centerSquare.center, gapSquare.center) > gapSide * MAX_CENTER_OFFSET) return null
            if (strayAreaInside(gap, except = index) > centerArea * MAX_STRAY_AREA_IN_GAP) return null

            return DetectedMarker(
                center = centerSquare.center,
                size = sqrt(gapSquare.size.width * gapSquare.size.height) / maxSide,
                corners = gapSquare.cornersScaledBy(OUTER_TO_GAP_SIZE),
            )
        }

        private fun strayAreaInside(
            index: Int,
            except: Int,
        ): Double {
            var total = 0.0
            var child = firstChild(index)
            while (child >= 0) {
                if (child != except) total += area(child)
                child = next(child)
            }
            return total
        }
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

    private fun distance(
        a: Point,
        b: Point,
    ) = hypot(a.x - b.x, a.y - b.y)
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
