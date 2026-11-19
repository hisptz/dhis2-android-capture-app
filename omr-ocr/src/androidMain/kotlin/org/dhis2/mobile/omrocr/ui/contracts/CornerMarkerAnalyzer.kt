package org.dhis2.mobile.omrocr.ui.contracts

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Rect
import org.dhis2.mobile.omrocr.data.OpenCvCornerMarkerDetector
import org.dhis2.mobile.omrocr.data.toCandidate
import org.dhis2.mobile.omrocr.ui.camera.AutoCaptureState
import org.dhis2.mobile.omrocr.ui.camera.AutoCaptureTracker
import org.dhis2.mobile.omrocr.ui.camera.SectionCorners
import org.dhis2.mobile.omrocr.ui.camera.findSectionMarkers
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.util.concurrent.TimeUnit

/**
 * Reports, for every preview frame, whether the section corner squares are inside [guide] and
 * steady enough to take the photo. [guide] is in normalized upright frame coordinates, or null
 * until the screen is laid out.
 */
internal class CornerMarkerAnalyzer(
    private val isAvailable: () -> Boolean,
    private val guide: () -> Rect?,
    private val onFrameAspect: (Float) -> Unit,
    private val onStateChanged: (AutoCaptureState) -> Unit,
) : ImageAnalysis.Analyzer {
    private val tracker = AutoCaptureTracker()

    override fun analyze(image: ImageProxy) {
        image.use {
            if (!isAvailable()) {
                onStateChanged(AutoCaptureState.Unavailable)
                return
            }
            val rotation = it.imageInfo.rotationDegrees
            val isSideways = rotation == 90 || rotation == 270
            val frameAspect =
                if (isSideways) it.height.toFloat() / it.width else it.width.toFloat() / it.height
            onFrameAspect(frameAspect)
            val area = guide() ?: return

            val timeMillis = TimeUnit.NANOSECONDS.toMillis(it.imageInfo.timestamp)
            onStateChanged(tracker.update(it.findSection(frameAspect, area), timeMillis))
        }
    }

    private fun ImageProxy.findSection(
        frameAspect: Float,
        area: Rect,
    ): SectionCorners? {
        // The first plane of a YUV frame is already the grayscale image, so it is used as is.
        val luminance = planes[0]
        val gray = Mat(height, width, CvType.CV_8UC1, luminance.buffer, luminance.rowStride.toLong())
        try {
            val candidates =
                OpenCvCornerMarkerDetector
                    .findMarkers(gray)
                    .map { marker -> marker.toCandidate(width, height, imageInfo.rotationDegrees) }
            return findSectionMarkers(candidates, frameAspect, area)?.corners
        } finally {
            gray.release()
        }
    }
}
