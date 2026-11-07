package com.eymen.browser.telefon

/** Collapse after a real downward scroll; even one upward pixel reveals the bar. */
internal class ChromeScrollState(private val hideDistance: Int = 24) {
    var hidden = false; private set
    private var lastY = 0
    private var downward = 0
    fun scroll(y: Int, canHide: Boolean): Boolean {
        val safeY = y.coerceAtLeast(0)
        val delta = safeY - lastY
        lastY = safeY
        if (!canHide || safeY == 0 || delta < 0) {
            downward = 0; hidden = false
        } else if (delta > 0) {
            downward = (downward.toLong() + delta).coerceAtMost(hideDistance.toLong()).toInt()
            if (downward >= hideDistance) hidden = true
        }
        return hidden
    }
    fun show() { hidden = false; downward = 0 }
    fun reset(y: Int = 0) { lastY = y.coerceAtLeast(0); show() }
}
