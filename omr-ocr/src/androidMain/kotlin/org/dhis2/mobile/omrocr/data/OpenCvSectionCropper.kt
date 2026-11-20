package org.dhis2.mobile.omrocr.data

import org.dhis2.mobile.omrocr.ui.camera.MarkerCandidate
import org.dhis2.mobile.omrocr.ui.camera.findSectionMarkers
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.roundToInt

internal object OpenCvSectionCropper {
    fun crop(gray: Mat): Mat? {
        val markers = OpenCvCornerMarkerDetector.findMarkers(gray)
        val candidates = markers.map { it.toCandidate(gray.cols(), gray.rows()) }
        val section =
            findSectionMarkers(candidates, frameAspect = gray.cols().toFloat() / gray.rows()) ?: return null

        fun MarkerCandidate.marker() = markers[candidates.indexOfFirst { it === this }]
        val topLeftMarker = section.topLeft.marker()
        val topRightMarker = section.topRight.marker()
        val bottomLeftMarker = section.bottomLeft.marker()

        val middle = midpoint(topRightMarker.center(), bottomLeftMarker.center())
        val topLeft = topLeftMarker.outerCorner(middle)
        val topRight = topRightMarker.outerCorner(middle)
        val bottomLeft = bottomLeftMarker.outerCorner(middle)
        val bottomRight = Point(topRight.x + bottomLeft.x - topLeft.x, topRight.y + bottomLeft.y - topLeft.y)

        val width = ((distance(topLeft, topRight) + distance(bottomLeft, bottomRight)) / 2).roundToInt()
        val height = ((distance(topLeft, bottomLeft) + distance(topRight, bottomRight)) / 2).roundToInt()
        if (width <= 0 || height <= 0) return null

        val source = MatOfPoint2f(topLeft, topRight, bottomRight, bottomLeft)
        val target =
            MatOfPoint2f(
                Point(0.0, 0.0),
                Point(width.toDouble(), 0.0),
                Point(width.toDouble(), height.toDouble()),
                Point(0.0, height.toDouble()),
            )
        val transform = Imgproc.getPerspectiveTransform(source, target)
        val cropped = Mat()
        try {
            Imgproc.warpPerspective(
                gray,
                cropped,
                transform,
                Size(width.toDouble(), height.toDouble()),
                Imgproc.INTER_LINEAR,
                Core.BORDER_REPLICATE,
            )
        } finally {
            source.release()
            target.release()
            transform.release()
        }
        return cropped
    }

    private fun DetectedMarker.center() = Point(box.x + box.width / 2.0, box.y + box.height / 2.0)

    private fun DetectedMarker.outerCorner(sectionMiddle: Point): Point = corners.maxBy { distance(it, sectionMiddle) }

    private fun midpoint(
        a: Point,
        b: Point,
    ) = Point((a.x + b.x) / 2, (a.y + b.y) / 2)

    private fun distance(
        a: Point,
        b: Point,
    ) = hypot(a.x - b.x, a.y - b.y)
}
