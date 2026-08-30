package com.docscan.util.textedit

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max

/**
 * Text Layout & Rendering Engine for Document Replacement.
 * Renders single or multi-line edited text accurately within original or expanded bounding boxes.
 */
object DocumentTextRenderer {

    /**
     * Renders a list of edit operations onto a Canvas representing the document bitmap.
     */
    fun renderOperationsOnCanvas(
        canvas: Canvas,
        operations: List<EditTextOperation>,
        bitmapWidth: Float,
        bitmapHeight: Float
    ) {
        for (op in operations) {
            if (op.isDeleted || op.newText.isBlank()) continue
            renderSingleOperation(canvas, op, bitmapWidth, bitmapHeight)
        }
    }

    private fun renderSingleOperation(
        canvas: Canvas,
        op: EditTextOperation,
        bitmapW: Float,
        bitmapH: Float
    ) {
        val rect = op.targetNormalizedRect
        val left = rect.left * bitmapW
        val top = rect.top * bitmapH
        val right = rect.right * bitmapW
        val bottom = rect.bottom * bitmapH
        val boxWidth = (right - left).coerceAtLeast(10f)
        val boxHeight = (bottom - top).coerceAtLeast(10f)

        val tfType = when (op.fontFamilyType) {
            "SERIF" -> Typeface.SERIF
            "MONO" -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT
        }

        val typeface = if (op.isBold) {
            if (op.isItalic) Typeface.create(tfType, Typeface.BOLD_ITALIC) else Typeface.create(tfType, Typeface.BOLD)
        } else if (op.isItalic) {
            Typeface.create(tfType, Typeface.ITALIC)
        } else {
            tfType
        }

        // Calibrate font size relative to bitmap dimensions
        val scaleFactor = bitmapW / 400f
        val calculatedTextSize = (op.fontSizeSp * scaleFactor).coerceIn(10f, 250f)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = op.textColor.toArgb()
            textSize = calculatedTextSize
            this.typeface = typeface
            isUnderlineText = op.isUnderline
        }

        val text = op.newText

        // If background color is specified and not transparent
        if (op.backgroundColor != Color.Transparent) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = op.backgroundColor.toArgb()
                style = Paint.Style.FILL
            }
            canvas.drawRect(RectF(left, top, right, bottom), bgPaint)
        }

        // Multi-line StaticLayout rendering
        val align = when (op.alignment) {
            "CENTER" -> Layout.Alignment.ALIGN_CENTER
            "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }

        // Measure text bounds
        val layoutWidth = max(10, boxWidth.toInt())
        val staticLayout = StaticLayout.Builder.obtain(
            text, 0, text.length, textPaint, layoutWidth
        )
            .setAlignment(align)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()

        val textTotalHeight = staticLayout.height.toFloat()

        // Vertically center text in bounding box if single line, or align to top
        val startY = if (staticLayout.lineCount == 1) {
            top + (boxHeight - textTotalHeight) / 2f
        } else {
            top
        }

        canvas.save()
        canvas.translate(left, startY)
        staticLayout.draw(canvas)
        canvas.restore()
    }
}
