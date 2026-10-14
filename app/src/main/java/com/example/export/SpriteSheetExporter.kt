package com.example.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.model.AnimationFrame
import com.example.model.PixelProject
import java.io.OutputStream
import kotlin.math.ceil
import kotlin.math.sqrt

object SpriteSheetExporter {

    data class SpriteSheetConfig(
        val columns: Int = 4,
        val spacing: Int = 2,
        val margin: Int = 4,
        val scale: Int = 2,
        val includeBackground: Boolean = false
    )

    fun createFramesSpriteSheet(
        frames: List<Bitmap>,
        config: SpriteSheetConfig
    ): Bitmap? {
        if (frames.isEmpty()) return null
        val count = frames.size
        val spriteW = frames[0].width * config.scale
        val spriteH = frames[0].height * config.scale

        val cols = config.columns.coerceIn(1, count)
        val rows = ceil(count.toDouble() / cols).toInt()

        val totalW = config.margin * 2 + cols * spriteW + (cols - 1) * config.spacing
        val totalH = config.margin * 2 + rows * spriteH + (rows - 1) * config.spacing

        // Memory safety check: limit sprite sheet size to 4096x4096
        if (totalW > 4096 || totalH > 4096) return null

        val sheetBitmap = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)
        val paint = Paint().apply {
            isFilterBitmap = false
            isDither = false
            isAntiAlias = false
        }

        val srcRect = Rect()
        val dstRect = Rect()

        for (i in 0 until count) {
            val col = i % cols
            val row = i / cols

            val destX = config.margin + col * (spriteW + config.spacing)
            val destY = config.margin + row * (spriteH + config.spacing)

            val frame = frames[i]
            srcRect.set(0, 0, frame.width, frame.height)
            dstRect.set(destX, destY, destX + spriteW, destY + spriteH)

            canvas.drawBitmap(frame, srcRect, dstRect, paint)
        }

        return sheetBitmap
    }

    fun exportToStream(
        sheetBitmap: Bitmap,
        outputStream: OutputStream
    ): Boolean {
        return try {
            sheetBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
