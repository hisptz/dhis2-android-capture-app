package org.dhis2.mobile.omrocr.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhis2.mobile.omrocr.domain.FormImageProcessor
import org.dhis2.mobile.omrocr.model.FormScanResult
import org.dhis2.mobile.omrocr.model.OmrField
import org.dhis2.mobile.omrocr.model.OmrFormConfig
import org.dhis2.mobile.omrocr.model.ScannedDataValue
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.roundToInt

private const val PREVIEW_MAX_SIDE_PX = 1280

class OpenCvFormImageProcessor : FormImageProcessor {
    override suspend fun process(
        image: ImageBitmap,
        config: OmrFormConfig,
    ): FormScanResult =
        withContext(Dispatchers.Default) {
            check(OpenCv.isLoaded) { "OpenCV native library could not be loaded" }

            val source = Mat()
            val gray = Mat()
            val preview = Mat()
            var section: Mat? = null
            try {
                Utils.bitmapToMat(image.asAndroidBitmap(), source)
                Imgproc.cvtColor(source, gray, Imgproc.COLOR_RGBA2GRAY)
                section = OpenCvSectionCropper.crop(gray)
                val page = section ?: gray

                val values =
                    config.fields.mapNotNull { field ->
                        readField(page, field)?.let { value ->
                            ScannedDataValue(
                                dataElementUid = field.dataElementUid,
                                categoryOptionComboUid = field.categoryOptionComboUid,
                                value = value,
                            )
                        }
                    }

                FormScanResult(
                    previewImage = page.toPreviewImage(preview),
                    values = values,
                )
            } finally {
                source.release()
                gray.release()
                preview.release()
                section?.release()
            }
        }

    /**
     * PLACEHOLDER: count the shaded circles of [field] here once the configuration is shared, and
     * return the value they stand for, or null when nothing is filled in. [page] is already cut
     * along the outer edges of the corner squares and upright, so circle positions can be given
     * relative to its width and height.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun readField(
        page: Mat,
        field: OmrField,
    ): String? = null

    private fun Mat.toPreviewImage(preview: Mat): ImageBitmap {
        val longestSide = max(cols(), rows())
        if (longestSide > PREVIEW_MAX_SIDE_PX) {
            val scale = PREVIEW_MAX_SIDE_PX.toDouble() / longestSide
            Imgproc.resize(
                this,
                preview,
                Size((cols() * scale).roundToInt().toDouble(), (rows() * scale).roundToInt().toDouble()),
                0.0,
                0.0,
                Imgproc.INTER_AREA,
            )
        } else {
            copyTo(preview)
        }
        val bitmap = createBitmap(preview.cols(), preview.rows())
        Utils.matToBitmap(preview, bitmap)
        return bitmap.asImageBitmap()
    }
}
