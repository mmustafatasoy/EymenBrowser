package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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

@Composable
private fun VpnText(
    content: String, size: Int = 14, bold: Boolean = false,
    color: Color = T.gece_yazi, modifier: Modifier = Modifier
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
    val fill = if (prominent && enabled) T.gece_vurgu else T.gece_yuzey2
    val foreground = if (prominent && enabled) T.gece_vurguUstu
        else if (enabled) T.gece_yazi else T.gece_yaziPasif
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

/** A real VPN profile screen, separate from cosmetic/tracker blocking. */
@Composable
internal fun VpnSettingsScreen(
    status: EymenVpnStatus,
    configPresent: Boolean,
    message: String,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    val title = when (status) {
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
    val busy = status == EymenVpnStatus.CONNECTING ||
        status == EymenVpnStatus.DISCONNECTING

    Column(
        Modifier.fillMaxSize().background(T.gece_zemin)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                EymenVector(EymenPaths.geri, color = T.gece_yazi)
            }
            VpnText(stringResource(R.string.vpn_title), 21, true)
        }
        VpnText(stringResource(R.string.vpn_subtitle), 14, color = T.gece_yaziIkincil)
        Column(
            Modifier.fillMaxWidth()
                .background(T.gece_yuzey1, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp)
                        .background(T.gece_yuzey3, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    EymenVector(
                        EymenPaths.kalkan,
                        color = if (status == EymenVpnStatus.HANDSHAKE_CONFIRMED)
                            T.gece_koruma else T.gece_yaziIkincil,
                        iconSize = 30.dp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    VpnText(stringResource(title), 17, true)
                    VpnText(
                        if (configPresent) stringResource(R.string.vpn_saved_crypt)
                        else stringResource(R.string.vpn_status_none),
                        12, color = T.gece_yaziIkincil
                    )
                }
            }
            if (message.isNotBlank()) {
                VpnText(message, 13, color = T.gece_yaziIkincil)
            }
        }
        VpnAction(
            stringResource(R.string.vpn_import),
            enabled = !busy && !tunnelActive,
            prominent = !configPresent,
            onClick = onImport
        )
        VpnAction(
            stringResource(R.string.vpn_connect),
            enabled = configPresent && !busy && !tunnelActive,
            prominent = configPresent && !tunnelActive,
            onClick = onConnect
        )
        if (tunnelActive || status == EymenVpnStatus.CONNECTING) {
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
            enabled = configPresent && !busy && !tunnelActive,
            prominent = false, onClick = onDelete
        )
        Column(
            Modifier.fillMaxWidth()
                .background(T.gece_yuzey1, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VpnText(stringResource(R.string.vpn_no_fake), 13, color = T.gece_yaziIkincil)
            VpnText(stringResource(R.string.vpn_dual_stack), 13, color = T.gece_yaziIkincil)
            VpnText(stringResource(R.string.vpn_privacy_note), 13, color = T.gece_yaziIkincil)
        }
    }
}
