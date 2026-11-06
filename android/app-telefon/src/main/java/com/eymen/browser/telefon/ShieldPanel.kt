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
    host: String?,
    enabled: Boolean,
    pageBlocked: Int,
    tabBlocked: Int,
    adTrackerBlocked: Int,
    analyticsBlocked: Int,
    otherTrackerBlocked: Int,
    strictMode: Boolean,
    canToggle: Boolean,
    onDismiss: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onModeChange: (Boolean) -> Unit
) {
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
                    T.gece_yuzey1,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                Modifier.width(40.dp).height(4.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(T.gece_cizgi, RoundedCornerShape(2.dp))
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(52.dp)
                        .background(T.gece_yuzey2, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    EymenVector(
                        EymenPaths.kalkan,
                        color = if (enabled) T.gece_koruma else T.gece_uyari,
                        iconSize = 30.dp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ShieldText(
                        stringResource(
                            if (enabled) R.string.shield_active else R.string.shield_inactive
                        ), 20, T.gece_yazi, strong = true
                    )
                    ShieldText(
                        host ?: stringResource(R.string.shield_no_site),
                        14, T.gece_yaziIkincil
                    )
                }
                Box(
                    Modifier.width(52.dp).height(32.dp)
                        .background(
                            if (enabled) T.gece_vurgu else T.gece_cizgi,
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
                            if (enabled) T.gece_vurguUstu else T.gece_yazi,
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
                            .background(T.gece_yuzey2, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        ShieldText(
                            number, 26,
                            if (available) T.gece_yazi else T.gece_yaziPasif,
                            strong = true
                        )
                        Spacer(Modifier.height(2.dp))
                        ShieldText(
                            stringResource(label), 13, T.gece_yaziIkincil
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ShieldText(
                    stringResource(R.string.shield_profile_title),
                    16, T.gece_yazi, strong = true
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
                                    if (strictMode == isStrict) T.gece_vurgu
                                    else T.gece_yuzey2,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onModeChange(isStrict) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ShieldText(
                                stringResource(label), 14,
                                if (strictMode == isStrict) T.gece_vurguUstu
                                else T.gece_yazi,
                                strong = strictMode == isStrict
                            )
                        }
                    }
                }
                ShieldText(
                    stringResource(
                        if (strictMode) R.string.shield_profile_strict_desc
                        else R.string.shield_profile_standard_desc
                    ), 12, T.gece_yaziIkincil
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
                            .background(T.gece_yuzey2, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        ShieldText(count.toString(), 22, T.gece_yazi, strong = true)
                        ShieldText(stringResource(label), 12, T.gece_yaziIkincil)
                    }
                }
            }
            ShieldText(
                stringResource(R.string.shield_count_scope), 12, T.gece_yaziIkincil
            )
            Column {
                listOf(
                    R.string.shield_cookie_dialog to R.string.shield_cookie_dialog_desc,
                    R.string.shield_fingerprint to R.string.shield_fingerprint_desc,
                    R.string.shield_https to R.string.shield_https_desc,
                    R.string.shield_autoplay to R.string.shield_autoplay_desc
                ).forEach { (title, subtitle) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            ShieldText(
                                stringResource(title), 15, T.gece_yazi,
                                strong = true
                            )
                            ShieldText(
                                stringResource(subtitle), 13, T.gece_yaziIkincil
                            )
                        }
                        ShieldText(
                            stringResource(R.string.menu_not_available), 12,
                            T.gece_yaziPasif
                        )
                    }
                }
            }
            ShieldText(
                stringResource(R.string.shield_scope_explanation),
                13, T.gece_yaziIkincil
            )
        }
    }
}
