package com.eymen.browser.telefon

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.eymen.browser.tema.EymenTokens as T
import kotlin.math.min
import kotlin.math.roundToInt

/** Shared presentation only. Native buttons, listeners, PIN fields and prompt decisions stay native. */
internal object BrowserDialogs {
    fun builder(context: Context): AlertDialog.Builder = StyledBuilder(context)
    private fun day(context: Context) = context.getSharedPreferences("eymen_browser_setup", Context.MODE_PRIVATE)
        .getBoolean("day_theme", true)
    private class StyledBuilder(base: Context) : AlertDialog.Builder(ContextThemeWrapper(base,
        if (day(base)) android.R.style.Theme_Material_Light_Dialog_Alert else android.R.style.Theme_Material_Dialog_Alert)) {
        private var heading: CharSequence? = null
        override fun setTitle(title: CharSequence?): AlertDialog.Builder {
            heading = title; return super.setTitle(title)
        }
        override fun setTitle(titleId: Int): AlertDialog.Builder = setTitle(context.getText(titleId))
        override fun create(): AlertDialog {
            val p = browserListPalette(day(context))
            val density = context.resources.displayMetrics.density
            fun px(value: Int) = (value * density).roundToInt()
            heading?.takeIf { it.isNotBlank() }?.let { title ->
                super.setCustomTitle(TextView(context).apply {
                    text = title; setTextColor(p.text.toArgb())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, T.telefon_dialog_baslik.toFloat())
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setPadding(px(T.telefon_dialog_kenar), px(T.telefon_dialog_kenar),
                        px(T.telefon_dialog_kenar), px(T.telefon_ekran_kucukBosluk))
                    if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
                })
            }
            val dialog = super.create()
            val decor = dialog.window?.decorView
            decor?.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    view.removeOnAttachStateChangeListener(this)
                    view.post {
                        if (!dialog.isShowing) return@post
                        dialog.window?.apply {
                            setDimAmount(T.telefon_dialog_perde.toFloat())
                            setBackgroundDrawable(GradientDrawable().apply {
                                setColor(p.background.toArgb()); cornerRadius = px(T.telefon_dialog_kose).toFloat()
                            })
                            setLayout(min(context.resources.displayMetrics.widthPixels - px(T.telefon_dialog_kenar) * 2,
                                px(T.telefon_dialog_enFazla)), ViewGroup.LayoutParams.WRAP_CONTENT)
                        }
                        dialog.findViewById<TextView>(android.R.id.message)?.apply {
                            setTextColor(p.text.toArgb())
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, T.telefon_dialog_govde.toFloat())
                            setTextIsSelectable(true)
                        }
                        fun inputs(parent: View) {
                            if (parent is EditText) {
                                parent.setTextColor(p.text.toArgb()); parent.setHintTextColor(p.muted.toArgb())
                                parent.setTextSize(TypedValue.COMPLEX_UNIT_SP,T.telefon_dialog_govde.toFloat())
                                parent.backgroundTintList = ColorStateList.valueOf(p.accent.toArgb())
                                parent.minHeight = px(T.telefon_dokunma_standart)
                            }
                            if (parent is ViewGroup) for (i in 0 until parent.childCount) inputs(parent.getChildAt(i))
                        }
                        inputs(view)
                        for (which in listOf(AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL)) {
                            dialog.getButton(which)?.apply {
                                isAllCaps = false; minHeight = px(T.telefon_dokunma_standart)
                                setTextSize(TypedValue.COMPLEX_UNIT_SP,T.telefon_dialog_dugme.toFloat())
                                val primary = which == AlertDialog.BUTTON_POSITIVE
                                setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),
                                    intArrayOf(p.disabled.toArgb(),if(primary)p.onAccent.toArgb() else p.accent.toArgb())))
                                if (primary) background = RippleDrawable(ColorStateList.valueOf(p.soft.toArgb()),
                                    GradientDrawable().apply { setColor(p.accent.toArgb()); cornerRadius=px(T.telefon_dialog_kose).toFloat() },null)
                            }
                        }
                    }
                }
                override fun onViewDetachedFromWindow(view: View) = Unit
            })
            return dialog
        }
    }
}
