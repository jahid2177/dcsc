package com.docscan.ui.components.textedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.docscan.util.AiOrchestrator
import com.docscan.util.textedit.EditTextOperation
import com.docscan.util.textedit.OcrTextBlock
import com.docscan.util.textedit.OcrTextLine
import com.docscan.util.textedit.OcrTextElement
import kotlinx.coroutines.launch

/**
 * Production-ready In-Place Text Editor Dialog inspired by professional scanner apps.
 * Allows inline editing of recognized OCR text blocks, lines, or words with font styling,
 * color estimation, inpainting removal of old text, and real-time formatting.
 */
@Composable
fun InPlaceTextEditorDialog(
    initialText: String,
    initialTextColor: Color,
    initialFontSizeSp: Float,
    initialIsBold: Boolean,
    initialIsItalic: Boolean,
    initialIsUnderline: Boolean,
    initialFontFamily: String,
    initialAlignment: String = "LEFT",
    isPreviouslyEdited: Boolean = false,
    onDismiss: () -> Unit,
    onApplyEdit: (
        newText: String,
        textColor: Color,
        fontSizeSp: Float,
        isBold: Boolean,
        isItalic: Boolean,
        isUnderline: Boolean,
        fontFamily: String,
        alignment: String
    ) -> Unit,
    onEraseFromDocument: () -> Unit,
    onResetToOriginal: (() -> Unit)? = null
) {
    var editedText by remember { mutableStateOf(initialText) }
    var selectedColor by remember { mutableStateOf(initialTextColor) }
    var fontSize by remember { mutableFloatStateOf(initialFontSizeSp.coerceIn(10f, 48f)) }
    var isBold by remember { mutableStateOf(initialIsBold) }
    var isItalic by remember { mutableStateOf(initialIsItalic) }
    var isUnderline by remember { mutableStateOf(initialIsUnderline) }
    var fontFamilyType by remember { mutableStateOf(initialFontFamily) }
    var alignment by remember { mutableStateOf(initialAlignment) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val presetColors = listOf(
        Color.Black,
        Color(0xFF1E293B), // Dark Slate
        Color(0xFFDC2626), // Crimson Red
        Color(0xFF2563EB), // Royal Blue
        Color(0xFF059669), // Emerald
        Color(0xFFD97706), // Amber
        Color(0xFF7C3AED)  // Purple
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1C1C1E),
            border = BorderStroke(1.dp, Color(0xFF3A3A3C)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("dialog_inplace_text_editor")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Title + Copy Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Edit Document Text",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "In-place OCR text replacement",
                            color = Color(0xFF9E9E9E),
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isPreviouslyEdited && onResetToOriginal != null) {
                            IconButton(
                                onClick = onResetToOriginal,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.RestartAlt,
                                    contentDescription = "Restore Original Text",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(initialText))
                                android.widget.Toast.makeText(context, "Copied OCR text", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Text",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // AI Assist Action Chips
                val coroutineScope = rememberCoroutineScope()
                var isAiProcessing by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI:", color = Color(0xFF2DBA8D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (isAiProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color(0xFF2DBA8D),
                            strokeWidth = 2.dp
                        )
                        Text("Optimizing...", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        listOf(
                            "Fix Spelling" to "fix spelling and typos",
                            "Format" to "clean up formatting and spacing",
                            "বাংলায় রূপান্তর" to "translate to Bengali",
                            "To English" to "translate to English"
                        ).forEach { (label, action) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2C2C2E),
                                border = BorderStroke(1.dp, Color(0xFF3A3A3C)),
                                modifier = Modifier.clickable {
                                    if (editedText.isNotBlank()) {
                                        isAiProcessing = true
                                        coroutineScope.launch {
                                            val improved = AiOrchestrator.editTextAi(editedText, action, context)
                                            if (improved.isNotBlank()) {
                                                editedText = improved
                                            }
                                            isAiProcessing = false
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    label,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Text Edit Field
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    label = { Text("Text Content", color = Color(0xFF8E8E93), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF2DBA8D),
                        unfocusedBorderColor = Color(0xFF48484A),
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 160.dp)
                        .testTag("input_edit_text_content"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Font Size Slider + Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Size", color = Color(0xFFD1D1D6), fontSize = 12.sp, fontWeight = FontWeight.Medium)

                    IconButton(
                        onClick = { fontSize = (fontSize - 1f).coerceAtLeast(10f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 10f..44f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2DBA8D),
                            activeTrackColor = Color(0xFF2DBA8D),
                            inactiveTrackColor = Color(0xFF48484A)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { fontSize = (fontSize + 1f).coerceAtMost(44f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        "${fontSize.toInt()}sp",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Font Family Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Font:", color = Color(0xFF8E8E93), fontSize = 11.sp)

                    listOf(
                        "DEFAULT" to "Sans",
                        "SERIF" to "Serif",
                        "MONO" to "Mono"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = fontFamilyType == key,
                            onClick = { fontFamilyType = key },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2DBA8D),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF2C2C2E),
                                labelColor = Color(0xFFD1D1D6)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Text Format & Alignment Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Style Chips: Bold, Italic, Underline
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = isBold,
                            onClick = { isBold = !isBold },
                            label = { Text("B", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2DBA8D),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF2C2C2E),
                                labelColor = Color(0xFFD1D1D6)
                            )
                        )
                        FilterChip(
                            selected = isItalic,
                            onClick = { isItalic = !isItalic },
                            label = { Text("I", fontStyle = FontStyle.Italic, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2DBA8D),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF2C2C2E),
                                labelColor = Color(0xFFD1D1D6)
                            )
                        )
                        FilterChip(
                            selected = isUnderline,
                            onClick = { isUnderline = !isUnderline },
                            label = { Text("U", textDecoration = TextDecoration.Underline, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2DBA8D),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF2C2C2E),
                                labelColor = Color(0xFFD1D1D6)
                            )
                        )
                    }

                    // Alignment Icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        IconButton(
                            onClick = { alignment = "LEFT" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.FormatAlignLeft,
                                contentDescription = "Left",
                                tint = if (alignment == "LEFT") Color(0xFF2DBA8D) else Color(0xFF8E8E93),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { alignment = "CENTER" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.FormatAlignCenter,
                                contentDescription = "Center",
                                tint = if (alignment == "CENTER") Color(0xFF2DBA8D) else Color(0xFF8E8E93),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { alignment = "RIGHT" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.FormatAlignRight,
                                contentDescription = "Right",
                                tint = if (alignment == "RIGHT") Color(0xFF2DBA8D) else Color(0xFF8E8E93),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Color Palette
                Text("Text Ink Color", color = Color(0xFF8E8E93), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presetColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selectedColor == c) 2.5.dp else 1.dp,
                                    color = if (selectedColor == c) Color(0xFF2DBA8D) else Color(0xFF55555A),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = c }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Erase / Wipe text button
                    TextButton(
                        onClick = onEraseFromDocument,
                        modifier = Modifier.testTag("btn_erase_text_from_doc")
                    ) {
                        Icon(
                            Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Erase Text", color = Color(0xFFFF5252), fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Color.White, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (editedText.isNotBlank()) {
                                    onApplyEdit(
                                        editedText,
                                        selectedColor,
                                        fontSize,
                                        isBold,
                                        isItalic,
                                        isUnderline,
                                        fontFamilyType,
                                        alignment
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DBA8D)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_apply_edit_text")
                        ) {
                            Text(
                                "Replace In-Place",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
