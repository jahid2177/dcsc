package com.docscan.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Modern live document edge overlay (ML Kit / Adobe Scan style).
 *
 * Behaviour:
 *  - Camera just opened / still searching  →  NO box at all (clean full preview).
 *  - Camera held steady over a document    →  blue 4-corner polygon appears.
 *  - Document locked (STABLE)              →  brighter outline + laser scan.
 *  - ID Card mode                          →  fixed CR-80 / passport frame.
 *
 * Palette (Google ML Kit Document Scanner):
 *   Outline #1A73E8 · Bright #4285F4 · Corner #8AB4F8 · Dim mask rgba(0,0,0,0.55)
 */
@Composable
fun BlueEdgeOverlay(
    corners: List<Offset>,
    state: com.docscan.util.DetectionState,
    scanMode: com.docscan.data.model.ScanMode,
    idCardStep: Int,
    idCardType: com.docscan.data.model.IdCardType = com.docscan.data.model.IdCardType.BANK_CARD,
    showGrid: Boolean,
    frameAspectRatio: Float = 3f / 4f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rect_scanner_fx")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.84f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rect_alpha"
    )

    val scanLinePos by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rect_laser"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        @Suppress("UNUSED_PARAMETER") showGrid
        @Suppress("UNUSED_PARAMETER") idCardStep

        if (scanMode == com.docscan.data.model.ScanMode.ID_CARD) {
            drawIdCardBlueFrame(this, w, h, idCardType, pulseAlpha, scanLinePos)
            return@Canvas
        }

        // Clean camera preview while searching.
        if (state == com.docscan.util.DetectionState.SEARCHING_DOCUMENT ||
            state == com.docscan.util.DetectionState.IDLE ||
            corners.size != 4
        ) {
            return@Canvas
        }

        val mapped = mapCornersToPreview(corners, w, h, frameAspectRatio)

        // The detector may calculate perspective corners internally, but the
        // user-facing UI deliberately converts them into ONE axis-aligned box.
        val left = mapped.minOf { it.x }.coerceIn(0f, w)
        val top = mapped.minOf { it.y }.coerceIn(0f, h)
        val right = mapped.maxOf { it.x }.coerceIn(0f, w)
        val bottom = mapped.maxOf { it.y }.coerceIn(0f, h)

        val rectW = right - left
        val rectH = bottom - top
        if (rectW < 32.dp.toPx() || rectH < 32.dp.toPx()) return@Canvas

        val inset = minOf(3.dp.toPx(), minOf(rectW, rectH) * 0.03f)
        val frame = Rect(
            left + inset,
            top + inset,
            right - inset,
            bottom - inset
        )

        drawDetectedRectangle(this, frame, state, pulseAlpha, scanLinePos)
    }
}

private fun mapCornersToPreview(
    corners: List<Offset>,
    w: Float,
    h: Float,
    frameAspectRatio: Float
): List<Offset> {
    val safeAspect = if (frameAspectRatio.isFinite() && frameAspectRatio > 0f) frameAspectRatio else 3f / 4f
    val destAspect = if (h > 0f) w / h else safeAspect
    var cropXFrac = 0f
    var cropYFrac = 0f

    if (safeAspect > destAspect) {
        cropXFrac = 0.5f * (1f - destAspect / safeAspect)
    } else if (safeAspect < destAspect) {
        cropYFrac = 0.5f * (1f - safeAspect / destAspect)
    }

    val xSpan = (1f - 2f * cropXFrac).coerceAtLeast(0.0001f)
    val ySpan = (1f - 2f * cropYFrac).coerceAtLeast(0.0001f)

    fun mapX(x: Float) = ((x - cropXFrac) / xSpan) * w
    fun mapY(y: Float) = ((y - cropYFrac) / ySpan) * h

    return corners.take(4).map {
        Offset(
            mapX(it.x.coerceIn(0f, 1f)).coerceIn(-w, 2f * w),
            mapY(it.y.coerceIn(0f, 1f)).coerceIn(-h, 2f * h)
        )
    }
}

private val BlueOutline = Color(0xFF1A73E8)
private val BlueOutlineBright = Color(0xFF4285F4)
private val BlueFill = Color(0xFF1A73E8)
private val BlueLaser = Color(0x998AB4F8)
private val OuterDim = Color(0x99000000)

private fun DrawScope.drawDetectedRectangle(
    drawScope: DrawScope,
    frame: Rect,
    state: com.docscan.util.DetectionState,
    pulseAlpha: Float,
    scanLine: Float
) {
    val stable = state == com.docscan.util.DetectionState.DOCUMENT_STABLE
    val radius = 12.dp.toPx()

    val outerPath = Path().apply {
        addRect(Rect(0f, 0f, drawScope.size.width, drawScope.size.height))
    }
    val holePath = Path().apply {
        addRoundRect(RoundRect(frame, CornerRadius(radius, radius)))
    }
    val maskPath = Path.combine(PathOperation.Difference, outerPath, holePath)
    drawScope.drawPath(maskPath, OuterDim)

    drawScope.drawRoundRect(
        color = BlueFill.copy(alpha = if (stable) 0.075f else 0.035f),
        topLeft = Offset(frame.left, frame.top),
        size = Size(frame.width, frame.height),
        cornerRadius = CornerRadius(radius, radius)
    )

    drawScope.drawRoundRect(
        color = if (stable) BlueOutlineBright.copy(alpha = pulseAlpha) else BlueOutline.copy(alpha = 0.94f * pulseAlpha),
        topLeft = Offset(frame.left, frame.top),
        size = Size(frame.width, frame.height),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = if (stable) 3.dp.toPx() else 2.4.dp.toPx())
    )

    // Fixed visual corner brackets — NOT perspective handles.
    val len = 22.dp.toPx()
    val sw = if (stable) 4.dp.toPx() else 3.2.dp.toPx()
    val c = BlueOutlineBright
    val l = frame.left; val t = frame.top; val r = frame.right; val b = frame.bottom
    drawScope.drawLine(c, Offset(l, t + len), Offset(l, t), sw, cap = StrokeCap.Round)
    drawScope.drawLine(c, Offset(l, t), Offset(l + len, t), sw, cap = StrokeCap.Round)
    drawScope.drawLine(c, Offset(r - len, t), Offset(r, t), sw, cap = StrokeCap.Round)
    drawScope.drawLine(c, Offset(r, t), Offset(r, t + len), sw, cap = StrokeCap.Round)
    drawScope.drawLine(c, Offset(l, b - len), Offset(l, b), sw, cap = StrokeCap.Round)
    drawScope.drawLine(c, Offset(l, b), Offset(l + len, b), sw, cap = StrokeCap.Round)
    drawScope.drawLine(c, Offset(r - len, b), Offset(r, b), sw, cap = StrokeCap.Round)
    drawScope.drawLine(c, Offset(r, b), Offset(r, b - len), sw, cap = StrokeCap.Round)

    if (stable) {
        val laserY = frame.top + scanLine.coerceIn(0f, 1f) * frame.height
        drawScope.drawLine(
            color = BlueLaser,
            start = Offset(frame.left + 8.dp.toPx(), laserY),
            end = Offset(frame.right - 8.dp.toPx(), laserY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawIdCardBlueFrame(
    drawScope: DrawScope,
    w: Float, h: Float,
    idCardType: com.docscan.data.model.IdCardType,
    pulseAlpha: Float,
    scanLine: Float
) {
    val ratio = if (idCardType == com.docscan.data.model.IdCardType.PASSPORT) 1.42f else 1.586f
    val cardW = if (idCardType == com.docscan.data.model.IdCardType.PASSPORT) w * 0.86f else w * 0.82f
    val cardH = cardW / ratio
    val left = (w - cardW) / 2f
    val top = (h - cardH) / 2.3f
    val frame = Rect(left, top, left + cardW, top + cardH)

    // Dim outside
    val outerPath = Path().apply { addRect(Rect(0f, 0f, w, h)) }
    val holePath = Path().apply {
        addRoundRect(RoundRect(frame, CornerRadius(18.dp.toPx(), 18.dp.toPx())))
    }
    val maskPath = Path.combine(PathOperation.Difference, outerPath, holePath)
    drawScope.drawPath(maskPath, OuterDim)

    drawScope.drawRoundRect(
        color = BlueOutline.copy(alpha = 0.92f * pulseAlpha),
        topLeft = Offset(frame.left, frame.top),
        size = Size(frame.width, frame.height),
        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
        style = Stroke(width = 3.dp.toPx())
    )

    // Corner brackets
    val bracket = 26.dp.toPx()
    val stroke = 4.dp.toPx()
    val c = BlueOutlineBright
    // TL
    drawScope.drawLine(c, Offset(frame.left, frame.top + bracket), Offset(frame.left, frame.top), strokeWidth = stroke)
    drawScope.drawLine(c, Offset(frame.left, frame.top), Offset(frame.left + bracket, frame.top), strokeWidth = stroke)
    // TR
    drawScope.drawLine(c, Offset(frame.right - bracket, frame.top), Offset(frame.right, frame.top), strokeWidth = stroke)
    drawScope.drawLine(c, Offset(frame.right, frame.top), Offset(frame.right, frame.top + bracket), strokeWidth = stroke)
    // BL
    drawScope.drawLine(c, Offset(frame.left, frame.bottom - bracket), Offset(frame.left, frame.bottom), strokeWidth = stroke)
    drawScope.drawLine(c, Offset(frame.left, frame.bottom), Offset(frame.left + bracket, frame.bottom), strokeWidth = stroke)
    // BR
    drawScope.drawLine(c, Offset(frame.right - bracket, frame.bottom), Offset(frame.right, frame.bottom), strokeWidth = stroke)
    drawScope.drawLine(c, Offset(frame.right, frame.bottom), Offset(frame.right, frame.bottom - bracket), strokeWidth = stroke)

    // Scan laser
    val laserY = frame.top + scanLine * frame.height
    drawScope.drawLine(
        color = BlueLaser,
        start = Offset(frame.left + 12f, laserY),
        end = Offset(frame.right - 12f, laserY),
        strokeWidth = 2.dp.toPx()
    )
}

private fun topBotY(pts: List<Offset>): Pair<Float, Float> {
    val ys = pts.map { it.y }
    return ys.minOrNull()!! to ys.maxOrNull()!!
}
