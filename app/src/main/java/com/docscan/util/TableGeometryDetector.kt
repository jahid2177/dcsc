package com.docscan.util

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.abs

/**
 * Reconstructs the real row/column structure of a table photographed inside a document
 * (salary slips, invoices, forms, etc.) using OCR word-level bounding boxes.
 *
 * Plain OCR text (a single flattened string) has no positional information, so a table
 * where columns are only visually separated by whitespace cannot be told apart from a
 * normal paragraph. This detector instead clusters words by their on-screen position:
 * lines that sit at roughly the same height become one table row, and words that line up
 * vertically across rows become one table column. The result is emitted as a simple
 * pipe-delimited grid (e.g. "Name | Designation | Salary") that [PdfTableExtractor] and
 * [ExcelExporter] already know how to turn into a real, structured table.
 */
object TableGeometryDetector {

    /**
     * Attempts to detect a genuine table on [bitmap] and returns it as pipe-delimited text
     * (one row per line, cells separated by " | ", empty cells preserved as "").
     * Returns null when the page does not look like a table with reasonable confidence,
     * so callers can fall back to normal paragraph OCR.
     */
    suspend fun detectTableText(bitmap: Bitmap): String? {
        val lines = TextRecognizerHelper.extractLinesWithWords(bitmap)
        if (lines.size < 3) return null

        val allWords = lines.flatMap { it.words }
        if (allWords.size < 6) return null

        // --- 1. Group lines into rows by vertical proximity ---
        val avgLineHeight = lines.map { it.rect.height() }.average().takeIf { it > 0 } ?: return null
        val sortedLines = lines.sortedBy { it.rect.centerY() }
        val rowGroups = mutableListOf<MutableList<TextRecognizerHelper.LineWithWordsItem>>()
        for (line in sortedLines) {
            val lastRow = rowGroups.lastOrNull()
            val lastCenter = lastRow?.let { row -> row.map { it.rect.centerY() }.average() }
            if (lastRow != null && lastCenter != null && abs(line.rect.centerY() - lastCenter) < avgLineHeight * 0.6) {
                lastRow.add(line)
            } else {
                rowGroups.add(mutableListOf(line))
            }
        }
        if (rowGroups.size < 3) return null

        // --- 2. Determine column bands from the union of every word's horizontal span ---
        val bitmapWidth = bitmap.width.takeIf { it > 0 } ?: 1000
        val gapThreshold = (bitmapWidth * 0.025f).coerceAtLeast(14f)
        val wordsByLeft = allWords.sortedBy { it.rect.left }
        data class Band(var left: Int, var right: Int)
        val bands = mutableListOf<Band>()
        for (word in wordsByLeft) {
            val current = bands.lastOrNull()
            if (current != null && word.rect.left - current.right < gapThreshold) {
                current.right = maxOf(current.right, word.rect.right)
            } else {
                bands.add(Band(word.rect.left, word.rect.right))
            }
        }
        if (bands.size < 2) return null

        fun bandIndexFor(rect: Rect): Int {
            val cx = rect.centerX()
            var bestIdx = 0
            var bestDist = Int.MAX_VALUE
            bands.forEachIndexed { idx, band ->
                val dist = when {
                    cx < band.left -> band.left - cx
                    cx > band.right -> cx - band.right
                    else -> 0
                }
                if (dist < bestDist) {
                    bestDist = dist
                    bestIdx = idx
                }
            }
            return bestIdx
        }

        // --- 3. Build the row x column grid ---
        val grid = mutableListOf<Array<String>>()
        for (row in rowGroups) {
            val cells = Array(bands.size) { "" }
            val wordsInRow = row.flatMap { line -> line.words }.sortedBy { it.rect.left }
            for (word in wordsInRow) {
                val col = bandIndexFor(word.rect)
                cells[col] = if (cells[col].isEmpty()) word.text else "${cells[col]} ${word.text}"
            }
            grid.add(cells)
        }

        // --- 4. Confidence check: does this really look like a table, not a paragraph? ---
        val multiColumnRows = grid.count { row -> row.count { it.isNotBlank() } > 1 }
        val occupiedRatio = multiColumnRows.toFloat() / grid.size
        if (occupiedRatio < 0.5f) return null

        val filledCells = grid.sumOf { row -> row.count { it.isNotBlank() } }
        val totalCells = grid.size * bands.size
        if (totalCells == 0 || filledCells.toFloat() / totalCells < 0.2f) return null

        // --- 5. Emit as pipe-delimited text (no wrapping pipes, empty cells preserved) ---
        return grid.joinToString("\n") { row -> row.joinToString(" | ") { it.trim() } }
    }
}
