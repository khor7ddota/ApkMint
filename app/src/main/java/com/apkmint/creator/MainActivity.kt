package com.apkmint.creator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apkmint.creator.engine.ApkBuildConfig
import com.apkmint.creator.engine.ApkBuilderEngine
import com.apkmint.creator.engine.SourceMode
import com.apkmint.creator.ui.AboutDeveloperDialog
import com.apkmint.creator.ui.CreateApkScreen
import com.apkmint.creator.ui.MyApksScreen
import com.apkmint.creator.ui.theme.ApkMintTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApkMintTheme {
                val context = LocalContext.current
                var selectedTab by remember { mutableIntStateOf(0) }
                var selectedBuilderMode by remember { mutableStateOf<SourceMode?>(null) }
                var pendingEditConfig by remember { mutableStateOf<ApkBuildConfig?>(null) }
                var previewTrigger by remember { mutableIntStateOf(0) }
                var showAboutDialog by remember { mutableStateOf(false) }
                var builtApks by remember { mutableStateOf(ApkBuilderEngine.listBuiltApks(context)) }

                val inBuilder = selectedTab == 0 && selectedBuilderMode != null

                Scaffold(
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White,
                                actionIconContentColor = Color.White
                            ),
                            navigationIcon = {
                                if (inBuilder) {
                                    IconButton(onClick = { selectedBuilderMode = null }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            },
                            title = {
                                val titleText = when {
                                    inBuilder && selectedBuilderMode == SourceMode.URL -> "Website to APK"
                                    inBuilder && selectedBuilderMode == SourceMode.HTML -> "HTML to APK"
                                    selectedTab == 1 -> "My Built APKs"
                                    else -> "ApkMint"
                                }
                                Text(titleText, fontWeight = FontWeight.Bold)
                            },
                            actions = {
                                if (inBuilder) {
                                    TextButton(onClick = { previewTrigger++ }) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Preview", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    IconButton(onClick = { showAboutDialog = true }) {
                                        Icon(Icons.Default.Person, contentDescription = "Developer Info")
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Build, contentDescription = null) },
                                label = { Text("Create APK") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = {
                                    builtApks = ApkBuilderEngine.listBuiltApks(context)
                                    selectedTab = 1
                                },
                                icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                label = { Text("My APKs (${builtApks.size})") }
                            )
                        }
                    }
                ) { innerPadding ->
                    if (selectedTab == 0) {
                        CreateApkScreen(
                            builtApks = builtApks,
                            selectedMode = selectedBuilderMode,
                            pendingEditConfig = pendingEditConfig,
                            previewTrigger = previewTrigger,
                            modifier = Modifier.padding(innerPadding),
                            onSelectedModeChange = { selectedBuilderMode = it },
                            onConsumePendingEdit = { pendingEditConfig = null },
                            onOpenDeveloperInfo = { showAboutDialog = true },
                            onApkBuilt = {
                                builtApks = ApkBuilderEngine.listBuiltApks(context)
                            }
                        )
                    } else {
                        MyApksScreen(
                            apks = builtApks,
                            onEditRebuild = { item ->
                                val loaded = ApkBuilderEngine.loadSavedProject(item)
                                if (loaded != null) {
                                    pendingEditConfig = loaded
                                    selectedBuilderMode = loaded.sourceMode
                                    selectedTab = 0
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Saved project data not found for older APK",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onDelete = { item ->
                                ApkBuilderEngine.deleteBuiltApk(item)
                                builtApks = ApkBuilderEngine.listBuiltApks(context)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                if (showAboutDialog) {
                    AboutDeveloperDialog(onDismiss = { showAboutDialog = false })
                }
            }
        }
    }
}
