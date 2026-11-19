package org.dhis2.mobile.omrocr.ui.camera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class FillCenterMappingTest {
    @Test
    fun `a frame wider than the view is cropped on the sides`() {
        // A 3:4 frame filling a 300 x 600 view is shown 450 wide, so 75 is cut on each side.
        val mapping = FillCenterMapping(Size(300f, 600f), frameAspect = 3f / 4f)

        assertEquals(Offset(-75f, 0f), mapping.toView(Offset(0f, 0f)))
        assertEquals(Offset(375f, 600f), mapping.toView(Offset(1f, 1f)))
        assertEquals(Offset(0.5f, 0.5f), mapping.toFrame(Offset(150f, 300f)))
    }

    @Test
    fun `a frame taller than the view is cropped at the top and bottom`() {
        val mapping = FillCenterMapping(Size(600f, 600f), frameAspect = 3f / 4f)

        assertEquals(Offset(0f, -100f), mapping.toView(Offset(0f, 0f)))
        // Shown 800 tall, so 100 is cut at the top and at the bottom.
        assertEquals(Rect(0f, 0.125f, 1f, 0.875f), mapping.toFrame(Rect(0f, 0f, 600f, 600f)))
    }
}
