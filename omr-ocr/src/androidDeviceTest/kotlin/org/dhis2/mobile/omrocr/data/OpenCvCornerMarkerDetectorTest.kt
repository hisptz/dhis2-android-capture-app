package org.dhis2.mobile.omrocr.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

@RunWith(AndroidJUnit4::class)
class OpenCvCornerMarkerDetectorTest {
    private val markerSize = 22f
    private val markerTopLefts =
        listOf(
            60f to 50f,
            560f to 50f,
            60f to 410f,
        )

    @Before
    fun loadOpenCv() {
        assertTrue(OpenCv.isLoaded)
    }

    @Test
    fun findsOnlyTheSolidCornerSquaresOfASection() {
        val markers = OpenCvCornerMarkerDetector.findMarkers(sectionFrame().toGray())

        assertEquals(3, markers.size)
        val centers = markers.map { (it.box.x + it.box.width / 2f) to (it.box.y + it.box.height / 2f) }
        markerTopLefts.forEach { (left, top) ->
            val expectedX = left + markerSize / 2
            val expectedY = top + markerSize / 2
            assertTrue(
                "No marker found near ($expectedX, $expectedY) in $centers",
                centers.any { (x, y) -> kotlin.math.abs(x - expectedX) < 3 && kotlin.math.abs(y - expectedY) < 3 },
            )
        }
    }

    @Test
    fun findsNothingOnAPageWithoutCornerSquares() {
        val frame = sectionFrame(drawMarkers = false)

        assertTrue(OpenCvCornerMarkerDetector.findMarkers(frame.toGray()).isEmpty())
    }

    private fun sectionFrame(drawMarkers: Boolean = true): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(225, 225, 220))
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 20, 20) }
        val outline =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(20, 20, 20)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

        if (drawMarkers) {
            markerTopLefts.forEach { (left, top) ->
                canvas.drawRect(left, top, left + markerSize, top + markerSize, ink)
            }
        }
        repeat(5) { column ->
            val x = 180f + column * 60f
            canvas.drawCircle(x, 200f, 11f, ink)
            canvas.drawCircle(x, 260f, 11f, outline)
        }
        canvas.drawRect(200f, 320f, 230f, 350f, outline)
        ink.textSize = 24f
        canvas.drawText("Number of children vaccinated", 140f, 140f, ink)
        return bitmap
    }

    private fun Bitmap.toGray(): Mat {
        val rgba = Mat()
        val gray = Mat()
        Utils.bitmapToMat(this, rgba)
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        rgba.release()
        return gray
    }
}
