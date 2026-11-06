package com.eymen.browser.telefon

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

/** Only normal-tab navigation snapshots. Never accept private GeckoSession data. */
internal data class StoredNormalTab(
    val url: String?,
    val groupId: Long?,
    val selected: Boolean
)

internal data class StoredWorkspace(
    val groups: List<TabGroupPreview>,
    val tabs: List<StoredNormalTab>
)

/**
 * A bounded, app-private session index. It stores *URLs and group labels*,
 * not cookies, passwords, form values, screenshots or private sessions.
 * GeckoView remains the sole browser engine and creates fresh sessions
 * for restored URLs on launch (no promise of full Back/Forward history).
 */
internal class TabWorkspaceStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "eymen_browser_normal_workspace_v108", Context.MODE_PRIVATE
    )

    companion object {
        const val MAX_TABS = 32
        const val MAX_GROUPS = 20
        private const val MAX_JSON_CHARS = 128_000
        private const val MAX_URL_CHARS = 2048
        private const val VERSION = 1
        private const val KEY = "workspace"
    }

    internal fun safeHttpsUrl(candidate: String?): String? {
        if (candidate.isNullOrBlank() || candidate.length > MAX_URL_CHARS ||
            candidate.any { it.isWhitespace() || it.isISOControl() }
        ) return null
        return try {
            val parsed = URI(candidate)
            if (parsed.scheme != "https" || parsed.host.isNullOrBlank() ||
                parsed.userInfo != null || parsed.fragment != null
            ) null else candidate
        } catch (_: Exception) {
            null
        }
    }

    fun read(): StoredWorkspace? {
        val raw = preferences.getString(KEY, null) ?: return null
        if (raw.length > MAX_JSON_CHARS) return null
        return try {
            val root = JSONObject(raw)
            if (root.optInt("version", -1) != VERSION) return null
            val groupArray = root.getJSONArray("groups")
            val tabArray = root.getJSONArray("tabs")
            if (groupArray.length() > MAX_GROUPS || tabArray.length() > MAX_TABS) {
                return null
            }
            val ids = mutableSetOf<Long>()
            val groups = buildList {
                repeat(groupArray.length()) { index ->
                    val item = groupArray.getJSONObject(index)
                    val id = item.getLong("id")
                    val title = item.getString("title")
                    if (id <= 0L || !ids.add(id) ||
                        title.isBlank() || title.length > 32
                    ) return null
                    add(TabGroupPreview(id, title))
                }
            }
            val tabs = buildList {
                repeat(tabArray.length()) { index ->
                    val item = tabArray.getJSONObject(index)
                    val rawUrl = item.optString("url", "").takeIf { it.isNotEmpty() }
                    val url = safeHttpsUrl(rawUrl)
                    // Never silently load an unverified URL from persistent state.
                    if (rawUrl != null && url == null) return null
                    val groupId = item.optLong("groupId", 0L)
                        .takeIf { it > 0L && it in ids }
                    add(StoredNormalTab(
                        url = url,
                        groupId = groupId,
                        selected = item.optBoolean("selected", false)
                    ))
                }
            }
            StoredWorkspace(groups = groups, tabs = tabs)
        } catch (_: Exception) {
            // Corruption or truncated content must never crash app startup.
            null
        }
    }

    /**
     * MainActivity constructs the list exclusively from !privateMode tabs.
     * A second URL/size check prevents accident-prone future call sites.
     */
    fun write(workspace: StoredWorkspace, durable: Boolean = false) {
        if (workspace.groups.size > MAX_GROUPS || workspace.tabs.size > MAX_TABS) return
        val groups = JSONArray()
        for (group in workspace.groups) {
            if (group.id <= 0L || group.title.isBlank() || group.title.length > 32)
                return
            groups.put(JSONObject().put("id", group.id).put("title", group.title))
        }
        val ids = workspace.groups.map { it.id }.toSet()
        val tabs = JSONArray()
        for (tab in workspace.tabs) {
            val validated = safeHttpsUrl(tab.url)
            if (tab.url != null && validated == null) return
            tabs.put(
                JSONObject()
                    .put("url", validated ?: "")
                    .put("groupId", tab.groupId?.takeIf { it in ids } ?: 0L)
                    .put("selected", tab.selected)
            )
        }
        val raw = JSONObject()
            .put("version", VERSION)
            .put("groups", groups)
            .put("tabs", tabs)
            .toString()
        if (raw.length > MAX_JSON_CHARS) return
        val editor = preferences.edit().putString(KEY, raw)
        if (durable) editor.commit() else editor.apply()
    }
}
