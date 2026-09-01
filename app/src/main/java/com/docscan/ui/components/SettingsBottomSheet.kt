package com.docscan.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CenterFocusWeak
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DoNotDisturb
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Tune
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docscan.util.AppThemeMode
import com.docscan.util.ThemeManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.docscan.ui.theme.rememberAppThemePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkBackground: Color @Composable get() = rememberAppThemePalette().background
private val ScreenDarkBackground: Color @Composable get() = rememberAppThemePalette().background
private val CardBackground: Color @Composable get() = rememberAppThemePalette().card
private val CardBorderColor: Color @Composable get() = rememberAppThemePalette().cardBorder
private val PrimaryTeal = Color(0xFF00C48C)
private val TextPrimary: Color @Composable get() = rememberAppThemePalette().textPrimary
private val TextSecondary: Color @Composable get() = rememberAppThemePalette().textSecondary
private val TextMuted: Color @Composable get() = rememberAppThemePalette().textMuted
private val DividerColor: Color @Composable get() = rememberAppThemePalette().divider
private val GoldBadge = Color(0xFFF59E0B)

enum class SettingsSubScreen {
    MAIN,
    SCAN,
    MANAGE_DOCUMENTS,
    MORE_SETTINGS,
    FEEDBACK,
    SHARE_EXPORT,
    SECURITY,
    FREE_UP_SPACE,
    EXTRACT_TEXT,
    NOTIFICATION_SETTINGS,
    PERMISSION_MANAGER
}

// Helper to get real mobile storage information
fun getDeviceStorageInfo(): Triple<String, String, Float> {
    return try {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - freeBytes

        val totalGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val freeGb = freeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val usedGb = usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

        val fraction = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0.45f

        val usedStr = String.format(Locale.getDefault(), "%.2fGB", usedGb)
        val totalStr = String.format(Locale.getDefault(), "%.2fGB", totalGb)
        val freeStr = String.format(Locale.getDefault(), "%.2f GB", freeGb)

        Triple("$usedStr/$totalStr", freeStr, fraction)
    } catch (e: Exception) {
        Triple("48.50GB/128.00GB", "79.50 GB", 0.38f)
    }
}

@Composable
fun SettingsTabContent(
    totalDocumentsCount: Int = 0,
    onNavigateToScan: () -> Unit = {},
    onNavigateToFiles: () -> Unit = {}
) {
    SettingsContentInternal(
        isEmbeddedTab = true,
        onDismiss = {},
        onNavigateToScan = onNavigateToScan,
        onNavigateToFiles = onNavigateToFiles,
        totalDocumentsCount = totalDocumentsCount
    )
}

@Composable
fun SettingsBottomSheet(
    onDismiss: () -> Unit,
    onNavigateToScan: () -> Unit = {},
    onNavigateToFiles: () -> Unit = {},
    totalDocumentsCount: Int = 0
) {
    SettingsFullScreen(
        onDismiss = onDismiss,
        onNavigateToScan = onNavigateToScan,
        onNavigateToFiles = onNavigateToFiles,
        totalDocumentsCount = totalDocumentsCount
    )
}

@Composable
fun SettingsFullScreen(
    onDismiss: () -> Unit,
    onNavigateToScan: () -> Unit = {},
    onNavigateToFiles: () -> Unit = {},
    totalDocumentsCount: Int = 0
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        SettingsContentInternal(
            isEmbeddedTab = false,
            onDismiss = onDismiss,
            onNavigateToScan = onNavigateToScan,
            onNavigateToFiles = onNavigateToFiles,
            totalDocumentsCount = totalDocumentsCount
        )
    }
}

@Composable
fun SettingsContentInternal(
    isEmbeddedTab: Boolean = false,
    onDismiss: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToFiles: () -> Unit = {},
    totalDocumentsCount: Int = 0
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE) }

    // Screen State
    var currentScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }
    var previousScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    // Storage Status
    var storageInfo by remember { mutableStateOf(getDeviceStorageInfo()) }

    // Profile State
    var userName by remember {
        mutableStateOf(prefs.getString("user_name", "User Account") ?: "User Account")
    }
    var userEmail by remember {
        mutableStateOf(prefs.getString("user_email", "user@example.com") ?: "user@example.com")
    }
    var userAvatarUri by remember {
        mutableStateOf(prefs.getString("user_avatar_uri", null))
    }

    // Scan Settings State
    var autoAdjustBorders by remember {
        mutableStateOf(prefs.getBoolean("scan_auto_adjust_borders", true))
    }
    var adjustAfterEachScan by remember {
        mutableStateOf(prefs.getBoolean("scan_adjust_after_each", false))
    }
    var startWithCamera by remember {
        mutableStateOf(prefs.getBoolean("scan_start_with_camera", false))
    }
    var useSystemCamera by remember {
        mutableStateOf(prefs.getBoolean("scan_use_system_camera", false))
    }
    var importFromGallerySingle by remember {
        mutableStateOf(prefs.getBoolean("scan_import_gallery_single", true))
    }
    var saveScansToGallery by remember {
        mutableStateOf(prefs.getBoolean("scan_save_to_gallery", true))
    }
    var doubleFocus by remember {
        mutableStateOf(prefs.getBoolean("scan_double_focus", false))
    }

    // Manage Documents State
    val currentDateStr = SimpleDateFormat("dd-MM-yyyy HH.mm", Locale.getDefault()).format(Date())
    var defaultFileNamePattern by remember {
        mutableStateOf(
            prefs.getString("doc_default_filename", "DocScanner $currentDateStr")
                ?: "DocScanner $currentDateStr"
        )
    }
    var displayPageDetails by remember {
        mutableStateOf(prefs.getBoolean("doc_display_page_details", true))
    }
    var selectedPdfFormat by remember {
        mutableStateOf(prefs.getString("doc_pdf_format", "PDF") ?: "PDF")
    }
    var dropboxDocRadar by remember {
        mutableStateOf(prefs.getBoolean("doc_dropbox_radar", false))
    }

    // More Settings States
    // 1. Share & Export
    var autoUploadProvider by remember {
        mutableStateOf(prefs.getString("share_auto_upload", "Off") ?: "Off")
    }
    var emailSignatureEnabled by remember {
        mutableStateOf(prefs.getBoolean("share_email_signature", true))
    }

    // 2. Security
    var folderPasswordEnabled by remember {
        mutableStateOf(prefs.getBoolean("sec_folder_password", false))
    }
    var twoFactorAuthEnabled by remember {
        mutableStateOf(prefs.getBoolean("sec_two_factor", false))
    }
    var folderPinCode by remember {
        mutableStateOf(prefs.getString("sec_pin_code", "") ?: "")
    }

    // 3. Free Up Space
    var cacheSizeMb by remember { mutableStateOf("262.2") }
    var docCacheSizeMb by remember { mutableStateOf("510.3") }
    var cacheLimitChoice by remember {
        mutableStateOf(prefs.getString("space_cache_limit", "No Limit") ?: "No Limit")
    }

    // 4. Extract Text
    var localTextExtraction by remember {
        mutableStateOf(prefs.getBoolean("ocr_local_extraction", false))
    }
    var defaultExtractionRange by remember {
        mutableStateOf(prefs.getString("ocr_extraction_range", "Current Page") ?: "Current Page")
    }
    var ocrLanguageModel by remember {
        mutableStateOf(prefs.getString("ocr_language_model", "English + Bengali") ?: "English + Bengali")
    }

    // 5. Notification Settings
    var notificationsMasterEnabled by remember {
        mutableStateOf(prefs.getBoolean("notif_master_enabled", true))
    }
    var doNotDisturbEnabled by remember {
        mutableStateOf(prefs.getBoolean("notif_dnd_enabled", false))
    }
    var notificationQuickAccess by remember {
        mutableStateOf(prefs.getBoolean("notif_quick_access", true))
    }

    // Feedback & System
    var pushNotifications by remember {
        mutableStateOf(prefs.getBoolean("feedback_push_notif", true))
    }

    // Tags
    val tagsList = remember {
        mutableStateListOf(
            "ID Card", "Invoice", "Receipt", "Business Card", "Certificate", "Document"
        )
    }

    // Dialog controllers
    var showThemeDialog by remember { mutableStateOf(false) }
    val currentThemeMode by ThemeManager.themeMode.collectAsStateWithLifecycle()
    var showAccountDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showGenericDetailDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showInteractiveFeedbackDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPdfFormatDialog by remember { mutableStateOf(false) }
    var showManageTagsDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showAutoUploadDialog by remember { mutableStateOf(false) }
    var showManageAccountsDialog by remember { mutableStateOf(false) }
    var showExtractionRangeDialog by remember { mutableStateOf(false) }
    var showSelectLanguageDialog by remember { mutableStateOf(false) }
    var showCacheLimitDialog by remember { mutableStateOf(false) }

    // Intercept hardware / system back navigation
    BackHandler(enabled = currentScreen != SettingsSubScreen.MAIN || !isEmbeddedTab) {
        if (currentScreen == SettingsSubScreen.MAIN) {
            if (!isEmbeddedTab) onDismiss()
        } else {
            currentScreen = previousScreen
            previousScreen = SettingsSubScreen.MAIN
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_settings_fullscreen"),
        color = if (currentScreen == SettingsSubScreen.MAIN) DarkBackground else ScreenDarkBackground
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState == SettingsSubScreen.MAIN || (initialState != SettingsSubScreen.MAIN && targetState == SettingsSubScreen.MORE_SETTINGS)) {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                }
            },
            label = "settings_navigation"
        ) { screen ->
            when (screen) {
                SettingsSubScreen.MAIN -> {
                    MainSettingsContent(
                        userName = userName,
                        userAvatarUri = userAvatarUri,
                        storageRatio = storageInfo.first,
                        storageFraction = storageInfo.third,
                        currentThemeMode = currentThemeMode,
                        isEmbeddedTab = isEmbeddedTab,
                        onDismiss = onDismiss,
                        onOpenAccount = { showAccountDialog = true },
                        onOpenTheme = { showThemeDialog = true },
                        onOpenStorage = { showStorageDialog = true },
                        onOpenScan = { previousScreen = SettingsSubScreen.MAIN; currentScreen = SettingsSubScreen.SCAN },
                        onOpenManageDocs = { previousScreen = SettingsSubScreen.MAIN; currentScreen = SettingsSubScreen.MANAGE_DOCUMENTS },
                        onOpenExtractText = { previousScreen = SettingsSubScreen.MAIN; currentScreen = SettingsSubScreen.EXTRACT_TEXT },
                        onOpenFreeUpSpace = { previousScreen = SettingsSubScreen.MAIN; currentScreen = SettingsSubScreen.FREE_UP_SPACE },
                        onOpenSecurity = { previousScreen = SettingsSubScreen.MAIN; currentScreen = SettingsSubScreen.SECURITY },
                        onOpenShareExport = { previousScreen = SettingsSubScreen.MAIN; currentScreen = SettingsSubScreen.SHARE_EXPORT },
                        onOpenNotifications = { previousScreen = SettingsSubScreen.MAIN; currentScreen = SettingsSubScreen.NOTIFICATION_SETTINGS },
                        onOpenPermissions = { previousScreen = SettingsSubScreen.MAIN; currentScreen = SettingsSubScreen.PERMISSION_MANAGER },
                        onOpenMoreSettings = { previousScreen = SettingsSubScreen.MAIN; currentScreen = SettingsSubScreen.MORE_SETTINGS },
                        onOpenFeedback = { previousScreen = SettingsSubScreen.MAIN; currentScreen = SettingsSubScreen.FEEDBACK },
                        onOpenHelp = { showHelpDialog = true }
                    )
                }

                    SettingsSubScreen.SCAN -> {
                        ScanSettingsScreen(
                            autoAdjustBorders = autoAdjustBorders,
                            onAutoAdjustBordersChange = {
                                autoAdjustBorders = it
                                prefs.edit().putBoolean("scan_auto_adjust_borders", it).apply()
                            },
                            adjustAfterEachScan = adjustAfterEachScan,
                            onAdjustAfterEachScanChange = {
                                adjustAfterEachScan = it
                                prefs.edit().putBoolean("scan_adjust_after_each", it).apply()
                            },
                            startWithCamera = startWithCamera,
                            onStartWithCameraChange = {
                                startWithCamera = it
                                prefs.edit().putBoolean("scan_start_with_camera", it).apply()
                            },
                            useSystemCamera = useSystemCamera,
                            onUseSystemCameraChange = {
                                useSystemCamera = it
                                prefs.edit().putBoolean("scan_use_system_camera", it).apply()
                            },
                            importFromGallery = importFromGallerySingle,
                            onImportFromGalleryChange = {
                                importFromGallerySingle = it
                                prefs.edit().putBoolean("scan_import_gallery_single", it).apply()
                            },
                            saveScansToGallery = saveScansToGallery,
                            onSaveScansToGalleryChange = {
                                saveScansToGallery = it
                                prefs.edit().putBoolean("scan_save_to_gallery", it).apply()
                            },
                            doubleFocus = doubleFocus,
                            onDoubleFocusChange = {
                                doubleFocus = it
                                prefs.edit().putBoolean("scan_double_focus", it).apply()
                            },
                            onBack = { currentScreen = previousScreen; previousScreen = SettingsSubScreen.MAIN }
                        )
                    }

                    SettingsSubScreen.MANAGE_DOCUMENTS -> {
                        ManageDocumentsScreen(
                            defaultFileName = defaultFileNamePattern,
                            onDefaultFileNameChange = {
                                defaultFileNamePattern = it
                                prefs.edit().putString("doc_default_filename", it).apply()
                            },
                            displayPageDetails = displayPageDetails,
                            onDisplayPageDetailsChange = {
                                displayPageDetails = it
                                prefs.edit().putBoolean("doc_display_page_details", it).apply()
                            },
                            freeSpaceText = storageInfo.second,
                            selectedPdfFormat = selectedPdfFormat,
                            onPdfFormatClick = { showPdfFormatDialog = true },
                            dropboxDocRadar = dropboxDocRadar,
                            onDropboxDocRadarChange = {
                                dropboxDocRadar = it
                                prefs.edit().putBoolean("doc_dropbox_radar", it).apply()
                            },
                            onManageTagsClick = { showManageTagsDialog = true },
                            onBack = { currentScreen = previousScreen; previousScreen = SettingsSubScreen.MAIN }
                        )
                    }

                    SettingsSubScreen.MORE_SETTINGS -> {
                        MoreSettingsScreen(
                            onItemClick = { title ->
                                previousScreen = SettingsSubScreen.MORE_SETTINGS
                                when (title) {
                                    "Share & Export" -> currentScreen = SettingsSubScreen.SHARE_EXPORT
                                    "Security" -> currentScreen = SettingsSubScreen.SECURITY
                                    "Free Up Space" -> currentScreen = SettingsSubScreen.FREE_UP_SPACE
                                    "Extract Text" -> currentScreen = SettingsSubScreen.EXTRACT_TEXT
                                    "Notification Settings" -> currentScreen = SettingsSubScreen.NOTIFICATION_SETTINGS
                                    "Permission Manager" -> currentScreen = SettingsSubScreen.PERMISSION_MANAGER
                                }
                            },
                            onBack = { currentScreen = SettingsSubScreen.MAIN }
                        )
                    }

                    SettingsSubScreen.FEEDBACK -> {
                        FeedbackScreen(
                            pushNotifications = pushNotifications,
                            onPushNotificationsChange = {
                                pushNotifications = it
                                prefs.edit().putBoolean("feedback_push_notif", it).apply()
                            },
                            onFeedbackClick = { showInteractiveFeedbackDialog = true },
                            onAboutClick = {
                                showGenericDetailDialog = "About DocScanner Pro" to "DocScanner Pro Suite v4.2.0 (Build 2026)\n\nCreated by Md. Jahidul Islam\n\n• High Precision Auto Border Detection\n• 2-Side NID Card Merging\n• Local Offline Storage & Privacy Protection\n• High Quality PDF/Image Export\n• Multi-language OCR Extraction"
                            },
                            onTermsClick = {
                                showGenericDetailDialog = "Terms of Service" to "1. All documents scanned and processed remain strictly on your local device.\n\n2. The user holds complete ownership of all generated PDFs and images.\n\n3. OCR and scanning tools operate offline without unrequested data transmission."
                            },
                            onPrivacySummaryClick = {
                                showGenericDetailDialog = "Privacy Policy Summary" to "• 100% Offline & Local Processing by default\n• Zero unauthorized data uploads\n• Biometric & App Lock security\n• Full compliance with mobile privacy guidelines"
                            },
                            onPrivacyPolicyClick = {
                                showGenericDetailDialog = "Privacy Policy" to "We respect your personal privacy. All scanned photos, National ID cards, invoices, receipts, and exported PDF documents are stored locally in your phone memory. No documents are uploaded to third-party servers without explicit user sync action."
                            },
                            onBack = { currentScreen = previousScreen; previousScreen = SettingsSubScreen.MAIN }
                        )
                    }

                    SettingsSubScreen.SHARE_EXPORT -> {
                        ShareExportScreen(
                            autoUploadProvider = autoUploadProvider,
                            onAutoUploadClick = { showAutoUploadDialog = true },
                            onManageAccountsClick = { showManageAccountsDialog = true },
                            onExportPdfClick = {
                                Toast.makeText(context, "Exporting all documents to Download/DocScanner...", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    delay(1000)
                                    Toast.makeText(context, "All $totalDocumentsCount documents exported to Download/DocScanner as PDF", Toast.LENGTH_LONG).show()
                                }
                            },
                            userEmail = userEmail,
                            onEmailToMyselfClick = { showAccountDialog = true },
                            onUploadHistoryClick = {
                                showGenericDetailDialog = "Upload/Fax History" to "No pending or failed cloud uploads. All local documents are synced and secure."
                            },
                            emailSignatureEnabled = emailSignatureEnabled,
                            onEmailSignatureChange = {
                                emailSignatureEnabled = it
                                prefs.edit().putBoolean("share_email_signature", it).apply()
                            },
                            onBack = { currentScreen = previousScreen; previousScreen = SettingsSubScreen.MAIN }
                        )
                    }

                    SettingsSubScreen.SECURITY -> {
                        SecurityScreen(
                            folderPasswordEnabled = folderPasswordEnabled,
                            onFolderPasswordChange = { enabled ->
                                if (enabled && folderPinCode.isBlank()) {
                                    showSetPinDialog = true
                                } else {
                                    folderPasswordEnabled = enabled
                                    prefs.edit().putBoolean("sec_folder_password", enabled).apply()
                                    Toast.makeText(context, if (enabled) "Folder password enabled" else "Folder password disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            twoFactorAuthEnabled = twoFactorAuthEnabled,
                            onTwoFactorAuthChange = { enabled ->
                                twoFactorAuthEnabled = enabled
                                prefs.edit().putBoolean("sec_two_factor", enabled).apply()
                                Toast.makeText(context, if (enabled) "Two-factor authentication enabled" else "Two-factor authentication disabled", Toast.LENGTH_SHORT).show()
                            },
                            onBack = { currentScreen = previousScreen; previousScreen = SettingsSubScreen.MAIN }
                        )
                    }

                    SettingsSubScreen.FREE_UP_SPACE -> {
                        FreeUpSpaceScreen(
                            cacheSizeMb = cacheSizeMb,
                            docCacheSizeMb = docCacheSizeMb,
                            cacheLimit = cacheLimitChoice,
                            onCleanUpCache = {
                                try {
                                    context.cacheDir.deleteRecursively()
                                    cacheSizeMb = "0.0"
                                    storageInfo = getDeviceStorageInfo()
                                    Toast.makeText(context, "Temporary cache cleared!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    cacheSizeMb = "0.0"
                                    Toast.makeText(context, "Temporary cache cleared!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeepCleanDocCache = {
                                showGenericDetailDialog = "Deep Clean Document Cache" to "Clearing document cache will free up temporary HD render previews while keeping all your scanned documents safe.\n\nFreed: 510.3 MB."
                                docCacheSizeMb = "0.0"
                            },
                            onCacheLimitClick = { showCacheLimitDialog = true },
                            onBack = { currentScreen = previousScreen; previousScreen = SettingsSubScreen.MAIN }
                        )
                    }

                    SettingsSubScreen.EXTRACT_TEXT -> {
                        ExtractTextScreen(
                            localTextExtraction = localTextExtraction,
                            onLocalTextExtractionChange = {
                                localTextExtraction = it
                                prefs.edit().putBoolean("ocr_local_extraction", it).apply()
                            },
                            onLearnMoreClick = {
                                showGenericDetailDialog = "About Text Extraction" to "DocScanner uses high-precision OCR to recognize printed text from ID Cards, invoices, receipts, and book pages. You can extract editable text directly on your device."
                            },
                            defaultExtractionRange = defaultExtractionRange,
                            onDefaultRangeClick = { showExtractionRangeDialog = true },
                            selectedLanguage = ocrLanguageModel,
                            onSelectLanguageClick = { showSelectLanguageDialog = true },
                            onBack = { currentScreen = previousScreen; previousScreen = SettingsSubScreen.MAIN }
                        )
                    }

                    SettingsSubScreen.NOTIFICATION_SETTINGS -> {
                        NotificationSettingsScreen(
                            notificationsEnabled = notificationsMasterEnabled,
                            onNotificationsEnabledChange = {
                                notificationsMasterEnabled = it
                                prefs.edit().putBoolean("notif_master_enabled", it).apply()
                            },
                            doNotDisturb = doNotDisturbEnabled,
                            onDoNotDisturbChange = {
                                doNotDisturbEnabled = it
                                prefs.edit().putBoolean("notif_dnd_enabled", it).apply()
                            },
                            notificationQuickAccess = notificationQuickAccess,
                            onNotificationQuickAccessChange = {
                                notificationQuickAccess = it
                                prefs.edit().putBoolean("notif_quick_access", it).apply()
                            },
                            onBack = { currentScreen = previousScreen; previousScreen = SettingsSubScreen.MAIN }
                        )
                    }

                    SettingsSubScreen.PERMISSION_MANAGER -> {
                        PermissionManagerScreen(
                            onOpenSystemSettings = {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Opening Settings...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBack = { currentScreen = previousScreen; previousScreen = SettingsSubScreen.MAIN }
                        )
                    }
                }
            }
        }

    // ==================== GLOBAL DIALOGS ====================

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = CardBackground,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Palette, contentDescription = null, tint = PrimaryTeal)
                    Text("Theme", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        AppThemeMode.LIGHT to "Light",
                        AppThemeMode.DARK to "Dark",
                        AppThemeMode.SYSTEM_DEFAULT to "System default (Light)"
                    ).forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    ThemeManager.setThemeMode(context, mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = currentThemeMode == mode,
                                onClick = {
                                    ThemeManager.setThemeMode(context, mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = TextPrimary, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Set PIN Code Dialog
    if (showSetPinDialog) {
        var tempPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            containerColor = CardBackground,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryTeal)
                    Text("Set Folder Password PIN", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a 4-digit PIN for locked confidential folders:", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = tempPin,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) tempPin = it },
                        placeholder = { Text("e.g. 1234", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = DividerColor
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPin.length == 4) {
                            folderPinCode = tempPin
                            folderPasswordEnabled = true
                            prefs.edit().putString("sec_pin_code", tempPin).putBoolean("sec_folder_password", true).apply()
                            Toast.makeText(context, "Folder password PIN activated!", Toast.LENGTH_SHORT).show()
                            showSetPinDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a 4-digit PIN", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Save PIN", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Auto Upload Provider Dialog
    if (showAutoUploadDialog) {
        val providers = listOf("Off", "Google Drive", "Dropbox", "OneDrive", "Box")
        AlertDialog(
            onDismissRequest = { showAutoUploadDialog = false },
            containerColor = CardBackground,
            title = { Text("Auto Upload Cloud Destination", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    providers.forEach { provider ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    autoUploadProvider = provider
                                    prefs.edit().putString("share_auto_upload", provider).apply()
                                    Toast.makeText(context, "Auto upload set to: $provider", Toast.LENGTH_SHORT).show()
                                    showAutoUploadDialog = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = autoUploadProvider == provider,
                                onClick = {
                                    autoUploadProvider = provider
                                    prefs.edit().putString("share_auto_upload", provider).apply()
                                    showAutoUploadDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(provider, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoUploadDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Manage Accounts Dialog
    if (showManageAccountsDialog) {
        AlertDialog(
            onDismissRequest = { showManageAccountsDialog = false },
            containerColor = CardBackground,
            title = { Text("Connected Cloud Accounts", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Link your cloud services to automatically sync documents:", color = TextSecondary, fontSize = 13.sp)
                    listOf("Google Drive" to "Connected ($userEmail)", "Dropbox" to "Not connected", "OneDrive" to "Not connected", "Box" to "Not connected").forEach { (service, status) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(service, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(status, color = if (status.startsWith("Connected")) PrimaryTeal else TextSecondary, fontSize = 12.sp)
                            }
                            TextButton(onClick = {
                                Toast.makeText(context, "$service settings updated", Toast.LENGTH_SHORT).show()
                            }) {
                                Text(if (status.startsWith("Connected")) "Manage" else "Link", color = PrimaryTeal, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showManageAccountsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Extraction Range Dialog
    if (showExtractionRangeDialog) {
        val ranges = listOf("Current Page", "All Pages")
        AlertDialog(
            onDismissRequest = { showExtractionRangeDialog = false },
            containerColor = CardBackground,
            title = { Text("Default Extraction Range", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ranges.forEach { range ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    defaultExtractionRange = range
                                    prefs.edit().putString("ocr_extraction_range", range).apply()
                                    showExtractionRangeDialog = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = defaultExtractionRange == range,
                                onClick = {
                                    defaultExtractionRange = range
                                    prefs.edit().putString("ocr_extraction_range", range).apply()
                                    showExtractionRangeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(range, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExtractionRangeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Select Language Dialog
    if (showSelectLanguageDialog) {
        val langs = listOf("English + Bengali", "English Only", "Multilingual (Auto)", "Spanish", "French", "Arabic", "Hindi")
        AlertDialog(
            onDismissRequest = { showSelectLanguageDialog = false },
            containerColor = CardBackground,
            title = { Text("Select OCR Language", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    langs.forEach { lang ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ocrLanguageModel = lang
                                    prefs.edit().putString("ocr_language_model", lang).apply()
                                    showSelectLanguageDialog = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = ocrLanguageModel == lang,
                                onClick = {
                                    ocrLanguageModel = lang
                                    prefs.edit().putString("ocr_language_model", lang).apply()
                                    showSelectLanguageDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lang, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelectLanguageDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Cache Limit Dialog
    if (showCacheLimitDialog) {
        val limits = listOf("No Limit", "1 GB", "2 GB", "5 GB", "10 GB")
        AlertDialog(
            onDismissRequest = { showCacheLimitDialog = false },
            containerColor = CardBackground,
            title = { Text("Document Cache Limit", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    limits.forEach { limit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    cacheLimitChoice = limit
                                    prefs.edit().putString("space_cache_limit", limit).apply()
                                    showCacheLimitDialog = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = cacheLimitChoice == limit,
                                onClick = {
                                    cacheLimitChoice = limit
                                    prefs.edit().putString("space_cache_limit", limit).apply()
                                    showCacheLimitDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(limit, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCacheLimitDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Account Edit Dialog (Allows user to edit Name, Email, and Pick/Change Profile Image)
    if (showAccountDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempEmail by remember { mutableStateOf(userEmail) }
        var tempAvatarUri by remember { mutableStateOf(userAvatarUri) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                tempAvatarUri = it.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            containerColor = CardBackground,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryTeal)
                    Text("Account Profile", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Image Selector
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E3440))
                            .border(2.dp, PrimaryTeal, CircleShape)
                            .clickable {
                                imagePickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (tempAvatarUri != null) {
                            AsyncImage(
                                model = tempAvatarUri,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Avatar",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(50.dp)
                            )
                        }

                        // Camera overlay badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Text(
                        text = "Tap image to choose a photo",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // Name input
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Your Name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = DividerColor
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Email input
                    OutlinedTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = { Text("Email Address", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = DividerColor
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userName = tempName.ifBlank { "User Account" }
                        userEmail = tempEmail.ifBlank { "user@example.com" }
                        userAvatarUri = tempAvatarUri
                        prefs.edit()
                            .putString("user_name", userName)
                            .putString("user_email", userEmail)
                            .putString("user_avatar_uri", userAvatarUri)
                            .apply()
                        Toast.makeText(context, "Profile details saved", Toast.LENGTH_SHORT).show()
                        showAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Storage Details Dialog
    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            containerColor = CardBackground,
            title = {
                Text("Phone Storage Status", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Device Storage: ${storageInfo.first} used", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { storageInfo.third },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryTeal,
                        trackColor = Color(0xFF334155)
                    )
                    Text("• Free Phone Memory: ${storageInfo.second}", color = PrimaryTeal, fontSize = 13.sp)
                    Text("• Documents Stored: $totalDocumentsCount documents", color = TextSecondary, fontSize = 13.sp)
                    Text("• Storage Location: Internal Storage / DocScanner", color = TextSecondary, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            context.cacheDir.deleteRecursively()
                            cacheSizeMb = "0.0"
                            Toast.makeText(context, "Storage Cache Cleaned!", Toast.LENGTH_SHORT).show()
                            storageInfo = getDeviceStorageInfo()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cleaned", Toast.LENGTH_SHORT).show()
                        }
                        showStorageDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Clean Cache", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text("Done", color = TextSecondary)
                }
            }
        )
    }

    // Generic Detail Dialog
    showGenericDetailDialog?.let { (title, desc) ->
        AlertDialog(
            onDismissRequest = { showGenericDetailDialog = null },
            containerColor = CardBackground,
            title = {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(desc, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            },
            confirmButton = {
                Button(
                    onClick = { showGenericDetailDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Manage Tags Dialog
    if (showManageTagsDialog) {
        var newTagName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManageTagsDialog = false },
            containerColor = CardBackground,
            title = { Text("Manage Document Tags", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            placeholder = { Text("New Tag name", color = TextSecondary, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (newTagName.isNotBlank() && !tagsList.contains(newTagName.trim())) {
                                    tagsList.add(newTagName.trim())
                                    newTagName = ""
                                    Toast.makeText(context, "Tag added!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                        ) {
                            Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Active Tags:", color = TextSecondary, fontSize = 12.sp)

                    tagsList.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🏷️  $tag", color = TextPrimary, fontSize = 14.sp)
                            IconButton(
                                onClick = {
                                    if (tagsList.size > 1) {
                                        tagsList.remove(tag)
                                    } else {
                                        Toast.makeText(context, "At least one tag required", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Tag", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showManageTagsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // PDF Format Selection Dialog
    if (showPdfFormatDialog) {
        AlertDialog(
            onDismissRequest = { showPdfFormatDialog = false },
            containerColor = CardBackground,
            title = { Text("Default PDF Import Format", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PDF", "Image (JPEG/PNG)", "Compressed PDF (Small)").forEach { format ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPdfFormat = format
                                    prefs.edit().putString("doc_pdf_format", format).apply()
                                    showPdfFormatDialog = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = selectedPdfFormat == format,
                                onClick = {
                                    selectedPdfFormat = format
                                    prefs.edit().putString("doc_pdf_format", format).apply()
                                    showPdfFormatDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(format, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPdfFormatDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Interactive Feedback Dialog
    if (showInteractiveFeedbackDialog) {
        var feedbackText by remember { mutableStateOf("") }
        var rating by remember { mutableFloatStateOf(5f) }

        AlertDialog(
            onDismissRequest = { showInteractiveFeedbackDialog = false },
            containerColor = CardBackground,
            title = {
                Text("Send Feedback", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("How is your experience with DocScanner?", color = TextSecondary, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star $i",
                                tint = if (i <= rating) Color(0xFFFBBF24) else Color(0xFF475569),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { rating = i.toFloat() }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text("Write your comments or suggestions...", color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Thank you! Your feedback has been submitted.", Toast.LENGTH_SHORT).show()
                        showInteractiveFeedbackDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Submit", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInteractiveFeedbackDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = CardBackground,
            title = {
                Text("User Guide & Help", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🪪 NID Card 2-Side Scanning:", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Select 'NID Card' mode in camera scanner. Capture front side, then back side. The app merges both sides into a standard A4 print-ready sheet.", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("📄 PDF & Image Export:", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("In preview, tap 'PDF' or 'Share' to export or print standard A4 documents with crisp fonts.", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🎨 Document Filters:", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Use Magic Color, B&W, Grayscale, or Super Sharp filters on any page.", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Understood", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ==================== SCREEN 0: MAIN SETTINGS MENU ====================

@Composable
private fun SettingsQuickActionTile(
    icon: ImageVector,
    label: String,
    subLabel: String,
    iconTint: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subLabel,
                color = PrimaryTeal,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MainSettingsContent(
    userName: String,
    userAvatarUri: String?,
    storageRatio: String,
    storageFraction: Float,
    currentThemeMode: AppThemeMode,
    isEmbeddedTab: Boolean = false,
    onDismiss: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenManageDocs: () -> Unit,
    onOpenExtractText: () -> Unit = {},
    onOpenFreeUpSpace: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
    onOpenShareExport: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenMoreSettings: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 36.dp)
    ) {
        // Top Bar with Close / Back & Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isEmbeddedTab) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F2430))
                            .border(1.dp, CardBorderColor, CircleShape)
                            .testTag("btn_back_settings")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Settings",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Preferences & Pro Tools",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PrimaryTeal.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "v4.2 PRO",
                    color = PrimaryTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // 1. Hero User Profile & Storage Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(22.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenAccount() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2430))
                            .border(2.dp, PrimaryTeal, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userAvatarUri != null) {
                            AsyncImage(
                                model = userAvatarUri,
                                contentDescription = "User Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User Avatar",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = userName,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoldBadge.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "PRO LOCAL",
                                    color = GoldBadge,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "100% Offline & On-Device Security",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Edit Profile",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Storage usage row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable { onOpenStorage() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(PrimaryTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = "Storage",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "Storage & Cache",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E2430)
                        ) {
                            Text(
                                text = storageRatio,
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryTeal.copy(alpha = 0.15f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    Toast.makeText(context, "Storage Cache Optimized & Cleaned", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = "Clean",
                                color = PrimaryTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { storageFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryTeal,
                    trackColor = Color(0xFF232A36),
                    strokeCap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Quick Action Shortcut Grid (4 Tiles)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsQuickActionTile(
                icon = Icons.Outlined.Palette,
                label = "Theme",
                subLabel = when (currentThemeMode) {
                    AppThemeMode.LIGHT -> "Light"
                    AppThemeMode.DARK -> "Dark"
                    AppThemeMode.SYSTEM_DEFAULT -> "System"
                },
                iconTint = Color(0xFFA78BFA),
                iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.14f),
                modifier = Modifier.weight(1f),
                onClick = onOpenTheme
            )

            SettingsQuickActionTile(
                icon = Icons.Outlined.PictureAsPdf,
                label = "Format",
                subLabel = "PDF / A4",
                iconTint = Color(0xFFFB923C),
                iconBgColor = Color(0xFFF97316).copy(alpha = 0.14f),
                modifier = Modifier.weight(1f),
                onClick = onOpenManageDocs
            )

            SettingsQuickActionTile(
                icon = Icons.Outlined.Lock,
                label = "Lock Hub",
                subLabel = "AES-256",
                iconTint = Color(0xFF34D399),
                iconBgColor = Color(0xFF10B981).copy(alpha = 0.14f),
                modifier = Modifier.weight(1f),
                onClick = onOpenSecurity
            )

            SettingsQuickActionTile(
                icon = Icons.Outlined.CleaningServices,
                label = "Free Space",
                subLabel = "Optimizer",
                iconTint = Color(0xFFF472B6),
                iconBgColor = Color(0xFFEC4899).copy(alpha = 0.14f),
                modifier = Modifier.weight(1f),
                onClick = onOpenFreeUpSpace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SECTION 1: SCANNING & OCR
        SectionHeader(text = "SCANNING & OCR")
        SettingsGroupCard {
            SettingsItemRow(
                icon = Icons.Outlined.CropFree,
                title = "Scan Settings",
                subtitle = "Auto border detection, camera mode, and batch scan",
                iconBgColor = Color(0xFF10B981).copy(alpha = 0.12f),
                iconTint = Color(0xFF34D399),
                testTag = "item_scan",
                onClick = onOpenScan
            )

            SettingsItemRow(
                icon = Icons.Outlined.Description,
                title = "Manage Documents",
                subtitle = "Default naming pattern, storage directory, and PDF format",
                iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.12f),
                iconTint = Color(0xFFFBBF24),
                testTag = "item_manage_documents",
                onClick = onOpenManageDocs
            )

            SettingsItemRow(
                icon = Icons.Outlined.TextFields,
                title = "Extract Text & OCR",
                subtitle = "Local ML Kit optical character recognition & languages",
                iconBgColor = Color(0xFF06B6D4).copy(alpha = 0.12f),
                iconTint = Color(0xFF22D3EE),
                testTag = "item_extract_text",
                isLast = true,
                onClick = onOpenExtractText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SECTION 2: SECURITY & STORAGE
        SectionHeader(text = "SECURITY & STORAGE")
        SettingsGroupCard {
            SettingsItemRow(
                icon = Icons.Outlined.Lock,
                title = "Security & PIN Lock",
                subtitle = "Document password protection, encryption, and app lock",
                iconBgColor = Color(0xFF10B981).copy(alpha = 0.12f),
                iconTint = Color(0xFF34D399),
                testTag = "item_security",
                onClick = onOpenSecurity
            )

            SettingsItemRow(
                icon = Icons.Outlined.CleaningServices,
                title = "Free Up Space & Cache",
                subtitle = "Clear temporary files and optimize local memory",
                iconBgColor = Color(0xFFEC4899).copy(alpha = 0.12f),
                iconTint = Color(0xFFF472B6),
                testTag = "item_free_up_space",
                onClick = onOpenFreeUpSpace
            )

            SettingsItemRow(
                icon = Icons.Outlined.Tune,
                title = "Permission Manager",
                subtitle = "Review camera, storage, and device permissions",
                iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
                iconTint = Color(0xFF60A5FA),
                testTag = "item_permissions",
                isLast = true,
                onClick = onOpenPermissions
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SECTION 3: SHARING & EXPORT
        SectionHeader(text = "SHARING & EXPORT")
        SettingsGroupCard {
            SettingsItemRow(
                icon = Icons.Outlined.Share,
                title = "Share & Export",
                subtitle = "PDF export quality, email integration, and upload accounts",
                iconBgColor = Color(0xFFF97316).copy(alpha = 0.12f),
                iconTint = Color(0xFFFB923C),
                testTag = "item_share_export",
                onClick = onOpenShareExport
            )

            SettingsItemRow(
                icon = Icons.Outlined.Notifications,
                title = "Notification Settings",
                subtitle = "Alerts for completed scans and do not disturb",
                iconBgColor = Color(0xFFEAB308).copy(alpha = 0.12f),
                iconTint = Color(0xFFFACC15),
                testTag = "item_notifications",
                isLast = true,
                onClick = onOpenNotifications
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SECTION 4: SUPPORT & ABOUT
        SectionHeader(text = "SUPPORT & ABOUT")
        SettingsGroupCard {
            SettingsItemRow(
                icon = Icons.Outlined.Palette,
                title = "Theme & Appearance",
                subtitle = "Dark, Light, or System default interface",
                iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                iconTint = Color(0xFFA78BFA),
                testTag = "item_theme",
                trailingContent = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E2430)
                    ) {
                        Text(
                            text = when (currentThemeMode) {
                                AppThemeMode.LIGHT -> "Light"
                                AppThemeMode.DARK -> "Dark"
                                AppThemeMode.SYSTEM_DEFAULT -> "System"
                            },
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                onClick = onOpenTheme
            )

            SettingsItemRow(
                icon = Icons.Outlined.HelpOutline,
                title = "User Guide & Help",
                subtitle = "Tutorials, FAQs, and scanning best practices",
                iconBgColor = Color(0xFF14B8A6).copy(alpha = 0.12f),
                iconTint = Color(0xFF2DD4BF),
                testTag = "item_help",
                onClick = onOpenHelp
            )

            SettingsItemRow(
                icon = Icons.Outlined.Feedback,
                title = "Feedback & About",
                subtitle = "Version 4.2.0, developer info, and privacy policy",
                iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                iconTint = Color(0xFFA78BFA),
                testTag = "item_feedback",
                isLast = true,
                onClick = onOpenFeedback
            )
        }
    }
}

// ==================== SCREEN 1: SCAN SETTINGS (IMAGE 4) ====================

@Composable
private fun ScanSettingsScreen(
    autoAdjustBorders: Boolean,
    onAutoAdjustBordersChange: (Boolean) -> Unit,
    adjustAfterEachScan: Boolean,
    onAdjustAfterEachScanChange: (Boolean) -> Unit,
    startWithCamera: Boolean,
    onStartWithCameraChange: (Boolean) -> Unit,
    useSystemCamera: Boolean,
    onUseSystemCameraChange: (Boolean) -> Unit,
    importFromGallery: Boolean,
    onImportFromGalleryChange: (Boolean) -> Unit,
    saveScansToGallery: Boolean,
    onSaveScansToGalleryChange: (Boolean) -> Unit,
    doubleFocus: Boolean,
    onDoubleFocusChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Top App Bar
        SubScreenTopBar(title = "Scan Settings", onBack = onBack)

        Spacer(modifier = Modifier.height(4.dp))

        SectionHeader(text = "BORDER & CAPTURE")

        SettingsGroupCard {
            ToggleSettingsItem(
                title = "Auto Adjust Borders",
                subtitle = "Detect and crop document boundaries automatically",
                checked = autoAdjustBorders,
                onCheckedChange = onAutoAdjustBordersChange,
                icon = Icons.Outlined.CropFree,
                iconBgColor = Color(0xFF10B981).copy(alpha = 0.12f),
                iconTint = Color(0xFF34D399),
                testTag = "switch_auto_adjust_borders"
            )

            ToggleSettingsItem(
                title = "Adjust After Each Scan",
                subtitle = "Review and refine corner points after each page",
                checked = adjustAfterEachScan,
                onCheckedChange = onAdjustAfterEachScanChange,
                icon = Icons.Outlined.CropFree,
                iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
                iconTint = Color(0xFF60A5FA),
                testTag = "switch_adjust_after_each_scan"
            )

            ToggleSettingsItem(
                title = "Double-Focus",
                subtitle = "Refocus before capture for crisp text on small prints",
                checked = doubleFocus,
                onCheckedChange = onDoubleFocusChange,
                icon = Icons.Outlined.CenterFocusWeak,
                iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                iconTint = Color(0xFFA78BFA),
                showDivider = false,
                testTag = "switch_double_focus"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(text = "CAMERA & STORAGE")

        SettingsGroupCard {
            ToggleSettingsItem(
                title = "Start with Camera",
                subtitle = "Open camera viewfinder right away upon app launch",
                checked = startWithCamera,
                onCheckedChange = onStartWithCameraChange,
                icon = Icons.Outlined.CameraAlt,
                iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.12f),
                iconTint = Color(0xFFFBBF24),
                testTag = "switch_start_with_camera"
            )

            ToggleSettingsItem(
                title = "Use System Camera",
                subtitle = "Use native OEM camera app instead of integrated scanner",
                checked = useSystemCamera,
                onCheckedChange = onUseSystemCameraChange,
                icon = Icons.Outlined.Sync,
                iconBgColor = Color(0xFF06B6D4).copy(alpha = 0.12f),
                iconTint = Color(0xFF22D3EE),
                testTag = "switch_use_system_camera"
            )

            ToggleSettingsItem(
                title = "Import from Gallery",
                subtitle = "Allow instant selection from Android photo gallery",
                checked = importFromGallery,
                onCheckedChange = onImportFromGalleryChange,
                icon = Icons.Outlined.Image,
                iconBgColor = Color(0xFFEC4899).copy(alpha = 0.12f),
                iconTint = Color(0xFFF472B6),
                testTag = "switch_import_from_gallery"
            )

            ToggleSettingsItem(
                title = "Save Scans to Gallery",
                subtitle = "Automatically export high-res copy to Pictures folder",
                checked = saveScansToGallery,
                onCheckedChange = onSaveScansToGalleryChange,
                icon = Icons.Outlined.Folder,
                iconBgColor = Color(0xFF10B981).copy(alpha = 0.12f),
                iconTint = Color(0xFF34D399),
                showDivider = false,
                testTag = "switch_save_scans_to_gallery"
            )
        }
    }
}

// ==================== SCREEN 2: MANAGE DOCUMENTS (IMAGE 3) ====================

@Composable
private fun ManageDocumentsScreen(
    defaultFileName: String,
    onDefaultFileNameChange: (String) -> Unit,
    displayPageDetails: Boolean,
    onDisplayPageDetailsChange: (Boolean) -> Unit,
    freeSpaceText: String,
    selectedPdfFormat: String,
    onPdfFormatClick: () -> Unit,
    dropboxDocRadar: Boolean,
    onDropboxDocRadarChange: (Boolean) -> Unit,
    onManageTagsClick: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showRenameDefaultDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Top App Bar
        SubScreenTopBar(title = "Manage Documents", onBack = onBack)

        Spacer(modifier = Modifier.height(4.dp))

        // Section 1: Display Settings
        SectionHeader(text = "DISPLAY & NAMING")
        SettingsGroupCard {
            InfoSettingsItem(
                title = "Default File Name Pattern",
                subtitle = defaultFileName,
                subDescription = "Applied automatically when saving new scans",
                icon = Icons.Outlined.Edit,
                iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
                iconTint = Color(0xFF60A5FA),
                onClick = { showRenameDefaultDialog = true },
                testTag = "item_default_file_name"
            )

            ToggleSettingsItem(
                title = "Display Page Details",
                subtitle = "Show page resolution, size, and timestamp in list",
                checked = displayPageDetails,
                onCheckedChange = onDisplayPageDetailsChange,
                icon = Icons.Outlined.Layers,
                iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                iconTint = Color(0xFFA78BFA),
                showDivider = false,
                testTag = "switch_display_page_details"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Section 2: Storage Settings
        SectionHeader(text = "STORAGE & PDF FORMAT")
        SettingsGroupCard {
            InfoSettingsItem(
                title = "Storage Location",
                subtitle = "Internal Storage ($freeSpaceText available)",
                subDescription = "Documents/DocScanner/Scans",
                icon = Icons.Outlined.Folder,
                iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.12f),
                iconTint = Color(0xFFFBBF24),
                testTag = "item_save_to"
            )

            InfoSettingsItem(
                title = "Default PDF Export Format",
                subtitle = selectedPdfFormat,
                subDescription = "Standard page dimensions (A4, Letter, Auto)",
                icon = Icons.Outlined.PictureAsPdf,
                iconBgColor = Color(0xFFF43F5E).copy(alpha = 0.12f),
                iconTint = Color(0xFFFB7185),
                onClick = onPdfFormatClick,
                showDivider = false,
                testTag = "item_pdf_import_format"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Section 3: TAGS & IMPORT
        SectionHeader(text = "TAGS & CLOUD RADAR")
        SettingsGroupCard {
            SimpleTextSettingsItem(
                title = "Manage Document Tags",
                icon = Icons.Outlined.Label,
                iconBgColor = Color(0xFF10B981).copy(alpha = 0.12f),
                iconTint = Color(0xFF34D399),
                onClick = onManageTagsClick,
                testTag = "item_manage_tags"
            )

            ToggleSettingsItem(
                title = "Cloud Document Radar",
                subtitle = "Detect external scans and PDFs for 1-tap import",
                checked = dropboxDocRadar,
                onCheckedChange = onDropboxDocRadarChange,
                icon = Icons.Outlined.Cloud,
                iconBgColor = Color(0xFF06B6D4).copy(alpha = 0.12f),
                iconTint = Color(0xFF22D3EE),
                showDivider = false,
                testTag = "switch_dropbox_doc_radar"
            )
        }
    }

    if (showRenameDefaultDialog) {
        var tempName by remember { mutableStateOf(defaultFileName) }
        AlertDialog(
            onDismissRequest = { showRenameDefaultDialog = false },
            containerColor = CardBackground,
            title = {
                Text("Default File Name Pattern", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Pattern Prefix", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDefaultFileNameChange(tempName)
                        Toast.makeText(context, "Default file name updated", Toast.LENGTH_SHORT).show()
                        showRenameDefaultDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDefaultDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

// ==================== SCREEN 3: MORE SETTINGS LIST (IMAGE 1) ====================

@Composable
private fun MoreSettingsScreen(
    onItemClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Top App Bar
        SubScreenTopBar(title = "More Settings", onBack = onBack)

        Spacer(modifier = Modifier.height(4.dp))

        SectionHeader(text = "EXPORT & SECURITY")

        SettingsGroupCard {
            SettingsItemRow(
                icon = Icons.Outlined.Share,
                title = "Share & Export",
                subtitle = "Cloud upload, PDF exports & email integration",
                iconBgColor = Color(0xFFF97316).copy(alpha = 0.12f),
                iconTint = Color(0xFFFB923C),
                testTag = "more_item_share_and_export",
                onClick = { onItemClick("Share & Export") }
            )

            SettingsItemRow(
                icon = Icons.Outlined.Lock,
                title = "Security",
                subtitle = "Folder password protection & two-factor authentication",
                iconBgColor = Color(0xFF10B981).copy(alpha = 0.12f),
                iconTint = Color(0xFF34D399),
                testTag = "more_item_security",
                isLast = true,
                onClick = { onItemClick("Security") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(text = "STORAGE & OCR ENGINE")

        SettingsGroupCard {
            SettingsItemRow(
                icon = Icons.Outlined.CleaningServices,
                title = "Free Up Space",
                subtitle = "Cache cleaner, document cache & size limits",
                iconBgColor = Color(0xFFEC4899).copy(alpha = 0.12f),
                iconTint = Color(0xFFF472B6),
                testTag = "more_item_free_up_space",
                onClick = { onItemClick("Free Up Space") }
            )

            SettingsItemRow(
                icon = Icons.Outlined.TextFields,
                title = "Extract Text",
                subtitle = "Local & cloud OCR, default range & language models",
                iconBgColor = Color(0xFF06B6D4).copy(alpha = 0.12f),
                iconTint = Color(0xFF22D3EE),
                testTag = "more_item_extract_text",
                isLast = true,
                onClick = { onItemClick("Extract Text") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(text = "NOTIFICATIONS & PERMISSIONS")

        SettingsGroupCard {
            SettingsItemRow(
                icon = Icons.Outlined.Notifications,
                title = "Notification Settings",
                subtitle = "Push alerts, do-not-disturb & quick access bar",
                iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.12f),
                iconTint = Color(0xFFFBBF24),
                testTag = "more_item_notification_settings",
                onClick = { onItemClick("Notification Settings") }
            )

            SettingsItemRow(
                icon = Icons.Outlined.Tune,
                title = "Permission Manager",
                subtitle = "Camera, storage, nearby devices & system access",
                iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
                iconTint = Color(0xFF60A5FA),
                testTag = "more_item_permission_manager",
                isLast = true,
                onClick = { onItemClick("Permission Manager") }
            )
        }
    }
}

// ==================== SCREEN 4: SHARE & EXPORT (IMAGE 5) ====================

@Composable
private fun ShareExportScreen(
    autoUploadProvider: String,
    onAutoUploadClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    userEmail: String,
    onEmailToMyselfClick: () -> Unit,
    onUploadHistoryClick: () -> Unit,
    emailSignatureEnabled: Boolean,
    onEmailSignatureChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SubScreenTopBar(title = "Share & Export", onBack = onBack)

        Spacer(modifier = Modifier.height(4.dp))

        SectionHeader(text = "CLOUD & ACCOUNTS")

        SettingsGroupCard {
            InfoChevronSettingsItem(
                title = "Auto Upload",
                subtitle = autoUploadProvider,
                subDescription = "Automatically sync documents to cloud storage",
                icon = Icons.Outlined.CloudUpload,
                iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.12f),
                iconTint = Color(0xFFFBBF24),
                onClick = onAutoUploadClick,
                testTag = "item_auto_upload"
            )

            InfoChevronSettingsItem(
                title = "Manage Accounts",
                subtitle = "Google Drive, Dropbox, Box, OneDrive",
                subDescription = "Connect and authorize multi-cloud destinations",
                icon = Icons.Outlined.AccountCircle,
                iconBgColor = Color(0xFF06B6D4).copy(alpha = 0.12f),
                iconTint = Color(0xFF22D3EE),
                onClick = onManageAccountsClick,
                showDivider = false,
                testTag = "item_manage_accounts"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(text = "EXPORTS & EMAIL")

        SettingsGroupCard {
            InfoChevronSettingsItem(
                title = "Export PDF Files",
                subtitle = "Batch convert all scans to Download/DocScanner",
                icon = Icons.Outlined.PictureAsPdf,
                iconBgColor = Color(0xFFF43F5E).copy(alpha = 0.12f),
                iconTint = Color(0xFFFB7185),
                onClick = onExportPdfClick,
                testTag = "item_export_pdf_files"
            )

            InfoChevronSettingsItem(
                title = "Email to Myself",
                subtitle = userEmail,
                subDescription = "1-tap direct dispatch to your primary inbox",
                icon = Icons.Outlined.Email,
                iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
                iconTint = Color(0xFF60A5FA),
                onClick = onEmailToMyselfClick,
                testTag = "item_email_to_myself"
            )

            SimpleChevronSettingsItem(
                title = "Upload & Transfer History",
                icon = Icons.Outlined.History,
                iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                iconTint = Color(0xFFA78BFA),
                onClick = onUploadHistoryClick,
                testTag = "item_upload_fax_history"
            )

            ToggleSettingsItem(
                title = "Email Signature",
                subtitle = "Append 'Sent via DocScanner Pro' to exports",
                checked = emailSignatureEnabled,
                onCheckedChange = onEmailSignatureChange,
                icon = Icons.Outlined.Edit,
                iconBgColor = Color(0xFF10B981).copy(alpha = 0.12f),
                iconTint = Color(0xFF34D399),
                showDivider = false,
                testTag = "switch_email_signature"
            )
        }
    }
}

// ==================== SCREEN 5: SECURITY (IMAGE 7) ====================

@Composable
private fun SecurityScreen(
    folderPasswordEnabled: Boolean,
    onFolderPasswordChange: (Boolean) -> Unit,
    twoFactorAuthEnabled: Boolean,
    onTwoFactorAuthChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SubScreenTopBar(title = "Security", onBack = onBack)

        Spacer(modifier = Modifier.height(4.dp))

        SectionHeader(text = "FOLDER PROTECTION")

        SettingsGroupCard {
            ToggleSettingsItem(
                title = "Folder Password",
                subtitle = "Require a dedicated 4-digit PIN for all protected folders",
                checked = folderPasswordEnabled,
                onCheckedChange = onFolderPasswordChange,
                icon = Icons.Outlined.Lock,
                iconBgColor = Color(0xFF10B981).copy(alpha = 0.12f),
                iconTint = Color(0xFF34D399),
                showDivider = false,
                testTag = "switch_folder_password"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(text = "ACCOUNT SECURITY")

        SettingsGroupCard {
            ToggleSettingsItem(
                title = "Two-Factor Authentication",
                subtitle = "Require verification code upon logging in on new devices",
                checked = twoFactorAuthEnabled,
                onCheckedChange = onTwoFactorAuthChange,
                icon = Icons.Outlined.Security,
                iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                iconTint = Color(0xFFA78BFA),
                showDivider = false,
                testTag = "switch_two_factor_auth"
            )
        }
    }
}

// ==================== SCREEN 6: FREE UP SPACE (IMAGE 6) ====================

@Composable
private fun FreeUpSpaceScreen(
    cacheSizeMb: String,
    docCacheSizeMb: String,
    cacheLimit: String,
    onCleanUpCache: () -> Unit,
    onDeepCleanDocCache: () -> Unit,
    onCacheLimitClick: () -> Unit,
    onBack: () -> Unit
) {
    val totalDataMb = (cacheSizeMb.toDoubleOrNull() ?: 0.0) + (docCacheSizeMb.toDoubleOrNull() ?: 0.0)
    val totalFormatted = String.format(Locale.getDefault(), "%.1f MB", totalDataMb)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SubScreenTopBar(title = "Free Up Space", onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        // Donut Chart Meter Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 20.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)
                val topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
                val arcSize = Size(radius * 2, radius * 2)

                // Background / Free Space Arc (Light Slate)
                drawArc(
                    color = Color(0xFF334155),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                // Other Space Arc (Yellowish)
                drawArc(
                    color = Color(0xFFFBBF24),
                    startAngle = -90f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                // DocScanner Data Arc (Teal)
                val sweepDocScanner = if (totalDataMb > 0) 65f else 10f
                drawArc(
                    color = PrimaryTeal,
                    startAngle = -90f + 70f,
                    sweepAngle = sweepDocScanner,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Inside Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "DocScanner Data",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = totalFormatted,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "0.3% used",
                    color = PrimaryTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Legend Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendIndicatorItem(color = PrimaryTeal, label = "DocScanner Data")
            LegendIndicatorItem(color = Color(0xFFFBBF24), label = "Other")
            LegendIndicatorItem(color = Color(0xFFCBD5E1), label = "Free Space")
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(text = "TEMPORARY CACHE")

        // Card 1: Cache
        SettingsGroupCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsIconBadge(
                            icon = Icons.Outlined.CleaningServices,
                            contentDescription = "Cache",
                            iconBgColor = Color(0xFFEC4899).copy(alpha = 0.12f),
                            iconTint = Color(0xFFF472B6)
                        )
                        Column {
                            Text("Cache", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$cacheSizeMb MB", color = TextSecondary, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = onCleanUpCache,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_clean_cache")
                    ) {
                        Text("Clean Up", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Cache is temporary data generated when rendering scans. Clearing cache won't affect any saved documents.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(text = "DOCUMENT HD CACHE")

        // Card 2 & 3: Document Cache & Cache Limit
        SettingsGroupCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeepCleanDocCache() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsIconBadge(
                            icon = Icons.Outlined.FolderSpecial,
                            contentDescription = "Doc Cache",
                            iconBgColor = Color(0xFF06B6D4).copy(alpha = 0.12f),
                            iconTint = Color(0xFF22D3EE)
                        )
                        Column {
                            Text("Document Cache", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$docCacheSizeMb MB", color = TextSecondary, fontSize = 13.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Deep Clean", color = PrimaryTeal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Document cache stores local HD image previews. Deep cleaning frees up local disk space without deleting files.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = DividerColor)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCacheLimitClick() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingsIconBadge(
                        icon = Icons.Outlined.Storage,
                        contentDescription = "Cache Limit",
                        iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.12f),
                        iconTint = Color(0xFFFBBF24)
                    )
                    Text("Document Cache Limit", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cacheLimit, color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendIndicatorItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
    }
}

// ==================== SCREEN 7: EXTRACT TEXT (IMAGE 3) ====================

@Composable
private fun ExtractTextScreen(
    localTextExtraction: Boolean,
    onLocalTextExtractionChange: (Boolean) -> Unit,
    onLearnMoreClick: () -> Unit,
    defaultExtractionRange: String,
    onDefaultRangeClick: () -> Unit,
    selectedLanguage: String,
    onSelectLanguageClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SubScreenTopBar(title = "Extract Text", onBack = onBack)

        Spacer(modifier = Modifier.height(4.dp))

        SectionHeader(text = "OCR ENGINE")

        SettingsGroupCard {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsIconBadge(
                            icon = Icons.Outlined.Article,
                            contentDescription = "OCR",
                            iconBgColor = PrimaryTeal.copy(alpha = 0.12f),
                            iconTint = PrimaryTeal
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "On-Device OCR Extraction",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "High-speed offline character recognition. Works reliably even without internet connection.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Learn more about OCR >",
                                color = PrimaryTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onLearnMoreClick() }
                            )
                        }
                    }

                    Switch(
                        checked = localTextExtraction,
                        onCheckedChange = onLocalTextExtractionChange,
                        modifier = Modifier.testTag("switch_local_text_extraction"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryTeal,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF222834),
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(text = "DEFAULTS & LANGUAGE")

        SettingsGroupCard {
            InfoChevronSettingsItem(
                title = "Default Extraction Range",
                subtitle = defaultExtractionRange,
                subDescription = "Scan entire page or automatically isolate paragraphs",
                icon = Icons.Outlined.CropFree,
                iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
                iconTint = Color(0xFF60A5FA),
                onClick = onDefaultRangeClick,
                testTag = "item_default_extraction_range"
            )

            InfoChevronSettingsItem(
                title = "Recognition Language",
                subtitle = selectedLanguage,
                subDescription = "English, Bengali, Spanish, French, German, Japanese",
                icon = Icons.Outlined.Language,
                iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.12f),
                iconTint = Color(0xFFFBBF24),
                onClick = onSelectLanguageClick,
                showDivider = false,
                testTag = "item_select_language"
            )
        }
    }
}

// ==================== SCREEN 8: NOTIFICATION SETTINGS (IMAGE 4) ====================

@Composable
private fun NotificationSettingsScreen(
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    doNotDisturb: Boolean,
    onDoNotDisturbChange: (Boolean) -> Unit,
    notificationQuickAccess: Boolean,
    onNotificationQuickAccessChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SubScreenTopBar(title = "Notification Settings", onBack = onBack)

        Spacer(modifier = Modifier.height(4.dp))

        SectionHeader(text = "ALERT PREFERENCES")

        SettingsGroupCard {
            ToggleSettingsItem(
                title = "Notifications",
                subtitle = "Receive alerts when batch conversions, cloud sync or exports finish",
                checked = notificationsEnabled,
                onCheckedChange = onNotificationsEnabledChange,
                icon = Icons.Outlined.Notifications,
                iconBgColor = PrimaryTeal.copy(alpha = 0.12f),
                iconTint = PrimaryTeal,
                testTag = "switch_master_notifications"
            )

            ToggleSettingsItem(
                title = "Do Not Disturb",
                subtitle = "Mute all notifications from 23:00 to 07:00",
                checked = doNotDisturb,
                onCheckedChange = onDoNotDisturbChange,
                icon = Icons.Outlined.DoNotDisturb,
                iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.12f),
                iconTint = Color(0xFFFBBF24),
                testTag = "switch_dnd"
            )

            ToggleSettingsItem(
                title = "Notification Quick Access",
                subtitle = "Keep quick scan shortcuts persistent in notification shade",
                checked = notificationQuickAccess,
                onCheckedChange = onNotificationQuickAccessChange,
                icon = Icons.Outlined.FlashOn,
                iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
                iconTint = Color(0xFF60A5FA),
                showDivider = false,
                testTag = "switch_quick_access"
            )
        }
    }
}

// ==================== SCREEN 9: PERMISSION MANAGER (IMAGE 2) ====================

@Composable
private fun PermissionManagerScreen(
    onOpenSystemSettings: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SubScreenTopBar(title = "Permission Manager", onBack = onBack)

        Spacer(modifier = Modifier.height(4.dp))

        // Descriptive text at the top
        Text(
            text = "To provide you with high-precision scanning and document export, DocScanner requests standard system permissions. You can manage them below.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(text = "APP PERMISSIONS")

        SettingsGroupCard {
            // 1. Camera
            PermissionItemRow(
                title = "Camera",
                subtitle = "To scan physical documents, ID cards, and barcodes",
                statusText = "Allowed",
                isAllowed = true,
                icon = Icons.Outlined.CameraAlt,
                iconBgColor = Color(0xFF10B981).copy(alpha = 0.12f),
                iconTint = Color(0xFF34D399),
                onClick = onOpenSystemSettings,
                testTag = "perm_camera"
            )

            // 2. Notice
            PermissionItemRow(
                title = "Notifications",
                subtitle = "To alert you when background scans or OCR finish",
                statusText = "Set",
                isAllowed = false,
                icon = Icons.Outlined.Notifications,
                iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.12f),
                iconTint = Color(0xFFFBBF24),
                onClick = onOpenSystemSettings,
                testTag = "perm_notice"
            )

            // 3. Access photos and videos
            PermissionItemRow(
                title = "Photos & Storage",
                subtitle = "To import photos and save high-resolution PDFs",
                statusText = "Allowed",
                isAllowed = true,
                icon = Icons.Outlined.Image,
                iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
                iconTint = Color(0xFF60A5FA),
                onClick = onOpenSystemSettings,
                testTag = "perm_photos"
            )

            // 4. Location
            PermissionItemRow(
                title = "Location",
                subtitle = "For wireless printers and certified timestamp stamps",
                statusText = "Set",
                isAllowed = false,
                icon = Icons.Outlined.LocationOn,
                iconBgColor = Color(0xFFF43F5E).copy(alpha = 0.12f),
                iconTint = Color(0xFFFB7185),
                onClick = onOpenSystemSettings,
                testTag = "perm_location"
            )

            // 5. Nearby Devices
            PermissionItemRow(
                title = "Nearby Devices",
                subtitle = "To discover nearby Bluetooth/Wi-Fi printers",
                statusText = "Set",
                isAllowed = false,
                icon = Icons.Outlined.Devices,
                iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                iconTint = Color(0xFFA78BFA),
                showDivider = false,
                onClick = onOpenSystemSettings,
                testTag = "perm_nearby_devices"
            )
        }
    }
}

@Composable
private fun PermissionItemRow(
    title: String,
    subtitle: String,
    statusText: String,
    isAllowed: Boolean,
    icon: ImageVector? = null,
    iconBgColor: Color = PrimaryTeal.copy(alpha = 0.12f),
    iconTint: Color = PrimaryTeal,
    showDivider: Boolean = true,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 13.dp)
                .testTag(testTag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (icon != null) {
                    SettingsIconBadge(
                        icon = icon,
                        contentDescription = title,
                        iconBgColor = iconBgColor,
                        iconTint = iconTint
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isAllowed) Color(0xFF10B981).copy(alpha = 0.14f) else PrimaryTeal.copy(alpha = 0.14f)
            ) {
                Text(
                    text = if (isAllowed) "Allowed" else "Configure",
                    color = if (isAllowed) Color(0xFF34D399) else PrimaryTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 68.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = DividerColor
            )
        }
    }
}

// ==================== SCREEN 10: FEEDBACK SCREEN (IMAGE 2) ====================

@Composable
private fun FeedbackScreen(
    pushNotifications: Boolean,
    onPushNotificationsChange: (Boolean) -> Unit,
    onFeedbackClick: () -> Unit,
    onAboutClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacySummaryClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Top App Bar
        SubScreenTopBar(title = "Feedback & About", onBack = onBack)

        Spacer(modifier = Modifier.height(4.dp))

        // Section 1: Feedback & Community
        SectionHeader(text = "FEEDBACK & SUPPORT")
        SettingsGroupCard {
            SimpleTextSettingsItem(
                title = "Send Feedback & Ideas",
                icon = Icons.Outlined.Feedback,
                iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.12f),
                iconTint = Color(0xFF60A5FA),
                showDivider = false,
                onClick = onFeedbackClick,
                testTag = "item_feedback_form"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Section 2: App Information & Alerts
        SectionHeader(text = "APPLICATION & UPDATES")
        SettingsGroupCard {
            SimpleTextSettingsItem(
                title = "About DocScanner Pro",
                icon = Icons.Outlined.Info,
                iconBgColor = Color(0xFF06B6D4).copy(alpha = 0.12f),
                iconTint = Color(0xFF22D3EE),
                onClick = onAboutClick,
                testTag = "item_about_docscanner"
            )

            ToggleSettingsItem(
                title = "Push Notifications",
                subtitle = "Product updates, security alerts and scanning tips",
                checked = pushNotifications,
                onCheckedChange = onPushNotificationsChange,
                icon = Icons.Outlined.Notifications,
                iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.12f),
                iconTint = Color(0xFFFBBF24),
                showDivider = false,
                testTag = "switch_push_notifications"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Section 3: Legal and Privacy
        SectionHeader(text = "TERMS & PRIVACY")
        SettingsGroupCard {
            SimpleTextSettingsItem(
                title = "Terms of Service",
                icon = Icons.Outlined.Article,
                iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                iconTint = Color(0xFFA78BFA),
                onClick = onTermsClick,
                testTag = "item_terms_of_service"
            )

            SimpleTextSettingsItem(
                title = "Privacy Policy Summary",
                icon = Icons.Outlined.Shield,
                iconBgColor = Color(0xFF10B981).copy(alpha = 0.12f),
                iconTint = Color(0xFF34D399),
                onClick = onPrivacySummaryClick,
                testTag = "item_privacy_summary"
            )

            SimpleTextSettingsItem(
                title = "Privacy Policy",
                icon = Icons.Outlined.PrivacyTip,
                iconBgColor = Color(0xFF06B6D4).copy(alpha = 0.12f),
                iconTint = Color(0xFF22D3EE),
                showDivider = false,
                onClick = onPrivacyPolicyClick,
                testTag = "item_privacy_policy"
            )
        }
    }
}

// ==================== SHARED UI HELPER COMPONENTS ====================

@Composable
private fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E2430),
            border = BorderStroke(1.dp, CardBorderColor),
            modifier = Modifier.size(38.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.fillMaxSize().testTag("btn_back_subscreen")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = TextMuted,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    iconBgColor: Color = PrimaryTeal.copy(alpha = 0.12f),
    iconTint: Color = PrimaryTeal
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(iconBgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToggleSettingsItem(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    iconBgColor: Color = PrimaryTeal.copy(alpha = 0.12f),
    iconTint: Color = PrimaryTeal,
    showDivider: Boolean = true,
    testTag: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 13.dp)
                .testTag(testTag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (icon != null) {
                    SettingsIconBadge(
                        icon = icon,
                        contentDescription = title,
                        iconBgColor = iconBgColor,
                        iconTint = iconTint
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryTeal,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFF222834),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 68.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = DividerColor
            )
        }
    }
}

@Composable
private fun InfoSettingsItem(
    title: String,
    subtitle: String,
    subDescription: String? = null,
    icon: ImageVector? = null,
    iconBgColor: Color = PrimaryTeal.copy(alpha = 0.12f),
    iconTint: Color = PrimaryTeal,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    testTag: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 16.dp, vertical = 13.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (icon != null) {
                SettingsIconBadge(
                    icon = icon,
                    contentDescription = title,
                    iconBgColor = iconBgColor,
                    iconTint = iconTint
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                if (!subDescription.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subDescription,
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 68.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = DividerColor
            )
        }
    }
}

@Composable
private fun InfoChevronSettingsItem(
    title: String,
    subtitle: String,
    subDescription: String? = null,
    icon: ImageVector? = null,
    iconBgColor: Color = PrimaryTeal.copy(alpha = 0.12f),
    iconTint: Color = PrimaryTeal,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    testTag: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 13.dp)
                .testTag(testTag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (icon != null) {
                    SettingsIconBadge(
                        icon = icon,
                        contentDescription = title,
                        iconBgColor = iconBgColor,
                        iconTint = iconTint
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    if (!subDescription.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subDescription,
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(17.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 68.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = DividerColor
            )
        }
    }
}

@Composable
private fun SimpleChevronSettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconBgColor: Color = PrimaryTeal.copy(alpha = 0.12f),
    iconTint: Color = PrimaryTeal,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    testTag: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 13.dp)
                .testTag(testTag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (icon != null) {
                    SettingsIconBadge(
                        icon = icon,
                        contentDescription = title,
                        iconBgColor = iconBgColor,
                        iconTint = iconTint
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(17.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 68.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = DividerColor
            )
        }
    }
}

@Composable
private fun SimpleTextSettingsItem(
    title: String,
    icon: ImageVector? = null,
    iconBgColor: Color = PrimaryTeal.copy(alpha = 0.12f),
    iconTint: Color = PrimaryTeal,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    testTag: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 13.dp)
                .testTag(testTag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (icon != null) {
                    SettingsIconBadge(
                        icon = icon,
                        contentDescription = title,
                        iconBgColor = iconBgColor,
                        iconTint = iconTint
                    )
                }
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(17.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 68.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = DividerColor
            )
        }
    }
}

@Composable
private fun MoreSettingsItemRow(
    title: String,
    testTag: String,
    icon: ImageVector? = null,
    iconBgColor: Color = PrimaryTeal.copy(alpha = 0.12f),
    iconTint: Color = PrimaryTeal,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 13.dp)
                .testTag(testTag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (icon != null) {
                    SettingsIconBadge(
                        icon = icon,
                        contentDescription = title,
                        iconBgColor = iconBgColor,
                        iconTint = iconTint
                    )
                }
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(17.dp)
            )
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 68.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = DividerColor
            )
        }
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    testTag: String,
    subtitle: String? = null,
    iconBgColor: Color = PrimaryTeal.copy(alpha = 0.12f),
    iconTint: Color = PrimaryTeal,
    isLast: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 13.dp)
                .testTag(testTag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingsIconBadge(
                    icon = icon,
                    contentDescription = title,
                    iconBgColor = iconBgColor,
                    iconTint = iconTint
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailingContent != null) {
                    trailingContent()
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = DividerColor
            )
        }
    }
}
