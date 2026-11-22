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
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class OpenCvCornerMarkerDetectorTest {
    private val markerSize = 35f
    private val markerTopLefts =
        listOf(
            60f to 50f,
            545f to 50f,
            545f to 395f,
            60f to 395f,
        )

    @Before
    fun loadOpenCv() {
        assertTrue(OpenCv.isLoaded)
    }

    @Test
    fun findsOnlyTheCornerMarkersOfASection() {
        val markers = OpenCvCornerMarkerDetector.findMarkers(sectionFrame().toGray())

        assertEquals(4, markers.size)
        val centers = markers.map { it.center.x to it.center.y }
        markerTopLefts.forEach { (left, top) ->
            val expectedX = left + markerSize / 2
            val expectedY = top + markerSize / 2
            assertTrue(
                "No marker found near ($expectedX, $expectedY) in $centers",
                centers.any { (x, y) -> abs(x - expectedX) < 3 && abs(y - expectedY) < 3 },
            )
        }
    }

    @Test
    fun findsNothingOnAPageWithoutCornerMarkers() {
        val frame = sectionFrame(drawMarkers = false)

        assertTrue(OpenCvCornerMarkerDetector.findMarkers(frame.toGray()).isEmpty())
    }

    private fun sectionFrame(drawMarkers: Boolean = true): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paperColor = Color.rgb(225, 225, 220)
        val shadeColor = Color.rgb(185, 185, 182)
        canvas.drawColor(paperColor)
        canvas.drawRect(40f, 390f, 600f, 435f, Paint().apply { color = shadeColor })
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 20, 20) }
        val outline =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(20, 20, 20)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

        canvas.drawRect(60f, 50f, 580f, 430f, outline)
        if (drawMarkers) {
            markerTopLefts.forEach { (left, top) ->
                val gapColor = if (top > 300f) shadeColor else paperColor
                canvas.drawMarker(left, top, markerSize, ink, gapColor)
            }
        }
        repeat(5) { column ->
            val x = 180f + column * 60f
            canvas.drawCircle(x, 200f, 11f, ink)
            canvas.drawCircle(x, 260f, 11f, outline)
        }
        canvas.drawRect(200f, 320f, 230f, 350f, outline)
        canvas.drawRect(300f, 320f, 330f, 350f, ink)
        ink.textSize = 24f
        canvas.drawText("Number of children vaccinated DOB", 120f, 140f, ink)
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

internal fun Canvas.drawMarker(
    left: Float,
    top: Float,
    size: Float,
    ink: Paint,
    gapColor: Int,
) {
    val unit = size / 7
    drawRect(left, top, left + size, top + size, ink)
    drawRect(left + unit, top + unit, left + size - unit, top + size - unit, Paint().apply { color = gapColor })
    drawRect(left + unit * 2, top + unit * 2, left + size - unit * 2, top + size - unit * 2, ink)
}
