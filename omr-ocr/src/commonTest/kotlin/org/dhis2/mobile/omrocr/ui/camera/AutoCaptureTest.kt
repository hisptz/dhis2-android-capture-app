package org.dhis2.mobile.omrocr.ui.camera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoCaptureTest {
    private val square = 1f

    private val topLeft = marker(0.2f, 0.2f)
    private val topRight = marker(0.8f, 0.2f)
    private val bottomRight = marker(0.8f, 0.7f)
    private val bottomLeft = marker(0.2f, 0.7f)
    private val sectionMarkers = listOf(topLeft, topRight, bottomRight, bottomLeft)

    private val upperSection = listOf(marker(0.2f, 0.05f), marker(0.8f, 0.05f), marker(0.8f, 0.35f), marker(0.2f, 0.35f))
    private val lowerSection = listOf(marker(0.2f, 0.45f), marker(0.8f, 0.45f), marker(0.8f, 0.8f), marker(0.2f, 0.8f))

    @Test
    fun `the four corner markers are named after their place in the image`() {
        val markers = findSectionMarkers(sectionMarkers.shuffled(), frameAspect = square)

        assertEquals(SectionMarkers(topLeft, topRight, bottomRight, bottomLeft), markers)
    }

    @Test
    fun `a slightly turned section keeps its corner names`() {
        val turned = sectionMarkers.map { it.copy(center = it.center.turnedAround(Offset(0.5f, 0.45f), degrees = 12.0)) }

        val markers = findSectionMarkers(turned.shuffled(), frameAspect = square)

        assertEquals(SectionMarkers(turned[0], turned[1], turned[2], turned[3]), markers)
    }

    @Test
    fun `three markers are not enough`() {
        assertNull(findSectionMarkers(sectionMarkers.drop(1), frameAspect = square))
    }

    @Test
    fun `four markers that are not the corners of a rectangle are not a section`() {
        val crooked = listOf(topLeft, topRight, marker(0.6f, 0.55f), bottomLeft)

        assertNull(findSectionMarkers(crooked, frameAspect = square))
    }

    @Test
    fun `two neighbouring sections are never joined into one`() {
        val left = listOf(marker(0.1f, 0.2f), marker(0.55f, 0.2f), marker(0.55f, 0.7f), marker(0.1f, 0.7f))
        val right = listOf(marker(0.65f, 0.2f), marker(0.95f, 0.2f), marker(0.95f, 0.7f), marker(0.65f, 0.7f))

        val markers = findSectionMarkers((left + right).shuffled(), frameAspect = square)!!

        assertEquals(left.toSet(), markers.all.toSet())
    }

    @Test
    fun `the section covering the middle of the search area is chosen`() {
        val markers = findSectionMarkers((upperSection + lowerSection).shuffled(), frameAspect = square)!!

        assertEquals(lowerSection.toSet(), markers.all.toSet())
    }

    @Test
    fun `the empty space between two sections is not taken for a section`() {
        val area = Rect(0f, 0f, 1f, 0.8f)
        val upperWithoutTopRight = listOf(marker(0.15f, 0.15f), marker(0.85f, 0.6f), marker(0.15f, 0.6f))
        val lowerTopMarkers = listOf(marker(0.15f, 0.75f), marker(0.85f, 0.75f))

        assertNull(findSectionMarkers(upperWithoutTopRight + lowerTopMarkers, frameAspect = square, area = area))
    }

    @Test
    fun `the section matching the expected corners is chosen`() {
        val expected = SectionCorners(Offset(0.21f, 0.06f), Offset(0.79f, 0.05f), Offset(0.8f, 0.34f), Offset(0.2f, 0.36f))

        val markers = findSectionMarkers((upperSection + lowerSection).shuffled(), frameAspect = square, expected = expected)!!

        assertEquals(upperSection.toSet(), markers.all.toSet())
    }

    @Test
    fun `no section is chosen when none is near the expected corners`() {
        val expected = SectionCorners(Offset(0.5f, 0.5f), Offset(0.9f, 0.5f), Offset(0.9f, 0.9f), Offset(0.5f, 0.9f))

        assertNull(findSectionMarkers(sectionMarkers, frameAspect = square, expected = expected))
    }

    @Test
    fun `markers outside the search area are ignored`() {
        val area = Rect(0.1f, 0.1f, 0.9f, 0.9f)
        val outside = listOf(marker(0.02f, 0.02f), marker(0.98f, 0.02f), marker(0.98f, 0.98f), marker(0.02f, 0.98f))

        val markers = findSectionMarkers(sectionMarkers + outside, frameAspect = square, area = area)

        assertEquals(SectionMarkers(topLeft, topRight, bottomRight, bottomLeft), markers)
    }

    @Test
    fun `a section with a corner outside the search area is not accepted`() {
        val area = Rect(0.1f, 0.1f, 0.9f, 0.65f)

        assertNull(findSectionMarkers(sectionMarkers, frameAspect = square, area = area))
    }

    @Test
    fun `the angle is measured on the picture, not on the normalized coordinates`() {
        val skewed = listOf(marker(0.2f, 0.2f), marker(0.8f, 0.5f), marker(0.65f, 0.8f), marker(0.05f, 0.5f))

        assertTrue(findSectionMarkers(skewed, frameAspect = square) != null)
        assertNull(findSectionMarkers(skewed, frameAspect = 2f))
    }

    @Test
    fun `a section too small in the frame is not accepted`() {
        val tiny = listOf(marker(0.45f, 0.45f), marker(0.55f, 0.45f), marker(0.55f, 0.55f), marker(0.45f, 0.55f))

        assertNull(findSectionMarkers(tiny, frameAspect = square))
    }

    @Test
    fun `markers of very different sizes are not accepted`() {
        val mixed = listOf(topLeft, topRight, bottomRight, marker(0.2f, 0.7f, size = 0.1f))

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

    private fun Offset.turnedAround(
        pivot: Offset,
        degrees: Double,
    ): Offset {
        val angle = degrees * PI / 180
        val dx = x - pivot.x
        val dy = y - pivot.y
        return Offset(
            pivot.x + (dx * cos(angle) - dy * sin(angle)).toFloat(),
            pivot.y + (dx * sin(angle) + dy * cos(angle)).toFloat(),
        )
    }

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
