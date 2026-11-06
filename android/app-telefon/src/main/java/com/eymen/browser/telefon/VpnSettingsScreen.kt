package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

private val LocalVpnColors = staticCompositionLocalOf { browserScreenPalette(false) }

@Composable
private fun VpnText(
    content: String, size: Int = 14, bold: Boolean = false,
    color: Color = LocalVpnColors.current.text, modifier: Modifier = Modifier
) {
    BasicText(
        content,
        modifier = modifier,
        style = TextStyle(
            color = color, fontSize = size.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = (size * 1.35f).sp
        )
    )
}

@Composable
private fun VpnAction(
    caption: String,
    enabled: Boolean,
    prominent: Boolean,
    onClick: () -> Unit
) {
    val p = LocalVpnColors.current
    val fill = if (prominent && enabled) p.accent else p.soft
    val foreground = if (prominent && enabled) p.onAccent
        else if (enabled) p.text else p.disabled
    Box(
        Modifier.fillMaxWidth().heightIn(min = 50.dp)
            .background(fill, RoundedCornerShape(15.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        VpnText(caption, 15, true, foreground)
    }
}

private fun vpnBytesLabel(value: Long): String = when {
    value >= 1_048_576 -> String.format(
        java.util.Locale.ROOT, "%.1f MiB", value / 1_048_576.0
    )
    value >= 1024 -> String.format(
        java.util.Locale.ROOT, "%.1f KiB", value / 1024.0
    )
    else -> "$value B"
}

/** A real VPN profile screen, separate from cosmetic/tracker blocking. */
@Composable
private fun VpnSettingsContent(
    status: EymenVpnStatus,
    configPresent: Boolean,
    importing: Boolean,
    peerStats: EymenVpnPeerStats?,
    message: String,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    val p = LocalVpnColors.current
    val title = if (importing) R.string.vpn_importing else when (status) {
        EymenVpnStatus.NO_CONFIGURATION -> R.string.vpn_status_none
        EymenVpnStatus.READY -> R.string.vpn_status_ready
        EymenVpnStatus.CONNECTING -> R.string.vpn_status_connecting
        EymenVpnStatus.TUNNEL_UNVERIFIED -> R.string.vpn_status_unverified
        EymenVpnStatus.HANDSHAKE_CONFIRMED -> R.string.vpn_status_verified
        EymenVpnStatus.DISCONNECTING -> R.string.vpn_status_stopping
        EymenVpnStatus.DISCONNECTED -> R.string.vpn_status_off
        EymenVpnStatus.ERROR -> R.string.vpn_status_error
    }
    val tunnelActive = status == EymenVpnStatus.TUNNEL_UNVERIFIED ||
        status == EymenVpnStatus.HANDSHAKE_CONFIRMED
    val busy = importing || status == EymenVpnStatus.CONNECTING ||
        status == EymenVpnStatus.DISCONNECTING
    // ERROR can happen after Android created the TUN; always allow a
    // disconnect/recovery path before changing or deleting credentials.
    val mayNeedDisconnect = tunnelActive ||
        status == EymenVpnStatus.CONNECTING ||
        status == EymenVpnStatus.ERROR

    Column(
        Modifier.fillMaxSize().background(p.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                EymenVector(EymenPaths.geri, color = p.text)
            }
            VpnText(stringResource(R.string.vpn_title), 21, true)
        }
        VpnText(stringResource(R.string.vpn_subtitle), 14, color = p.muted)
        Column(
            Modifier.fillMaxWidth()
                .background(p.surface, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp)
                        .background(p.soft, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    EymenVector(
                        EymenPaths.kalkan,
                        color = if (status == EymenVpnStatus.HANDSHAKE_CONFIRMED)
                            p.protection else p.muted,
                        iconSize = 30.dp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    VpnText(stringResource(title), 17, true)
                    VpnText(
                        if (configPresent) stringResource(R.string.vpn_saved_crypt)
                        else stringResource(R.string.vpn_status_none),
                        12, color = p.muted
                    )
                }
            }
            if (message.isNotBlank()) {
                VpnText(message, 13, color = p.muted)
            }
        }
        if (tunnelActive && peerStats != null) {
            Column(
                Modifier.fillMaxWidth()
                    .background(p.surface, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        VpnText(stringResource(R.string.vpn_received), 12,
                            color = p.muted)
                        VpnText(vpnBytesLabel(peerStats.receivedBytes), 17, true)
                    }
                    Column(Modifier.weight(1f)) {
                        VpnText(stringResource(R.string.vpn_sent), 12,
                            color = p.muted)
                        VpnText(vpnBytesLabel(peerStats.sentBytes), 17, true)
                    }
                }
                val handshakeLabel = peerStats.lastHandshakeAgeSeconds?.let { age ->
                    "$age " + stringResource(R.string.vpn_seconds_ago)
                } ?: stringResource(R.string.vpn_never)
                VpnText(
                    stringResource(R.string.vpn_last_handshake) + ": " + handshakeLabel,
                    13, color = p.muted
                )
                VpnText(stringResource(R.string.vpn_stats_note), 12,
                    color = p.muted)
            }
        }
        VpnAction(
            stringResource(R.string.vpn_import),
            enabled = !busy && !mayNeedDisconnect,
            prominent = !configPresent,
            onClick = onImport
        )
        VpnAction(
            stringResource(R.string.vpn_connect),
            enabled = configPresent && !busy && !mayNeedDisconnect,
            prominent = configPresent && !tunnelActive,
            onClick = onConnect
        )
        if (mayNeedDisconnect) {
            VpnAction(
                stringResource(R.string.vpn_disconnect),
                enabled = status != EymenVpnStatus.DISCONNECTING,
                prominent = false, onClick = onDisconnect
            )
            VpnAction(
                stringResource(R.string.vpn_refresh),
                enabled = tunnelActive,
                prominent = false, onClick = onRefresh
            )
        }
        VpnAction(
            stringResource(R.string.vpn_remove),
            enabled = configPresent && !busy && !mayNeedDisconnect,
            prominent = false, onClick = onDelete
        )
        Column(
            Modifier.fillMaxWidth()
                .background(p.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VpnText(stringResource(R.string.vpn_no_fake), 13, color = p.muted)
            VpnText(stringResource(R.string.vpn_monitoring), 13, color = p.muted)
            VpnText(stringResource(R.string.vpn_always_on_unavailable), 13, color = p.muted)
            VpnText(stringResource(R.string.vpn_dual_stack), 13, color = p.muted)
            VpnText(stringResource(R.string.vpn_privacy_note), 13, color = p.muted)
        }
    }
}

@Composable
internal fun VpnSettingsScreen(
    status: EymenVpnStatus, configPresent: Boolean, importing: Boolean,
    peerStats: EymenVpnPeerStats?, message: String, dayMode: Boolean = false,
    onBack: () -> Unit, onImport: () -> Unit, onConnect: () -> Unit,
    onDisconnect: () -> Unit, onRefresh: () -> Unit, onDelete: () -> Unit
) {
    CompositionLocalProvider(LocalVpnColors provides browserScreenPalette(dayMode)) {
        VpnSettingsContent(status, configPresent, importing, peerStats, message,
            onBack, onImport, onConnect, onDisconnect, onRefresh, onDelete)
    }
}
