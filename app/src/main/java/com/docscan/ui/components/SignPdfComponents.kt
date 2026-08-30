package com.docscan.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.docscan.util.SignOverlayPlacement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class SignDrawingPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

// Custom Design Colors matching user reference screenshots
val SignDarkCanvas = Color(0xFF141416)
val SignCardBg = Color(0xFF222226)
val SignSheetBg = Color(0xFF1E1E22)
val SignTeal = Color(0xFF00BFA5)
val SignTealLight = Color(0xFF1DE9B6)
val SignTealContainer = Color(0xFF103E38)
val SignTextPrimary = Color(0xFFFFFFFF)
val SignTextSecondary = Color(0xFF9E9EA4)
val SignBorder = Color(0xFF2C2C32)

/**
 * Custom Hero Illustration for Screen 1 Header (Document, Signature Bounding Box, Pen, Desk plant)
 */
@Composable
fun SignHeroIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Subtle warm background shadow card
        drawRoundRect(
            color = Color(0xFF2A2A30),
            topLeft = Offset(w * 0.25f, h * 0.10f),
            size = Size(w * 0.65f, h * 0.82f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // 2. White Paper Document
        drawRoundRect(
            color = Color(0xFFFAFAFC),
            topLeft = Offset(w * 0.20f, h * 0.05f),
            size = Size(w * 0.65f, h * 0.82f),
            cornerRadius = CornerRadius(14f, 14f)
        )

        // Document header accent line
        drawRoundRect(
            color = Color(0xFFE2E8F0),
            topLeft = Offset(w * 0.28f, h * 0.14f),
            size = Size(w * 0.40f, 6f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Sub-lines
        drawRoundRect(
            color = Color(0xFFEEF2F6),
            topLeft = Offset(w * 0.28f, h * 0.22f),
            size = Size(w * 0.48f, 5f),
            cornerRadius = CornerRadius(2.5f, 2.5f)
        )
        drawRoundRect(
            color = Color(0xFFEEF2F6),
            topLeft = Offset(w * 0.28f, h * 0.28f),
            size = Size(w * 0.35f, 5f),
            cornerRadius = CornerRadius(2.5f, 2.5f)
        )

        // 3. Signature Bounding Box with Teal border
        val boxLeft = w * 0.15f
        val boxTop = h * 0.36f
        val boxWidth = w * 0.58f
        val boxHeight = h * 0.38f

        drawRoundRect(
            color = Color(0x2200BFA5),
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(8f, 8f)
        )

        drawRoundRect(
            color = Color(0xFF00BFA5),
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = 3.5f)
        )

        // Corner resize handles
        val handleRadius = 6.5f
        val tealHandleColor = Color(0xFF00BFA5)
        drawCircle(tealHandleColor, handleRadius, Offset(boxLeft, boxTop))
        drawCircle(tealHandleColor, handleRadius, Offset(boxLeft + boxWidth, boxTop))
        drawCircle(tealHandleColor, handleRadius, Offset(boxLeft, boxTop + boxHeight))
        drawCircle(tealHandleColor, handleRadius, Offset(boxLeft + boxWidth, boxTop + boxHeight))

        // 4. Stylized Handwritten Signature Stroke inside Box
        val sigPath = Path().apply {
            val startX = boxLeft + boxWidth * 0.18f
            val startY = boxTop + boxHeight * 0.70f
            moveTo(startX, startY)
            cubicTo(
                boxLeft + boxWidth * 0.25f, boxTop + boxHeight * 0.20f,
                boxLeft + boxWidth * 0.35f, boxTop + boxHeight * 0.15f,
                boxLeft + boxWidth * 0.40f, boxTop + boxHeight * 0.65f
            )
            cubicTo(
                boxLeft + boxWidth * 0.45f, boxTop + boxHeight * 0.85f,
                boxLeft + boxWidth * 0.55f, boxTop + boxHeight * 0.35f,
                boxLeft + boxWidth * 0.65f, boxTop + boxHeight * 0.55f
            )
            cubicTo(
                boxLeft + boxWidth * 0.72f, boxTop + boxHeight * 0.75f,
                boxLeft + boxWidth * 0.82f, boxTop + boxHeight * 0.45f,
                boxLeft + boxWidth * 0.90f, boxTop + boxHeight * 0.50f
            )
        }
        drawPath(
            path = sigPath,
            color = Color(0xFF1E293B),
            style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 5. Stylized Pen / Quill accent
        val penStart = Offset(w * 0.78f, h * 0.25f)
        val penEnd = Offset(w * 0.60f, h * 0.48f)
        drawLine(
            color = Color(0xFF00BFA5),
            start = penStart,
            end = penEnd,
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
        // Pen tip
        drawLine(
            color = Color(0xFF334155),
            start = penEnd,
            end = Offset(w * 0.57f, h * 0.52f),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Add Signature Bottom Sheet (Matching Reference Screenshot 2)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSignatureBottomSheet(
    onDismiss: () -> Unit,
    onCreateSignature: () -> Unit,
    onScanSignature: () -> Unit,
    onImportFromGallery: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SignSheetBg,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Signature",
                    color = SignTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2C2C32),
                    modifier = Modifier.size(32.dp),
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SignTextSecondary,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Option 1: Create a Signature
            AddSignatureActionRow(
                icon = Icons.Default.Edit,
                title = "Create a Signature",
                onClick = {
                    onDismiss()
                    onCreateSignature()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Option 2: Scan a Signature
            AddSignatureActionRow(
                icon = Icons.Default.CameraAlt,
                title = "Scan a Signature",
                onClick = {
                    onDismiss()
                    onScanSignature()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Option 3: Import from Gallery
            AddSignatureActionRow(
                icon = Icons.Default.Image,
                title = "Import from Gallery",
                onClick = {
                    onDismiss()
                    onImportFromGallery()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AddSignatureActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF26262B),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SignTeal,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                color = SignTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Full-featured Draw Signature Dialog
 */
@Composable
fun SignatureDrawingDialog(
    onDismiss: () -> Unit,
    onSignatureDrawn: (Bitmap) -> Unit
) {
    val paths = remember { mutableStateListOf<SignDrawingPath>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    val colorOptions = listOf(
        Color(0xFF0F172A), // Black Ink
        Color(0xFF1E40AF), // Deep Blue Ink
        Color(0xFF991B1B), // Crimson Red Ink
        Color(0xFF047857)  // Forest Green Ink
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SignSheetBg,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Draw, contentDescription = null, tint = SignTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Draw Signature",
                            color = SignTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2C2C32),
                        modifier = Modifier.size(30.dp),
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SignTextSecondary,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Drawing Canvas Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFAFAFA))
                        .border(1.5.dp, Color(0xFF475569), RoundedCornerShape(12.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedColor, strokeWidth) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPoints = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentPoints = currentPoints + change.position
                                    },
                                    onDragEnd = {
                                        if (currentPoints.isNotEmpty()) {
                                            paths.add(SignDrawingPath(currentPoints, selectedColor, strokeWidth))
                                            currentPoints = emptyList()
                                        }
                                    },
                                    onDragCancel = {
                                        currentPoints = emptyList()
                                    }
                                )
                            }
                    ) {
                        // Watermark line
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(40f, size.height - 40f),
                            end = Offset(size.width - 40f, size.height - 40f),
                            strokeWidth = 2f
                        )

                        // Draw completed paths
                        paths.forEach { pathData ->
                            if (pathData.points.size > 1) {
                                val p = Path().apply {
                                    moveTo(pathData.points.first().x, pathData.points.first().y)
                                    for (i in 1 until pathData.points.size) {
                                        lineTo(pathData.points[i].x, pathData.points[i].y)
                                    }
                                }
                                drawPath(
                                    path = p,
                                    color = pathData.color,
                                    style = Stroke(
                                        width = pathData.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // Draw active path
                        if (currentPoints.size > 1) {
                            val activePath = Path().apply {
                                moveTo(currentPoints.first().x, currentPoints.first().y)
                                for (i in 1 until currentPoints.size) {
                                    lineTo(currentPoints[i].x, currentPoints[i].y)
                                }
                            }
                            drawPath(
                                path = activePath,
                                color = selectedColor,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    if (paths.isEmpty() && currentPoints.isEmpty()) {
                        Text(
                            text = "Sign here with finger or stylus",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ink Colors & Tools
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color Palette
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorOptions.forEach { col ->
                            Surface(
                                shape = CircleShape,
                                color = col,
                                modifier = Modifier
                                    .size(32.dp)
                                    .border(
                                        width = if (selectedColor == col) 2.5.dp else 1.dp,
                                        color = if (selectedColor == col) SignTeal else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                onClick = { selectedColor = col }
                            ) {}
                        }
                    }

                    // Undo & Clear Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2C2C32),
                            modifier = Modifier.size(36.dp),
                            onClick = {
                                if (paths.isNotEmpty()) paths.removeAt(paths.size - 1)
                            }
                        ) {
                            Icon(
                                Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = if (paths.isNotEmpty()) SignTextPrimary else Color.Gray,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2C2C32),
                            modifier = Modifier.size(36.dp),
                            onClick = { paths.clear() }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear",
                                tint = if (paths.isNotEmpty()) Color(0xFFEF4444) else Color.Gray,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stroke Width Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thickness:",
                        color = SignTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 3f..14f,
                        colors = SliderDefaults.colors(
                            thumbColor = SignTeal,
                            activeTrackColor = SignTeal,
                            inactiveTrackColor = Color(0xFF33333A)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF3E3E46))
                    ) {
                        Text("Cancel", color = SignTextSecondary)
                    }

                    Button(
                        onClick = {
                            if (paths.isNotEmpty()) {
                                val bmp = renderDrawingToTransparentBitmap(paths, 700, 350)
                                onSignatureDrawn(bmp)
                            }
                        },
                        enabled = paths.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SignTeal,
                            disabledContainerColor = Color(0xFF2A2A30)
                        )
                    ) {
                        Text("Save Signature", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun renderDrawingToTransparentBitmap(
    paths: List<SignDrawingPath>,
    targetWidth: Int,
    targetHeight: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    paths.forEach { pathData ->
        if (pathData.points.size > 1) {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = pathData.color.toArgb()
                strokeWidth = pathData.strokeWidth * (targetWidth / 400f)
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }

            val p = android.graphics.Path().apply {
                moveTo(pathData.points.first().x * (targetWidth / 400f), pathData.points.first().y * (targetHeight / 220f))
                for (i in 1 until pathData.points.size) {
                    lineTo(pathData.points[i].x * (targetWidth / 400f), pathData.points[i].y * (targetHeight / 220f))
                }
            }
            canvas.drawPath(p, paint)
        }
    }
    return bitmap
}

/**
 * Interactive Placed Signature / Stamp Overlay with Move, Resize Handle, and Delete
 */
@Composable
fun InteractiveSignPlacementOverlay(
    overlay: SignOverlayPlacement,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdate: (SignOverlayPlacement) -> Unit,
    onDelete: () -> Unit,
    pageWidthDp: Float,
    pageHeightDp: Float
) {
    var posX by remember(overlay.x) { mutableFloatStateOf(overlay.x) }
    var posY by remember(overlay.y) { mutableFloatStateOf(overlay.y) }
    var widthRatio by remember(overlay.widthRatio) { mutableFloatStateOf(overlay.widthRatio) }

    val overlayWidthDp = (pageWidthDp * widthRatio).coerceIn(40f, pageWidthDp * 0.95f)
    val aspect = overlay.bitmap.height.toFloat() / overlay.bitmap.width.toFloat()
    val overlayHeightDp = overlayWidthDp * aspect

    // Center coordinates translated to top-left for Compose Box positioning
    val leftDp = (posX * pageWidthDp - overlayWidthDp / 2f).coerceIn(0f, pageWidthDp - overlayWidthDp)
    val topDp = (posY * pageHeightDp - overlayHeightDp / 2f).coerceIn(0f, pageHeightDp - overlayHeightDp)

    Box(
        modifier = Modifier
            .offset { IntOffset((leftDp * 2.75f).roundToInt(), (topDp * 2.75f).roundToInt()) } // fallback / measured in Parent Box with local density
    )
}

/**
 * Page-contained Interactive Overlay View positioned inside a BoxWithConstraints of the Page
 */
@Composable
fun PageOverlayContainer(
    overlay: SignOverlayPlacement,
    isSelected: Boolean,
    pageWidthPx: Float,
    pageHeightPx: Float,
    onSelect: () -> Unit,
    onUpdate: (SignOverlayPlacement) -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current

    var relCenterX by remember(overlay.x) { mutableFloatStateOf(overlay.x) }
    var relCenterY by remember(overlay.y) { mutableFloatStateOf(overlay.y) }
    var relWidth by remember(overlay.widthRatio) { mutableFloatStateOf(overlay.widthRatio) }

    val aspect = overlay.bitmap.height.toFloat() / overlay.bitmap.width.toFloat()
    val overlayWidthPx = (pageWidthPx * relWidth).coerceIn(80f, pageWidthPx * 0.95f)
    val overlayHeightPx = overlayWidthPx * aspect

    val leftPx = (relCenterX * pageWidthPx - overlayWidthPx / 2f).coerceIn(0f, pageWidthPx - overlayWidthPx)
    val topPx = (relCenterY * pageHeightPx - overlayHeightPx / 2f).coerceIn(0f, pageHeightPx - overlayHeightPx)

    val leftDp = with(density) { leftPx.toDp() }
    val topDp = with(density) { topPx.toDp() }
    val widthDp = with(density) { overlayWidthPx.toDp() }
    val heightDp = with(density) { overlayHeightPx.toDp() }

    Box(
        modifier = Modifier
            .offset(x = leftDp, y = topDp)
            .size(width = widthDp, height = heightDp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newCenterX = (relCenterX + dragAmount.x / pageWidthPx).coerceIn(0.05f, 0.95f)
                        val newCenterY = (relCenterY + dragAmount.y / pageHeightPx).coerceIn(0.05f, 0.95f)
                        relCenterX = newCenterX
                        relCenterY = newCenterY
                        onUpdate(overlay.copy(x = newCenterX, y = newCenterY))
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelect() }
    ) {
        // Bounding Box border when selected (Teal border matching reference image)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    BorderStroke(
                        width = if (isSelected) 1.8.dp else 0.dp,
                        color = if (isSelected) SignTeal else Color.Transparent
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
                .padding(2.dp)
        ) {
            AsyncImage(
                model = overlay.bitmap,
                contentDescription = "Signature Overlay",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bounding Box Controls when selected
        if (isSelected) {
            // Delete button at Top-Left (Matching Reference Screenshot 4: teal/white circled '✕')
            Surface(
                shape = CircleShape,
                color = SignTeal,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-10).dp, y = (-10).dp)
                    .size(24.dp)
                    .clickable { onDelete() }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Signature",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }

            // Resize handle at Bottom-Right (Diagonal double arrow ↗↙)
            Surface(
                shape = CircleShape,
                color = SignTeal,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
                    .size(24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaRatio = (dragAmount.x + dragAmount.y) / (pageWidthPx * 1.2f)
                            val newRatio = (relWidth + deltaRatio).coerceIn(0.12f, 0.90f)
                            relWidth = newRatio
                            onUpdate(overlay.copy(widthRatio = newRatio))
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.OpenWith,
                    contentDescription = "Resize Signature",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}
