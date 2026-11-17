package com.apkmint.creator.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apkmint.creator.engine.ApkBuildConfig
import com.apkmint.creator.engine.ApkBuilderEngine
import com.apkmint.creator.engine.BuiltApkInfo
import com.apkmint.creator.engine.SourceMode
import com.apkmint.creator.engine.WebIconFetcher
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PACKAGE_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

private val COLOR_SWATCHES = listOf(
    "#1565C0", "#0D9488", "#7C3AED", "#DC2626",
    "#EA580C", "#16A34A", "#111827", "#FFFFFF"
)

@Composable
fun CreateApkScreen(
    builtApks: List<BuiltApkInfo>,
    selectedMode: SourceMode?,
    pendingEditConfig: ApkBuildConfig? = null,
    previewTrigger: Int,
    modifier: Modifier = Modifier,
    onSelectedModeChange: (SourceMode?) -> Unit,
    onConsumePendingEdit: () -> Unit = {},
    onOpenDeveloperInfo: () -> Unit = {},
    onApkBuilt: (BuiltApkInfo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) }

    var config by remember { mutableStateOf(ApkBuildConfig()) }
    var isPackageManuallyEdited by remember { mutableStateOf(false) }
    var versionCodeText by remember { mutableStateOf(config.versionCode.toString()) }
    var iconPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFetchingIcon by remember { mutableStateOf(false) }
    var importedZipName by remember { mutableStateOf<String?>(null) }

    var showLivePreview by remember { mutableStateOf(false) }
    var showTextIconDialog by remember { mutableStateOf(false) }
    var isBuilding by remember { mutableStateOf(false) }
    var buildStep by remember { mutableStateOf("") }
    var buildProgress by remember { mutableFloatStateOf(0f) }
    var builtResult by remember { mutableStateOf<BuiltApkInfo?>(null) }
    var buildError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingEditConfig) {
        if (pendingEditConfig != null) {
            config = pendingEditConfig
            isPackageManuallyEdited = true
            versionCodeText = pendingEditConfig.versionCode.toString()
            iconPreviewBitmap = pendingEditConfig.iconUri?.let { uri ->
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
            }
            importedZipName = null
            activeTab = 0
            onConsumePendingEdit()
        }
    }

    LaunchedEffect(previewTrigger) {
        if (previewTrigger > 0) {
            showLivePreview = true
        }
    }

    BackHandler(enabled = selectedMode != null) {
        onSelectedModeChange(null)
        activeTab = 0
    }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            config = config.copy(iconUri = uri)
            iconPreviewBitmap = runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
    }

    val htmlFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    config = config.copy(htmlCode = text, htmlZipUri = null)
                    importedZipName = null
                    Toast.makeText(context, "HTML file loaded", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "Failed to read HTML file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val zipFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = queryDisplayName(context, uri) ?: "website.zip"
            config = config.copy(htmlZipUri = uri)
            importedZipName = fileName
            Toast.makeText(context, "Imported $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    val isPackageValid = PACKAGE_REGEX.matches(config.packageName.trim())

    if (selectedMode == null) {
        ModeSelectionHome(
            builtApks = builtApks,
            modifier = modifier,
            onSelectMode = { mode ->
                config = config.copy(sourceMode = mode)
                onSelectedModeChange(mode)
                activeTab = 0
            },
            onSelectTemplate = { tpl ->
                config = config.copy(
                    sourceMode = SourceMode.HTML,
                    htmlCode = tpl.html,
                    htmlZipUri = null,
                    appName = tpl.defaultAppName,
                    packageName = tpl.defaultPackage
                )
                importedZipName = null
                onSelectedModeChange(SourceMode.HTML)
                activeTab = 0
            },
            onOpenDeveloperInfo = onOpenDeveloperInfo
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {

            // 4-Tab Builder Navigation
            val tabs = listOf(
                "Source" to if (config.sourceMode == SourceMode.URL) Icons.Default.Language else Icons.Default.Code,
                "Identity" to Icons.Default.Badge,
                "Design" to Icons.Default.Palette,
                "Features" to Icons.Default.Tune
            )

            PrimaryTabRow(selectedTabIndex = activeTab) {
                tabs.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title, maxLines = 1) },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            // Tab Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (activeTab) {
                    0 -> SourceTabContent(
                        config = config,
                        importedZipName = importedZipName,
                        onConfigChange = { config = it },
                        onImportHtml = { htmlFileLauncher.launch("text/html") },
                        onImportZip = { zipFileLauncher.launch("application/zip") },
                        onClearZip = {
                            importedZipName = null
                            config = config.copy(htmlZipUri = null)
                        },
                        onPreviewClick = { showLivePreview = true }
                    )
                    1 -> IdentityTabContent(
                        config = config,
                        versionCodeText = versionCodeText,
                        iconPreviewBitmap = iconPreviewBitmap,
                        isFetchingIcon = isFetchingIcon,
                        isPackageValid = isPackageValid,
                        onPickIcon = { iconPickerLauncher.launch("image/*") },
                        onOpenTextIcon = { showTextIconDialog = true },
                        onFetchWebIcon = {
                            isFetchingIcon = true
                            scope.launch {
                                val res = WebIconFetcher.fetchWebsiteIcon(context, config.url)
                                isFetchingIcon = false
                                res.onSuccess { (uri, bmp) ->
                                    config = config.copy(iconUri = uri)
                                    iconPreviewBitmap = bmp
                                    Toast.makeText(context, "Website icon fetched!", Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    Toast.makeText(
                                        context,
                                        "Failed to fetch icon: ${err.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onAppNameChange = { newName ->
                            config = if (!isPackageManuallyEdited) {
                                config.copy(appName = newName, packageName = generatePackageFromName(newName))
                            } else {
                                config.copy(appName = newName)
                            }
                        },
                        onPackageNameChange = { newPkg ->
                            isPackageManuallyEdited = true
                            config = config.copy(packageName = newPkg.lowercase().replace(" ", ""))
                        },
                        onResetAutoPackage = {
                            isPackageManuallyEdited = false
                            config = config.copy(packageName = generatePackageFromName(config.appName))
                        },
                        onConfigChange = { config = it },
                        onVersionCodeChange = { raw ->
                            val digits = raw.filter { it.isDigit() }
                            versionCodeText = digits
                            config = config.copy(versionCode = digits.toIntOrNull()?.coerceAtLeast(1) ?: 1)
                        }
                    )
                    2 -> DesignTabContent(
                        config = config,
                        iconBitmap = iconPreviewBitmap,
                        onConfigChange = { config = it }
                    )
                    3 -> FeaturesTabContent(
                        config = config,
                        onConfigChange = { config = it }
                    )
                }
            }

            // Sticky Bottom Bar: Next + Build APK
            HorizontalDivider()
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeTab < tabs.lastIndex) {
                        OutlinedButton(
                            onClick = { activeTab++ },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Button(
                        onClick = {
                            if (config.appName.isBlank()) {
                                Toast.makeText(context, "Please enter an App Name", Toast.LENGTH_SHORT).show()
                                activeTab = 1
                                return@Button
                            }
                            if (!isPackageValid) {
                                Toast.makeText(context, "Invalid Package Name", Toast.LENGTH_SHORT).show()
                                activeTab = 1
                                return@Button
                            }
                            if (config.sourceMode == SourceMode.URL && config.url.isBlank()) {
                                Toast.makeText(context, "Please enter a Website URL", Toast.LENGTH_SHORT).show()
                                activeTab = 0
                                return@Button
                            }
                            isBuilding = true
                            buildStep = "Starting..."
                            buildProgress = 0.05f
                            scope.launch {
                                val result = ApkBuilderEngine.buildApk(context, config) { step, frac ->
                                    buildStep = step
                                    buildProgress = frac
                                }
                                isBuilding = false
                                result.onSuccess { info ->
                                    builtResult = info
                                    onApkBuilt(info)
                                }.onFailure { err ->
                                    buildError = err.message ?: "Unknown build error"
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Build Signed APK", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showLivePreview) {
        LivePreviewDialog(
            config = config,
            onDismiss = { showLivePreview = false }
        )
    }

    if (showTextIconDialog) {
        TextIconGeneratorDialog(
            initialAppName = config.appName,
            onDismiss = { showTextIconDialog = false },
            onApplyIcon = { uri, bmp ->
                config = config.copy(iconUri = uri)
                iconPreviewBitmap = bmp
                showTextIconDialog = false
            }
        )
    }

    if (isBuilding) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Building APK...") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(buildStep, style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { buildProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {}
        )
    }

    builtResult?.let { info ->
        AlertDialog(
            onDismissRequest = { builtResult = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A)) },
            title = { Text("APK Built Successfully!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("App: ${info.appName}", fontWeight = FontWeight.SemiBold)
                    Text("Package: ${info.packageName}", style = MaterialTheme.typography.bodySmall)
                    Text("File: ${info.fileName}", style = MaterialTheme.typography.bodySmall)
                    Text("Size: ${formatSize(info.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Saved to Downloads/ApkMint/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    ApkIntentHelper.installApk(context, info.filePath)
                }) {
                    Text("Install APK")
                }
            },
            dismissButton = {
                Row {
                    IconButton(onClick = {
                        ApkIntentHelper.shareApk(context, info.filePath, info.appName)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    TextButton(onClick = { builtResult = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    buildError?.let { msg ->
        AlertDialog(
            onDismissRequest = { buildError = null },
            title = { Text("Build Failed") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { buildError = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ModeSelectionHome(
    builtApks: List<BuiltApkInfo>,
    modifier: Modifier = Modifier,
    onSelectMode: (SourceMode) -> Unit,
    onSelectTemplate: (StarterTemplate) -> Unit,
    onOpenDeveloperInfo: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gradient Hero Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0F766E), Color(0xFF0D9488), Color(0xFF10B981))
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                text = "ON-DEVICE APK BUILDER",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        Text(
                            text = "${builtApks.size} Built",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Build Android APKs\nin Seconds",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Turn any website link or HTML/CSS/JS project into a signed, installable Android app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        ModeOptionCard(
            title = "Website to APK",
            description = "Convert any live website link into a fast Android app with dynamic status bar & auto icon fetch.",
            badge = "ONLINE / WEB LINK",
            icon = Icons.Default.Language,
            accentColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = { onSelectMode(SourceMode.URL) }
        )

        ModeOptionCard(
            title = "HTML to APK",
            description = "Paste HTML code or import an .html / .zip project (HTML + CSS + JS) to build an offline app.",
            badge = "OFFLINE / HTML & ZIP",
            icon = Icons.Default.Code,
            accentColor = MaterialTheme.colorScheme.tertiary,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            onClick = { onSelectMode(SourceMode.HTML) }
        )

        // Quick Starter HTML Templates
        Text(
            text = "Quick HTML Templates",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HtmlStarterTemplates.all.forEach { tpl ->
                ElevatedCard(
                    onClick = { onSelectTemplate(tpl) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(148.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = tpl.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = tpl.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Recent Builds Shortcut
        if (builtApks.isNotEmpty()) {
            Text(
                text = "Recent Builds",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            builtApks.take(2).forEach { apk ->
                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(apk.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${apk.packageName} • ${formatSize(apk.sizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { ApkIntentHelper.installApk(context, apk.filePath) }) {
                            Icon(Icons.Default.InstallMobile, contentDescription = "Install", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { ApkIntentHelper.shareApk(context, apk.filePath, apk.appName) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            }
        }

        DeveloperCreditCard(onOpenDialog = onOpenDeveloperInfo)

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    description: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SourceTabContent(
    config: ApkBuildConfig,
    importedZipName: String?,
    onConfigChange: (ApkBuildConfig) -> Unit,
    onImportHtml: () -> Unit,
    onImportZip: () -> Unit,
    onClearZip: () -> Unit,
    onPreviewClick: () -> Unit
) {
    if (config.sourceMode == SourceMode.URL) {
        SectionCard(title = "Website URL") {
            OutlinedTextField(
                value = config.url,
                onValueChange = { onConfigChange(config.copy(url = it)) },
                label = { Text("Website URL") },
                placeholder = { Text("https://yourwebsite.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = onPreviewClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Test Website in Live Preview")
            }
            Text(
                text = "Tip: Switch to the Identity tab to automatically fetch the website's icon.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        SectionCard(title = "HTML Source / File") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onImportHtml,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import .html")
                }
                OutlinedButton(
                    onClick = onImportZip,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import .zip")
                }
            }

            if (importedZipName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ZIP: $importedZipName (must contain index.html)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearZip) {
                        Text("Clear")
                    }
                }
            } else {
                OutlinedTextField(
                    value = config.htmlCode,
                    onValueChange = { onConfigChange(config.copy(htmlCode = it)) },
                    label = { Text("HTML Source Code") },
                    minLines = 8,
                    maxLines = 14,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { onConfigChange(config.copy(htmlCode = ApkBuildConfig.DEFAULT_HTML_TEMPLATE)) }
                    ) {
                        Text("Reset Sample")
                    }
                    TextButton(
                        onClick = { onConfigChange(config.copy(htmlCode = "")) }
                    ) {
                        Text("Clear Code")
                    }
                    Spacer(Modifier.weight(1f))
                }
                OutlinedButton(onClick = onPreviewClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Test HTML in Live Preview")
                }
            }
        }
    }
}

@Composable
private fun IdentityTabContent(
    config: ApkBuildConfig,
    versionCodeText: String,
    iconPreviewBitmap: Bitmap?,
    isFetchingIcon: Boolean,
    isPackageValid: Boolean,
    onPickIcon: () -> Unit,
    onOpenTextIcon: () -> Unit,
    onFetchWebIcon: () -> Unit,
    onAppNameChange: (String) -> Unit,
    onPackageNameChange: (String) -> Unit,
    onResetAutoPackage: () -> Unit,
    onConfigChange: (ApkBuildConfig) -> Unit,
    onVersionCodeChange: (String) -> Unit
) {
    SectionCard(title = "App Icon & Identity") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable(onClick = onPickIcon),
                contentAlignment = Alignment.Center
            ) {
                if (iconPreviewBitmap != null) {
                    Image(
                        bitmap = iconPreviewBitmap.asImageBitmap(),
                        contentDescription = "App Icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Pick Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("App Icon", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (iconPreviewBitmap != null) "Custom icon selected (tap box to change)" else "Tap box for gallery, or create below",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onOpenTextIcon) {
                    Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create Text Icon")
                }
                if (config.sourceMode == SourceMode.URL) {
                    OutlinedButton(
                        enabled = !isFetchingIcon && config.url.isNotBlank(),
                        onClick = onFetchWebIcon
                    ) {
                        if (isFetchingIcon) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Fetching...")
                        } else {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Fetch Icon from Web")
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = config.appName,
            onValueChange = onAppNameChange,
            label = { Text("App Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.packageName,
            onValueChange = onPackageNameChange,
            label = { Text("Package Name (Auto / Editable)") },
            isError = !isPackageValid,
            supportingText = {
                if (!isPackageValid) Text("Format must be like com.example.myapp")
            },
            trailingIcon = {
                IconButton(onClick = onResetAutoPackage) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = "Auto-generate")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = config.versionName,
                onValueChange = { onConfigChange(config.copy(versionName = it)) },
                label = { Text("Version Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = versionCodeText,
                onValueChange = onVersionCodeChange,
                label = { Text("Version Code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DesignTabContent(
    config: ApkBuildConfig,
    iconBitmap: Bitmap?,
    onConfigChange: (ApkBuildConfig) -> Unit
) {
    SectionCard(title = "Live App Mockup") {
        MiniPhoneMockup(config = config, iconBitmap = iconBitmap)
    }

    SectionCard(title = "Status Bar, Progress & Orientation") {
        SettingSwitchRow(
            title = "Dynamic Status Bar",
            subtitle = "Auto-match page header & Day/Night theme",
            checked = config.dynamicStatusBar,
            onCheckedChange = { onConfigChange(config.copy(dynamicStatusBar = it)) }
        )

        SettingSwitchRow(
            title = "Floating Action Bubble",
            subtitle = "Draggable menu on right-middle (Back, Forward, Refresh, Share)",
            checked = config.showTopToolbar,
            onCheckedChange = { onConfigChange(config.copy(showTopToolbar = it)) }
        )

        ColorPaletteRow(
            label = if (config.dynamicStatusBar) "Fallback Status Bar Color" else "Status Bar Color",
            selectedHex = config.statusBarColorHex,
            onSelect = { onConfigChange(config.copy(statusBarColorHex = it)) }
        )

        ColorPaletteRow(
            label = "Loading Progress Bar Color",
            selectedHex = config.progressBarColorHex,
            onSelect = { onConfigChange(config.copy(progressBarColorHex = it)) }
        )

        Text("Screen Orientation", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("auto" to "Auto", "portrait" to "Portrait", "landscape" to "Landscape").forEach { (key, label) ->
                FilterChip(
                    selected = config.orientation == key,
                    onClick = { onConfigChange(config.copy(orientation = key)) },
                    label = { Text(label) }
                )
            }
        }
    }

    SectionCard(title = "Splash Screen") {
        SettingSwitchRow(
            title = "Enable Splash Screen",
            subtitle = "Show app icon & name on launch",
            checked = config.splashEnabled,
            onCheckedChange = { onConfigChange(config.copy(splashEnabled = it)) }
        )

        if (config.splashEnabled) {
            Text(
                text = "Splash Duration: ${(config.splashDurationMs / 1000f)}s",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = config.splashDurationMs.toFloat(),
                onValueChange = {
                    val snapped = ((it / 500f).roundToInt() * 500L).coerceIn(500L, 4000L)
                    onConfigChange(config.copy(splashDurationMs = snapped))
                },
                valueRange = 500f..4000f,
                steps = 6
            )
            ColorPaletteRow(
                label = "Splash Background",
                selectedHex = config.splashBgColorHex,
                onSelect = { onConfigChange(config.copy(splashBgColorHex = it)) }
            )
            ColorPaletteRow(
                label = "Splash Text Color",
                selectedHex = config.splashTextColorHex,
                onSelect = { onConfigChange(config.copy(splashTextColorHex = it)) }
            )
        }
    }
}

@Composable
private fun FeaturesTabContent(
    config: ApkBuildConfig,
    onConfigChange: (ApkBuildConfig) -> Unit
) {
    SectionCard(title = "WebView & App Behavior") {
        SettingSwitchRow("JavaScript", "Required for modern websites", config.javaScriptEnabled) {
            onConfigChange(config.copy(javaScriptEnabled = it))
        }
        SettingSwitchRow("DOM Storage & Cache", "Save local storage & sessions", config.domStorageEnabled) {
            onConfigChange(config.copy(domStorageEnabled = it))
        }
        SettingSwitchRow("Pinch-to-Zoom", "Allow users to zoom pages", config.zoomEnabled) {
            onConfigChange(config.copy(zoomEnabled = it))
        }
        SettingSwitchRow("Pull-to-Refresh", "Swipe down to reload page", config.pullToRefresh) {
            onConfigChange(config.copy(pullToRefresh = it))
        }
        SettingSwitchRow("File Upload", "Support <input type=\"file\">", config.allowFileUpload) {
            onConfigChange(config.copy(allowFileUpload = it))
        }
        SettingSwitchRow("File Downloads", "Download files via DownloadManager", config.allowDownloads) {
            onConfigChange(config.copy(allowDownloads = it))
        }
        SettingSwitchRow("Open External Links", "Open other domains in phone browser", config.openExternalLinksInBrowser) {
            onConfigChange(config.copy(openExternalLinksInBrowser = it))
        }
        SettingSwitchRow("Exit Confirmation", "Ask before closing app on Back press", config.exitConfirmEnabled) {
            onConfigChange(config.copy(exitConfirmEnabled = it))
        }
        SettingSwitchRow("Edge Swipe Back/Forward", "Swipe from left edge to go back, right edge for forward", config.swipeNavigation) {
            onConfigChange(config.copy(swipeNavigation = it))
        }
        SettingSwitchRow("Desktop Site Mode", "Request desktop version of websites", config.desktopMode) {
            onConfigChange(config.copy(desktopMode = it))
        }
        SettingSwitchRow("Keep Screen On", "Prevent screen from sleeping while open", config.keepScreenOn) {
            onConfigChange(config.copy(keepScreenOn = it))
        }
        SettingSwitchRow("Block Screenshots", "Prevent screenshots & screen recording", config.blockScreenshots) {
            onConfigChange(config.copy(blockScreenshots = it))
        }
        OutlinedTextField(
            value = config.userAgent,
            onValueChange = { onConfigChange(config.copy(userAgent = it)) },
            label = { Text("Custom User-Agent (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    SectionCard(title = "Hardware Permissions") {
        SettingSwitchRow("Camera Access", "Allow WebRTC / QR scanner camera", config.allowCamera) {
            onConfigChange(config.copy(allowCamera = it))
        }
        SettingSwitchRow("Microphone Access", "Allow voice & audio recording", config.allowMicrophone) {
            onConfigChange(config.copy(allowMicrophone = it))
        }
        SettingSwitchRow("Location (GPS)", "Allow maps & geolocation requests", config.allowLocation) {
            onConfigChange(config.copy(allowLocation = it))
        }
    }

    SectionCard(title = "Custom CSS & JS Injection") {
        OutlinedTextField(
            value = config.customCss,
            onValueChange = { onConfigChange(config.copy(customCss = it)) },
            label = { Text("Custom CSS (optional)") },
            placeholder = { Text("body { background: #000 !important; }") },
            minLines = 3,
            maxLines = 6,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = config.customJs,
            onValueChange = { onConfigChange(config.copy(customJs = it)) },
            label = { Text("Custom JavaScript (optional)") },
            placeholder = { Text("console.log('Injected by ApkMint');") },
            minLines = 3,
            maxLines = 6,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun ColorPaletteRow(
    label: String,
    selectedHex: String,
    onSelect: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = { showPicker = true },
                shape = RoundedCornerShape(99.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(parseComposeColor(selectedHex, Color.Gray))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text("Color Picker ($selectedHex)", style = MaterialTheme.typography.labelMedium)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            COLOR_SWATCHES.forEach { hex ->
                val color = parseComposeColor(hex, Color.Gray)
                val isSelected = selectedHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) }
                )
            }
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            title = label,
            initialHex = selectedHex,
            onDismiss = { showPicker = false },
            onColorSelected = { hex ->
                onSelect(hex)
                showPicker = false
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun generatePackageFromName(appName: String): String {
    val asciiSlug = appName.lowercase().replace(Regex("[^a-z0-9]"), "")
    val slug = when {
        asciiSlug.isEmpty() && appName.isNotBlank() -> "app${kotlin.math.abs(appName.hashCode()) % 10000}"
        asciiSlug.isEmpty() -> "myapp"
        asciiSlug.first().isDigit() -> "app$asciiSlug"
        else -> asciiSlug
    }
    return "com.apkmint.$slug"
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) {
            return cursor.getString(idx)
        }
    }
    return null
}

internal fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format("%.2f MB", mb) else String.format("%.0f KB", kb)
}
