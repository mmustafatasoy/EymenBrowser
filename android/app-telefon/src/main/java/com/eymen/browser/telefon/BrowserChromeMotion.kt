package com.eymen.browser.telefon

import android.animation.ValueAnimator
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import org.mozilla.geckoview.GeckoView
import com.eymen.browser.tema.EymenTokens as T
import kotlin.math.abs
import kotlin.math.roundToInt

internal class BrowserGeckoView(context: Context) : GeckoView(context) {
    var onBrowserTouch: ((MotionEvent) -> Unit)? = null
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        onBrowserTouch?.invoke(event)
        return super.dispatchTouchEvent(event)
    }
}

/** Use native scroll notifications and Gecko's dynamic viewport, without page scripts. */
internal class BrowserChromeMotion(private val engine: GeckoView,private val toolbar: View,
    private val canCollapse: () -> Boolean) {
    private val policy = ChromeScrollState(T.telefon_liste_gizleEsik)
    private var animation: ValueAnimator? = null
    private var fraction = 0f
    private var height = 0
    private var bottom = false
    private var fullscreen = false
    private var lastTouch = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var fingers = false
    private var suppressHideUntil = 0L
    var hidden = false; private set

    fun configure(barHeight: Int,atBottom: Boolean,full: Boolean) {
        animation?.cancel()
        // Always clear clipping before reducing the engine's maximum. Gecko
        // requires -maxHeight <= clipping <= 0, including fullscreen changes.
        engine.setVerticalClipping(0)
        height = if (full) 0 else barHeight.coerceAtLeast(0)
        bottom = atBottom; fullscreen = full
        engine.setDynamicToolbarMaxHeight(height)
        policy.show(); fraction = 0f; hidden = false
        applyFrame()
    }
    fun onTouch(event: MotionEvent) {
        if (event.pointerCount != 1) { show(); fingers = false; return }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { fingers = true;lastX = event.rawX;lastY = event.rawY;lastTouch = SystemClock.uptimeMillis() }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX-lastX;val dy = event.rawY-lastY
                lastX=event.rawX;lastY=event.rawY;lastTouch=SystemClock.uptimeMillis()
                if (dy > 1f && abs(dy) > abs(dx)) {
                    suppressHideUntil = lastTouch+150
                    show()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { fingers=false;lastTouch=SystemClock.uptimeMillis() }
        }
    }
    fun onScroll(y: Int) {
        val now=SystemClock.uptimeMillis()
        if (height<=0 || !canCollapse() || engine.shouldPinOnScreen()) {
            policy.scroll(y,false);show();return
        }
        if (!(fingers || now-lastTouch<1500) || now<suppressHideUntil) return
        animateTo(policy.scroll(y,true))
    }
    fun reset(y: Int = 0) { policy.reset(y);show(false) }
    fun show(animated: Boolean = true) { policy.show();animateTo(false,animated) }
    private fun animateTo(hide: Boolean,animated: Boolean = true) {
        if (hidden==hide && (animation?.isRunning==true || fraction==if(hide)1f else 0f)) return
        hidden=hide;animation?.cancel()
        val target=if(hide)1f else 0f
        if(!animated || height==0){fraction=target;applyFrame();return}
        toolbar.visibility=View.VISIBLE
        animation=ValueAnimator.ofFloat(fraction,target).apply {
            duration=T.hareket_aracCubuguMs.toLong();interpolator=DecelerateInterpolator()
            addUpdateListener { fraction=it.animatedValue as Float;applyFrame() };start()
        }
    }
    private fun applyFrame() {
        val visible=(height*(1f-fraction)).roundToInt().coerceIn(0,height)
        toolbar.translationY=if(bottom)(height-visible).toFloat() else -(height-visible).toFloat()
        toolbar.visibility=if(fullscreen)View.GONE else if(visible==0)View.INVISIBLE else View.VISIBLE
        // Gecko's dynamic-toolbar offset is 0 while shown, -maxHeight while
        // hidden. Compute both from the same integer to keep sticky footers
        // and APZ hit testing aligned at every density.
        engine.translationY=if(bottom)0f else visible.toFloat()
        engine.setVerticalClipping(-(height-visible))
    }
    fun dispose(){animation?.cancel()}
}
