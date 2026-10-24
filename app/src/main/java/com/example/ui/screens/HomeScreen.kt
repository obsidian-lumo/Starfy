package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ConnectionState
import com.example.model.SubscriptionInfo
import com.example.model.VpnServer
import com.example.ui.VpnViewModel
import com.example.ui.theme.SpeedUnit
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusConnecting
import com.example.ui.util.FormatUtils

@Composable
fun HomeScreen(
    viewModel: VpnViewModel,
    onNavigateToServers: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onToggleConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val subscriptionInfo by viewModel.subscriptionInfo.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingSub.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var inputUrl by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!subscriptionInfo.isConfigured) {
            item(key = "onboarding_card", contentType = "onboarding") {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddLink,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Добро пожаловать в Twelve VPN",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Вставьте ссылку на вашу персональную подписку VLESS / Hysteria для загрузки серверов",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            placeholder = { Text("https://...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            inputUrl = clip.getItemAt(0).text?.toString()?.trim() ?: ""
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Вставить из буфера",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (inputUrl.isNotBlank()) {
                                    viewModel.setSubscriptionUrl(inputUrl.trim())
                                } else {
                                    Toast.makeText(context, "Вставьте ссылку подписки", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isRefreshing,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Загрузка...")
                            } else {
                                Text(
                                    "Загрузить серверы",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "hero_connect", contentType = "hero") {
            ConnectionHeroSection(
                connectionState = connectionState,
                isConfigured = subscriptionInfo.isConfigured && selectedServer != null,
                onToggle = {
                    if (subscriptionInfo.isConfigured && selectedServer != null) {
                        if (settings.hapticFeedback) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onToggleConnection()
                    } else {
                        Toast.makeText(context, "Сначала добавьте подписку выше", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        selectedServer?.let { srv ->
            item(key = "selected_server_${srv.id}", contentType = "selected_server") {
                SelectedServerCard(
                    server = srv,
                    onClick = onNavigateToServers
                )
            }
        }

        item(key = "live_traffic_section", contentType = "traffic") {
            AnimatedVisibility(
                visible = connectionState == ConnectionState.CONNECTED,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(140))
            ) {
                LiveTrafficSection(
                    viewModel = viewModel,
                    speedUnit = settings.speedUnit,
                    showLiveGraph = settings.showLiveGraph
                )
            }
        }

        if (subscriptionInfo.isConfigured) {
            item(key = "subscription_preview", contentType = "sub_preview") {
                SubscriptionPreviewCard(
                    subscriptionInfo = subscriptionInfo,
                    onClick = onNavigateToSubscription
                )
            }
        }

        if (subscriptionInfo.announcement.isNotBlank()) {
            item(key = "announcement_banner", contentType = "announcement") {
                AnnouncementBanner(announcement = subscriptionInfo.announcement)
            }
        }
    }
}

@Composable
private fun ConnectionHeroSection(
    connectionState: ConnectionState,
    isConfigured: Boolean,
    onToggle: () -> Unit
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    val isConnecting = connectionState == ConnectionState.CONNECTING

    val primaryStatusColor = when (connectionState) {
        ConnectionState.CONNECTED -> StatusConnected
        ConnectionState.CONNECTING -> StatusConnecting
        ConnectionState.DISCONNECTING -> StatusConnecting
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.primary
    }

    val radialBrush = remember(primaryStatusColor) {
        Brush.radialGradient(
            colors = listOf(
                primaryStatusColor.copy(alpha = 0.35f),
                Color.Transparent
            )
        )
    }

    val borderBrush = remember(primaryStatusColor) {
        Brush.linearGradient(
            colors = listOf(
                primaryStatusColor,
                primaryStatusColor.copy(alpha = 0.5f)
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = primaryStatusColor.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, primaryStatusColor.copy(alpha = 0.4f)),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(primaryStatusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        !isConfigured -> "ТРЕБУЕТСЯ ПОДПИСКА"
                        connectionState == ConnectionState.CONNECTED -> "ЗАЩИЩЕНО • ПОДКЛЮЧЕНО"
                        connectionState == ConnectionState.CONNECTING -> "ПОДКЛЮЧЕНИЕ К СЕРВЕРУ…"
                        connectionState == ConnectionState.DISCONNECTING -> "ОТКЛЮЧЕНИЕ…"
                        else -> "НЕ ЗАЩИЩЕНО • ОТКЛЮЧЕНО"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = primaryStatusColor
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .testTag("vpn_toggle_button")
        ) {
            // Only animate on GPU RenderNode when actively connected/connecting; 0 idle CPU usage
            if (isConnected || isConnecting) {
                PulseHaloRing(color = primaryStatusColor)
            }

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(radialBrush)
            )

            Surface(
                onClick = onToggle,
                shape = CircleShape,
                color = if (isConnected) StatusConnected else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(136.dp)
                    .border(
                        width = 3.dp,
                        brush = borderBrush,
                        shape = CircleShape
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            color = StatusConnecting,
                            modifier = Modifier.size(44.dp),
                            strokeWidth = 3.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Подключиться к VPN",
                            modifier = Modifier.size(54.dp),
                            tint = if (isConnected) Color.White else primaryStatusColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                !isConfigured -> "Добавьте ссылку на подписку для начала"
                connectionState == ConnectionState.CONNECTED -> "Нажмите кнопку, чтобы отключить VPN"
                connectionState == ConnectionState.CONNECTING -> "Установка защищённого соединения…"
                connectionState == ConnectionState.DISCONNECTING -> "Завершение сеанса…"
                else -> "Нажмите для мгновенного подключения"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PulseHaloRing(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .size(188.dp)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                alpha = pulseAlpha
                shape = CircleShape
                clip = true
            }
            .background(color)
    )
}

@Composable
private fun SelectedServerCard(
    server: VpnServer,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_server_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = server.flag, fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "${server.protocol} • ${server.security.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (server.trafficMultiplier != "x1") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = server.trafficMultiplier,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            server.pingMs?.let { ping ->
                val pingColor = when {
                    ping < 50 -> StatusConnected
                    ping < 90 -> StatusConnecting
                    else -> MaterialTheme.colorScheme.error
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = pingColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, pingColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(pingColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$ping ms",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = pingColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveTrafficSection(
    viewModel: VpnViewModel,
    speedUnit: SpeedUnit,
    showLiveGraph: Boolean
) {
    // Isolated collection: 1-second traffic ticks only recompose this child section, not HomeScreen LazyColumn
    val trafficStats by viewModel.trafficStats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showLiveGraph) {
            LiveSpeedWaveCard(
                speedBps = trafficStats.downloadSpeedBps,
                speedUnit = speedUnit
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TrafficMetricCard(
                icon = Icons.Default.ArrowDownward,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Загрузка",
                value = FormatUtils.formatSpeed(trafficStats.downloadSpeedBps, speedUnit),
                modifier = Modifier.weight(1f)
            )

            TrafficMetricCard(
                icon = Icons.Default.ArrowUpward,
                iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                title = "Отдача",
                value = FormatUtils.formatSpeed(trafficStats.uploadSpeedBps, speedUnit),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TrafficMetricCard(
                icon = Icons.Default.Schedule,
                iconTint = StatusConnecting,
                title = "Время сессии",
                value = FormatUtils.formatDuration(trafficStats.durationSeconds),
                modifier = Modifier.weight(1f)
            )

            TrafficMetricCard(
                icon = Icons.Default.CloudDownload,
                iconTint = StatusConnected,
                title = "Трафик сессии",
                value = FormatUtils.formatBytes(trafficStats.sessionDownloadedBytes + trafficStats.sessionUploadedBytes),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrafficMetricCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LiveSpeedWaveCard(
    speedBps: Long,
    speedUnit: SpeedUnit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "График скорости в реальном времени",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = FormatUtils.formatSpeed(speedBps, speedUnit),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SpeedWaveCanvas(
                isFlowing = speedBps > 0,
                primaryColor = primaryColor
            )
        }
    }
}

@Composable
private fun SpeedWaveCanvas(
    isFlowing: Boolean,
    primaryColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val reusablePath = remember { Path() }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val amplitude = (height / 2.8f) * (if (isFlowing) 1f else 0.25f)

        reusablePath.reset()
        val steps = 48
        val dx = width / steps

        for (i in 0..steps) {
            val x = i * dx
            val angle = (i.toFloat() / steps) * 4f * Math.PI.toFloat() + phase
            val y = midY + kotlin.math.sin(angle).toFloat() * amplitude
            if (i == 0) {
                reusablePath.moveTo(x, y)
            } else {
                reusablePath.lineTo(x, y)
            }
        }

        drawPath(
            path = reusablePath,
            color = primaryColor,
            style = Stroke(
                width = 2.5f.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
private fun SubscriptionPreviewCard(
    subscriptionInfo: SubscriptionInfo,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_preview_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subscriptionInfo.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusConnected.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Активна",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StatusConnected,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { subscriptionInfo.usageRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Использовано: ${FormatUtils.formatBytes(subscriptionInfo.usedBytes)} / ${FormatUtils.formatBytes(subscriptionInfo.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Осталось: ${FormatUtils.formatBytes(subscriptionInfo.remainingBytes)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AnnouncementBanner(announcement: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = announcement,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
