package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

object ImageImporter {

    /**
     * Imports an image from Uri and resamples it down to target width and height using nearest-neighbor.
     */
    fun importImageToPixelGrid(
        context: Context,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int
    ): IntArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val sourceBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (sourceBitmap == null) return null

            val scaledBitmap = Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, false)
            val pixels = IntArray(targetWidth * targetHeight)
            scaledBitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
            pixels
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
