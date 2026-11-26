package org.dhis2.mobile.omrocr.data

import org.opencv.android.OpenCVLoader

internal object OpenCv {
    val isLoaded: Boolean by lazy { OpenCVLoader.initLocal() }
}
