package org.dhis2.mobile.omrocr.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.dhis2.mobile.omrocr.ui.camera.SectionCorners
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class OpenCvSectionCropperTest {
    private val photoWidth = 1000
    private val photoHeight = 1300
    private val sectionWidth = 700f
    private val sectionHeight = 500f
    private val centeredTop = 400f
    private val upperTop = 100f
    private val lowerTop = 700f
    private val sectionLeft = 150f
    private val markerSize = 56f

    @Before
    fun loadOpenCv() {
        assertTrue(OpenCv.isLoaded)
    }

    @Test
    fun cropsAlongTheOuterEdgesOfTheCornerMarkers() {
        val cropped = OpenCvSectionCropper.crop(photo().toGray())!!

        assertClose(sectionWidth, cropped.cols())
        assertClose(sectionHeight, cropped.rows())
        assertTrue("The marker ring should be in the top-left corner", cropped.isDarkAt(4, 4))
        assertTrue("The marker ring should be in the bottom-right corner", cropped.isDarkAt(cropped.cols() - 5, cropped.rows() - 5))
        assertTrue("The reference mark should be near the top-left", cropped.isDarkAtShare(0.2, 0.25))
    }

    @Test
    fun cropsTheExpectedSectionWhenTwoAreInThePhoto() {
        val upperCorners = cornersOf(upperTop)
        val lowerCorners = cornersOf(lowerTop)

        val upper = OpenCvSectionCropper.crop(photo(withLowerSection = true).toGray(), expected = upperCorners)!!
        val lower = OpenCvSectionCropper.crop(photo(withLowerSection = true).toGray(), expected = lowerCorners)!!

        assertTrue("The upper section has its reference mark near the top-left", upper.isDarkAtShare(0.2, 0.25))
        assertTrue("The lower section has its reference mark near the bottom-right", lower.isDarkAtShare(0.8, 0.75))
        assertTrue(!lower.isDarkAtShare(0.2, 0.25))
    }

    @Test
    fun straightensASlightlyTurnedPhoto() {
        val cropped = OpenCvSectionCropper.crop(photo().rotated(8f).toGray())!!

        assertClose(sectionWidth, cropped.cols())
        assertClose(sectionHeight, cropped.rows())
        assertTrue("The reference mark should be near the top-left", cropped.isDarkAtShare(0.2, 0.25))
    }

    @Test
    fun returnsNothingWhenTheCornerMarkersAreMissing() {
        assertNull(OpenCvSectionCropper.crop(photo(drawMarkers = false).toGray()))
    }

    private fun photo(
        drawMarkers: Boolean = true,
        withLowerSection: Boolean = false,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(photoWidth, photoHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(120, 100, 80))
        val paper = Paint().apply { color = Color.rgb(230, 230, 225) }
        canvas.drawRect(sectionLeft - 60, upperTop - 60, sectionLeft + sectionWidth + 60, photoHeight - 30f, paper)
        if (withLowerSection) {
            canvas.drawSection(upperTop, drawMarkers, markX = 0.2f, markY = 0.25f)
            canvas.drawSection(lowerTop, drawMarkers, markX = 0.8f, markY = 0.75f)
        } else {
            canvas.drawSection(centeredTop, drawMarkers, markX = 0.2f, markY = 0.25f)
        }
        return bitmap
    }

    private fun Canvas.drawSection(
        top: Float,
        drawMarkers: Boolean,
        markX: Float,
        markY: Float,
    ) {
        val ink = Paint().apply { color = Color.rgb(15, 15, 15) }
        val frame =
            Paint().apply {
                color = Color.rgb(15, 15, 15)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
        val right = sectionLeft + sectionWidth
        val bottom = top + sectionHeight
        drawRect(sectionLeft, top, right, bottom, frame)
        if (drawMarkers) {
            val gap = Color.rgb(230, 230, 225)
            drawMarker(sectionLeft, top, markerSize, ink, gap)
            drawMarker(right - markerSize, top, markerSize, ink, gap)
            drawMarker(right - markerSize, bottom - markerSize, markerSize, ink, gap)
            drawMarker(sectionLeft, bottom - markerSize, markerSize, ink, gap)
        }
        drawCircle(sectionLeft + sectionWidth * markX, top + sectionHeight * markY, 18f, ink)
    }

    private fun cornersOf(top: Float): SectionCorners {
        fun point(
            x: Float,
            y: Float,
        ) = Offset(x / photoWidth, y / photoHeight)
        val near = markerSize / 2
        val right = sectionLeft + sectionWidth
        val bottom = top + sectionHeight
        return SectionCorners(
            topLeft = point(sectionLeft + near, top + near),
            topRight = point(right - near, top + near),
            bottomRight = point(right - near, bottom - near),
            bottomLeft = point(sectionLeft + near, bottom - near),
        )
    }

    private fun Bitmap.rotated(degrees: Float): Bitmap =
        Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)

    private fun Bitmap.toGray(): Mat {
        val rgba = Mat()
        val gray = Mat()
        Utils.bitmapToMat(this, rgba)
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        rgba.release()
        return gray
    }

    private fun Mat.isDarkAtShare(
        x: Double,
        y: Double,
    ): Boolean = isDarkAt((cols() * x).toInt(), (rows() * y).toInt())

    private fun Mat.isDarkAt(
        x: Int,
        y: Int,
    ): Boolean {
        val centerX = x.coerceIn(4, cols() - 5)
        val centerY = y.coerceIn(4, rows() - 5)
        val region = submat(Rect(centerX - 4, centerY - 4, 9, 9))
        return try {
            Core.mean(region).`val`[0] < 80
        } finally {
            region.release()
        }
    }

    private fun assertClose(
        expected: Float,
        actual: Int,
    ) {
        assertTrue("Expected about $expected but was $actual", abs(expected - actual) <= 4)
    }
}
