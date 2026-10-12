package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.model.PixelLayer
import com.example.model.PixelProject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PngExporter {

    /**
     * Composites all visible layers from bottom to top into a 1x logical resolution Android Bitmap.
     */
    fun compositeToBitmap(
        project: PixelProject,
        includeBackground: Boolean = true
    ): Bitmap {
        val width = project.width
        val height = project.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val compositedPixels = IntArray(width * height)

        // Fill background if specified
        if (includeBackground && project.backgroundColor != null) {
            val bgColor = project.backgroundColor!!
            compositedPixels.fill(bgColor)
        }

        // Iterate through visible layers from bottom to top
        for (layer in project.layers) {
            if (!layer.isVisible || layer.opacity <= 0.001f) continue

            val opacity = layer.opacity
            for (i in 0 until minOf(compositedPixels.size, layer.pixels.size)) {
                val srcColor = layer.pixels[i]
                val srcA = AndroidColor.alpha(srcColor)
                if (srcA == 0) continue

                val effectiveSrcA = (srcA * opacity).toInt().coerceIn(0, 255)
                if (effectiveSrcA == 0) continue

                val dstColor = compositedPixels[i]
                val dstA = AndroidColor.alpha(dstColor)

                if (dstA == 0 || effectiveSrcA == 255) {
                    val r = AndroidColor.red(srcColor)
                    val g = AndroidColor.green(srcColor)
                    val b = AndroidColor.blue(srcColor)
                    compositedPixels[i] = AndroidColor.argb(effectiveSrcA, r, g, b)
                } else {
                    // Standard Porter-Duff source-over alpha blending
                    val sa = effectiveSrcA / 255f
                    val da = dstA / 255f
                    val outA = sa + da * (1f - sa)

                    val sr = AndroidColor.red(srcColor)
                    val sg = AndroidColor.green(srcColor)
                    val sb = AndroidColor.blue(srcColor)

                    val dr = AndroidColor.red(dstColor)
                    val dg = AndroidColor.green(dstColor)
                    val db = AndroidColor.blue(dstColor)

                    val outR = ((sr * sa + dr * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                    val outG = ((sg * sa + dg * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                    val outB = ((sb * sa + db * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                    val finalA = (outA * 255).toInt().coerceIn(0, 255)

                    compositedPixels[i] = AndroidColor.argb(finalA, outR, outG, outB)
                }
            }
        }

        bitmap.setPixels(compositedPixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Scales bitmap to an exact integer scale using strictly NEAREST NEIGHBOR (filter = false).
     * This produces crisp, razor-sharp square pixels with zero anti-aliasing.
     */
    fun createScaledPixelBitmap(
        baseBitmap: Bitmap,
        scale: Int
    ): Bitmap {
        if (scale <= 1) return baseBitmap
        val targetWidth = baseBitmap.width * scale
        val targetHeight = baseBitmap.height * scale
        return Bitmap.createScaledBitmap(baseBitmap, targetWidth, targetHeight, false)
    }

    /**
     * Generates a Base64 encoded PNG string for fast project thumbnails.
     */
    fun generateThumbnailBase64(project: PixelProject): String {
        val baseBitmap = compositeToBitmap(project, includeBackground = true)
        val stream = ByteArrayOutputStream()
        baseBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Exports project to an OutputStream (e.g. for Storage Access Framework URI).
     */
    fun exportToStream(
        project: PixelProject,
        scale: Int,
        includeBackground: Boolean,
        outputStream: OutputStream
    ): Boolean {
        return try {
            val baseBitmap = compositeToBitmap(project, includeBackground)
            val finalBitmap = createScaledPixelBitmap(baseBitmap, scale)
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves PNG to app cache directory and launches Android Native ShareSheet.
     */
    fun shareArtwork(
        context: Context,
        project: PixelProject,
        scale: Int = 8,
        includeBackground: Boolean = true
    ): Intent? {
        return try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val sanitizedTitle = project.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
            val file = File(cacheDir, "${sanitizedTitle}_${project.width}x${project.height}_${scale}x.png")

            val baseBitmap = compositeToBitmap(project, includeBackground)
            val finalBitmap = createScaledPixelBitmap(baseBitmap, scale)

            FileOutputStream(file).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, project.title)
                putExtra(Intent.EXTRA_TEXT, "Created with PixelForge - ${project.title} (${project.width}x${project.height})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(shareIntent, "Share Pixel Art")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
