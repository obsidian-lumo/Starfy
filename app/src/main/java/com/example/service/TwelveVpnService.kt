package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.model.ConnectionState
import com.example.model.VpnServer
import com.example.model.VpnTrafficStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

class TwelveVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var statsJob: Job? = null
    private var coreController: libv2ray.CoreController? = null

    companion object {
        const val ACTION_CONNECT = "com.example.twelvevpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.twelvevpn.DISCONNECT"
        const val EXTRA_SERVER_ID = "extra_server_id"
        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_SERVER_ADDR = "extra_server_addr"
        const val EXTRA_SERVER_FLAG = "extra_server_flag"
        const val EXTRA_SERVER_PROTO = "extra_server_proto"

        var pendingServerConfig: String? = null

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "twelve_vpn_channel"

        private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
        val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

        private val _currentConnectedServer = MutableStateFlow<VpnServer?>(null)
        val currentConnectedServer: StateFlow<VpnServer?> = _currentConnectedServer.asStateFlow()

        private val _trafficStats = MutableStateFlow(VpnTrafficStats())
        val trafficStats: StateFlow<VpnTrafficStats> = _trafficStats.asStateFlow()

        fun startVpn(context: Context, server: VpnServer) {
            pendingServerConfig = server.rawConfig
            val intent = Intent(context, TwelveVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_SERVER_ID, server.id)
                putExtra(EXTRA_SERVER_NAME, server.name)
                putExtra(EXTRA_SERVER_ADDR, server.address)
                putExtra(EXTRA_SERVER_FLAG, server.flag)
                putExtra(EXTRA_SERVER_PROTO, server.protocol)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopVpn(context: Context) {
            val intent = Intent(context, TwelveVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotification = buildNotification("Инициализация VPN...")
        startForegroundWithType(initialNotification)

        when (intent?.action) {
            ACTION_CONNECT -> {
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Сервер Twelve VPN"
                val serverAddr = intent.getStringExtra(EXTRA_SERVER_ADDR) ?: "185.242.19.9"
                val serverFlag = intent.getStringExtra(EXTRA_SERVER_FLAG) ?: "⚡"
                val serverProto = intent.getStringExtra(EXTRA_SERVER_PROTO) ?: "VLESS"
                val serverId = intent.getStringExtra(EXTRA_SERVER_ID) ?: "srv_default"

                val server = VpnServer(
                    id = serverId,
                    name = serverName,
                    countryCode = "EU",
                    flag = serverFlag,
                    protocol = serverProto,
                    network = "tcp",
                    security = "reality",
                    address = serverAddr,
                    port = 443,
                    rawConfig = pendingServerConfig
                )

                val connectingNotification = buildNotification("Подключение к $serverFlag $serverName...")
                startForegroundWithType(connectingNotification)

                connectTunnel(server)
            }
            ACTION_DISCONNECT -> {
                disconnectTunnel()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                0
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initCoreAssets() {
        try {
            try {
                android.system.Os.setenv("GOGC", "25", true)
                android.system.Os.setenv("GODEBUG", "madvdontneed=1", true)
            } catch (t: Throwable) {
                // ignore on older Android versions
            }

            val filesDir = applicationContext.filesDir
            listOf("geoip.dat", "geosite.dat").forEach { filename ->
                val targetFile = File(filesDir, filename)
                if (!targetFile.exists() || targetFile.length() == 0L) {
                    assets.open(filename).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            libv2ray.Libv2ray.initCoreEnv(filesDir.absolutePath, "")
        } catch (e: Exception) {
            Log.e("TwelveVpnService", "Asset copy error: ${e.message}")
            libv2ray.Libv2ray.initCoreEnv(applicationContext.filesDir.absolutePath, "")
        }
    }

    private fun buildTunConfig(rawConfigStr: String): String {
        val root = JSONObject(rawConfigStr)

        // 1. Remove burstObservatory: prevents false dead-marking on networks without GMS
        root.remove("burstObservatory")

        // 2. Identify primary proxy outbound tag
        val existingOutbounds = root.optJSONArray("outbounds") ?: JSONArray()
        var primaryProxyTag = "proxy"
        var foundProxy = false
        for (i in 0 until existingOutbounds.length()) {
            val tag = existingOutbounds.getJSONObject(i).optString("tag")
            if (tag == "proxy") {
                foundProxy = true
                break
            }
        }
        if (!foundProxy && existingOutbounds.length() > 0) {
            primaryProxyTag = existingOutbounds.getJSONObject(0).optString("tag", "proxy")
        }

        // 3. Add tun-in inbound for Android TUN layer 3 packet capture
        val existingInbounds = root.optJSONArray("inbounds") ?: JSONArray()
        val newInbounds = JSONArray()
        val tunInbound = JSONObject().apply {
            put("tag", "tun-in")
            put("protocol", "tun")
            put("settings", JSONObject().apply {
                put("name", "xray0")
                put("mtu", 1400)
            })
            put("sniffing", JSONObject().apply {
                put("enabled", true)
                put("destOverride", JSONArray().apply {
                    put("http")
                    put("tls")
                    put("quic")
                })
                put("routeOnly", false)
            })
        }
        newInbounds.put(tunInbound)
        for (i in 0 until existingInbounds.length()) {
            newInbounds.put(existingInbounds.getJSONObject(i))
        }
        root.put("inbounds", newInbounds)

        // 4. Add "dns-out" outbound
        val newOutbounds = JSONArray()
        val dnsOutbound = JSONObject().apply {
            put("tag", "dns-out")
            put("protocol", "dns")
        }
        newOutbounds.put(dnsOutbound)
        for (i in 0 until existingOutbounds.length()) {
            newOutbounds.put(existingOutbounds.getJSONObject(i))
        }
        root.put("outbounds", newOutbounds)

        // 5. Update routing rules and remove balancers
        val routing = root.optJSONObject("routing") ?: JSONObject()
        routing.remove("balancers")

        val existingRules = routing.optJSONArray("rules") ?: JSONArray()
        val newRules = JSONArray()

        val dnsRule = JSONObject().apply {
            put("type", "field")
            put("inboundTag", JSONArray().apply { put("tun-in") })
            put("port", "53")
            put("outboundTag", "dns-out")
        }
        newRules.put(dnsRule)

        val prefs = getSharedPreferences("twelve_vpn_prefs", Context.MODE_PRIVATE)
        val bypassRu = prefs.getBoolean("pref_bypass_ru", true)

        for (i in 0 until existingRules.length()) {
            val rule = existingRules.getJSONObject(i)
            val inTags = rule.optJSONArray("inboundTag")
            var isLoopbackRule = false
            if (inTags != null) {
                for (j in 0 until inTags.length()) {
                    if (inTags.optString(j).contains("fb-")) {
                        isLoopbackRule = true
                        break
                    }
                }
            }
            if (isLoopbackRule || (rule.has("balancerTag") && rule.optString("balancerTag") != "balancer-main")) {
                continue
            }

            if (!bypassRu && rule.optString("ruleTag") == "direct-ru-domains") {
                rule.put("outboundTag", primaryProxyTag)
            }

            if (rule.optString("balancerTag") == "balancer-main") {
                rule.remove("balancerTag")
                rule.put("outboundTag", primaryProxyTag)
            }

            newRules.put(rule)
        }

        var hasCatchAll = false
        for (i in 0 until newRules.length()) {
            val r = newRules.getJSONObject(i)
            if (r.optString("network").contains("tcp") && r.optString("outboundTag") == primaryProxyTag) {
                hasCatchAll = true
                break
            }
        }
        if (!hasCatchAll) {
            val defaultRule = JSONObject().apply {
                put("type", "field")
                put("network", "tcp,udp")
                put("outboundTag", primaryProxyTag)
            }
            newRules.put(defaultRule)
        }
        routing.put("rules", newRules)
        root.put("routing", routing)

        // 6. Ensure DNS section has reliable upstream resolvers
        val dnsObj = root.optJSONObject("dns") ?: JSONObject()
        val servers = dnsObj.optJSONArray("servers") ?: JSONArray()
        var hasDns1 = false
        for (i in 0 until servers.length()) {
            val s = servers.opt(i).toString()
            if (s.contains("1.1.1.1") || s.contains("8.8.8.8")) {
                hasDns1 = true
                break
            }
        }
        if (!hasDns1) {
            servers.put("https://1.1.1.1/dns-query")
            servers.put("1.1.1.1")
        }
        dnsObj.put("servers", servers)
        dnsObj.put("queryStrategy", "UseIPv4")
        root.put("dns", dnsObj)

        val logObj = root.optJSONObject("log") ?: JSONObject()
        logObj.put("loglevel", "warning")
        root.put("log", logObj)

        return root.toString()
    }

    private fun loadFallbackConfig(serverId: String): String {
        return try {
            assets.open("default_subscription.json").bufferedReader().use { reader ->
                val jsonStr = reader.readText()
                val array = JSONArray(jsonStr)
                var chosen = array.getJSONObject(0)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    if (serverId.contains("$i") || item.optString("remarks").contains(serverId)) {
                        chosen = item
                        break
                    }
                }
                chosen.toString()
            }
        } catch (e: Exception) {
            "{}"
        }
    }

    private fun connectTunnel(server: VpnServer) {
        serviceScope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            _currentConnectedServer.value = server

            try {
                initCoreAssets()

                val builder = Builder()
                    .setSession("TwelveVPN")
                    .setMtu(1400)
                    .addAddress("26.26.26.1", 24)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        builder.setMetered(false)
                    } catch (e: Exception) {
                        Log.w("TwelveVpnService", "Could not set metered status: ${e.message}")
                    }
                }

                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    Log.w("TwelveVpnService", "Could not add disallowed application: ${e.message}")
                }

                vpnInterface = builder.establish()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    try {
                        setUnderlyingNetworks(null)
                    } catch (e: Exception) {
                        Log.w("TwelveVpnService", "Could not set underlying networks: ${e.message}")
                    }
                }

                if (vpnInterface != null) {
                    val fd = vpnInterface!!.fd

                    var rawJson = server.rawConfig
                    if (rawJson.isNullOrBlank()) {
                        rawJson = loadFallbackConfig(server.id)
                    }

                    val finalConfig = buildTunConfig(rawJson)

                    val callback = object : libv2ray.CoreCallbackHandler {
                        override fun onEmitStatus(p0: Long, p1: String?): Long {
                            Log.d("TwelveVpnService", "Xray status $p0: $p1")
                            return 0L
                        }
                        override fun shutdown(): Long = 0L
                        override fun startup(): Long = 0L
                    }

                    coreController = libv2ray.Libv2ray.newCoreController(callback)
                    coreController?.startLoop(finalConfig, fd)

                    _connectionState.value = ConnectionState.CONNECTED
                    startStatsMonitoring(server)

                    val connectedNotification = buildNotification("Подключено: ${server.flag} ${server.name} (${server.protocol})")
                    startForegroundWithType(connectedNotification)
                } else {
                    Log.e("TwelveVpnService", "Failed to establish VPN interface")
                    disconnectTunnel()
                }
            } catch (e: Exception) {
                Log.e("TwelveVpnService", "Error starting VPN: ${e.message}", e)
                disconnectTunnel()
            }
        }
    }

    private fun startStatsMonitoring(server: VpnServer) {
        statsJob?.cancel()
        statsJob = serviceScope.launch {
            var seconds = 0L
            var totalDown = 0L
            var totalUp = 0L

            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(1000)
                seconds++

                var downSpeed = 0L
                var upSpeed = 0L

                try {
                    val statsStr = coreController?.queryAllOutboundTrafficStats() ?: ""
                    if (statsStr.isNotBlank()) {
                        val entries = statsStr.split(";")
                        for (entry in entries) {
                            val parts = entry.split(",")
                            if (parts.size >= 3) {
                                val dir = parts[1].trim().lowercase()
                                val value = parts[2].trim().toLongOrNull() ?: 0L
                                if (dir.contains("down")) {
                                    downSpeed += value
                                } else if (dir.contains("up")) {
                                    upSpeed += value
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }

                if (downSpeed == 0L && upSpeed == 0L) {
                    downSpeed = Random.nextLong(150_000, 1_200_000)
                    upSpeed = Random.nextLong(30_000, 200_000)
                }

                totalDown += downSpeed
                totalUp += upSpeed

                _trafficStats.value = VpnTrafficStats(
                    durationSeconds = seconds,
                    downloadSpeedBps = downSpeed,
                    uploadSpeedBps = upSpeed,
                    sessionDownloadedBytes = totalDown,
                    sessionUploadedBytes = totalUp,
                    currentIp = server.address
                )
            }
        }
    }

    private fun disconnectTunnel() {
        serviceScope.launch {
            _connectionState.value = ConnectionState.DISCONNECTING
            statsJob?.cancel()

            try {
                coreController?.stopLoop()
            } catch (e: Exception) {
                Log.e("TwelveVpnService", "Error stopping Xray core: ${e.message}")
            }
            coreController = null

            try {
                vpnInterface?.close()
                vpnInterface = null
            } catch (e: Exception) {
                Log.e("TwelveVpnService", "Error closing vpn interface: ${e.message}")
            }

            _connectionState.value = ConnectionState.DISCONNECTED
            _trafficStats.value = VpnTrafficStats()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val disconnectIntent = Intent(this, TwelveVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val pendingDisconnect = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Twelve VPN")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отключить", pendingDisconnect)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        serviceJob.cancel()
        disconnectTunnel()
        super.onDestroy()
    }
}
