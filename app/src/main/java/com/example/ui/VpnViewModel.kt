package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SubscriptionRepository
import com.example.model.AppSettings
import com.example.model.ConnectionState
import com.example.model.ServerCategory
import com.example.model.SubscriptionInfo
import com.example.model.VpnServer
import com.example.model.VpnTrafficStats
import com.example.service.TwelveVpnService
import com.example.ui.theme.AppThemeColor
import com.example.ui.theme.CornerStyle
import com.example.ui.theme.SpeedUnit
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class VpnViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("twelve_vpn_prefs", Context.MODE_PRIVATE)
    private val repository: SubscriptionRepository = SubscriptionRepository(application)

    companion object {
        private const val PREF_SUBSCRIPTION_URL = "pref_subscription_url"
        private const val PREF_THEME_COLOR = "pref_theme_color"
        private const val PREF_THEME_MODE = "pref_theme_mode"
        private const val PREF_BYPASS_RU = "pref_bypass_ru"
        private const val PREF_KILL_SWITCH = "pref_kill_switch"
        private const val PREF_AUTO_CONNECT = "pref_auto_connect"
        private const val PREF_DNS_PROVIDER = "pref_dns_provider"
        private const val PREF_PROTOCOL_MODE = "pref_protocol_mode"
        private const val PREF_NOTIF_ENABLED = "pref_notif_enabled"
        private const val PREF_AMOLED_MODE = "pref_amoled_mode"
        private const val PREF_DYNAMIC_COLOR = "pref_dynamic_color"
        private const val PREF_CORNER_STYLE = "pref_corner_style"
        private const val PREF_SPEED_UNIT = "pref_speed_unit"
        private const val PREF_HAPTIC = "pref_haptic"
        private const val PREF_SHOW_GRAPH = "pref_show_graph"

        private val EUROPE_CODES = setOf("AT", "DE", "NL", "PL", "LT", "SE", "EE", "EU", "GB", "FR", "CH")
    }

    val connectionState: StateFlow<ConnectionState> = TwelveVpnService.connectionState
    val trafficStats: StateFlow<VpnTrafficStats> = TwelveVpnService.trafficStats

    private val _servers = MutableStateFlow<List<VpnServer>>(emptyList())
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer.asStateFlow()

    private val _subscriptionInfo = MutableStateFlow(
        SubscriptionInfo(
            url = prefs.getString(PREF_SUBSCRIPTION_URL, "") ?: ""
        )
    )
    val subscriptionInfo: StateFlow<SubscriptionInfo> = _subscriptionInfo.asStateFlow()

    private val savedThemeMode = try {
        ThemeMode.valueOf(prefs.getString(PREF_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
    } catch (e: Exception) {
        ThemeMode.DARK
    }

    private val savedThemeColor = try {
        val raw = AppThemeColor.valueOf(
            prefs.getString(PREF_THEME_COLOR, AppThemeColor.CYAN.name) ?: AppThemeColor.CYAN.name
        )
        when (savedThemeMode) {
            ThemeMode.DARK -> raw.compatibleWith(isDarkTheme = true)
            ThemeMode.LIGHT -> raw.compatibleWith(isDarkTheme = false)
            ThemeMode.SYSTEM -> raw
        }
    } catch (e: Exception) {
        AppThemeColor.CYAN
    }

    private val savedCornerStyle = try {
        CornerStyle.valueOf(prefs.getString(PREF_CORNER_STYLE, CornerStyle.ROUNDED.name) ?: CornerStyle.ROUNDED.name)
    } catch (e: Exception) {
        CornerStyle.ROUNDED
    }

    private val savedSpeedUnit = try {
        SpeedUnit.valueOf(prefs.getString(PREF_SPEED_UNIT, SpeedUnit.BYTES.name) ?: SpeedUnit.BYTES.name)
    } catch (e: Exception) {
        SpeedUnit.BYTES
    }

    private val _appSettings = MutableStateFlow(
        AppSettings(
            bypassRussianDomains = prefs.getBoolean(PREF_BYPASS_RU, true),
            killSwitch = prefs.getBoolean(PREF_KILL_SWITCH, false),
            autoConnect = prefs.getBoolean(PREF_AUTO_CONNECT, false),
            dnsProvider = prefs.getString(PREF_DNS_PROVIDER, "Cloudflare (1.1.1.1)") ?: "Cloudflare (1.1.1.1)",
            protocolMode = prefs.getString(PREF_PROTOCOL_MODE, "Авто (Рекомендуется)") ?: "Авто (Рекомендуется)",
            notificationsEnabled = prefs.getBoolean(PREF_NOTIF_ENABLED, true),
            themeColor = savedThemeColor,
            themeMode = savedThemeMode,
            amoledMode = prefs.getBoolean(PREF_AMOLED_MODE, false),
            dynamicColor = prefs.getBoolean(PREF_DYNAMIC_COLOR, false),
            cornerStyle = savedCornerStyle,
            speedUnit = savedSpeedUnit,
            hapticFeedback = prefs.getBoolean(PREF_HAPTIC, true),
            showLiveGraph = prefs.getBoolean(PREF_SHOW_GRAPH, true)
        )
    )
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ServerCategory.ALL)
    val selectedCategory: StateFlow<ServerCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshingSub = MutableStateFlow(false)
    val isRefreshingSub: StateFlow<Boolean> = _isRefreshingSub.asStateFlow()

    private val _isTestingAllPings = MutableStateFlow(false)
    val isTestingAllPings: StateFlow<Boolean> = _isTestingAllPings.asStateFlow()

    val filteredServers: StateFlow<List<VpnServer>> = combine(
        _servers,
        _selectedCategory,
        _searchQuery
    ) { allServers, category, query ->
        val trimmedQuery = query.trim()
        allServers.filter { server ->
            val matchesCategory = when (category) {
                ServerCategory.ALL -> true
                ServerCategory.FAST -> server.category == ServerCategory.FAST || server.name.contains("⚡")
                ServerCategory.GAMING -> server.category == ServerCategory.GAMING || server.protocol.contains("Hysteria", ignoreCase = true)
                ServerCategory.LTE -> server.category == ServerCategory.LTE || server.name.contains("LTE", ignoreCase = true)
                ServerCategory.EUROPE -> server.category == ServerCategory.EUROPE || server.countryCode in EUROPE_CODES
                ServerCategory.USA -> server.category == ServerCategory.USA || server.countryCode == "US"
            }
            val matchesQuery = trimmedQuery.isEmpty() ||
                server.name.contains(trimmedQuery, ignoreCase = true) ||
                server.countryCode.contains(trimmedQuery, ignoreCase = true) ||
                server.protocol.contains(trimmedQuery, ignoreCase = true) ||
                server.address.contains(trimmedQuery, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val savedUrl = prefs.getString(PREF_SUBSCRIPTION_URL, "") ?: ""
        if (savedUrl.isNotBlank()) {
            refreshSubscription()
        }
    }

    fun selectServer(server: VpnServer) {
        _selectedServer.value = server
    }

    fun setCategory(category: ServerCategory) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSubscriptionUrl(newUrl: String) {
        val trimmed = newUrl.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString(PREF_SUBSCRIPTION_URL, trimmed).apply()
            _subscriptionInfo.value = _subscriptionInfo.value.copy(url = trimmed)
            refreshSubscription()
        }
    }

    fun removeSubscription() {
        prefs.edit().remove(PREF_SUBSCRIPTION_URL).apply()
        _subscriptionInfo.value = SubscriptionInfo(url = "")
        _servers.value = emptyList()
        _selectedServer.value = null
    }

    fun refreshSubscription() {
        val currentUrl = _subscriptionInfo.value.url.trim()
        if (currentUrl.isBlank()) return
        if (_isRefreshingSub.value) return

        viewModelScope.launch {
            _isRefreshingSub.value = true
            try {
                val (updatedInfo, updatedServers) = repository.fetchSubscription(currentUrl)
                _subscriptionInfo.value = updatedInfo
                if (updatedServers.isNotEmpty()) {
                    _servers.value = updatedServers
                    val currentSelected = _selectedServer.value
                    val stillExists = currentSelected?.let { sel ->
                        updatedServers.find { it.id == sel.id || it.address == sel.address }
                    }
                    _selectedServer.value = stillExists ?: updatedServers.first()
                }
            } catch (e: Exception) {
                _subscriptionInfo.value = _subscriptionInfo.value.copy(
                    errorMessage = e.localizedMessage
                )
            } finally {
                _isRefreshingSub.value = false
            }
        }
    }

    fun toggleVpn(context: Context) {
        val server = _selectedServer.value
        if (server == null && connectionState.value == ConnectionState.DISCONNECTED) {
            return
        }

        when (connectionState.value) {
            ConnectionState.CONNECTED -> {
                TwelveVpnService.stopVpn(context)
            }
            ConnectionState.DISCONNECTED -> {
                if (server != null) {
                    TwelveVpnService.startVpn(context, server)
                }
            }
            else -> {}
        }
    }

    fun testServerPing(server: VpnServer) {
        viewModelScope.launch {
            _servers.value = _servers.value.map {
                if (it.id == server.id) it.copy(isTestingPing = true) else it
            }
            val latency = repository.measurePing(server)
            _servers.value = _servers.value.map {
                if (it.id == server.id) it.copy(isTestingPing = false, pingMs = latency) else it
            }
            if (_selectedServer.value?.id == server.id) {
                _selectedServer.value = _selectedServer.value?.copy(pingMs = latency)
            }
        }
    }

    fun testAllPings() {
        if (_isTestingAllPings.value) return
        viewModelScope.launch {
            _isTestingAllPings.value = true
            try {
                val currentList = _servers.value
                val semaphore = Semaphore(6)
                val updatedMap = coroutineScope {
                    currentList.map { srv ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                srv.id to repository.measurePing(srv)
                            }
                        }
                    }.awaitAll().toMap()
                }

                _servers.value = _servers.value.map { srv ->
                    updatedMap[srv.id]?.let { srv.copy(pingMs = it, isTestingPing = false) } ?: srv
                }
                _selectedServer.value?.let { selected ->
                    updatedMap[selected.id]?.let { latency ->
                        _selectedServer.value = selected.copy(pingMs = latency)
                    }
                }
            } finally {
                _isTestingAllPings.value = false
            }
        }
    }

    fun setThemeColor(color: AppThemeColor) {
        updateSettings { it.copy(themeColor = color, dynamicColor = false) }
    }

    fun setThemeMode(mode: ThemeMode, systemIsDark: Boolean = true) {
        updateSettings { current ->
            val targetDark = when (mode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemIsDark
            }
            current.copy(
                themeMode = mode,
                themeColor = current.themeColor.compatibleWith(targetDark)
            )
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        updateSettings { it.copy(dynamicColor = enabled) }
    }

    fun setAmoledMode(enabled: Boolean) {
        updateSettings { it.copy(amoledMode = enabled) }
    }

    fun setCornerStyle(style: CornerStyle) {
        updateSettings { it.copy(cornerStyle = style) }
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        updateSettings { it.copy(speedUnit = unit) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        updateSettings { it.copy(hapticFeedback = enabled) }
    }

    fun setShowLiveGraph(enabled: Boolean) {
        updateSettings { it.copy(showLiveGraph = enabled) }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val newSettings = transform(_appSettings.value)
        _appSettings.value = newSettings
        prefs.edit()
            .putBoolean(PREF_BYPASS_RU, newSettings.bypassRussianDomains)
            .putBoolean(PREF_KILL_SWITCH, newSettings.killSwitch)
            .putBoolean(PREF_AUTO_CONNECT, newSettings.autoConnect)
            .putString(PREF_DNS_PROVIDER, newSettings.dnsProvider)
            .putString(PREF_PROTOCOL_MODE, newSettings.protocolMode)
            .putBoolean(PREF_NOTIF_ENABLED, newSettings.notificationsEnabled)
            .putString(PREF_THEME_COLOR, newSettings.themeColor.name)
            .putString(PREF_THEME_MODE, newSettings.themeMode.name)
            .putBoolean(PREF_AMOLED_MODE, newSettings.amoledMode)
            .putBoolean(PREF_DYNAMIC_COLOR, newSettings.dynamicColor)
            .putString(PREF_CORNER_STYLE, newSettings.cornerStyle.name)
            .putString(PREF_SPEED_UNIT, newSettings.speedUnit.name)
            .putBoolean(PREF_HAPTIC, newSettings.hapticFeedback)
            .putBoolean(PREF_SHOW_GRAPH, newSettings.showLiveGraph)
            .apply()
    }
}
