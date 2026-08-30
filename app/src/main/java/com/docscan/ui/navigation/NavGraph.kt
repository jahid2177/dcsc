package com.docscan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.docscan.data.model.ScanMode
import com.docscan.ui.screens.AddWatermarkScreen
import com.docscan.ui.screens.CameraScanScreen
import com.docscan.ui.screens.CompressPdfScreen
import com.docscan.ui.screens.CropFilterScreen
import com.docscan.ui.screens.CropScreen
import com.docscan.ui.screens.DocumentDetailScreen
import com.docscan.ui.screens.DocumentPreviewScreen
import com.docscan.ui.screens.ExtractPdfPagesScreen
import com.docscan.ui.screens.ExtractTextScreen
import com.docscan.ui.screens.HomeScreen
import com.docscan.ui.screens.MergeFilesScreen
import com.docscan.ui.screens.PassportPhotoMakerScreen
import com.docscan.ui.screens.PdfToExcelScreen
import com.docscan.ui.screens.PdfToImageScreen
import com.docscan.ui.screens.PdfToWordScreen
import com.docscan.ui.screens.WordReaderScreen
import com.docscan.ui.screens.ExcelReaderScreen
import com.docscan.ui.screens.ReorderPagesScreen
import com.docscan.ui.screens.SignPdfEditorScreen
import com.docscan.ui.screens.SignPdfSelectionScreen
import com.docscan.ui.screens.SinglePageEditorScreen
import com.docscan.ui.viewmodel.ScannerViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ExtractPdfPages : Screen("extract_pdf_pages")
    object ExtractPdfPagesDoc : Screen("extract_pdf_pages/{docId}") {
        fun createRoute(docId: Long) = "extract_pdf_pages/$docId"
    }
    object ExtractText : Screen("extract_text")
    object ExtractTextDoc : Screen("extract_text/{docId}") {
        fun createRoute(docId: Long) = "extract_text/$docId"
    }
    object AddWatermark : Screen("add_watermark")
    object AddWatermarkDoc : Screen("add_watermark/{docId}") {
        fun createRoute(docId: Long) = "add_watermark/$docId"
    }
    object ReorderPages : Screen("reorder_pages")
    object ReorderDocumentPages : Screen("reorder_document/{docId}") {
        fun createRoute(docId: Long) = "reorder_document/$docId"
    }
    object MergeFiles : Screen("merge_files")
    object CompressPdf : Screen("compress_pdf")
    object SignPdf : Screen("sign_pdf")
    object SignPdfEditor : Screen("sign_pdf_editor/{docId}") {
        fun createRoute(docId: Long) = "sign_pdf_editor/$docId"
    }
    object ToWord : Screen("to_word")
    object ToWordDoc : Screen("to_word/{docId}") {
        fun createRoute(docId: Long) = "to_word/$docId"
    }
    object ToExcel : Screen("to_excel")
    object ToExcelDoc : Screen("to_excel/{docId}") {
        fun createRoute(docId: Long) = "to_excel/$docId"
    }
    object PdfToImage : Screen("pdf_to_image")
    object PdfToImageDoc : Screen("pdf_to_image/{docId}") {
        fun createRoute(docId: Long) = "pdf_to_image/$docId"
    }
    object Camera : Screen("camera")
    object CameraAppend : Screen("camera/{docId}") {
        fun createRoute(docId: Long) = "camera/$docId"
    }
    object Crop : Screen("crop")
    object CropAppend : Screen("crop/{docId}") {
        fun createRoute(docId: Long) = "crop/$docId"
    }
    object DocumentPreview : Screen("document_preview")
    object DocumentDetail : Screen("detail/{docId}") {
        fun createRoute(docId: Long) = "detail/$docId"
    }
    object SinglePageEditor : Screen("single_page_editor/{pageId}") {
        fun createRoute(pageId: Long) = "single_page_editor/$pageId"
    }
    object PassportPhotoMaker : Screen("passport_photo_maker")
    object WordReader : Screen("word_reader?filePath={filePath}&uri={uri}&title={title}") {
        fun createRoute(filePath: String? = null, uri: String? = null, title: String? = null): String {
            val ep = java.net.URLEncoder.encode(filePath ?: "", "UTF-8")
            val eu = java.net.URLEncoder.encode(uri ?: "", "UTF-8")
            val et = java.net.URLEncoder.encode(title ?: "", "UTF-8")
            return "word_reader?filePath=$ep&uri=$eu&title=$et"
        }
    }
    object ExcelReader : Screen("excel_reader?filePath={filePath}&uri={uri}&title={title}") {
        fun createRoute(filePath: String? = null, uri: String? = null, title: String? = null): String {
            val ep = java.net.URLEncoder.encode(filePath ?: "", "UTF-8")
            val eu = java.net.URLEncoder.encode(uri ?: "", "UTF-8")
            val et = java.net.URLEncoder.encode(title ?: "", "UTF-8")
            return "excel_reader?filePath=$ep&uri=$eu&title=$et"
        }
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: ScannerViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                },
                onNavigateToCrop = {
                    navController.navigate(Screen.Crop.route)
                },
                onNavigateToDocumentPreview = {
                    navController.navigate(Screen.DocumentPreview.route)
                },
                onNavigateToDocumentDetail = { docId ->
                    // If this document has a previously exported Word/Excel file attached,
                    // reopen it with the app's own reader (like CamScanner) instead of the
                    // plain scanned-page detail view.
                    val doc = viewModel.documentsList.value.firstOrNull { it.id == docId }
                    when {
                        !doc?.wordFilePath.isNullOrBlank() -> {
                            navController.navigate(
                                Screen.WordReader.createRoute(filePath = doc?.wordFilePath, title = doc?.title)
                            )
                        }
                        !doc?.excelFilePath.isNullOrBlank() -> {
                            navController.navigate(
                                Screen.ExcelReader.createRoute(filePath = doc?.excelFilePath, title = doc?.title)
                            )
                        }
                        else -> {
                            navController.navigate(Screen.DocumentDetail.createRoute(docId))
                        }
                    }
                },
                onNavigateToMergeFiles = {
                    navController.navigate(Screen.MergeFiles.route)
                },
                onNavigateToSignPdf = {
                    navController.navigate(Screen.SignPdf.route)
                },
                onNavigateToCompressPdf = {
                    navController.navigate(Screen.CompressPdf.route)
                },
                onNavigateToReorderPages = {
                    navController.navigate(Screen.ReorderPages.route)
                },
                onNavigateToReorderDoc = { docId ->
                    navController.navigate(Screen.ReorderDocumentPages.createRoute(docId))
                },
                onNavigateToAddWatermark = {
                    navController.navigate(Screen.AddWatermark.route)
                },
                onNavigateToAddWatermarkDoc = { docId ->
                    navController.navigate(Screen.AddWatermarkDoc.createRoute(docId))
                },
                onNavigateToExtractPdfPages = {
                    navController.navigate(Screen.ExtractPdfPages.route)
                },
                onNavigateToExtractPdfPagesDoc = { docId ->
                    navController.navigate(Screen.ExtractPdfPagesDoc.createRoute(docId))
                },
                onNavigateToExtractText = {
                    navController.navigate(Screen.ExtractText.route)
                },
                onNavigateToExtractTextDoc = { docId ->
                    navController.navigate(Screen.ExtractTextDoc.createRoute(docId))
                },
                onNavigateToPassportPhoto = {
                    navController.navigate(Screen.PassportPhotoMaker.route)
                },
                onNavigateToToWord = {
                    navController.navigate(Screen.ToWord.route)
                },
                onNavigateToToWordDoc = { docId ->
                    navController.navigate(Screen.ToWordDoc.createRoute(docId))
                },
                onNavigateToToExcel = {
                    navController.navigate(Screen.ToExcel.route)
                },
                onNavigateToToExcelDoc = { docId ->
                    navController.navigate(Screen.ToExcelDoc.createRoute(docId))
                },
                onNavigateToPdfToImages = {
                    navController.navigate(Screen.PdfToImage.route)
                },
                onNavigateToPdfToImagesDoc = { docId ->
                    navController.navigate(Screen.PdfToImageDoc.createRoute(docId))
                },
                onNavigateToWordReader = { filePath ->
                    navController.navigate(Screen.WordReader.createRoute(filePath = filePath))
                },
                onNavigateToExcelReader = { filePath ->
                    navController.navigate(Screen.ExcelReader.createRoute(filePath = filePath))
                }
            )
        }

        // PDF Extract / Extract PDF Pages Root Selection Screen (State 1)
        composable(Screen.ExtractPdfPages.route) {
            ExtractPdfPagesScreen(
                viewModel = viewModel,
                initialDocId = null,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDocumentDetail = { docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // PDF Extract / Extract PDF Pages for specific document (State 2 & 3)
        composable(
            route = Screen.ExtractPdfPagesDoc.route,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            ExtractPdfPagesScreen(
                viewModel = viewModel,
                initialDocId = docId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDocumentDetail = { targetDocId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(targetDocId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Extract Text & OCR Studio Screen (Root)
        composable(Screen.ExtractText.route) {
            ExtractTextScreen(
                viewModel = viewModel,
                initialDocId = null,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                }
            )
        }

        // Extract Text & OCR Studio Screen for specific document
        composable(
            route = Screen.ExtractTextDoc.route,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            ExtractTextScreen(
                viewModel = viewModel,
                initialDocId = docId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                }
            )
        }

        // Add Watermark Root Selection Screen (State 1)
        composable(Screen.AddWatermark.route) {
            AddWatermarkScreen(
                viewModel = viewModel,
                initialDocId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Add Watermark Editor for a specific document (State 2 & 3)
        composable(
            route = Screen.AddWatermarkDoc.route,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            AddWatermarkScreen(
                viewModel = viewModel,
                initialDocId = docId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Reorder Pages Root Screen (Screen A: Document List & Device Import)
        composable(Screen.ReorderPages.route) {
            ReorderPagesScreen(
                viewModel = viewModel,
                initialDocId = null,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenDocumentDetail = { docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Reorder Pages for specific document (Screen B)
        composable(
            route = Screen.ReorderDocumentPages.route,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            ReorderPagesScreen(
                viewModel = viewModel,
                initialDocId = docId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenDocumentDetail = { targetDocId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(targetDocId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Compress PDF 3-Screen Workflow
        composable(Screen.CompressPdf.route) {
            CompressPdfScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenDocumentDetail = { docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docId))
                }
            )
        }

        // Sign PDF File Selection Workflow (Screen 1)
        composable(Screen.SignPdf.route) {
            SignPdfSelectionScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSignEditor = { docId ->
                    navController.navigate(Screen.SignPdfEditor.createRoute(docId))
                },
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                }
            )
        }

        // Sign PDF Editor Workflow (Screens 2, 3, and 4)
        composable(
            route = Screen.SignPdfEditor.route,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            SignPdfEditorScreen(
                documentId = docId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSavedAndOpenDoc = { newDocId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(newDocId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Merge Files Dedicated Workflow (Screens 1, 2, and 3)
        composable(Screen.MergeFiles.route) {
            MergeFilesScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onMergeCompleted = { newDocId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(newDocId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Camera Screen (New Scan)
        composable(Screen.Camera.route) {
            CameraScanScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCrop = {
                    navController.navigate(Screen.Crop.route)
                },
                onNavigateToDocumentPreview = {
                    navController.navigate(Screen.DocumentPreview.route)
                }
            )
        }

        // Camera Screen (Append to existing Document)
        composable(
            route = Screen.CameraAppend.route,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            CameraScanScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCrop = {
                    navController.navigate(Screen.CropAppend.createRoute(docId))
                },
                onNavigateToDocumentPreview = {
                    navController.navigate(Screen.DocumentPreview.route)
                }
            )
        }

        // Crop Screen (Reference Screenshot 1: 4-Corner / Edge Handles, Left, Right, Auto Crop, All, Confirm)
        composable(Screen.Crop.route) {
            CropScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onConfirmCrop = {
                    if (viewModel.scanMode.value == ScanMode.ID_CARD) {
                        // Two-part ID Card preview screen (Screenshot 2 & 3: White A4 Document Sheet with 2 card slots)
                        navController.navigate(Screen.DocumentPreview.route) {
                            popUpTo(Screen.Camera.route) { inclusive = false }
                        }
                    } else {
                        // All other scan modes (Single, Batch, Scan, Smart Erase, Question Set, Translate, Extract Text, To Word, Sign):
                        // Directly save the scanned document and open the document details screen!
                        viewModel.saveScannedDocument { docId ->
                            navController.navigate(Screen.DocumentDetail.createRoute(docId)) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }
                },
                onRetake = { pageIndex ->
                    viewModel.currentCropPageIndex.value = pageIndex
                    navController.navigate(Screen.Camera.route)
                }
            )
        }

        // Document Preview Screen (Reference Screenshot 2 & 3: White Document Sheet, Front & Back Cards, Filter, Watermark, Compare)
        composable(Screen.DocumentPreview.route) {
            DocumentPreviewScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCrop = { pageIndex ->
                    viewModel.currentCropPageIndex.value = pageIndex
                    navController.navigate(Screen.Crop.route)
                },
                onRetake = { pageIndex ->
                    viewModel.currentCropPageIndex.value = pageIndex
                    navController.navigate(Screen.Camera.route)
                },
                onSaved = { savedDocId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(savedDocId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Crop & Filter Screen (Append to existing document)
        composable(
            route = Screen.CropAppend.route,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            CropFilterScreen(
                viewModel = viewModel,
                targetDocumentId = docId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onFinishAndOpenDoc = { finishedDocId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(finishedDocId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Document Details & Multi-page Management Screen
        composable(
            route = Screen.DocumentDetail.route,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            DocumentDetailScreen(
                documentId = docId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCameraForAppend = { targetDocId ->
                    navController.navigate(Screen.CameraAppend.createRoute(targetDocId))
                },
                onNavigateToCrop = {
                    navController.navigate(Screen.CropAppend.createRoute(docId))
                },
                onNavigateToSinglePageEditor = { targetPageId ->
                    navController.navigate(Screen.SinglePageEditor.createRoute(targetPageId))
                }
            )
        }

        // Dedicated Single Page Editor Screen
        composable(
            route = Screen.SinglePageEditor.route,
            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getLong("pageId") ?: 0L
            SinglePageEditorScreen(
                pageId = pageId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Modern 300 DPI Studio Passport Photo Maker Screen
        composable(Screen.PassportPhotoMaker.route) {
            PassportPhotoMakerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // To Word Dedicated Workflow (Tools -> To Word)
        composable(Screen.ToWord.route) {
            val documents by viewModel.documentsList.collectAsStateWithLifecycle()
            PdfToWordScreen(
                viewModel = viewModel,
                initialDocument = null,
                allDocuments = documents,
                onNavigateToScan = {
                    navController.navigate(Screen.Camera.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // To Word Dedicated Workflow for specific Document
        composable(
            route = Screen.ToWordDoc.route,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            val documents by viewModel.documentsList.collectAsStateWithLifecycle()
            val initialDoc = documents.firstOrNull { it.id == docId }
            PdfToWordScreen(
                viewModel = viewModel,
                initialDocument = initialDoc,
                allDocuments = documents,
                onNavigateToScan = {
                    navController.navigate(Screen.Camera.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // To Excel Dedicated Workflow (State 1: Selection & Device Import)
        composable(Screen.ToExcel.route) {
            val documents by viewModel.documentsList.collectAsStateWithLifecycle()
            PdfToExcelScreen(
                viewModel = viewModel,
                initialDocument = null,
                allDocuments = documents,
                onNavigateToScan = {
                    navController.navigate(Screen.Camera.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // To Excel Dedicated Workflow for specific Document
        composable(
            route = Screen.ToExcelDoc.route,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            val documents by viewModel.documentsList.collectAsStateWithLifecycle()
            val initialDoc = documents.firstOrNull { it.id == docId }
            PdfToExcelScreen(
                viewModel = viewModel,
                initialDocument = initialDoc,
                allDocuments = documents,
                onNavigateToScan = {
                    navController.navigate(Screen.Camera.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // PDF to Images Dedicated Workflow (State 1: Selection & Device Import)
        composable(Screen.PdfToImage.route) {
            val documents by viewModel.documentsList.collectAsStateWithLifecycle()
            PdfToImageScreen(
                viewModel = viewModel,
                initialDocument = null,
                allDocuments = documents,
                onNavigateToDocumentDetail = { docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // PDF to Images Dedicated Workflow for specific Document
        composable(
            route = Screen.PdfToImageDoc.route,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            val documents by viewModel.documentsList.collectAsStateWithLifecycle()
            val initialDoc = documents.firstOrNull { it.id == docId }
            PdfToImageScreen(
                viewModel = viewModel,
                initialDocument = initialDoc,
                allDocuments = documents,
                onNavigateToDocumentDetail = { targetDocId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(targetDocId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Advanced Word Reader Screen
        composable(
            route = Screen.WordReader.route,
            arguments = listOf(
                navArgument("filePath") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("uri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("filePath")
            val rawUri = backStackEntry.arguments?.getString("uri")
            val rawTitle = backStackEntry.arguments?.getString("title")

            val filePath = if (!rawPath.isNullOrBlank()) java.net.URLDecoder.decode(rawPath, "UTF-8") else null
            val uri = if (!rawUri.isNullOrBlank()) java.net.URLDecoder.decode(rawUri, "UTF-8") else null
            val title = if (!rawTitle.isNullOrBlank()) java.net.URLDecoder.decode(rawTitle, "UTF-8") else null

            WordReaderScreen(
                initialFilePath = filePath,
                initialUriString = uri,
                initialTitle = title,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Advanced Excel Reader Screen
        composable(
            route = Screen.ExcelReader.route,
            arguments = listOf(
                navArgument("filePath") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("uri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("filePath")
            val rawUri = backStackEntry.arguments?.getString("uri")
            val rawTitle = backStackEntry.arguments?.getString("title")

            val filePath = if (!rawPath.isNullOrBlank()) java.net.URLDecoder.decode(rawPath, "UTF-8") else null
            val uri = if (!rawUri.isNullOrBlank()) java.net.URLDecoder.decode(rawUri, "UTF-8") else null
            val title = if (!rawTitle.isNullOrBlank()) java.net.URLDecoder.decode(rawTitle, "UTF-8") else null

            ExcelReaderScreen(
                initialFilePath = filePath,
                initialUriString = uri,
                initialTitle = title,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
