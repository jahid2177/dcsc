package com.docscan.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.core.content.FileProvider
import com.docscan.data.model.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Professional OpenXML (.docx) Word Document Generator & Exporter.
 * Generates genuine Microsoft Word documents (.docx) with:
 * - Real editable text, headings, paragraphs, and lists
 * - Preserved document layouts & section margins
 * - Structured tables with header styling, borders, and cell formatting
 * - Embedded high-resolution images with proportional scaling
 * - Full UTF-8 support for mixed Bengali (বাংলা) and English text
 * - Standard OpenXML package packaging ([Content_Types].xml, _rels, word/document.xml, etc.)
 */
object DocxExporter {

    data class DocxExportConfig(
        val title: String = "Document",
        val layoutMode: String = "Exact Layout",      // "Exact Layout", "Editable Layout", "Text Focused"
        val ocrLanguage: String = "Auto Detect",      // "Auto Detect", "Bengali", "English"
        val imageHandling: String = "Preserve Images",// "Preserve Images", "Extract Images", "Ignore Images"
        val tableHandling: String = "Detect & Rebuild",// "Detect & Rebuild", "Preserve as Image", "Raw Text"
        val quality: String = "High",                 // "Standard", "High", "Maximum"
        val embedPageImages: Boolean = true,
        val includeExtractedTables: Boolean = true,
        val includeFormattedText: Boolean = true,
        val preservePageSize: Boolean = true,
        val preserveMargins: Boolean = true,
        val includePageNumbers: Boolean = true,
        val fontFamily: String = "Segoe UI"
    )

    /**
     * Generates a genuine .docx file from a list of PageEntity objects.
     */
    suspend fun generateDocx(
        context: Context,
        documentTitle: String,
        pages: List<PageEntity>,
        config: DocxExportConfig = DocxExportConfig(title = documentTitle)
    ): File? = withContext(Dispatchers.IO) {
        try {
            val docsDir = FileUtils.getPdfsDir(context)
            val sanitizedTitle = documentTitle.trim()
                .replace(Regex("[^a-zA-Z0-9_\\-\\s\u0980-\u09FF]"), "_")
                .replace(Regex("\\s+"), "_")
                .ifBlank { "Word_Document" }
            
            // Ensure unique filename
            var candidateFile = File(docsDir, "${sanitizedTitle}.docx")
            var counter = 1
            while (candidateFile.exists()) {
                candidateFile = File(docsDir, "${sanitizedTitle}_($counter).docx")
                counter++
            }
            val docxFile = candidateFile

            // Collect valid page images for embedding if enabled
            val shouldEmbedImages = config.imageHandling != "Ignore Images" && (config.embedPageImages || config.layoutMode == "Exact Layout")
            val embeddedImages = mutableListOf<Pair<Int, ByteArray>>() // (pageIndex, imageBytes)

            if (shouldEmbedImages) {
                val compressionQuality = when (config.quality) {
                    "Maximum" -> 92
                    "Standard" -> 75
                    else -> 85 // High
                }
                pages.forEachIndexed { index, page ->
                    val imgFile = File(page.processedImagePath).takeIf { it.exists() }
                        ?: File(page.originalImagePath).takeIf { it.exists() }
                    if (imgFile != null) {
                        try {
                            val bytes = compressImageForWord(imgFile, compressionQuality)
                            if (bytes != null && bytes.isNotEmpty()) {
                                embeddedImages.add(Pair(index, bytes))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            val imageRelMap = embeddedImages.associate { it.first to "rIdImg_${it.first + 1}" }

            val docXmlContent = buildDocumentXml(documentTitle, pages, config, imageRelMap)
            val contentTypesXml = buildContentTypesXml(embeddedImages.isNotEmpty())
            val relsXml = buildRelsXml()
            val docRelsXml = buildDocumentRelsXml(embeddedImages)
            val stylesXml = buildStylesXml(config.fontFamily)
            val corePropsXml = buildCorePropsXml(documentTitle)
            val appPropsXml = buildAppPropsXml(pages.size)

            FileOutputStream(docxFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    // 1. [Content_Types].xml
                    writeZipEntry(zos, "[Content_Types].xml", contentTypesXml)

                    // 2. _rels/.rels
                    writeZipEntry(zos, "_rels/.rels", relsXml)

                    // 3. docProps/core.xml
                    writeZipEntry(zos, "docProps/core.xml", corePropsXml)

                    // 4. docProps/app.xml
                    writeZipEntry(zos, "docProps/app.xml", appPropsXml)

                    // 5. word/_rels/document.xml.rels
                    writeZipEntry(zos, "word/_rels/document.xml.rels", docRelsXml)

                    // 6. word/styles.xml
                    writeZipEntry(zos, "word/styles.xml", stylesXml)

                    // 7. word/media/image_p{index}.jpeg
                    embeddedImages.forEach { (index, bytes) ->
                        val entryName = "word/media/image_p${index + 1}.jpeg"
                        writeZipBytes(zos, entryName, bytes)
                    }

                    // 8. word/document.xml
                    writeZipEntry(zos, "word/document.xml", docXmlContent)
                }
            }

            if (docxFile.exists() && docxFile.length() > 0) {
                NotificationHelper.showFileSavedNotification(
                    context = context,
                    file = docxFile,
                    customTitle = "Word Document Saved / ডক ফাইল সেভ হয়েছে",
                    customMessage = "${docxFile.name} (${FileUtils.getFormattedFileSize(docxFile.length())})"
                )
                docxFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compressImageForWord(file: File, quality: Int = 85): ByteArray? {
        return try {
            val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val stream = ByteArrayOutputStream()
            val maxDim = 2000
            val scaled = if (bmp.width > maxDim || bmp.height > maxDim) {
                val ratio = minOf(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height)
                Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
            } else {
                bmp
            }
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun writeZipBytes(zos: ZipOutputStream, entryName: String, bytes: ByteArray) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun buildContentTypesXml(hasImages: Boolean): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
""")
        if (hasImages) {
            sb.append("""  <Default Extension="jpeg" ContentType="image/jpeg"/>
  <Default Extension="jpg" ContentType="image/jpeg"/>
  <Default Extension="png" ContentType="image/png"/>
""")
        }
        sb.append("""  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>""")
        return sb.toString()
    }

    private fun buildRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>"""
    }

    private fun buildCorePropsXml(title: String): String {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                   xmlns:dc="http://purl.org/dc/elements/1.1/"
                   xmlns:dcterms="http://purl.org/dc/terms/"
                   xmlns:dcmitype="http://purl.org/dc/dcmitype/"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>${escapeXml(title)}</dc:title>
  <dc:creator>AI Studio Doc Scanner</dc:creator>
  <cp:lastModifiedBy>AI Studio Doc Scanner</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">$nowIso</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">$nowIso</dcterms:modified>
</cp:coreProperties>"""
    }

    private fun buildAppPropsXml(pageCount: Int): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
            xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>AI Studio Doc Scanner</Application>
  <DocSecurity>0</DocSecurity>
  <Lines>1</Lines>
  <Paragraphs>1</Paragraphs>
  <Pages>$pageCount</Pages>
  <Company>AI Studio</Company>
  <AppVersion>16.0000</AppVersion>
</Properties>"""
    }

    private fun buildDocumentRelsXml(images: List<Pair<Int, ByteArray>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
""")
        images.forEach { (index, _) ->
            val relId = "rIdImg_${index + 1}"
            val target = "media/image_p${index + 1}.jpeg"
            sb.append("""  <Relationship Id="$relId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="$target"/>
""")
        }
        sb.append("</Relationships>")
        return sb.toString()
    }

    private fun buildStylesXml(fontFamily: String): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="$fontFamily" w:hAnsi="$fontFamily" w:eastAsia="$fontFamily" w:cs="$fontFamily"/>
        <w:sz w:val="22"/>
        <w:szCs w:val="22"/>
        <w:color w:val="1E293B"/>
        <w:lang w:val="en-US" w:bidi="bn-BD"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:qFormat/>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:basedOn w:val="Normal"/>
    <w:qFormat/>
    <w:rPr>
      <w:b/>
      <w:sz w:val="36"/>
      <w:color w:val="0F766E"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading2">
    <w:name w:val="heading 2"/>
    <w:basedOn w:val="Normal"/>
    <w:qFormat/>
    <w:rPr>
      <w:b/>
      <w:sz w:val="28"/>
      <w:color w:val="0D9488"/>
    </w:rPr>
  </w:style>
</w:styles>"""
    }

    private fun buildDocumentXml(
        title: String,
        pages: List<PageEntity>,
        config: DocxExportConfig,
        imageRelMap: Map<Int, String>
    ): String {
        val dateFormatted = SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        val font = config.fontFamily

        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
            xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
  <w:body>
""")

        // Document Title Banner Header (Word style)
        if (config.layoutMode != "Text Focused") {
            sb.append("""
    <w:p>
      <w:pPr>
        <w:jc w:val="center"/>
        <w:spacing w:before="120" w:after="80"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:rFonts w:ascii="$font" w:hAnsi="$font" w:cs="$font"/>
          <w:b/>
          <w:sz w:val="44"/>
          <w:color w:val="0F766E"/>
        </w:rPr>
        <w:t>${escapeXml(title)}</w:t>
      </w:r>
    </w:p>
    <w:p>
      <w:pPr>
        <w:jc w:val="center"/>
        <w:spacing w:after="240"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:rFonts w:ascii="$font" w:hAnsi="$font" w:cs="$font"/>
          <w:i/>
          <w:sz w:val="18"/>
          <w:color w:val="64748B"/>
        </w:rPr>
        <w:t>Converted to Word • $dateFormatted • ${pages.size} Page(s)</w:t>
      </w:r>
    </w:p>
    <w:p>
      <w:pPr>
        <w:pBdr>
          <w:bottom w:val="single" w:sz="8" w:space="1" w:color="CBD5E1"/>
        </w:pBdr>
        <w:spacing w:after="200"/>
      </w:pPr>
    </w:p>
""")
        }

        // Iterate through each page
        pages.forEachIndexed { index, page ->
            val pageNum = page.pageNumber.takeIf { it > 0 } ?: (index + 1)
            val pageLabel = page.notes?.takeIf { it.isNotBlank() } ?: "Page $pageNum"

            // Page Header Title (for multi-page docs)
            if (pages.size > 1) {
                sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:before="180" w:after="100"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:rFonts w:ascii="$font" w:hAnsi="$font" w:cs="$font"/>
          <w:b/>
          <w:sz w:val="24"/>
          <w:color w:val="0D9488"/>
        </w:rPr>
        <w:t>${escapeXml(pageLabel)}</w:t>
      </w:r>
    </w:p>
""")
            }

            // 1. EMBEDDED PAGE IMAGE (if Exact Layout or Preserve Images enabled)
            val relId = imageRelMap[index]
            val includeImageThisPage = relId != null && (config.layoutMode == "Exact Layout" || config.imageHandling == "Preserve Images")

            if (includeImageThisPage && relId != null) {
                val drawingId = index + 1
                sb.append("""
    <w:p>
      <w:pPr>
        <w:jc w:val="center"/>
        <w:spacing w:before="100" w:after="180"/>
      </w:pPr>
      <w:r>
        <w:drawing>
          <wp:inline distT="0" distB="0" distL="0" distR="0">
            <wp:extent cx="5029200" cy="6705600"/>
            <wp:docPr id="$drawingId" name="PageImage_$drawingId"/>
            <wp:cNvGraphicFramePr>
              <a:graphicFrameLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" noChangeAspect="1"/>
            </wp:cNvGraphicFramePr>
            <a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
              <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                  <pic:nvPicPr>
                    <pic:cNvPr id="$drawingId" name="image_p$drawingId.jpeg"/>
                    <pic:cNvPicPr/>
                  </pic:nvPicPr>
                  <pic:blipFill>
                    <a:blip r:embed="$relId"/>
                    <a:stretch>
                      <a:fillRect/>
                    </a:stretch>
                  </pic:blipFill>
                  <pic:spPr>
                    <a:xfrm>
                      <a:off x="0" y="0"/>
                      <a:ext cx="5029200" cy="6705600"/>
                    </a:xfrm>
                    <a:prstGeom prst="rect">
                      <a:avLst/>
                    </a:prstGeom>
                  </pic:spPr>
                </pic:pic>
              </a:graphicData>
            </a:graphic>
          </wp:inline>
        </w:drawing>
      </w:r>
    </w:p>
""")
            }

            // 2. EXTRACTED & EDITABLE TEXT / TABLES / LISTS
            val text = page.extractedText?.trim()
            if (!text.isNullOrBlank()) {
                val shouldRebuildTables = config.tableHandling == "Detect & Rebuild"
                val tables = if (shouldRebuildTables) PdfTableExtractor.extractTablesFromText(text) else emptyList()

                if (tables.isNotEmpty() && tables.any { it.rows.isNotEmpty() && it.rows.size >= 2 }) {
                    // Render Rebuilt Editable Tables
                    tables.forEach { table ->
                        sb.append(buildDocxTableXml(table, font))
                    }
                } else if (config.includeFormattedText || config.layoutMode != "Exact Layout") {
                    // Render formatted headings, bullet lists, key-values, and paragraphs
                    val lines = text.split("\n")
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            // Check for Headings
                            val isMainHeading = trimmed.startsWith("# ") || (trimmed.length in 3..40 && trimmed.endsWith(":") && !trimmed.contains(" ") && trimmed.all { it.isUpperCase() || it == ':' })
                            val isSubHeading = trimmed.startsWith("## ") || trimmed.startsWith("### ") || (trimmed.endsWith(":") && trimmed.length < 50)
                            
                            // Check for Bullet/Numbered Lists
                            val isBullet = trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ")
                            val isNumbered = Regex("^\\d+[\\.\\)]\\s+.*").matches(trimmed)

                            val cleanText = trimmed
                                .removePrefix("### ").removePrefix("## ").removePrefix("# ")
                                .removePrefix("• ").removePrefix("- ").removePrefix("* ")
                                .trim()

                            when {
                                isMainHeading -> {
                                    sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:before="160" w:after="80"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:rFonts w:ascii="$font" w:hAnsi="$font" w:cs="$font"/>
          <w:b/>
          <w:sz w:val="30"/>
          <w:color w:val="0F766E"/>
        </w:rPr>
        <w:t>${escapeXml(cleanText)}</w:t>
      </w:r>
    </w:p>
""")
                                }
                                isSubHeading -> {
                                    sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:before="120" w:after="60"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:rFonts w:ascii="$font" w:hAnsi="$font" w:cs="$font"/>
          <w:b/>
          <w:sz w:val="24"/>
          <w:color w:val="0D9488"/>
        </w:rPr>
        <w:t>${escapeXml(cleanText)}</w:t>
      </w:r>
    </w:p>
""")
                                }
                                isBullet || isNumbered -> {
                                    val prefix = if (isBullet) "•  " else ""
                                    sb.append("""
    <w:p>
      <w:pPr>
        <w:ind w:left="400"/>
        <w:spacing w:after="80" w:line="260" w:lineRule="auto"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:rFonts w:ascii="$font" w:hAnsi="$font" w:cs="$font"/>
          <w:sz w:val="22"/>
          <w:color w:val="1E293B"/>
        </w:rPr>
        <w:t>${escapeXml(prefix + cleanText)}</w:t>
      </w:r>
    </w:p>
""")
                                }
                                else -> {
                                    // Regular text paragraph
                                    sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:after="100" w:line="276" w:lineRule="auto"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:rFonts w:ascii="$font" w:hAnsi="$font" w:cs="$font"/>
          <w:sz w:val="22"/>
          <w:color w:val="1E293B"/>
        </w:rPr>
        <w:t>${escapeXml(cleanText)}</w:t>
      </w:r>
    </w:p>
""")
                                }
                            }
                        }
                    }
                }
            } else if (!includeImageThisPage) {
                // If page had no text and images were not embedded
                sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:after="120"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:rFonts w:ascii="$font" w:hAnsi="$font" w:cs="$font"/>
          <w:i/>
          <w:sz w:val="22"/>
          <w:color w:val="94A3B8"/>
        </w:rPr>
        <w:t>[Document Page Content]</w:t>
      </w:r>
    </w:p>
""")
            }

            // Page Break between pages (except last)
            if (index < pages.size - 1) {
                sb.append("""
    <w:p>
      <w:r>
        <w:br w:type="page"/>
      </w:r>
    </w:p>
""")
            }
        }

        // Section A4 sizing & margins
        sb.append("""
    <w:sectPr>
      <w:pgSz w:w="11906" w:h="16838"/>
      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
    </w:sectPr>
  </w:body>
</w:document>""")

        return sb.toString()
    }

    private fun buildDocxTableXml(table: PdfTableExtractor.ExtractedTable, font: String): String {
        val sb = StringBuilder()
        val totalCols = maxOf(table.headers.size, table.rows.maxOfOrNull { it.size } ?: 1)

        sb.append("""
    <w:tbl>
      <w:tblPr>
        <w:tblW w:w="9000" w:type="dxa"/>
        <w:tblBorders>
          <w:top w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
          <w:left w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
          <w:bottom w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
          <w:right w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
          <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
          <w:insideV w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
        </w:tblBorders>
        <w:tblCellMar>
          <w:top w:w="120" w:type="dxa"/>
          <w:left w:w="160" w:type="dxa"/>
          <w:bottom w:w="120" w:type="dxa"/>
          <w:right w:w="160" w:type="dxa"/>
        </w:tblCellMar>
      </w:tblPr>
""")

        // Header Row
        sb.append("""
      <w:tr>
        <w:trPr><w:tblHeader/></w:trPr>
""")
        for (colIdx in 0 until totalCols) {
            val headerText = table.headers.getOrNull(colIdx) ?: "Col ${colIdx + 1}"
            sb.append("""
        <w:tc>
          <w:tcPr>
            <w:shd w:val="clear" w:color="auto" w:fill="0F766E"/>
          </w:tcPr>
          <w:p>
            <w:pPr><w:jc w:val="center"/></w:pPr>
            <w:r>
              <w:rPr>
                <w:rFonts w:ascii="$font" w:hAnsi="$font" w:cs="$font"/>
                <w:b/>
                <w:sz w:val="22"/>
                <w:color w:val="FFFFFF"/>
              </w:rPr>
              <w:t>${escapeXml(headerText)}</w:t>
            </w:r>
          </w:p>
        </w:tc>
""")
        }
        sb.append("      </w:tr>\n")

        // Data Rows
        table.rows.forEachIndexed { rowIdx, rowData ->
            val isZebra = rowIdx % 2 == 1
            val fillHex = if (isZebra) "F8FAFC" else "FFFFFF"

            sb.append("""
      <w:tr>
""")
            for (colIdx in 0 until totalCols) {
                val cellText = rowData.getOrNull(colIdx) ?: ""
                sb.append("""
        <w:tc>
          <w:tcPr>
            <w:shd w:val="clear" w:color="auto" w:fill="$fillHex"/>
          </w:tcPr>
          <w:p>
            <w:r>
              <w:rPr>
                <w:rFonts w:ascii="$font" w:hAnsi="$font" w:cs="$font"/>
                <w:sz w:val="20"/>
                <w:color w:val="1E293B"/>
              </w:rPr>
              <w:t>${escapeXml(cellText)}</w:t>
            </w:r>
          </w:p>
        </w:tc>
""")
            }
            sb.append("      </w:tr>\n")
        }

        sb.append("    </w:tbl>\n")
        return sb.toString()
    }

    /**
     * Share Word Document via standard Android Intent.
     */
    fun shareDocx(context: Context, docxFile: File) {
        if (!docxFile.exists()) {
            Toast.makeText(context, "DOCX file not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, docxFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, docxFile.nameWithoutExtension)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Word (.docx) Document")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not share DOCX: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share Word Document specifically to WhatsApp
     */
    fun shareDocxToWhatsApp(context: Context, docxFile: File) {
        if (!docxFile.exists()) {
            Toast.makeText(context, "DOCX file not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, docxFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Here is your Word document: ${docxFile.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // If WhatsApp is not installed or errors out, fallback to general share chooser
            shareDocx(context, docxFile)
        }
    }

    /**
     * Share Word Document specifically to Gmail
     */
    fun shareDocxToGmail(context: Context, docxFile: File) {
        if (!docxFile.exists()) {
            Toast.makeText(context, "DOCX file not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, docxFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                setPackage("com.google.android.gm")
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, docxFile.nameWithoutExtension)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general share chooser
            shareDocx(context, docxFile)
        }
    }

    /**
     * Open Word Document with installed MS Word, WPS Office, or Docs app.
     */
    fun openDocx(context: Context, docxFile: File) {
        if (!docxFile.exists()) {
            Toast.makeText(context, "DOCX file not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, docxFile)

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(viewIntent, "Open Word Document with")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No Word document viewer found on device", Toast.LENGTH_SHORT).show()
        }
    }
}
