package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

internal enum class SettingsAction {
    SHIELD, PASSWORDS, SEARCH_ENGINE, APPEARANCE, EXTENSIONS,
    SITE_PERMISSIONS, DOWNLOADS, LANGUAGE, CHILD_MODE, ABOUT
}
private data class SettingRow(
    val title: Int, val icon: String, val action: SettingsAction,
    val implemented: Boolean = false
)
private val settingsRows = listOf(
    SettingRow(R.string.settings_shield, EymenPaths.kalkan, SettingsAction.SHIELD, true),
    SettingRow(R.string.settings_passwords, EymenPaths.kilit, SettingsAction.PASSWORDS),
    SettingRow(R.string.menu_settings_search, EymenPaths.ara, SettingsAction.SEARCH_ENGINE, true),
    SettingRow(R.string.settings_appearance, EymenPaths.ay, SettingsAction.APPEARANCE, true),
    SettingRow(R.string.menu_extensions, EymenPaths.eklenti, SettingsAction.EXTENSIONS),
    SettingRow(R.string.settings_site_permissions, EymenPaths.site, SettingsAction.SITE_PERMISSIONS),
    SettingRow(R.string.downloads, EymenPaths.indir, SettingsAction.DOWNLOADS, true),
    SettingRow(R.string.settings_language, EymenPaths.cevir, SettingsAction.LANGUAGE),
    SettingRow(R.string.settings_child_mode, EymenPaths.cocuk, SettingsAction.CHILD_MODE),
    SettingRow(R.string.settings_about_update, EymenPaths.bilgi, SettingsAction.ABOUT, true)
)
@Composable
private fun SettingsText(
    value: String, size: Int, color: Color, modifier: Modifier = Modifier,
    strong: Boolean = false
) {
    BasicText(
        value, modifier, maxLines = 2, overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color, fontSize = size.sp,
            lineHeight = (size * 1.3f).sp,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal
        )
    )
}

/** T10 approved list layout, with real settings only and explicit sync limits. */
@Composable
internal fun SettingsScreen(
    globalShield: Boolean,
    searchEngine: String,
    dayMode: Boolean,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = settingsRows.filter { row ->
        query.isBlank() || stringResource(row.title).contains(query, ignoreCase = true)
    }
    Column(
        Modifier.fillMaxSize().background(T.gece_zemin)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                EymenVector(EymenPaths.geri, color = T.gece_yazi)
            }
            SettingsText(stringResource(R.string.settings), 24, T.gece_yazi, strong = true)
        }
        Row(
            Modifier.fillMaxWidth().height(48.dp)
                .background(T.gece_yuzey2, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EymenVector(EymenPaths.ara, color = T.gece_yaziIkincil, iconSize = 20.dp)
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query, onValueChange = { query = it },
                singleLine = true, modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = T.gece_yazi, fontSize = 16.sp),
                decorationBox = { inner ->
                    Box {
                        if (query.isBlank()) {
                            SettingsText(
                                stringResource(R.string.settings_search_hint),
                                16, T.gece_yaziIkincil
                            )
                        }
                        inner()
                    }
                }
            )
        }
        Row(
            Modifier.fillMaxWidth()
                .background(T.gece_yuzey1, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).background(
                    T.gece_yuzey3, RoundedCornerShape(22.dp)
                ), contentAlignment = Alignment.Center
            ) {
                SettingsText("E", 18, T.gece_yazi, strong = true)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                SettingsText(
                    stringResource(R.string.settings_sync_off),
                    15, T.gece_yazi, strong = true
                )
                SettingsText(
                    stringResource(R.string.settings_sync_pending),
                    13, T.gece_yaziIkincil
                )
            }
        }
        Column(
            Modifier.weight(1f)
                .background(T.gece_yuzey1, RoundedCornerShape(16.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 2.dp)
        ) {
            filtered.forEach { row ->
                val note = when (row.action) {
                    SettingsAction.SHIELD -> stringResource(
                        if (globalShield) R.string.settings_enabled else R.string.settings_disabled
                    )
                    SettingsAction.SEARCH_ENGINE -> searchEngine
                    SettingsAction.APPEARANCE -> stringResource(
                        if (dayMode) R.string.theme_day else R.string.theme_night
                    )
                    SettingsAction.ABOUT -> stringResource(R.string.settings_version_label)
                    else -> if (row.implemented) "" else stringResource(R.string.menu_not_available)
                }
                Row(
                    Modifier.fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(enabled = row.implemented) {
                            onAction(row.action)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EymenVector(
                        row.icon, color = T.gece_yaziIkincil, iconSize = 22.dp
                    )
                    Spacer(Modifier.width(14.dp))
                    SettingsText(
                        stringResource(row.title), 15,
                        if (row.implemented) T.gece_yazi else T.gece_yaziPasif,
                        modifier = Modifier.weight(1f), strong = true
                    )
                    SettingsText(note, 13, T.gece_yaziIkincil)
                }
            }
            if (filtered.isEmpty()) {
                SettingsText(
                    stringResource(R.string.settings_no_results),
                    14, T.gece_yaziIkincil, modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
