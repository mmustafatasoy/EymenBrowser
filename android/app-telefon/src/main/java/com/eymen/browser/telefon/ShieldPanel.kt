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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

@Composable
private fun ShieldText(
    text: String, size: Int, color: Color,
    modifier: Modifier = Modifier,
    strong: Boolean = false
) {
    BasicText(
        text = text, modifier = modifier, maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color, fontSize = size.sp,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = (size * 1.33f).sp
        )
    )
}

/** T05 approved bottom sheet. Only Gecko-reported blocked events are counted. */
@Composable
internal fun ShieldPanel(
    dayMode: Boolean,
    host: String?,
    enabled: Boolean,
    pageBlocked: Int,
    tabBlocked: Int,
    adTrackerBlocked: Int,
    analyticsBlocked: Int,
    otherTrackerBlocked: Int,
    strictMode: Boolean,
    globalEnabled: Boolean,
    cosmeticEnabled: Boolean,
    cosmeticAvailable: Boolean,
    cookieReject: Boolean,
    fingerprint: Boolean,
    httpsOnly: Boolean,
    autoplay: Boolean,
    canToggle: Boolean,
    onDismiss: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onModeChange: (Boolean) -> Unit,
    onGlobalToggle: (Boolean) -> Unit,
    onCosmeticToggle: (Boolean) -> Unit,
    onPrivacyToggle: (PrivacyOption, Boolean) -> Unit,
    onManagePrivacy: () -> Unit,
    onSitePermissions: () -> Unit
) {
    val p = browserScreenPalette(dayMode)
    val warning = if (dayMode) T.gunduz_uyari else T.gece_uyari
    Box(Modifier.fillMaxSize().background(Color(0x993E4350))) {
        val close = stringResource(R.string.shield_close)
        val stateDescription = stringResource(if (enabled) R.string.shield_active else R.string.shield_inactive)
        val switchLabel = stringResource(R.string.shield_toggle)
        Box(
            Modifier.fillMaxSize().clickable(onClick = onDismiss)
                .semantics { contentDescription = close }
        )
        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(624.dp)
                .background(
                    p.surface,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                Modifier.width(40.dp).height(4.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(p.line, RoundedCornerShape(2.dp))
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(52.dp)
                        .background(p.soft, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    EymenVector(
                        EymenPaths.kalkan,
                        color = if (enabled) p.protection else warning,
                        iconSize = 30.dp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ShieldText(
                        stringResource(
                            if (enabled) R.string.shield_active else R.string.shield_inactive
                        ), 20, p.text, strong = true
                    )
                    ShieldText(
                        host ?: stringResource(R.string.shield_no_site),
                        14, p.muted
                    )
                }
                Box(
                    Modifier.width(52.dp).height(32.dp)
                        .background(
                            if (enabled) p.accent else p.line,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = canToggle) { onToggle(!enabled) }
                        .semantics {
                            role = Role.Switch
                            contentDescription = "$switchLabel · $stateDescription"
                        }
                        .padding(4.dp),
                    contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        Modifier.size(24.dp).background(
                            if (enabled) p.onAccent else p.text,
                            RoundedCornerShape(12.dp)
                        )
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(pageBlocked.toString(), R.string.shield_page_blocks, true),
                    Triple(tabBlocked.toString(), R.string.shield_tab_blocks, true),
                    Triple(adTrackerBlocked.toString(), R.string.shield_ads_trackers, true)
                ).forEach { (number, label, available) ->
                    Column(
                        Modifier.weight(1f)
                            .background(p.soft, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        ShieldText(
                            number, 26,
                            if (available) p.text else p.disabled,
                            strong = true
                        )
                        Spacer(Modifier.height(2.dp))
                        ShieldText(
                            stringResource(label), 13, p.muted
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    .background(p.soft, RoundedCornerShape(14.dp))
                    .clickable { onGlobalToggle(!globalEnabled) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShieldText(
                    stringResource(R.string.settings_shield), 15, p.text,
                    modifier = Modifier.weight(1f), strong = true
                )
                ShieldText(
                    stringResource(
                        if (globalEnabled) R.string.settings_enabled
                        else R.string.settings_disabled
                    ), 14, if (globalEnabled) p.protection else warning
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ShieldText(
                    stringResource(R.string.shield_profile_title),
                    16, p.text, strong = true
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        false to R.string.shield_profile_standard,
                        true to R.string.shield_profile_strict
                    ).forEach { (isStrict, label) ->
                        Box(
                            Modifier.weight(1f).heightIn(min = 44.dp)
                                .background(
                                    if (strictMode == isStrict) p.accent
                                    else p.soft,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onModeChange(isStrict) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ShieldText(
                                stringResource(label), 14,
                                if (strictMode == isStrict) p.onAccent
                                else p.text,
                                strong = strictMode == isStrict
                            )
                        }
                    }
                }
                ShieldText(
                    stringResource(
                        if (strictMode) R.string.shield_profile_strict_desc
                        else R.string.shield_profile_standard_desc
                    ), 12, p.muted
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    analyticsBlocked to R.string.shield_analytics_trackers,
                    otherTrackerBlocked to R.string.shield_other_trackers
                ).forEach { (count, label) ->
                    Column(
                        Modifier.weight(1f)
                            .background(p.soft, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        ShieldText(count.toString(), 22, p.text, strong = true)
                        ShieldText(stringResource(label), 12, p.muted)
                    }
                }
            }
            ShieldText(
                stringResource(R.string.shield_count_scope), 12, p.muted
            )
            Column(
                Modifier.fillMaxWidth().background(
                    p.soft, RoundedCornerShape(14.dp)
                ).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(enabled = cosmeticAvailable) {
                            onCosmeticToggle(!cosmeticEnabled)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        ShieldText(
                            stringResource(R.string.shield_cosmetic_title),
                            15, p.text, strong = true
                        )
                        ShieldText(
                            stringResource(
                                if (cosmeticAvailable) R.string.shield_cosmetic_beta
                                else R.string.shield_cosmetic_unavailable
                            ),
                            12, p.muted
                        )
                    }
                    ShieldText(
                        stringResource(
                            if (cosmeticAvailable && cosmeticEnabled)
                                R.string.settings_enabled
                            else R.string.settings_disabled
                        ),
                        14,
                        if (cosmeticAvailable && cosmeticEnabled)
                            p.protection else p.disabled,
                        strong = true
                    )
                }
                ShieldText(
                    stringResource(R.string.shield_cosmetic_scope),
                    12, p.muted
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_kucukBosluk.dp)) {
                listOf(
                    Triple(PrivacyOption.COOKIE_REJECT, R.string.shield_cookie_dialog, R.string.shield_cookie_dialog_desc),
                    Triple(PrivacyOption.FINGERPRINT, R.string.shield_fingerprint, R.string.shield_fingerprint_desc),
                    Triple(PrivacyOption.HTTPS, R.string.shield_https, R.string.shield_https_desc),
                    Triple(PrivacyOption.AUTOPLAY, R.string.shield_autoplay, R.string.shield_autoplay_desc)
                ).forEach { (option, title, subtitle) ->
                    val chosen = when (option) {
                        PrivacyOption.COOKIE_REJECT -> cookieReject
                        PrivacyOption.FINGERPRINT -> fingerprint
                        PrivacyOption.HTTPS -> httpsOnly
                        else -> autoplay
                    }
                    SettingsToggle(stringResource(title), stringResource(subtitle), chosen, p) { onPrivacyToggle(option, it) }
                }
                ScreenButton(stringResource(R.string.cookie_policy_title), p, onManagePrivacy)
                ScreenButton(stringResource(R.string.settings_site_permissions), p, onSitePermissions)
            }
            ShieldText(
                stringResource(R.string.shield_scope_explanation),
                13, p.muted
            )
        }
    }
}
