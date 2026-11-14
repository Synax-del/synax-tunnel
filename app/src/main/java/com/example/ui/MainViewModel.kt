package com.example.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppUserSettings
import com.example.data.CloudTunnelRepository
import com.example.data.ConnectionSessionEntity
import com.example.data.DefaultServers
import com.example.data.GoogleAuthHelper
import com.example.data.OperationType
import com.example.data.TunnelDatabase
import com.example.data.TunnelPreferencesRepository
import com.example.data.TunnelProfileEntity
import com.example.data.handleFirestoreError
import com.example.model.AdminNotificationMessage
import com.example.model.AppThemeMode
import com.example.model.CloudConfigDocument
import com.example.model.CloudTweak
import com.example.model.ConnectionConfig
import com.example.model.HostCheckResult
import com.example.model.InstalledAppItem
import com.example.model.LogLevel
import com.example.model.PortScanResult
import com.example.model.TunnelLogEntry
import com.example.model.TunnelProtocol
import com.example.model.TunnelServer
import com.example.model.TunnelStatus
import com.example.model.TunnelTelemetry
import com.example.vpn.CipherVpnService
import com.example.vpn.NetworkDiagnostics
import com.example.vpn.SpeedTestEngine
import com.example.vpn.SpeedTestState
import com.example.vpn.TunnelStateHolder
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab(val label: String) {
    CONFIGS("Configs"),
    TOOLS("Tools"),
    HOME("Home"),
    LOGS("Logs"),
    MONITOR("Monitor"),
}

data class ToolsUiState(
    val hostQuery: String = "m.tiktok.com",
    val hostPort: Int = 443,
    val hostMethod: String = "HEAD",
    val isCheckingHost: Boolean = false,
    val hostResult: HostCheckResult? = null,
    val scanHostQuery: String = "1.1.1.1",
    val isScanningPorts: Boolean = false,
    val portScanResults: List<PortScanResult> = emptyList(),
    val targetIpPrefix: String = "10.0.",
    val localInterfaces: List<Pair<String, String>> = emptyList(),
    val publicIp: String? = null,
    val isHuntingIp: Boolean = false,
    val ipHuntError: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = TunnelPreferencesRepository(application)
    private val dao = TunnelDatabase.getInstance(application).tunnelDao()
    private val cloudRepo = CloudTunnelRepository(application)

    val config: StateFlow<ConnectionConfig> = prefsRepo.configFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionConfig())

    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    val appSettings: StateFlow<AppUserSettings> = prefsRepo.appSettingsFlow
        .map { settings ->
            if (!_settingsLoaded.value) {
                _settingsLoaded.value = true
                _liveRemainingSeconds.value = settings.remainingTimeSeconds
            }
            settings
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppUserSettings())

    private val _liveRemainingSeconds = MutableStateFlow(4L * 3600L)
    val liveRemainingSeconds: StateFlow<Long> = _liveRemainingSeconds.asStateFlow()

    val currentUser: StateFlow<FirebaseUser?> = GoogleAuthHelper.authStateFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val cloudTweaks: StateFlow<List<CloudTweak>> = currentUser
        .map { it?.uid }
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                cloudRepo.observeActiveTweaks()
                    .catch { err ->
                        handleFirestoreError(err, OperationType.LIST, "cloud_tweaks")
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userCloudConfigs: StateFlow<List<CloudConfigDocument>> = currentUser
        .map { it?.uid }
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                cloudRepo.observeUserCloudConfigs(uid)
                    .catch { err ->
                        handleFirestoreError(err, OperationType.LIST, "cloud_configs")
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val cloudNotifications: StateFlow<List<AdminNotificationMessage>> = currentUser
        .map { it?.uid }
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                cloudRepo.observeActiveNotifications()
                    .catch { err ->
                        handleFirestoreError(err, OperationType.LIST, "admin_notifications")
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _localAnnouncement = MutableStateFlow<AdminNotificationMessage?>(null)
    private val _dismissedAnnouncementIds = MutableStateFlow<Set<String>>(emptySet())

    val activeAnnouncement: StateFlow<AdminNotificationMessage?> = combine(
        _localAnnouncement,
        cloudNotifications,
        _dismissedAnnouncementIds,
    ) { localMsg, cloudList, dismissedIds ->
        val candidate = localMsg ?: cloudList.firstOrNull()
        if (candidate != null && candidate.id !in dismissedIds) candidate else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val telemetry: StateFlow<TunnelTelemetry> = TunnelStateHolder.telemetry
    val logs: StateFlow<List<TunnelLogEntry>> = TunnelStateHolder.logs
    val serverLatencies: StateFlow<Map<String, Long?>> = TunnelStateHolder.serverLatencies

    val profiles: StateFlow<List<TunnelProfileEntity>> = dao.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<ConnectionSessionEntity>> = dao.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(AppTab.HOME)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _isPingingServers = MutableStateFlow(false)
    val isPingingServers: StateFlow<Boolean> = _isPingingServers.asStateFlow()

    private val _toolsState = MutableStateFlow(ToolsUiState())
    val toolsState: StateFlow<ToolsUiState> = _toolsState.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _speedTestState = MutableStateFlow(SpeedTestState())
    val speedTestState: StateFlow<SpeedTestState> = _speedTestState.asStateFlow()
    private var speedTestJob: Job? = null

    private val _isCloudBusy = MutableStateFlow(false)
    val isCloudBusy: StateFlow<Boolean> = _isCloudBusy.asStateFlow()

    private val _lastExportedShareCode = MutableStateFlow<String?>(null)
    val lastExportedShareCode: StateFlow<String?> = _lastExportedShareCode.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 12)
    val toastEvents = _toastEvents.asSharedFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            TunnelDatabase.seedDefaultsIfEmpty(dao)
            loadInstalledLauncherApps()
            refreshIpInfo()
        }
        startSessionCountdownLoop()
    }

    private fun startSessionCountdownLoop() {
        viewModelScope.launch {
            var persistTick = 0
            while (isActive) {
                delay(1000L)
                if (telemetry.value.status == TunnelStatus.CONNECTED) {
                    val next = (_liveRemainingSeconds.value - 1L).coerceAtLeast(0L)
                    _liveRemainingSeconds.value = next
                    persistTick++
                    if (persistTick >= 10 || next == 0L) {
                        persistTick = 0
                        prefsRepo.setRemainingTimeSeconds(next)
                    }
                    if (next == 0L) {
                        CipherVpnService.stop(getApplication())
                        TunnelStateHolder.log(LogLevel.WARN, "Session time expired — tunnel stopped")
                        _toastEvents.tryEmit("Tunnel time expired. Tap + Add Time to continue.")
                    }
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            prefsRepo.setThemeMode(mode)
        }
    }

    fun cycleDayNightMode() {
        val next = when (appSettings.value.themeMode) {
            AppThemeMode.NIGHT -> AppThemeMode.DAY
            AppThemeMode.DAY, AppThemeMode.SYSTEM -> AppThemeMode.NIGHT
        }
        setThemeMode(next)
        _toastEvents.tryEmit("${next.label} mode enabled")
    }

    // --- HA Tunnel Plus style Add Time ---
    fun addTimeSeconds(deltaSeconds: Long, label: String) {
        viewModelScope.launch {
            val next = (_liveRemainingSeconds.value + deltaSeconds).coerceIn(0L, 30L * 86400L)
            _liveRemainingSeconds.value = next
            prefsRepo.setRemainingTimeSeconds(next)
            TunnelStateHolder.log(LogLevel.INFO, "Added tunnel time: $label")
            _toastEvents.tryEmit(label)
        }
    }

    fun redeemTimeCode(rawCode: String) {
        val code = rawCode.trim().uppercase()
        val reward = when (code) {
            "SYNAX24" -> 24L * 3600L to "+24h redeemed (SYNAX24)"
            "SYNAX12" -> 12L * 3600L to "+12h redeemed (SYNAX12)"
            "SYNAX6" -> 6L * 3600L to "+6h redeemed (SYNAX6)"
            "FREE2H" -> 2L * 3600L to "+2h redeemed (FREE2H)"
            else -> null
        }
        if (reward != null) {
            addTimeSeconds(reward.first, reward.second)
        } else {
            _toastEvents.tryEmit("Invalid time voucher code")
        }
    }

    // --- Telegram Popup ---
    fun dismissTelegramPopup(dontShowAgain: Boolean) {
        if (dontShowAgain) {
            viewModelScope.launch {
                prefsRepo.setTelegramPopupDismissed(true)
            }
        }
    }

    // --- Side Menu Speed Test (Fast.com style) ---
    fun runSpeedTest() {
        speedTestJob?.cancel()
        speedTestJob = viewModelScope.launch {
            SpeedTestEngine.runSpeedTest().collect { state ->
                _speedTestState.value = state
            }
        }
    }

    // --- Hidden Admin Panel Actions ---
    fun publishAdminTweak(name: String, carrierTag: String, notes: String, tweakConfig: ConnectionConfig) {
        viewModelScope.launch {
            val cleanName = name.trim()
            val cleanCarrier = carrierTag.trim().ifBlank { "All Networks" }
            val cleanNotes = notes.trim().ifBlank { "$cleanCarrier • ${tweakConfig.methodDisplaySummary()}" }

            // Always store in local Room profiles so it works seamlessly offline & online
            dao.insertProfile(
                TunnelProfileEntity(
                    name = cleanName,
                    author = "Synax Admin",
                    notes = cleanNotes,
                    protocol = tweakConfig.protocol.name,
                    serverId = tweakConfig.serverId,
                    customHost = tweakConfig.customHost,
                    customPort = tweakConfig.customPort,
                    useSniHost = tweakConfig.useSniHost,
                    sniHost = tweakConfig.sniHost,
                    usePayload = tweakConfig.usePayload,
                    payload = tweakConfig.payload,
                    usePayloadAfterTls = tweakConfig.usePayloadAfterTls,
                    payloadAfterTls = tweakConfig.payloadAfterTls,
                    useProxy = tweakConfig.useProxy,
                    proxyHost = tweakConfig.proxyHost,
                    proxyPort = tweakConfig.proxyPort,
                    obfuscatedTlsCh = tweakConfig.obfuscatedTlsCh,
                    tcpFastOpen = tweakConfig.tcpFastOpen,
                    preferIpv6 = tweakConfig.preferIpv6,
                    isPreset = false,
                ),
            )

            if (currentUser.value != null) {
                val res = cloudRepo.publishCloudTweak(cleanName, cleanCarrier, cleanNotes, tweakConfig)
                if (res.isSuccess) {
                    _toastEvents.tryEmit("Tweak \"$cleanName\" published to Cloud & Local")
                } else {
                    val errMsg = res.exceptionOrNull()?.localizedMessage ?: "Cloud permission denied"
                    _toastEvents.tryEmit("Saved locally (Cloud sync: $errMsg)")
                }
            } else {
                _toastEvents.tryEmit("Tweak \"$cleanName\" added locally")
            }
        }
    }

    fun deleteCloudTweak(tweakId: String) {
        viewModelScope.launch {
            cloudRepo.deleteCloudTweak(tweakId)
                .onSuccess { _toastEvents.tryEmit("Cloud tweak deleted") }
                .onFailure { _toastEvents.tryEmit("Delete failed: ${it.localizedMessage}") }
        }
    }

    fun sendAdminNotification(title: String, message: String, actionUrl: String) {
        val cleanTitle = title.trim()
        val cleanMsg = message.trim()
        val cleanUrl = actionUrl.trim()

        // 1. Fire live Android system broadcast notification
        runCatching {
            CipherVpnService.showAdminBroadcastNotification(getApplication(), cleanTitle, cleanMsg)
        }

        // 2. Display immediate in-app announcement banner
        _localAnnouncement.value = AdminNotificationMessage(
            id = "local_${System.currentTimeMillis()}",
            title = cleanTitle,
            message = cleanMsg,
            actionUrl = cleanUrl,
            createdBy = currentUser.value?.uid ?: "admin",
            isActive = true,
        )
        TunnelStateHolder.log(LogLevel.INFO, "Admin Broadcast: $cleanTitle — $cleanMsg")

        // 3. Push to Firestore if authenticated as admin
        viewModelScope.launch {
            if (currentUser.value != null) {
                val res = cloudRepo.publishAdminNotification(cleanTitle, cleanMsg, cleanUrl)
                if (res.isSuccess) {
                    _toastEvents.tryEmit("Notification broadcast sent & synced to Cloud")
                } else {
                    _toastEvents.tryEmit("Sent local notification (Cloud: ${res.exceptionOrNull()?.localizedMessage})")
                }
            } else {
                _toastEvents.tryEmit("Admin notification sent")
            }
        }
    }

    fun dismissAnnouncement(id: String) {
        _dismissedAnnouncementIds.value = _dismissedAnnouncementIds.value + id
        if (_localAnnouncement.value?.id == id) {
            _localAnnouncement.value = null
        }
    }

    fun updateTelegramUrl(url: String) {
        viewModelScope.launch {
            prefsRepo.setTelegramUrl(url)
            _toastEvents.tryEmit("Telegram channel link updated")
        }
    }

    fun updateAdminPin(pin: String) {
        viewModelScope.launch {
            prefsRepo.setAdminPin(pin)
            _toastEvents.tryEmit("Admin PIN updated")
        }
    }

    // --- Cloud Config Export & Import ---
    fun exportCurrentToCloud(name: String, author: String, notes: String, isPublic: Boolean = true) {
        if (currentUser.value == null) {
            _toastEvents.tryEmit("Please sign in with Google to export to Cloud")
            return
        }
        viewModelScope.launch {
            _isCloudBusy.value = true
            val res = cloudRepo.exportCloudConfig(
                name = name.ifBlank { "Synax Config" },
                author = author.ifBlank { currentUser.value?.displayName ?: "Synax User" },
                notes = notes.ifBlank { config.value.methodDisplaySummary() },
                config = config.value,
                isPublic = isPublic,
            )
            _isCloudBusy.value = false
            res.onSuccess { shareCode ->
                _lastExportedShareCode.value = shareCode
                TunnelStateHolder.log(LogLevel.INFO, "Exported Cloud Config code: $shareCode")
                _toastEvents.tryEmit("Cloud Config exported: $shareCode")
            }.onFailure { err ->
                _toastEvents.tryEmit("Cloud export failed: ${err.localizedMessage}")
            }
        }
    }

    fun clearLastExportedShareCode() {
        _lastExportedShareCode.value = null
    }

    fun importFromCloudCode(rawCode: String) {
        if (currentUser.value == null) {
            _toastEvents.tryEmit("Sign in with Google first to import Cloud Configs")
            return
        }
        viewModelScope.launch {
            _isCloudBusy.value = true
            val res = cloudRepo.importCloudConfigByCode(rawCode)
            _isCloudBusy.value = false
            res.onSuccess { doc ->
                val applied = CloudTunnelRepository.cloudConfigToConnectionConfig(doc, config.value)
                prefsRepo.saveConfig(applied)
                dao.insertProfile(
                    TunnelProfileEntity(
                        name = "${doc.name} (${doc.shareCode})",
                        author = doc.author.ifBlank { "Cloud" },
                        notes = doc.notes.ifBlank { "Imported from Cloud" },
                        protocol = applied.protocol.name,
                        serverId = applied.serverId,
                        customHost = applied.customHost,
                        customPort = applied.customPort,
                        useSniHost = applied.useSniHost,
                        sniHost = applied.sniHost,
                        usePayload = applied.usePayload,
                        payload = applied.payload,
                        usePayloadAfterTls = applied.usePayloadAfterTls,
                        payloadAfterTls = applied.payloadAfterTls,
                        useProxy = applied.useProxy,
                        proxyHost = applied.proxyHost,
                        proxyPort = applied.proxyPort,
                        obfuscatedTlsCh = applied.obfuscatedTlsCh,
                        tcpFastOpen = applied.tcpFastOpen,
                        preferIpv6 = applied.preferIpv6,
                        isPreset = false,
                    ),
                )
                TunnelStateHolder.log(LogLevel.INFO, "Imported Cloud Config ${doc.shareCode}: ${doc.name}")
                _toastEvents.tryEmit("Imported & applied \"${doc.name}\"")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Cloud import failed")
            }
        }
    }

    fun applyCloudConfig(doc: CloudConfigDocument) {
        val next = CloudTunnelRepository.cloudConfigToConnectionConfig(doc, config.value)
        updateConfig(next)
        TunnelStateHolder.log(LogLevel.INFO, "Loaded Cloud Config: ${doc.name} (${doc.shareCode})")
        _toastEvents.tryEmit("Applied \"${doc.name}\"")
    }

    fun applyCloudTweak(tweak: CloudTweak) {
        val next = CloudTunnelRepository.tweakToConnectionConfig(tweak, config.value)
        updateConfig(next)
        TunnelStateHolder.log(LogLevel.INFO, "Loaded Cloud Tweak: ${tweak.name} (${tweak.carrierTag})")
        _toastEvents.tryEmit("Applied tweak \"${tweak.name}\"")
    }

    fun deleteCloudConfig(shareCode: String) {
        viewModelScope.launch {
            cloudRepo.deleteCloudConfig(shareCode)
                .onSuccess { _toastEvents.tryEmit("Deleted cloud config $shareCode") }
                .onFailure { _toastEvents.tryEmit("Delete failed: ${it.localizedMessage}") }
        }
    }

    // --- Core Tunnel Configuration & Diagnostics ---
    fun updateConfig(updated: ConnectionConfig) {
        viewModelScope.launch {
            prefsRepo.saveConfig(updated)
        }
    }

    fun selectServer(server: TunnelServer, customHost: String? = null, customPort: Int? = null) {
        val current = config.value
        val next = current.copy(
            serverId = server.id,
            customHost = customHost ?: if (server.isCustom) current.customHost else server.host,
            customPort = customPort ?: if (server.isCustom) current.customPort else server.port,
        )
        updateConfig(next)
        TunnelStateHolder.log(LogLevel.INFO, "Selected server ${server.code} (${server.country})")
    }

    fun pingAllServers() {
        if (_isPingingServers.value) return
        viewModelScope.launch {
            _isPingingServers.value = true
            val current = config.value
            val servers = DefaultServers.presetServers
            servers.map { server ->
                async {
                    val host = if (server.isCustom) current.customHost.ifBlank { "1.1.1.1" } else server.host
                    val port = if (server.isCustom) current.customPort else server.port
                    val rtt = NetworkDiagnostics.pingTcp(host, port)
                    TunnelStateHolder.updateLatency(server.id, rtt)
                }
            }.awaitAll()
            _isPingingServers.value = false
        }
    }

    fun applyProfile(profile: TunnelProfileEntity) {
        val current = config.value
        val protocol = runCatching { TunnelProtocol.valueOf(profile.protocol) }.getOrDefault(TunnelProtocol.TCP)
        val next = current.copy(
            serverId = profile.serverId,
            customHost = profile.customHost,
            customPort = profile.customPort,
            protocol = protocol,
            useSniHost = profile.useSniHost,
            sniHost = profile.sniHost,
            usePayload = profile.usePayload,
            payload = profile.payload.ifBlank { current.payload },
            usePayloadAfterTls = profile.usePayloadAfterTls,
            payloadAfterTls = profile.payloadAfterTls.ifBlank { current.payloadAfterTls },
            useProxy = profile.useProxy,
            proxyHost = profile.proxyHost,
            proxyPort = profile.proxyPort,
            obfuscatedTlsCh = profile.obfuscatedTlsCh,
            tcpFastOpen = profile.tcpFastOpen,
            preferIpv6 = profile.preferIpv6,
        )
        updateConfig(next)
        TunnelStateHolder.log(LogLevel.INFO, "Loaded profile: ${profile.name}")
        _toastEvents.tryEmit("Applied \"${profile.name}\"")
    }

    fun saveCurrentAsProfile(name: String, author: String, notes: String) {
        val cfg = config.value
        viewModelScope.launch {
            dao.insertProfile(
                TunnelProfileEntity(
                    name = name.trim().ifBlank { "Custom Profile" },
                    author = author.trim().ifBlank { "Synax User" },
                    notes = notes.trim().ifBlank { cfg.methodDisplaySummary() },
                    protocol = cfg.protocol.name,
                    serverId = cfg.serverId,
                    customHost = cfg.customHost,
                    customPort = cfg.customPort,
                    useSniHost = cfg.useSniHost,
                    sniHost = cfg.sniHost,
                    usePayload = cfg.usePayload,
                    payload = cfg.payload,
                    usePayloadAfterTls = cfg.usePayloadAfterTls,
                    payloadAfterTls = cfg.payloadAfterTls,
                    useProxy = cfg.useProxy,
                    proxyHost = cfg.proxyHost,
                    proxyPort = cfg.proxyPort,
                    obfuscatedTlsCh = cfg.obfuscatedTlsCh,
                    tcpFastOpen = cfg.tcpFastOpen,
                    preferIpv6 = cfg.preferIpv6,
                    isPreset = false,
                ),
            )
            _toastEvents.tryEmit("Profile saved")
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            dao.deleteProfile(id)
            _toastEvents.tryEmit("Profile deleted")
        }
    }

    fun exportConfigToUri(uri: Uri, name: String, author: String, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val json = TunnelPreferencesRepository.exportToJson(
                    config = config.value,
                    profileName = name.ifBlank { "Synax Tunnel Export" },
                    author = author.ifBlank { "Synax Tunnel" },
                    notes = notes,
                )
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Unable to open destination file")
            }.onSuccess {
                _toastEvents.tryEmit("Exported .synax configuration file")
                TunnelStateHolder.log(LogLevel.INFO, "Configuration exported to file")
            }.onFailure { err ->
                _toastEvents.tryEmit("Export failed: ${err.localizedMessage}")
            }
        }
    }

    fun importConfigFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("Cannot read selected file")
                val (profile, importedConfig) = TunnelPreferencesRepository.importFromJson(text).getOrThrow()
                dao.insertProfile(profile)
                prefsRepo.saveConfig(importedConfig)
                profile.name
            }.onSuccess { importedName ->
                _toastEvents.tryEmit("Imported & applied \"$importedName\"")
                TunnelStateHolder.log(LogLevel.INFO, "Imported config profile: $importedName")
            }.onFailure { err ->
                _toastEvents.tryEmit("Invalid config file: ${err.localizedMessage}")
            }
        }
    }

    fun clearLogs() = TunnelStateHolder.clearLogs()

    fun clearSessionHistory() {
        viewModelScope.launch {
            dao.clearSessions()
            _toastEvents.tryEmit("Session history cleared")
        }
    }

    // --- Network Tools ---
    fun updateHostQuery(host: String) {
        _toolsState.value = _toolsState.value.copy(hostQuery = host)
    }

    fun updateHostPort(port: Int) {
        _toolsState.value = _toolsState.value.copy(hostPort = port)
    }

    fun updateHostMethod(method: String) {
        _toolsState.value = _toolsState.value.copy(hostMethod = method)
    }

    fun runHostCheck() {
        val state = _toolsState.value
        if (state.isCheckingHost) return
        viewModelScope.launch {
            _toolsState.value = state.copy(isCheckingHost = true, hostResult = null)
            val result = NetworkDiagnostics.checkHost(state.hostQuery, state.hostPort, state.hostMethod)
            _toolsState.value = _toolsState.value.copy(isCheckingHost = false, hostResult = result)
            TunnelStateHolder.log(
                LogLevel.INFO,
                "Host Check ${result.host}: HTTP=${result.httpStatusCode ?: "-"} TLS=${result.tlsVersion ?: "-"} (${result.rttMs}ms)",
            )
        }
    }

    fun updateScanHostQuery(host: String) {
        _toolsState.value = _toolsState.value.copy(scanHostQuery = host)
    }

    fun runPortScan() {
        val state = _toolsState.value
        if (state.isScanningPorts || state.scanHostQuery.isBlank()) return
        viewModelScope.launch {
            _toolsState.value = state.copy(isScanningPorts = true)
            val results = NetworkDiagnostics.scanPorts(state.scanHostQuery)
            _toolsState.value = _toolsState.value.copy(isScanningPorts = false, portScanResults = results)
        }
    }

    fun updateTargetIpPrefix(prefix: String) {
        _toolsState.value = _toolsState.value.copy(targetIpPrefix = prefix)
    }

    fun refreshIpInfo() {
        viewModelScope.launch {
            _toolsState.value = _toolsState.value.copy(isHuntingIp = true, ipHuntError = null)
            val local = NetworkDiagnostics.getLocalInterfaceIps()
            val pubResult = NetworkDiagnostics.fetchPublicIp()
            _toolsState.value = _toolsState.value.copy(
                localInterfaces = local,
                publicIp = pubResult.getOrNull(),
                ipHuntError = pubResult.exceptionOrNull()?.localizedMessage,
                isHuntingIp = false,
            )
        }
    }

    fun toggleExcludedPackage(packageName: String) {
        val current = config.value
        val nextSet = if (packageName in current.excludedPackages) {
            current.excludedPackages - packageName
        } else {
            current.excludedPackages + packageName
        }
        updateConfig(current.copy(excludedPackages = nextSet))
    }

    private suspend fun loadInstalledLauncherApps() = withContext(Dispatchers.IO) {
        val pm = getApplication<Application>().packageManager
        val selfPkg = getApplication<Application>().packageName
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }
        val apps = resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == selfPkg) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
                InstalledAppItem(packageName = pkg, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
        _installedApps.value = apps
    }
}
