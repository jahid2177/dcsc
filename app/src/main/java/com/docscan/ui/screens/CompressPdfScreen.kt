package com.docscan.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.docscan.data.model.DocumentEntity
import com.docscan.ui.viewmodel.ScannerViewModel
import com.docscan.util.CompressedPdfResult
import com.docscan.util.CompressionLevel
import com.docscan.util.FileUtils
import com.docscan.util.PdfCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Theme Color Tokens
private val CompressDarkBg = Color(0xFF141619)
private val CompressSurface = Color(0xFF1F2227)
private val CompressTeal = Color(0xFF00C48C)
private val CompressTealLight = Color(0xFF00D2A0)
private val CompressTealDim = Color(0x1F00C48C)
private val CompressBorder = Color(0xFF2C3038)
private val CompressTextPrimary = Color.White
private val CompressTextSecondary = Color(0xFF9EABB8)
private val CompressTextMuted = Color(0xFF677282)

enum class CompressWorkflowStep {
    SELECT,     // Screen 1: Choose file from Device or App
    CONFIGURE,  // Screen 2: Select compression level (Medium / High)
    PREVIEW     // Screen 3: View compressed PDF & multi-page swipe & share
}

@Composable
fun CompressPdfScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocumentDetail: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allDocuments by viewModel.allDocuments.collectAsState(initial = emptyList())

    // Multi-Step Workflow State
    var currentStep by remember { mutableStateOf(CompressWorkflowStep.SELECT) }

    // Selected Document & Compression State
    var selectedDocument by remember { mutableStateOf<DocumentEntity?>(null) }
    var selectedCompressionLevel by remember { mutableStateOf(CompressionLevel.MEDIUM) }
    var compressedResult by remember { mutableStateOf<CompressedPdfResult?>(null) }

    // Loading & Progress States
    var isCompressing by remember { mutableStateOf(false) }
    var compressionProgress by remember { mutableFloatStateOf(0f) }
    var compressionStatusText by remember { mutableStateOf("Compressing PDF...") }

    // Handle Hardware & Gesture Back Navigation
    BackHandler {
        when (currentStep) {
            CompressWorkflowStep.PREVIEW -> currentStep = CompressWorkflowStep.CONFIGURE
            CompressWorkflowStep.CONFIGURE -> currentStep = CompressWorkflowStep.SELECT
            CompressWorkflowStep.SELECT -> onNavigateBack()
        }
    }

    // System File Picker for Screen 1: "Create or Import -> Device"
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isCompressing = true
            compressionStatusText = "Importing file from device..."
            coroutineScope.launch {
                try {
                    val importedDocs = viewModel.importFilesForMerge(uris)
                    isCompressing = false
                    val first = importedDocs.firstOrNull()
                    if (first != null) {
                        selectedDocument = first
                        currentStep = CompressWorkflowStep.CONFIGURE
                    } else {
                        Toast.makeText(context, "Could not load selected document.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    isCompressing = false
                    Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CompressDarkBg)
    ) {
        when (currentStep) {
            CompressWorkflowStep.SELECT -> {
                CompressFileSelectionScreen(
                    allDocuments = allDocuments,
                    onNavigateBack = onNavigateBack,
                    onDeviceClick = {
                        filePickerLauncher.launch(arrayOf("application/pdf", "image/*"))
                    },
                    onDocumentSelected = { doc ->
                        selectedDocument = doc
                        currentStep = CompressWorkflowStep.CONFIGURE
                    }
                )
            }

            CompressWorkflowStep.CONFIGURE -> {
                selectedDocument?.let { doc ->
                    CompressLevelSelectionScreen(
                        document = doc,
                        selectedLevel = selectedCompressionLevel,
                        onLevelSelected = { selectedCompressionLevel = it },
                        onNavigateBack = {
                            currentStep = CompressWorkflowStep.SELECT
                        },
                        onCompressClick = {
                            isCompressing = true
                            compressionProgress = 0f
                            compressionStatusText = "Compressing PDF..."

                            coroutineScope.launch {
                                val pages = viewModel.getPagesForDocumentDirect(doc.id)
                                val result = PdfCompressor.compressDocument(
                                    context = context,
                                    documentTitle = doc.title,
                                    pages = pages,
                                    level = selectedCompressionLevel,
                                    onProgress = { fraction, status ->
                                        compressionProgress = fraction
                                        compressionStatusText = status
                                    }
                                )
                                isCompressing = false
                                if (result != null) {
                                    compressedResult = result
                                    currentStep = CompressWorkflowStep.PREVIEW
                                } else {
                                    Toast.makeText(context, "Compression failed. Please try again.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }

            CompressWorkflowStep.PREVIEW -> {
                compressedResult?.let { result ->
                    val docName = selectedDocument?.title ?: "Document"
                    CompressedPdfPreviewScreen(
                        documentTitle = docName,
                        compressedResult = result,
                        onNavigateBack = {
                            currentStep = CompressWorkflowStep.CONFIGURE
                        }
                    )
                }
            }
        }

        // Global Compression / Processing Progress Dialog Overlay
        if (isCompressing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .zIndex(100f)
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CompressSurface,
                    border = BorderStroke(1.dp, CompressTeal.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = CompressTeal,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Compressing PDF...",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = compressionStatusText,
                            color = CompressTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// SCREEN 1: COMPRESS FILE SELECTION SCREEN (Matching Reference Screenshot 1)
// =============================================================================

@Composable
private fun CompressFileSelectionScreen(
    allDocuments: List<DocumentEntity>,
    onNavigateBack: () -> Unit,
    onDeviceClick: () -> Unit,
    onDocumentSelected: (DocumentEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. TOP HERO SECTION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Top Left Back Arrow
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("button_compress_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hero Content: Title, Subtitle on Left, Illustration on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compress",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reduce file size to save space.",
                            color = CompressTextSecondary,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Original Styled Hero Illustration
                    CompressHeroIllustration(
                        modifier = Modifier.size(width = 112.dp, height = 98.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. CREATE OR IMPORT SECTION
                Text(
                    text = "Create or Import",
                    color = CompressTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Large Device Button Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CompressSurface,
                    border = BorderStroke(1.dp, CompressBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("button_import_device")
                        .clickable { onDeviceClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Blue Device / Folder Icon Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2563EB),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "Device",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = "Device",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. SELECT FROM THIS APP SECTION
                Text(
                    text = "Select from This App",
                    color = CompressTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        // Empty App Documents State
        if (allDocuments.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = CompressTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No documents found in DocScanner",
                        color = CompressTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Device' above to import a PDF to compress",
                        color = CompressTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // Scrollable Document List (Tapping any row immediately selects document & opens Screen 2)
            items(
                items = allDocuments,
                key = { doc -> doc.id }
            ) { doc ->
                CompressDocumentItem(
                    document = doc,
                    onClick = { onDocumentSelected(doc) }
                )
            }
        }
    }
}

/**
 * Single document item in Screen 1 list.
 */
@Composable
private fun CompressDocumentItem(
    document: DocumentEntity,
    onClick: () -> Unit
) {
    // Format date matching reference screenshot: e.g. "27/08/2026 1:12 pm"
    val dateText = remember(document.createdAt) {
        val sdf = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
        sdf.format(Date(document.createdAt)).lowercase()
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("item_doc_${document.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Document Thumbnail (56dp x 72dp rounded)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CompressSurface,
                border = BorderStroke(1.dp, CompressBorder),
                modifier = Modifier.size(width = 56.dp, height = 72.dp)
            ) {
                if (!document.thumbnailPath.isNullOrBlank() && File(document.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(document.thumbnailPath),
                        contentDescription = document.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = CompressTeal,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Center Document Title & Metadata
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = document.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateText,
                        color = CompressTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = " | ",
                        color = Color(0xFF4B5563),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "📄 ${document.pageCount}",
                        color = CompressTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Original Hero Illustration matching the card in Screenshot 1.
 */
@Composable
private fun CompressHeroIllustration(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.shadow(6.dp, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Docs Done Right",
                    color = Color(0xFF1E293B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "All in one app. Convert,",
                    color = Color(0xFF64748B),
                    fontSize = 6.sp,
                    maxLines = 1
                )
            }

            // Size comparison chips: 5.8MB -> 2.6MB
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 5.8MB tag
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = "5.8MB",
                        color = Color(0xFF64748B),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }

                // Arrow
                Text(
                    text = "→",
                    color = CompressTeal,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )

                // 2.6MB tag (Teal highlighted)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(0.5.dp, CompressTeal.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "2.6MB",
                        color = Color(0xFF047857),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Text(
                text = "reduced storage with ease.",
                color = Color(0xFF94A3B8),
                fontSize = 5.sp,
                maxLines = 1
            )
        }
    }
}

// =============================================================================
// SCREEN 2: SELECT COMPRESSION LEVEL (Matching Reference Screenshot 2)
// =============================================================================

@Composable
private fun CompressLevelSelectionScreen(
    document: DocumentEntity,
    selectedLevel: CompressionLevel,
    onLevelSelected: (CompressionLevel) -> Unit,
    onNavigateBack: () -> Unit,
    onCompressClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar: "← Compress"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("button_level_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Compress",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center: Selected Document Large Icon & Document Title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Circular Teal Badge (~76dp)
            Surface(
                shape = CircleShape,
                color = CompressTeal,
                modifier = Modifier
                    .size(76.dp)
                    .shadow(12.dp, CircleShape, spotColor = CompressTeal.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(36.dp)) {
                        val w = size.width
                        val h = size.height

                        // White document body with folded top-right corner
                        val path = Path().apply {
                            moveTo(w * 0.15f, h * 0.1f)
                            lineTo(w * 0.62f, h * 0.1f)
                            lineTo(w * 0.85f, h * 0.33f)
                            lineTo(w * 0.85f, h * 0.9f)
                            lineTo(w * 0.15f, h * 0.9f)
                            close()
                        }
                        drawPath(path, Color.White)

                        // Fold corner outline
                        val foldPath = Path().apply {
                            moveTo(w * 0.62f, h * 0.1f)
                            lineTo(w * 0.62f, h * 0.33f)
                            lineTo(w * 0.85f, h * 0.33f)
                        }
                        drawPath(foldPath, CompressTeal, style = Stroke(width = 2.5f))

                        // Horizontal lines representing text
                        for (i in 1..3) {
                            drawLine(
                                color = CompressTeal,
                                start = Offset(w * 0.28f, h * 0.38f + i * h * 0.12f),
                                end = Offset(w * 0.72f, h * 0.38f + i * h * 0.12f),
                                strokeWidth = 2.5f
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document Name
            Text(
                text = document.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Compression Level Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Select compression level:",
                color = CompressTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Medium (Default selected)
            CompressionOptionCard(
                title = "Medium",
                subtitle = "Medium size, better quality",
                isSelected = (selectedLevel == CompressionLevel.MEDIUM),
                onClick = { onLevelSelected(CompressionLevel.MEDIUM) },
                testTag = "option_medium"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Option 2: High
            CompressionOptionCard(
                title = "High",
                subtitle = "Smaller size, standard quality",
                isSelected = (selectedLevel == CompressionLevel.HIGH),
                onClick = { onLevelSelected(CompressionLevel.HIGH) },
                testTag = "option_high"
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Fixed Bottom Compress Button
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = onCompressClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CompressTeal,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = CompressTeal.copy(alpha = 0.5f))
                    .testTag("button_start_compress")
            ) {
                Text(
                    text = "Compress",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * Selectable Compression Option Card (Medium / High)
 */
@Composable
private fun CompressionOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val cardBorder = if (isSelected) {
        BorderStroke(1.5.dp, CompressTeal)
    } else {
        BorderStroke(1.dp, CompressBorder)
    }

    val cardBg = if (isSelected) {
        CompressTealDim
    } else {
        CompressSurface
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) CompressTeal else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = CompressTextSecondary,
                    fontSize = 13.sp
                )
            }

            // Visible Teal Checkmark on Right when Selected
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = CompressTeal,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// =============================================================================
// SCREEN 3: COMPRESSED PDF PREVIEW (Matching Reference Screenshot 3)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompressedPdfPreviewScreen(
    documentTitle: String,
    compressedResult: CompressedPdfResult,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val totalPages = compressedResult.pageCount.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { totalPages })

    var showShareBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar: "← Document Name"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("button_preview_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = documentTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // PDF PREVIEW AREA with SWIPE ANYWHERE TO CHANGE PAGE
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0F1113)),
            contentAlignment = Alignment.Center
        ) {
            // HorizontalPager covers the full viewing area allowing smooth swipe navigation anywhere
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("pager_pdf_preview")
            ) { pageIndex ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val pageBitmap = compressedResult.previewBitmaps.getOrNull(pageIndex)
                    if (pageBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .fillMaxHeight(0.92f)
                        ) {
                            Image(
                                bitmap = pageBitmap.asImageBitmap(),
                                contentDescription = "Page ${pageIndex + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Fallback Placeholder if page bitmap is missing
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(0.707f) // A4 ratio
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Page ${pageIndex + 1} of $totalPages",
                                    color = Color.DarkGray,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Compact Page Indicator Floating Pill: ◀ 1/5 ▶
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC1F2227),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Arrow ◀
                    val canGoPrev = pagerState.currentPage > 0
                    IconButton(
                        onClick = {
                            if (canGoPrev) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = canGoPrev,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Page",
                            tint = if (canGoPrev) Color.White else Color(0xFF55555A),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Current Page / Total Pages (e.g. 1/5)
                    Text(
                        text = "${pagerState.currentPage + 1}/$totalPages",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right Arrow ▶
                    val canGoNext = pagerState.currentPage < totalPages - 1
                    IconButton(
                        onClick = {
                            if (canGoNext) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = canGoNext,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Page",
                            tint = if (canGoNext) Color.White else Color(0xFF55555A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // FIXED BOTTOM SHARE BUTTON: "Share (2.2MB)" with actual calculated compressed size
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = { showShareBottomSheet = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CompressTeal,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = CompressTeal.copy(alpha = 0.5f))
                    .testTag("button_share_compressed")
            ) {
                Text(
                    text = "Share (${compressedResult.formattedSize})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }

    // SHARE / SAVE OPTIONS BOTTOM SHEET
    if (showShareBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareBottomSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = CompressSurface,
            scrimColor = Color(0x99000000)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = documentTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Compressed Size: ${compressedResult.formattedSize}",
                            color = CompressTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CompressTealDim
                    ) {
                        Text(
                            text = "${compressedResult.pageCount} Pages",
                            color = CompressTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = CompressBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Option 1: Share PDF (Native Android Sharesheet)
                ShareActionItem(
                    title = "Share PDF",
                    subtitle = "Send via WhatsApp, Gmail, Drive, etc.",
                    icon = Icons.Default.Share,
                    iconBgColor = CompressTealDim,
                    iconTint = CompressTeal,
                    onClick = {
                        showShareBottomSheet = false
                        PdfCompressor.sharePdf(context, compressedResult.file)
                    }
                )

                // Option 2: Save to Device
                ShareActionItem(
                    title = "Save to Device",
                    subtitle = "Export to Downloads / DocScanner folder",
                    icon = Icons.Default.Download,
                    iconBgColor = Color(0xFF2563EB).copy(alpha = 0.15f),
                    iconTint = Color(0xFF3B82F6),
                    onClick = {
                        showShareBottomSheet = false
                        PdfCompressor.savePdfToDevice(context, compressedResult.file)
                    }
                )

                // Option 3: Open in PDF Viewer
                ShareActionItem(
                    title = "Open in PDF Viewer",
                    subtitle = "View with default or external PDF reader",
                    icon = Icons.Default.OpenInNew,
                    iconBgColor = Color(0xFF10B981).copy(alpha = 0.15f),
                    iconTint = Color(0xFF10B981),
                    onClick = {
                        showShareBottomSheet = false
                        PdfCompressor.openPdf(context, compressedResult.file)
                    }
                )
            }
        }
    }
}

@Composable
private fun ShareActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBgColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = CompressTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
