package org.dhis2.mobile.omrocr.data

import androidx.compose.ui.geometry.Offset
import org.dhis2.mobile.omrocr.ui.camera.MarkerCandidate
import org.dhis2.mobile.omrocr.ui.camera.markerSize
import org.dhis2.mobile.omrocr.ui.camera.rotateNormalized
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min

private const val MIN_MARKER_AREA_FRACTION = 0.0002
private const val MAX_MARKER_AREA_FRACTION = 0.03

private const val MIN_MARKER_ASPECT = 0.5
private const val MAX_MARKER_ASPECT = 2.0

private const val MIN_MARKER_FILL = 0.85

private const val THRESHOLD_OFFSET = 10.0

internal data class DetectedMarker(
    val box: Rect,
    val corners: List<Point>,
)

internal object OpenCvCornerMarkerDetector {
    fun findMarkers(gray: Mat): List<DetectedMarker> {
        val blurred = Mat()
        val binary = Mat()
        val hierarchy = Mat()
        val contours = mutableListOf<MatOfPoint>()
        try {
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            val blockSize = max(min(gray.cols(), gray.rows()) / 4, 3) or 1
            Imgproc.adaptiveThreshold(
                blurred,
                binary,
                255.0,
                Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY_INV,
                blockSize,
                THRESHOLD_OFFSET,
            )
            // RETR_LIST, not RETR_EXTERNAL: on a dark table the squares are nested inside the table's outline.
            Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

            val frameArea = gray.cols().toDouble() * gray.rows()
            return contours.mapNotNull { contour ->
                val area = Imgproc.contourArea(contour)
                if (area < frameArea * MIN_MARKER_AREA_FRACTION || area > frameArea * MAX_MARKER_AREA_FRACTION) {
                    return@mapNotNull null
                }
                val box = Imgproc.boundingRect(contour)
                val aspect = box.width.toDouble() / box.height
                if (aspect < MIN_MARKER_ASPECT || aspect > MAX_MARKER_ASPECT) return@mapNotNull null
                if (binary.darkShareOf(box) < MIN_MARKER_FILL) return@mapNotNull null
                DetectedMarker(box = box, corners = contour.squareCorners())
            }
        } finally {
            blurred.release()
            binary.release()
            hierarchy.release()
            contours.forEach { it.release() }
        }
    }

    private fun MatOfPoint.squareCorners(): List<Point> {
        val points = MatOfPoint2f(*toArray())
        return try {
            val corners = arrayOfNulls<Point>(4)
            Imgproc.minAreaRect(points).points(corners)
            corners.filterNotNull()
        } finally {
            points.release()
        }
    }

    private fun Mat.darkShareOf(box: Rect): Double {
        val region = submat(box)
        return try {
            Core.countNonZero(region).toDouble() / (box.width.toDouble() * box.height)
        } finally {
            region.release()
        }
    }
}

internal fun DetectedMarker.toCandidate(
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int = 0,
): MarkerCandidate {
    val width = imageWidth.toFloat()
    val height = imageHeight.toFloat()
    return MarkerCandidate(
        center =
            rotateNormalized(
                Offset((box.x + box.width / 2f) / width, (box.y + box.height / 2f) / height),
                rotationDegrees,
            ),
        size = markerSize(box.width.toFloat(), box.height.toFloat(), width, height),
    )
}
