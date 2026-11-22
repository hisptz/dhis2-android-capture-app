package org.dhis2.mobile.omrocr.data

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.createBitmap
import org.dhis2.mobile.omrocr.ui.camera.FULL_FRAME
import org.dhis2.mobile.omrocr.ui.camera.MarkerCandidate
import org.dhis2.mobile.omrocr.ui.camera.SectionCorners
import org.dhis2.mobile.omrocr.ui.camera.findSectionMarkers
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.roundToInt

internal object OpenCvSectionCropper {
    fun crop(
        photo: Bitmap,
        expected: SectionCorners?,
        area: Rect = FULL_FRAME,
    ): Bitmap? {
        if (!OpenCv.isLoaded) return null
        val rgba = Mat()
        val gray = Mat()
        var section: Mat? = null
        try {
            Utils.bitmapToMat(photo, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            section = crop(gray, expected, area) ?: return null
            return createBitmap(section.cols(), section.rows()).also { Utils.matToBitmap(section, it) }
        } finally {
            rgba.release()
            gray.release()
            section?.release()
        }
    }

    fun crop(
        gray: Mat,
        expected: SectionCorners? = null,
        area: Rect = FULL_FRAME,
    ): Mat? {
        val markers = OpenCvCornerMarkerDetector.findMarkers(gray)
        val candidates = markers.map { it.toCandidate(gray.cols(), gray.rows()) }
        val section =
            findSectionMarkers(
                candidates = candidates,
                frameAspect = gray.cols().toFloat() / gray.rows(),
                area = area,
                expected = expected,
            ) ?: return null

        fun MarkerCandidate.marker() = markers[candidates.indexOfFirst { it === this }]
        val chosen = section.all.map { it.marker() }
        val middle = Point(chosen.sumOf { it.center.x } / chosen.size, chosen.sumOf { it.center.y } / chosen.size)
        val (topLeft, topRight, bottomRight, bottomLeft) = chosen.map { it.outerCorner(middle) }

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

    private fun DetectedMarker.outerCorner(sectionMiddle: Point): Point = corners.maxBy { distance(it, sectionMiddle) }

    private fun distance(
        a: Point,
        b: Point,
    ) = hypot(a.x - b.x, a.y - b.y)
}
