package org.dhis2.mobile.omrocr.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    private val sectionLeft = 150f
    private val sectionTop = 120f
    private val sectionWidth = 700f
    private val sectionHeight = 500f
    private val markerSize = 30f

    @Before
    fun loadOpenCv() {
        assertTrue(OpenCv.isLoaded)
    }

    @Test
    fun cropsAlongTheOuterEdgesOfTheCornerSquares() {
        val cropped = OpenCvSectionCropper.crop(photo().toGray())!!

        assertClose(sectionWidth, cropped.cols())
        assertClose(sectionHeight, cropped.rows())
        assertTrue("The top-left square should be in the top-left corner", cropped.isDarkAt(0.02, 0.03))
        assertTrue("The bottom-right corner has no square", !cropped.isDarkAt(0.98, 0.97))
        assertTrue("The reference mark should be near the top-left", cropped.isDarkAt(0.2, 0.25))
    }

    @Test
    fun turnsAnUpsideDownPhotoTheRightWayUp() {
        val upsideDown = photo().rotated(180f)

        val cropped = OpenCvSectionCropper.crop(upsideDown.toGray())!!

        assertClose(sectionWidth, cropped.cols())
        assertClose(sectionHeight, cropped.rows())
        assertTrue("The reference mark should be near the top-left", cropped.isDarkAt(0.2, 0.25))
        assertTrue(!cropped.isDarkAt(0.8, 0.75))
    }

    @Test
    fun returnsNothingWhenTheCornerSquaresAreMissing() {
        assertNull(OpenCvSectionCropper.crop(photo(drawMarkers = false).toGray()))
    }

    /** A section on a dark table, with a mark near its top-left corner to check orientation. */
    private fun photo(drawMarkers: Boolean = true): Bitmap {
        val bitmap = Bitmap.createBitmap(1000, 750, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(120, 100, 80))
        val paper = Paint().apply { color = Color.rgb(230, 230, 225) }
        canvas.drawRect(sectionLeft - 40, sectionTop - 40, sectionLeft + sectionWidth + 40, sectionTop + sectionHeight + 40, paper)
        val ink = Paint().apply { color = Color.rgb(15, 15, 15) }
        if (drawMarkers) {
            val right = sectionLeft + sectionWidth
            val bottom = sectionTop + sectionHeight
            canvas.drawRect(sectionLeft, sectionTop, sectionLeft + markerSize, sectionTop + markerSize, ink)
            canvas.drawRect(right - markerSize, sectionTop, right, sectionTop + markerSize, ink)
            canvas.drawRect(sectionLeft, bottom - markerSize, sectionLeft + markerSize, bottom, ink)
        }
        canvas.drawCircle(sectionLeft + sectionWidth * 0.2f, sectionTop + sectionHeight * 0.25f, 18f, ink)
        return bitmap
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

    private fun Mat.isDarkAt(
        x: Double,
        y: Double,
    ): Boolean {
        val centerX = (cols() * x).toInt().coerceIn(4, cols() - 5)
        val centerY = (rows() * y).toInt().coerceIn(4, rows() - 5)
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
