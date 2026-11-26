package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.overlay.FloatingOverlayService
import com.example.ui.MainViewModel
import com.example.ui.components.TopStudioBar
import com.example.ui.i18n.stringsFor
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SystemOverlayScreen
import com.example.ui.screens.VoiceStudioScreen
import com.example.ui.theme.VoxDeckTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoxDeckRoot(viewModel = viewModel)
        }
    }
}

private data class NavTab(
    val route: AppRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
private fun VoxDeckRoot(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val language by viewModel.language.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val cards by viewModel.filteredCards.collectAsStateWithLifecycle()
    val playingCardId by viewModel.playingCardId.collectAsStateWithLifecycle()
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val isOverlayRunning by viewModel.isOverlayRunning.collectAsStateWithLifecycle()

    val voiceProfiles by viewModel.allVoiceProfiles.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedVoiceProfile.collectAsStateWithLifecycle()
    val selectedGender by viewModel.selectedGender.collectAsStateWithLifecycle()
    val pitch by viewModel.pitch.collectAsStateWithLifecycle()
    val speed by viewModel.speed.collectAsStateWithLifecycle()
    val editableVoiceId by viewModel.editableVoiceId.collectAsStateWithLifecycle()
    val ttsText by viewModel.ttsText.collectAsStateWithLifecycle()
    val isSynthesizing by viewModel.isSynthesizing.collectAsStateWithLifecycle()
    val cartesiaApiKey by viewModel.cartesiaApiKey.collectAsStateWithLifecycle()

    val rootStatus by viewModel.rootStatus.collectAsStateWithLifecycle()
    val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val routingMode by viewModel.routingMode.collectAsStateWithLifecycle()
    val customShellCommand by viewModel.customShellCommand.collectAsStateWithLifecycle()
    val commandOutput by viewModel.commandOutput.collectAsStateWithLifecycle()

    val strings = stringsFor(language)
    val snackbarHostState = remember { SnackbarHostState() }

    var hasOverlayPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
                viewModel.refreshShizuku()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.uiMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val mediaImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importMediaUri(uri)
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBackup(uri)
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    var pendingStartOverlay by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingStartOverlay) {
            pendingStartOverlay = false
            val serviceIntent = Intent(context, FloatingOverlayService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    fun requestOverlayPermissionScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    fun toggleFloatingOverlay(enable: Boolean) {
        val serviceIntent = Intent(context, FloatingOverlayService::class.java)
        if (!enable) {
            context.stopService(serviceIntent)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            requestOverlayPermissionScreen()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingStartOverlay = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val tabs = listOf(
        NavTab(
            route = AppRoute.Soundboard,
            label = strings.navSoundboard,
            selectedIcon = Icons.Filled.GraphicEq,
            unselectedIcon = Icons.Outlined.GraphicEq,
            tag = "nav_soundboard"
        ),
        NavTab(
            route = AppRoute.VoiceStudio,
            label = strings.navVoiceStudio,
            selectedIcon = Icons.Filled.RecordVoiceOver,
            unselectedIcon = Icons.Outlined.RecordVoiceOver,
            tag = "nav_voice_studio"
        ),
        NavTab(
            route = AppRoute.SystemOverlay,
            label = strings.navSystemOverlay,
            selectedIcon = Icons.Filled.Layers,
            unselectedIcon = Icons.Outlined.Layers,
            tag = "nav_system_overlay"
        ),
        NavTab(
            route = AppRoute.Settings,
            label = strings.navSettings,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            tag = "nav_settings"
        )
    )

    fun navigateTo(route: AppRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    VoxDeckTheme(darkTheme = darkMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopStudioBar(
                        strings = strings,
                        language = language,
                        darkMode = darkMode,
                        isPlaying = playingCardId != null || isTtsSpeaking,
                        onToggleLanguage = viewModel::toggleLanguage,
                        onToggleDarkMode = { viewModel.setDarkMode(!darkMode) },
                        onStopAll = viewModel::stopAllSounds
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDest?.hasRoute(tab.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDest?.hasRoute(tab.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(
                            navController = navController,
                            startDestination = AppRoute.Soundboard,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable<AppRoute.Soundboard> {
                                DashboardScreen(
                                    strings = strings,
                                    cards = cards,
                                    playingCardId = playingCardId,
                                    volume = volume,
                                    searchQuery = searchQuery,
                                    selectedFilter = selectedFilter,
                                    isOverlayRunning = isOverlayRunning,
                                    onVolumeChange = viewModel::updateVolume,
                                    onSearchChange = viewModel::setSearchQuery,
                                    onFilterChange = viewModel::setFilter,
                                    onPlayToggle = viewModel::togglePlayCard,
                                    onFavoriteToggle = viewModel::toggleFavorite,
                                    onRenameCard = viewModel::renameCard,
                                    onDeleteCard = viewModel::deleteCard,
                                    onImportMediaClick = {
                                        mediaImportLauncher.launch(arrayOf("audio/*", "video/*"))
                                    },
                                    onOverlayToggle = ::toggleFloatingOverlay,
                                    onStopAll = viewModel::stopAllSounds
                                )
                            }

                            composable<AppRoute.VoiceStudio> {
                                VoiceStudioScreen(
                                    strings = strings,
                                    language = language,
                                    hasCartesiaKey = cartesiaApiKey.isNotBlank(),
                                    profiles = voiceProfiles,
                                    selectedProfile = selectedProfile,
                                    selectedGender = selectedGender,
                                    pitch = pitch,
                                    speed = speed,
                                    editableVoiceId = editableVoiceId,
                                    ttsText = ttsText,
                                    isSynthesizing = isSynthesizing,
                                    onSelectGender = viewModel::selectGender,
                                    onSelectProfile = viewModel::selectVoiceProfile,
                                    onPitchChange = viewModel::setPitch,
                                    onSpeedChange = viewModel::setSpeed,
                                    onVoiceIdChange = viewModel::setEditableVoiceId,
                                    onSaveVoiceParams = viewModel::persistCurrentVoiceParams,
                                    onPreviewVoice = viewModel::previewSelectedVoice,
                                    onTtsTextChange = viewModel::setTtsText,
                                    onPlayDirect = { viewModel.playOrSaveTts(saveToCard = false) },
                                    onSaveToCard = { viewModel.playOrSaveTts(saveToCard = true) },
                                    onAddCustomProfile = viewModel::addCustomVoiceProfile,
                                    onDeleteCustomProfile = viewModel::deleteCustomVoiceProfile
                                )
                            }

                            composable<AppRoute.SystemOverlay> {
                                SystemOverlayScreen(
                                    strings = strings,
                                    hasOverlayPermission = hasOverlayPermission,
                                    isOverlayRunning = isOverlayRunning,
                                    rootStatus = rootStatus,
                                    shizukuStatus = shizukuStatus,
                                    routingMode = routingMode,
                                    customShellCommand = customShellCommand,
                                    commandOutput = commandOutput,
                                    onRequestOverlayPermission = ::requestOverlayPermissionScreen,
                                    onToggleOverlay = ::toggleFloatingOverlay,
                                    onCheckRoot = viewModel::checkRootAccess,
                                    onRefreshShizuku = viewModel::refreshShizuku,
                                    onRequestShizuku = viewModel::requestShizukuPermission,
                                    onSelectRoutingMode = viewModel::setRoutingMode,
                                    onExecuteShellCommand = viewModel::executeAudioShellCommand
                                )
                            }

                            composable<AppRoute.Settings> {
                                SettingsScreen(
                                    strings = strings,
                                    language = language,
                                    darkMode = darkMode,
                                    savedApiKey = cartesiaApiKey,
                                    onSaveApiKey = viewModel::saveCartesiaApiKey,
                                    onSetLanguage = viewModel::setLanguage,
                                    onSetDarkMode = viewModel::setDarkMode,
                                    onExportBackupClick = {
                                        exportBackupLauncher.launch("voxdeck_backup.zip")
                                    },
                                    onImportBackupClick = {
                                        importBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
