package com.docscan.util.textedit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Text Removal & Inpainting Engine. Completely eliminates original document text
 * underneath edited/deleted regions by sampling surrounding background paper texture and gradients.
 */
object DocumentTextInpainter {

    /**
     * Wipes multiple text regions on a copy of the source bitmap.
     */
    suspend fun inpaintRegions(
        source: Bitmap,
        regions: List<RectF>
    ): Bitmap = withContext(Dispatchers.Default) {
        if (regions.isEmpty()) return@withContext source

        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val w = output.width.toFloat()
        val h = output.height.toFloat()

        for (normRect in regions) {
            inpaintSingleRegionOnCanvas(output, canvas, normRect, w, h)
        }

        output
    }

    /**
     * Wipes a single text region on a copy of the source bitmap.
     */
    suspend fun inpaintRegion(
        source: Bitmap,
        normRect: RectF
    ): Bitmap = withContext(Dispatchers.Default) {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val w = output.width.toFloat()
        val h = output.height.toFloat()
        inpaintSingleRegionOnCanvas(output, canvas, normRect, w, h)
        output
    }

    private fun inpaintSingleRegionOnCanvas(
        bitmap: Bitmap,
        canvas: Canvas,
        normRect: RectF,
        bitmapW: Float,
        bitmapH: Float
    ) {
        val w = bitmap.width
        val h = bitmap.height

        val left = (normRect.left * bitmapW).toInt().coerceIn(0, w - 1)
        val top = (normRect.top * bitmapH).toInt().coerceIn(0, h - 1)
        val right = (normRect.right * bitmapW).toInt().coerceIn(left + 1, w)
        val bottom = (normRect.bottom * bitmapH).toInt().coerceIn(top + 1, h)

        val boxW = right - left
        val boxH = bottom - top

        // Sample top border color
        val topSampleY = (top - (boxH * 0.25f).toInt().coerceIn(2, 20)).coerceIn(0, h - 1)
        val bottomSampleY = (bottom + (boxH * 0.25f).toInt().coerceIn(2, 20)).coerceIn(0, h - 1)

        val stepX = max(1, boxW / 8)
        var topR = 0L; var topG = 0L; var topB = 0L; var topCount = 0
        var botR = 0L; var botG = 0L; var botB = 0L; var botCount = 0

        for (x in left until right step stepX) {
            val pTop = bitmap.getPixel(x, topSampleY)
            val lumTop = 0.299 * AndroidColor.red(pTop) + 0.587 * AndroidColor.green(pTop) + 0.114 * AndroidColor.blue(pTop)
            if (lumTop > 70) {
                topR += AndroidColor.red(pTop)
                topG += AndroidColor.green(pTop)
                topB += AndroidColor.blue(pTop)
                topCount++
            }

            val pBot = bitmap.getPixel(x, bottomSampleY)
            val lumBot = 0.299 * AndroidColor.red(pBot) + 0.587 * AndroidColor.green(pBot) + 0.114 * AndroidColor.blue(pBot)
            if (lumBot > 70) {
                botR += AndroidColor.red(pBot)
                botG += AndroidColor.green(pBot)
                botB += AndroidColor.blue(pBot)
                botCount++
            }
        }

        val topColor = if (topCount > 0) {
            AndroidColor.rgb((topR / topCount).toInt(), (topG / topCount).toInt(), (topB / topCount).toInt())
        } else {
            AndroidColor.WHITE
        }

        val botColor = if (botCount > 0) {
            AndroidColor.rgb((botR / botCount).toInt(), (botG / botCount).toInt(), (botB / botCount).toInt())
        } else {
            topColor
        }

        // Expand boundary slightly by 1-2 pixels to absorb anti-aliased font edges
        val marginX = (bitmapW * 0.004f).coerceAtLeast(2f)
        val marginY = (bitmapH * 0.003f).coerceAtLeast(2f)

        val cleanRect = RectF(
            (left.toFloat() - marginX).coerceAtLeast(0f),
            (top.toFloat() - marginY).coerceAtLeast(0f),
            (right.toFloat() + marginX).coerceAtMost(bitmapW),
            (bottom.toFloat() + marginY).coerceAtMost(bitmapH)
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                cleanRect.left, cleanRect.top,
                cleanRect.left, cleanRect.bottom,
                topColor, botColor,
                Shader.TileMode.CLAMP
            )
        }

        canvas.drawRect(cleanRect, paint)
    }
}
