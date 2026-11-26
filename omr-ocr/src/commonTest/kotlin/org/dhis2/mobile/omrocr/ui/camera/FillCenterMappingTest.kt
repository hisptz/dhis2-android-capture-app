package org.dhis2.mobile.omrocr.ui.camera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class FillCenterMappingTest {
    @Test
    fun `a frame wider than the view is cropped on the sides`() {
        val mapping = FillCenterMapping(Size(300f, 600f), frameAspect = 3f / 4f)

        assertEquals(Offset(-75f, 0f), mapping.toView(Offset(0f, 0f)))
        assertEquals(Offset(375f, 600f), mapping.toView(Offset(1f, 1f)))
        assertEquals(Offset(0.5f, 0.5f), mapping.toFrame(Offset(150f, 300f)))
    }

    @Test
    fun `a frame taller than the view is cropped at the top and bottom`() {
        val mapping = FillCenterMapping(Size(600f, 600f), frameAspect = 3f / 4f)

        assertEquals(Offset(0f, -100f), mapping.toView(Offset(0f, 0f)))
        assertEquals(Rect(0f, 0.125f, 1f, 0.875f), mapping.toFrame(Rect(0f, 0f, 600f, 600f)))
    }
}
