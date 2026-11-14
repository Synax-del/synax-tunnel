package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GoogleAuthHelper
import com.example.model.AppThemeMode
import com.example.model.TunnelStatus
import com.example.ui.components.AddTimeDialog
import com.example.ui.components.AdminPanelDialog
import com.example.ui.components.ConnectionMethodDialog
import com.example.ui.components.ServerPickerSheet
import com.example.ui.components.SettingsSheet
import com.example.ui.components.SpeedTestSheet
import com.example.ui.components.TelegramJoinDialog
import com.example.ui.screens.ConfigsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.MonitorScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.formatDuration
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTunnelColors
import com.example.ui.theme.ProGold
import com.example.vpn.CipherVpnService
import com.example.vpn.SpeedTestPhase
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CipherTunnelApp(
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val config by viewModel.config.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.liveRemainingSeconds.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val cloudTweaks by viewModel.cloudTweaks.collectAsStateWithLifecycle()
    val userCloudConfigs by viewModel.userCloudConfigs.collectAsStateWithLifecycle()
    val activeAnnouncement by viewModel.activeAnnouncement.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val latencies by viewModel.serverLatencies.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isPinging by viewModel.isPingingServers.collectAsStateWithLifecycle()
    val toolsState by viewModel.toolsState.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val speedTestState by viewModel.speedTestState.collectAsStateWithLifecycle()
    val isCloudBusy by viewModel.isCloudBusy.collectAsStateWithLifecycle()
    val lastExportedShareCode by viewModel.lastExportedShareCode.collectAsStateWithLifecycle()

    var showConnectionMethodDialog by remember { mutableStateOf(false) }
    var showServerPicker by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAddTimeDialog by remember { mutableStateOf(false) }
    var showTelegramDialog by remember { mutableStateOf(false) }
    var showSpeedTestSheet by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var hasTriggeredStartupTelegram by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableIntStateOf(0) }

    var pendingExportMeta by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val systemDark = isSystemInDarkTheme()
    val isCurrentlyDark = when (appSettings.themeMode) {
        AppThemeMode.DAY -> false
        AppThemeMode.NIGHT -> true
        AppThemeMode.SYSTEM -> systemDark
    }

    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(context, scope)
    }

    LaunchedEffect(settingsLoaded, appSettings.telegramPopupDismissed) {
        if (settingsLoaded && !hasTriggeredStartupTelegram) {
            hasTriggeredStartupTelegram = true
            if (!appSettings.telegramPopupDismissed) {
                showTelegramDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun openExternalUrl(rawUrl: String) {
        val clean = rawUrl.trim()
        val finalUrl = if (clean.startsWith("http://") || clean.startsWith("https://")) clean else "https://$clean"
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            scope.launch { snackbarHostState.showSnackbar("Unable to open link: $finalUrl") }
        }
    }

    fun triggerGoogleSignIn() {
        if (activity == null) return
        GoogleAuthHelper.signInWithGoogle(
            activity = activity,
            scope = scope,
            onSuccess = { user ->
                scope.launch {
                    snackbarHostState.showSnackbar("Signed in as ${user?.email ?: user?.displayName ?: "Google User"}")
                }
            },
            onCancelled = {
                scope.launch { snackbarHostState.showSnackbar("Google Sign-In cancelled") }
            },
            onError = { err ->
                scope.launch { snackbarHostState.showSnackbar(err) }
            },
        )
    }

    // VPN permission consent launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            CipherVpnService.start(context)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("VPN permission is required to establish the tunnel")
            }
        }
    }

    // Android 13+ notification permission launcher (non-blocking if denied)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            CipherVpnService.start(context)
        }
    }

    // SAF Import / Export launchers
    val importConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importConfigFromUri(uri)
        }
    }

    val exportConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val meta = pendingExportMeta
        if (uri != null && meta != null) {
            viewModel.exportConfigToUri(uri, meta.first, meta.second, meta.third)
        }
        pendingExportMeta = null
    }

    fun requestStartVpn() {
        if (remainingSeconds <= 0L) {
            showAddTimeDialog = true
            scope.launch { snackbarHostState.showSnackbar("No tunnel time remaining. Add time to connect!") }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            CipherVpnService.start(context)
        }
    }

    fun handleStartOrStop() {
        val active = telemetry.status == TunnelStatus.CONNECTED ||
            telemetry.status == TunnelStatus.CONNECTING ||
            telemetry.status == TunnelStatus.RECONNECTING
        if (active) {
            CipherVpnService.stop(context)
        } else {
            requestStartVpn()
        }
    }

    if (drawerState.isOpen) {
        BackHandler {
            scope.launch { drawerState.close() }
        }
    } else if (selectedTab != AppTab.HOME) {
        BackHandler {
            viewModel.selectTab(AppTab.HOME)
        }
    }

    val tunnelColors = LocalTunnelColors.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.widthIn(max = 310.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Drawer Brand Header
                    Surface(
                        color = tunnelColors.topBarContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        append("SYNAX TUNNEL ")
                                        withStyle(SpanStyle(color = ProGold, fontWeight = FontWeight.ExtraBold)) {
                                            append("PRO")
                                        }
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tunnelColors.onTopBar,
                                )
                                IconButton(
                                    onClick = viewModel::cycleDayNightMode,
                                    modifier = Modifier.testTag("drawer_theme_toggle"),
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentlyDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                        contentDescription = "Toggle Day/Night Mode",
                                        tint = tunnelColors.onTopBar,
                                    )
                                }
                            }
                            Text(
                                text = "SNI Injector • Cloud Sync • Fast Speed Test",
                                style = MaterialTheme.typography.labelSmall,
                                color = tunnelColors.onTopBar.copy(alpha = 0.85f),
                            )
                        }
                    }

                    // Google Cloud Account Card in Side Menu
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.displayName ?: "Cloud Sync",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = currentUser?.email ?: "Sign in to sync configs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (currentUser == null) {
                                TextButton(onClick = ::triggerGoogleSignIn) {
                                    Text("Sign in")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        GoogleAuthHelper.signOut(context, scope) {
                                            scope.launch { snackbarHostState.showSnackbar("Signed out") }
                                        }
                                    },
                                ) {
                                    Text("Sign out")
                                }
                            }
                        }
                    }

                    // Side Menu Actions including Fast.com-style Internet Speed Checker
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                        label = { Text("Internet Speed Checker") },
                        badge = {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = "FAST",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showSpeedTestSheet = true
                            if (speedTestState.phase == SpeedTestPhase.IDLE) {
                                viewModel.runSpeedTest()
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("drawer_speed_test_item"),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.AccessTimeFilled, contentDescription = null) },
                        label = { Text("Add Tunnel Time") },
                        badge = {
                            Text(
                                text = formatDuration(remainingSeconds),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showAddTimeDialog = true
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("drawer_add_time_item"),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.CloudSync, contentDescription = null) },
                        label = { Text("Cloud & File Configs") },
                        selected = selectedTab == AppTab.CONFIGS,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.selectTab(AppTab.CONFIGS)
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("drawer_configs_item"),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null) },
                        label = { Text("Join Telegram Channel") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showTelegramDialog = true
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("drawer_telegram_item"),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        label = { Text("Tunnel Settings") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showSettingsSheet = true
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("drawer_settings_item"),
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))

                    // Day / Night Mode Selector in Side Menu
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "DAY & NIGHT MODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = appSettings.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("drawer_theme_${mode.name.lowercase()}"),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Hidden Admin Trigger #2: 5-tap version footer (undisclosed to normal users)
                    Text(
                        text = "Synax Tunnel v2.4.0 • Build 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                versionTapCount++
                                if (versionTapCount >= 5) {
                                    versionTapCount = 0
                                    scope.launch { drawerState.close() }
                                    showAdminPanel = true
                                }
                            }
                            .padding(vertical = 16.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Surface(
                    color = tunnelColors.topBarContainer,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("topbar_menu_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Open Side Menu",
                                tint = tunnelColors.onTopBar,
                            )
                        }

                        // Hidden Admin Trigger #1: long-press on SYNAX TUNNEL title
                        Text(
                            text = buildAnnotatedString {
                                append("SYNAX TUNNEL ")
                                withStyle(SpanStyle(color = ProGold, fontWeight = FontWeight.ExtraBold)) {
                                    append("PRO")
                                }
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = tunnelColors.onTopBar,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { showAdminPanel = true },
                                )
                                .testTag("topbar_brand_title"),
                        )

                        IconButton(
                            onClick = viewModel::cycleDayNightMode,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("topbar_theme_toggle"),
                        ) {
                            Icon(
                                imageVector = if (isCurrentlyDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Toggle Day or Night Mode",
                                tint = tunnelColors.onTopBar,
                            )
                        }

                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("topbar_settings_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Tunnel Settings",
                                tint = tunnelColors.onTopBar,
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    windowInsets = WindowInsets.navigationBars,
                ) {
                    AppTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(tab) },
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            icon = {
                                when (tab) {
                                    AppTab.CONFIGS -> Icon(
                                        imageVector = if (selected) {
                                            Icons.AutoMirrored.Filled.Assignment
                                        } else {
                                            Icons.AutoMirrored.Outlined.Assignment
                                        },
                                        contentDescription = tab.label,
                                    )
                                    AppTab.TOOLS -> Icon(
                                        imageVector = if (selected) Icons.Filled.Build else Icons.Outlined.Build,
                                        contentDescription = tab.label,
                                    )
                                    AppTab.HOME -> Icon(
                                        imageVector = if (selected) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = tab.label,
                                    )
                                    AppTab.LOGS -> BadgedBox(
                                        badge = {
                                            if (logs.isNotEmpty()) {
                                                Badge(
                                                    containerColor = Color(0xFFB91C1C),
                                                    contentColor = Color.White,
                                                ) {
                                                    Text(logs.size.coerceAtMost(99).toString())
                                                }
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = if (selected) {
                                                Icons.AutoMirrored.Filled.Subject
                                            } else {
                                                Icons.AutoMirrored.Outlined.Subject
                                            },
                                            contentDescription = tab.label,
                                        )
                                    }
                                    AppTab.MONITOR -> BadgedBox(
                                        badge = {
                                            if (sessions.isNotEmpty()) {
                                                Badge(
                                                    containerColor = Color(0xFFB91C1C),
                                                    contentColor = Color.White,
                                                ) {
                                                    Text(sessions.size.coerceAtMost(99).toString())
                                                }
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = if (selected) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                            contentDescription = tab.label,
                                        )
                                    }
                                }
                            },
                            label = {
                                if (selected) {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            },
                            alwaysShowLabel = false,
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                ) {
                    when (selectedTab) {
                        AppTab.HOME -> HomeScreen(
                            config = config,
                            telemetry = telemetry,
                            serverLatencies = latencies,
                            remainingTimeSeconds = remainingSeconds,
                            activeAnnouncement = activeAnnouncement,
                            onOpenServerPicker = { showServerPicker = true },
                            onOpenConnectionMethod = { showConnectionMethodDialog = true },
                            onOpenPresets = { viewModel.selectTab(AppTab.CONFIGS) },
                            onOpenAddTime = { showAddTimeDialog = true },
                            onDismissAnnouncement = viewModel::dismissAnnouncement,
                            onOpenAnnouncementUrl = ::openExternalUrl,
                            onStartOrStop = ::handleStartOrStop,
                            onReconnect = { CipherVpnService.reconnect(context) },
                        )
                        AppTab.CONFIGS -> ConfigsScreen(
                            profiles = profiles,
                            currentUser = currentUser,
                            cloudTweaks = cloudTweaks,
                            userCloudConfigs = userCloudConfigs,
                            isCloudBusy = isCloudBusy,
                            lastExportedShareCode = lastExportedShareCode,
                            onSignInGoogleClick = ::triggerGoogleSignIn,
                            onApplyProfile = { profile ->
                                viewModel.applyProfile(profile)
                                viewModel.selectTab(AppTab.HOME)
                            },
                            onDeleteProfile = viewModel::deleteProfile,
                            onSaveCurrentProfile = viewModel::saveCurrentAsProfile,
                            onImportFileClick = {
                                importConfigLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            onExportFileRequest = { name, author, notes ->
                                pendingExportMeta = Triple(name, author, notes)
                                val safeName = name.ifBlank { "synax_config" }
                                    .lowercase()
                                    .replace(Regex("[^a-z0-9_]+"), "_")
                                exportConfigLauncher.launch("$safeName.synax.json")
                            },
                            onExportCloudRequest = { name, author, notes ->
                                viewModel.exportCurrentToCloud(name, author, notes)
                            },
                            onImportCloudCode = viewModel::importFromCloudCode,
                            onClearExportedShareCode = viewModel::clearLastExportedShareCode,
                            onApplyCloudTweak = { tweak ->
                                viewModel.applyCloudTweak(tweak)
                                viewModel.selectTab(AppTab.HOME)
                            },
                            onApplyCloudConfig = { doc ->
                                viewModel.applyCloudConfig(doc)
                                viewModel.selectTab(AppTab.HOME)
                            },
                            onDeleteCloudConfig = viewModel::deleteCloudConfig,
                            onCopiedToast = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                        )
                        AppTab.TOOLS -> ToolsScreen(
                            state = toolsState,
                            onHostQueryChange = viewModel::updateHostQuery,
                            onHostPortChange = viewModel::updateHostPort,
                            onHostMethodChange = viewModel::updateHostMethod,
                            onRunHostCheck = viewModel::runHostCheck,
                            onScanHostChange = viewModel::updateScanHostQuery,
                            onRunPortScan = viewModel::runPortScan,
                            onTargetPrefixChange = viewModel::updateTargetIpPrefix,
                            onRefreshIp = viewModel::refreshIpInfo,
                            onOpenSettings = { showSettingsSheet = true },
                        )
                        AppTab.LOGS -> LogsScreen(
                            logs = logs,
                            onClearLogs = viewModel::clearLogs,
                            onCopiedToast = {
                                scope.launch { snackbarHostState.showSnackbar("Copied logs to clipboard") }
                            },
                        )
                        AppTab.MONITOR -> MonitorScreen(
                            telemetry = telemetry,
                            sessions = sessions,
                            onClearSessions = viewModel::clearSessionHistory,
                        )
                    }
                }
            }
        }
    }

    if (showConnectionMethodDialog) {
        ConnectionMethodDialog(
            initialConfig = config,
            onDismiss = { showConnectionMethodDialog = false },
            onSave = { updated ->
                viewModel.updateConfig(updated)
                showConnectionMethodDialog = false
                scope.launch { snackbarHostState.showSnackbar("Connection method saved") }
            },
        )
    }

    if (showServerPicker) {
        ServerPickerSheet(
            config = config,
            latencies = latencies,
            isPinging = isPinging,
            onPingAll = viewModel::pingAllServers,
            onSelectServer = viewModel::selectServer,
            onDismiss = { showServerPicker = false },
        )
    }

    if (showSettingsSheet) {
        SettingsSheet(
            config = config,
            themeMode = appSettings.themeMode,
            installedApps = installedApps,
            onThemeModeChange = viewModel::setThemeMode,
            onUpdateConfig = viewModel::updateConfig,
            onToggleExcludedApp = viewModel::toggleExcludedPackage,
            onDismiss = { showSettingsSheet = false },
        )
    }

    if (showAddTimeDialog) {
        AddTimeDialog(
            remainingSeconds = remainingSeconds,
            onAddTimeSeconds = viewModel::addTimeSeconds,
            onRedeemCode = viewModel::redeemTimeCode,
            onDismiss = { showAddTimeDialog = false },
        )
    }

    if (showTelegramDialog) {
        TelegramJoinDialog(
            telegramUrl = appSettings.telegramUrl,
            onJoinClick = { dontShowAgain ->
                viewModel.dismissTelegramPopup(dontShowAgain)
                showTelegramDialog = false
                openExternalUrl(appSettings.telegramUrl)
            },
            onDismiss = { dontShowAgain ->
                viewModel.dismissTelegramPopup(dontShowAgain)
                showTelegramDialog = false
            },
        )
    }

    if (showSpeedTestSheet) {
        SpeedTestSheet(
            state = speedTestState,
            onRunTest = viewModel::runSpeedTest,
            onDismiss = { showSpeedTestSheet = false },
        )
    }

    if (showAdminPanel) {
        AdminPanelDialog(
            expectedPin = appSettings.adminPin,
            currentConfig = config,
            cloudTweaks = cloudTweaks,
            signedInEmail = currentUser?.email,
            telegramUrl = appSettings.telegramUrl,
            onSignInGoogleClick = ::triggerGoogleSignIn,
            onPublishTweak = viewModel::publishAdminTweak,
            onDeleteCloudTweak = viewModel::deleteCloudTweak,
            onSendNotification = viewModel::sendAdminNotification,
            onUpdateTelegramUrl = viewModel::updateTelegramUrl,
            onUpdateAdminPin = viewModel::updateAdminPin,
            onDismiss = { showAdminPanel = false },
        )
    }
}
