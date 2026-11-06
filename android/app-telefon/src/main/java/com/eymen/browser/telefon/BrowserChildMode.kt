package com.eymen.browser.telefon

import android.app.AlertDialog
import android.text.InputType
import android.util.Base64
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import java.security.SecureRandom

/** PIN gates changes and exit; enabled browsing uses private tabs and an HTTPS allowlist. */
internal class BrowserChildMode(private val activity: ComponentActivity, private val onChanged: () -> Unit) {
    private val prefs = activity.getSharedPreferences("eymen_child_mode", 0)
    var enabled by mutableStateOf(prefs.getBoolean("enabled", false)); private set
    val allowed = mutableStateListOf<String>().apply { addAll(prefs.getStringSet("hosts", emptySet()).orEmpty().sorted()) }
    var message by mutableStateOf(""); private set
    var busy by mutableStateOf(false); private set
    private var dialog: AlertDialog? = null
    val hasPin get() = prefs.contains("hash") && prefs.contains("salt")
    fun allows(url: String) = !enabled || ChildModePolicy.allows(url, allowed.toSet())

    fun configurePin() = parent { askNewPin() }
    fun toggle() {
        if (!hasPin) { askNewPin(); return }
        parent {
            if (!enabled && allowed.isEmpty()) { message = activity.getString(R.string.child_add_first); return@parent }
            enabled = !enabled; prefs.edit().putBoolean("enabled", enabled).commit(); onChanged()
        }
    }
    fun add(input: String) {
        val host = ChildModePolicy.host(input)
        if (host == null) { message = activity.getString(R.string.child_host_invalid); return }
        if (!hasPin) { message = activity.getString(R.string.child_set_pin_first); return }
        parent {
            if (allowed.size >= 100) return@parent
            if (host !in allowed) { allowed.add(host); saveHosts() }
        }
    }
    fun remove(host: String) = parent {
        allowed.remove(host); saveHosts()
        if (enabled) onChanged()
    }
    fun parent(done: () -> Unit) {
        if (!hasPin) { done(); return }
        if (busy) return
        if (System.currentTimeMillis() < prefs.getLong("locked_until", 0L)) {
            message = activity.getString(R.string.child_wait); return
        }
        val input = pinField()
        dialog = AlertDialog.Builder(activity).setTitle(R.string.child_parent_pin).setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val chars = input.text.toString().toCharArray(); input.text.clear()
                busy = true
                Thread {
                    val ok = try {
                        ChildModePolicy.matches(chars, Base64.decode(prefs.getString("salt", ""), Base64.NO_WRAP),
                            Base64.decode(prefs.getString("hash", ""), Base64.NO_WRAP))
                    } catch (_: Exception) { false }
                    activity.runOnUiThread {
                        busy = false
                        if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                        if (ok) {
                            prefs.edit().putInt("failures", 0).remove("locked_until").apply(); done()
                        } else {
                            val failures = prefs.getInt("failures", 0) + 1
                            prefs.edit().putInt("failures", if (failures >= 5) 0 else failures)
                                .putLong("locked_until", if (failures >= 5) System.currentTimeMillis() + 30_000 else 0).apply()
                            message = activity.getString(R.string.child_wrong_pin)
                        }
                    }
                }.start()
            }.setOnDismissListener { input.text.clear() }.show()
    }

    private fun askNewPin() {
        val first = pinField()
        dialog = AlertDialog.Builder(activity).setTitle(R.string.child_set_pin).setMessage(R.string.child_pin_note)
            .setView(first).setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val chars = first.text.toString().toCharArray(); first.text.clear()
                if (chars.size !in 4..12 || chars.any { !it.isDigit() }) {
                    chars.fill('\u0000'); message = activity.getString(R.string.child_pin_invalid); return@setPositiveButton
                }
                val second = pinField()
                dialog = AlertDialog.Builder(activity).setTitle(R.string.child_repeat_pin).setView(second)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> chars.fill('\u0000') }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val confirmation = second.text.toString().toCharArray(); second.text.clear()
                        if (!chars.contentEquals(confirmation)) {
                            chars.fill('\u0000'); confirmation.fill('\u0000')
                            message = activity.getString(R.string.child_pin_mismatch); return@setPositiveButton
                        }
                        confirmation.fill('\u0000'); busy = true
                        val pinToDerive = chars.copyOf(); chars.fill('\u0000')
                        Thread {
                            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                            val hash = ChildModePolicy.derive(pinToDerive, salt)
                            prefs.edit().putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                                .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP)).commit()
                            activity.runOnUiThread { busy = false; message = activity.getString(R.string.child_pin_saved) }
                        }.start()
                    }.setOnDismissListener { second.text.clear(); chars.fill('\u0000') }.show()
            }.setOnDismissListener { first.text.clear() }.show()
    }
    private fun pinField() = EditText(activity).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        setSingleLine(true)
    }
    private fun saveHosts() { prefs.edit().putStringSet("hosts", allowed.toSet()).commit() }
    fun stop() { dialog?.dismiss(); dialog = null }
}
