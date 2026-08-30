package com.docscan.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.docscan.data.model.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

enum class CompressionLevel(
    val title: String,
    val subtitle: String,
    val maxDimension: Int,
    val jpegQuality: Int
) {
    MEDIUM(
        title = "Medium",
        subtitle = "Medium size, better quality",
        maxDimension = 1500,
        jpegQuality = 75
    ),
    HIGH(
        title = "High",
        subtitle = "Smaller size, standard quality",
        maxDimension = 1000,
        jpegQuality = 45
    )
}

data class CompressedPdfResult(
    val file: File,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val formattedSize: String,
    val pageCount: Int,
    val previewBitmaps: List<Bitmap> = emptyList()
)

object PdfCompressor {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1fMB", mb)
        } else {
            String.format(Locale.US, "%d KB", kb.toLong().coerceAtLeast(1L))
        }
    }

    /**
     * Compresses an existing document's pages according to the selected compression level.
     */
    suspend fun compressDocument(
        context: Context,
        documentTitle: String,
        pages: List<PageEntity>,
        level: CompressionLevel,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): CompressedPdfResult? = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) return@withContext null

        val pdfDir = FileUtils.getPdfsDir(context)
        val sanitizedTitle = documentTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Document" }
        val outputFile = getUniqueOutputFile(pdfDir, sanitizedTitle)

        val pdfDocument = PdfDocument()
        val previewBitmaps = mutableListOf<Bitmap>()

        try {
            var originalTotalBytes = 0L
            val totalPages = pages.size

            onProgress(0.1f, "Preparing document pages...")

            pages.forEachIndexed { index, pageEntity ->
                val stepFraction = 0.1f + (0.7f * (index.toFloat() / totalPages.coerceAtLeast(1)))
                onProgress(stepFraction, "Optimizing page ${index + 1} of $totalPages...")

                val srcFile = File(pageEntity.processedImagePath)
                if (srcFile.exists()) {
                    originalTotalBytes += srcFile.length()
                }

                // Load source bitmap
                val rawBitmap = FileUtils.loadBitmap(pageEntity.processedImagePath, maxDimension = 3000)
                    ?: return@forEachIndexed

                // Process and scale according to compression level
                val compressedBmp = compressAndScaleBitmap(rawBitmap, level)
                rawBitmap.recycle()

                // Standard A4 dimensions
                val pageWidth = 595
                val pageHeight = 842
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)

                val margin = 20f
                val availableWidth = pageWidth - (margin * 2)
                val availableHeight = pageHeight - (margin * 2)

                val bmpRatio = compressedBmp.width.toFloat() / compressedBmp.height.toFloat()
                val availRatio = availableWidth / availableHeight

                val drawWidth: Float
                val drawHeight: Float
                if (bmpRatio > availRatio) {
                    drawWidth = availableWidth
                    drawHeight = availableWidth / bmpRatio
                } else {
                    drawHeight = availableHeight
                    drawWidth = availableHeight * bmpRatio
                }

                val left = margin + (availableWidth - drawWidth) / 2f
                val top = margin + (availableHeight - drawHeight) / 2f

                val destRect = RectF(left, top, left + drawWidth, top + drawHeight)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(compressedBmp, null, destRect, paint)

                pdfDocument.finishPage(page)
                compressedBmp.recycle()
            }

            onProgress(0.85f, "Writing compressed PDF...")

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            onProgress(0.95f, "Generating preview...")

            // Render preview bitmaps from the newly generated compressed PDF
            val renderedPreviews = renderPdfFileToBitmaps(outputFile)
            previewBitmaps.addAll(renderedPreviews)

            val finalLength = outputFile.length()
            val formatted = formatFileSize(finalLength)

            onProgress(1.0f, "Completed")

            CompressedPdfResult(
                file = outputFile,
                originalSizeBytes = originalTotalBytes.coerceAtLeast(finalLength),
                compressedSizeBytes = finalLength,
                formattedSize = formatted,
                pageCount = previewBitmaps.size.coerceAtLeast(totalPages),
                previewBitmaps = previewBitmaps
            )
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Compresses an arbitrary list of Bitmaps (e.g. from an imported PDF/image).
     */
    suspend fun compressBitmaps(
        context: Context,
        documentTitle: String,
        bitmaps: List<Bitmap>,
        level: CompressionLevel,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): CompressedPdfResult? = withContext(Dispatchers.IO) {
        if (bitmaps.isEmpty()) return@withContext null

        val pdfDir = FileUtils.getPdfsDir(context)
        val sanitizedTitle = documentTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Document" }
        val outputFile = getUniqueOutputFile(pdfDir, sanitizedTitle)

        val pdfDocument = PdfDocument()
        val previewBitmaps = mutableListOf<Bitmap>()

        try {
            val totalPages = bitmaps.size
            var estOriginalBytes = 0L

            onProgress(0.1f, "Preparing pages...")

            bitmaps.forEachIndexed { index, rawBmp ->
                estOriginalBytes += (rawBmp.byteCount.toLong() / 3L) // estimate raw size
                val stepFraction = 0.1f + (0.7f * (index.toFloat() / totalPages.coerceAtLeast(1)))
                onProgress(stepFraction, "Optimizing page ${index + 1} of $totalPages...")

                val compressedBmp = compressAndScaleBitmap(rawBmp, level)

                val pageWidth = 595
                val pageHeight = 842
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)

                val margin = 20f
                val availableWidth = pageWidth - (margin * 2)
                val availableHeight = pageHeight - (margin * 2)

                val bmpRatio = compressedBmp.width.toFloat() / compressedBmp.height.toFloat()
                val availRatio = availableWidth / availableHeight

                val drawWidth: Float
                val drawHeight: Float
                if (bmpRatio > availRatio) {
                    drawWidth = availableWidth
                    drawHeight = availableWidth / bmpRatio
                } else {
                    drawHeight = availableHeight
                    drawWidth = availableHeight * bmpRatio
                }

                val left = margin + (availableWidth - drawWidth) / 2f
                val top = margin + (availableHeight - drawHeight) / 2f

                val destRect = RectF(left, top, left + drawWidth, top + drawHeight)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(compressedBmp, null, destRect, paint)

                pdfDocument.finishPage(page)
                compressedBmp.recycle()
            }

            onProgress(0.85f, "Writing compressed PDF...")

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            onProgress(0.95f, "Generating preview...")

            val renderedPreviews = renderPdfFileToBitmaps(outputFile)
            previewBitmaps.addAll(renderedPreviews)

            val finalLength = outputFile.length()
            val formatted = formatFileSize(finalLength)

            onProgress(1.0f, "Completed")

            CompressedPdfResult(
                file = outputFile,
                originalSizeBytes = estOriginalBytes.coerceAtLeast(finalLength),
                compressedSizeBytes = finalLength,
                formattedSize = formatted,
                pageCount = previewBitmaps.size.coerceAtLeast(totalPages),
                previewBitmaps = previewBitmaps
            )
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun compressAndScaleBitmap(src: Bitmap, level: CompressionLevel): Bitmap {
        val maxDim = level.maxDimension
        val w = src.width
        val h = src.height

        val scaled = if (w > maxDim || h > maxDim) {
            val scale = maxDim.toFloat() / maxOf(w, h)
            val targetW = (w * scale).toInt().coerceAtLeast(1)
            val targetH = (h * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(src, targetW, targetH, true)
        } else {
            src.copy(Bitmap.Config.ARGB_8888, true)
        }

        // Compress to JPEG bytes with specified quality
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, level.jpegQuality, baos)
        if (scaled != src) {
            scaled.recycle()
        }

        val bytes = baos.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: src
    }

    private fun renderPdfFileToBitmaps(file: File): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val width = (page.width * 2).coerceAtLeast(1080)
                val height = ((page.height.toFloat() / page.width.toFloat()) * width).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { renderer?.close() } catch (ignored: Exception) {}
            try { pfd?.close() } catch (ignored: Exception) {}
        }
        return bitmaps
    }

    private fun getUniqueOutputFile(dir: File, baseName: String): File {
        var candidate = File(dir, "${baseName}_compressed.pdf")
        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "${baseName}_compressed_$counter.pdf")
            counter++
        }
        return candidate
    }

    /**
     * Native Android share via Sharesheet and FileProvider.
     */
    fun sharePdf(context: Context, pdfFile: File) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "PDF file does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.provider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, pdfFile.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Compressed PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not share PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves the compressed PDF to external device Downloads/DocScanner storage.
     */
    fun savePdfToDevice(context: Context, pdfFile: File): File? {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return null
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, pdfFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DocScanner")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        pdfFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    Toast.makeText(context, "Saved to Downloads/DocScanner/${pdfFile.name}", Toast.LENGTH_LONG).show()
                    NotificationHelper.showFileSavedNotification(
                        context = context,
                        fileName = pdfFile.name,
                        fileUri = uri,
                        filePath = "Downloads/DocScanner/${pdfFile.name}",
                        mimeType = "application/pdf",
                        customTitle = "Compressed PDF Saved / পিডিএফ সেভ হয়েছে",
                        customMessage = "${pdfFile.name} saved to Downloads/DocScanner"
                    )
                    pdfFile
                } else {
                    fallbackDirectSave(context, pdfFile)
                }
            } else {
                fallbackDirectSave(context, pdfFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackDirectSave(context, pdfFile)
        }
    }

    private fun fallbackDirectSave(context: Context, pdfFile: File): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val docScannerDir = File(downloadsDir, "DocScanner")
            if (!docScannerDir.exists()) docScannerDir.mkdirs()
            val destFile = File(docScannerDir, pdfFile.name)
            pdfFile.copyTo(destFile, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("application/pdf"), null)
            Toast.makeText(context, "Saved to Downloads/DocScanner/${pdfFile.name}", Toast.LENGTH_LONG).show()
            NotificationHelper.showFileSavedNotification(
                context = context,
                file = destFile,
                customTitle = "Compressed PDF Saved / পিডিএফ সেভ হয়েছে",
                customMessage = "${destFile.name} saved to Downloads/DocScanner"
            )
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Saved in app documents: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
            pdfFile
        }
    }

    /**
     * Opens PDF in external/system viewer app.
     */
    fun openPdf(context: Context, pdfFile: File) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "PDF file does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.provider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(viewIntent, "Open PDF with")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No PDF viewer app found on device", Toast.LENGTH_SHORT).show()
        }
    }
}
