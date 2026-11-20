package org.dhis2.mobile.omrocr.ui.camera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoCaptureTest {
    private val square = 1f

    private val topLeft = marker(0.2f, 0.2f)
    private val topRight = marker(0.8f, 0.2f)
    private val bottomLeft = marker(0.2f, 0.7f)
    private val sectionMarkers = listOf(topLeft, topRight, bottomLeft)

    @Test
    fun `the three corner markers are named after the form corners`() {
        val markers = findSectionMarkers(sectionMarkers.shuffled(), frameAspect = square)

        assertEquals(SectionMarkers(topLeft, topRight, bottomLeft), markers)
        assertClose(Offset(0.8f, 0.7f), markers!!.corners.bottomRight)
    }

    @Test
    fun `an upside down section keeps its form corners`() {
        val turned = sectionMarkers.map { it.copy(center = Offset(1f - it.center.x, 1f - it.center.y)) }

        val corners = findSectionMarkers(turned, frameAspect = square)!!.corners

        assertClose(Offset(0.8f, 0.8f), corners.topLeft)
        assertClose(Offset(0.2f, 0.8f), corners.topRight)
        assertClose(Offset(0.8f, 0.3f), corners.bottomLeft)
        assertClose(Offset(0.2f, 0.3f), corners.bottomRight)
    }

    @Test
    fun `a section turned a quarter keeps its form corners`() {
        val turned = sectionMarkers.map { it.copy(center = rotateNormalized(it.center, 90)) }

        val corners = findSectionMarkers(turned, frameAspect = square)!!.corners

        assertClose(rotateNormalized(topLeft.center, 90), corners.topLeft)
        assertClose(rotateNormalized(topRight.center, 90), corners.topRight)
        assertClose(rotateNormalized(bottomLeft.center, 90), corners.bottomLeft)
    }

    @Test
    fun `dark squares inside the section do not replace the corner markers`() {
        val noise = listOf(marker(0.5f, 0.4f), marker(0.3f, 0.5f), marker(0.7f, 0.3f), marker(0.6f, 0.6f))

        val markers = findSectionMarkers(sectionMarkers + noise, frameAspect = square)

        assertEquals(SectionMarkers(topLeft, topRight, bottomLeft), markers)
    }

    @Test
    fun `markers outside the search area are ignored`() {
        val area = Rect(0.1f, 0.1f, 0.9f, 0.9f)
        val outside = listOf(marker(0.02f, 0.02f), marker(0.98f, 0.02f), marker(0.02f, 0.98f))

        val markers = findSectionMarkers(sectionMarkers + outside, frameAspect = square, area = area)

        assertEquals(SectionMarkers(topLeft, topRight, bottomLeft), markers)
    }

    @Test
    fun `a section whose missing corner falls outside the search area is not accepted`() {
        val area = Rect(0.1f, 0.1f, 0.9f, 0.65f)
        val tilted = listOf(marker(0.3f, 0.2f), marker(0.85f, 0.3f), marker(0.2f, 0.6f))

        assertNull(findSectionMarkers(tilted, frameAspect = square, area = area))
        assertTrue(findSectionMarkers(tilted, frameAspect = square) != null)
    }

    @Test
    fun `two markers are not enough`() {
        assertNull(findSectionMarkers(sectionMarkers.drop(1), frameAspect = square))
    }

    @Test
    fun `markers that do not meet at a right angle are not a section`() {
        val slanted = listOf(marker(0.2f, 0.2f), marker(0.8f, 0.2f), marker(0.6f, 0.7f))

        assertNull(findSectionMarkers(slanted, frameAspect = square))
    }

    @Test
    fun `the angle is measured on the picture, not on the normalized coordinates`() {
        val skewed = listOf(marker(0.2f, 0.2f), marker(0.8f, 0.5f), marker(0.05f, 0.5f))

        assertTrue(findSectionMarkers(skewed, frameAspect = square) != null)
        assertNull(findSectionMarkers(skewed, frameAspect = 2f))
    }

    @Test
    fun `a section too small in the frame is not accepted`() {
        val tiny = listOf(marker(0.45f, 0.45f), marker(0.55f, 0.45f), marker(0.45f, 0.55f))

        assertNull(findSectionMarkers(tiny, frameAspect = square))
    }

    @Test
    fun `markers of very different sizes are not accepted`() {
        val mixed = listOf(topLeft, topRight, marker(0.2f, 0.7f, size = 0.1f))

        assertNull(findSectionMarkers(mixed, frameAspect = square))
    }

    @Test
    fun `frame points are rotated into the upright image`() {
        val point = Offset(0.1f, 0.3f)

        assertEquals(point, rotateNormalized(point, 0))
        assertClose(Offset(0.7f, 0.1f), rotateNormalized(point, 90))
        assertClose(Offset(0.9f, 0.7f), rotateNormalized(point, 180))
        assertClose(Offset(0.3f, 0.9f), rotateNormalized(point, 270))
    }

    @Test
    fun `tracker searches until the section is found`() {
        val tracker = AutoCaptureTracker(holdMillis = 800)

        assertEquals(AutoCaptureState.Searching, tracker.update(null, 0))
    }

    @Test
    fun `tracker is ready once the section stays still for the hold time`() {
        val tracker = AutoCaptureTracker(holdMillis = 800)
        val corners = corners()

        val holding = tracker.update(corners, 1_000)
        assertIs<AutoCaptureState.Holding>(holding)
        assertEquals(0f, holding.progress)
        assertEquals(0.5f, (tracker.update(corners, 1_400) as AutoCaptureState.Holding).progress)
        assertIs<AutoCaptureState.Ready>(tracker.update(corners, 1_800))
    }

    @Test
    fun `moving the camera restarts the countdown`() {
        val tracker = AutoCaptureTracker(holdMillis = 800, maxCornerShift = 0.02f)

        tracker.update(corners(), 0)
        tracker.update(corners(), 600)
        val moved = tracker.update(corners(shift = 0.05f), 700)

        assertEquals(0f, (moved as AutoCaptureState.Holding).progress)
        assertIs<AutoCaptureState.Holding>(tracker.update(corners(shift = 0.05f), 1_400))
        assertIs<AutoCaptureState.Ready>(tracker.update(corners(shift = 0.05f), 1_500))
    }

    @Test
    fun `a few missed frames keep the countdown, more go back to searching`() {
        val tracker = AutoCaptureTracker(holdMillis = 800, maxMissedFrames = 2)

        tracker.update(corners(), 0)
        assertIs<AutoCaptureState.Holding>(tracker.update(null, 900))
        assertIs<AutoCaptureState.Holding>(tracker.update(null, 950))
        assertIs<AutoCaptureState.Ready>(tracker.update(corners(), 1_000))

        tracker.update(null, 1_100)
        tracker.update(null, 1_200)
        assertEquals(AutoCaptureState.Searching, tracker.update(null, 1_300))
    }

    @Test
    fun `a missed frame never completes the countdown`() {
        val tracker = AutoCaptureTracker(holdMillis = 800)

        tracker.update(corners(), 0)

        assertIs<AutoCaptureState.Holding>(tracker.update(null, 5_000))
    }

    private fun marker(
        x: Float,
        y: Float,
        size: Float = 0.03f,
    ) = MarkerCandidate(center = Offset(x, y), size = size)

    private fun corners(shift: Float = 0f) =
        SectionCorners(
            topLeft = Offset(0.2f + shift, 0.2f),
            topRight = Offset(0.8f + shift, 0.2f),
            bottomRight = Offset(0.8f + shift, 0.7f),
            bottomLeft = Offset(0.2f + shift, 0.7f),
        )

    private fun assertClose(
        expected: Offset,
        actual: Offset,
    ) {
        assertTrue(abs(expected.x - actual.x) < 1e-4f && abs(expected.y - actual.y) < 1e-4f, "Expected $expected but was $actual")
    }
}
