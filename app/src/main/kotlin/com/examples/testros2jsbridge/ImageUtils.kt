package com.example.testros2jsbridge

object ImageUtils {
    init {
        System.loadLibrary("imageUtils")
    }
    external fun bgrBase64ToArgb(base64: String, bitmap: android.graphics.Bitmap, width: Int, height: Int)
}