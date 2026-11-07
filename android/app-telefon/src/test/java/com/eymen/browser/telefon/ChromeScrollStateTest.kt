package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test

class ChromeScrollStateTest {
    @Test fun smallDownwardJitterDoesNotHideControls() {
        val state=ChromeScrollState(24)
        assertFalse(state.scroll(5,true));assertFalse(state.scroll(20,true));assertTrue(state.scroll(24,true))
    }
    @Test fun OneUpwardPixelRevealsAfterDeepScrolling() {
        val state=ChromeScrollState();assertTrue(state.scroll(1000,true))
        assertFalse(state.scroll(999,true));assertFalse(state.hidden)
        assertFalse(state.scroll(1000,true));assertTrue(state.scroll(1023,true))
    }
    @Test fun overlaysAndTopOfPageKeepControlsVisible() {
        val state=ChromeScrollState();assertTrue(state.scroll(400,true))
        assertFalse(state.scroll(800,false));assertFalse(state.scroll(0,true));assertFalse(state.scroll(-20,true))
    }
    @Test fun changingTabsDoesNotTreatSavedScrollPositionAsGesture() {
        val state=ChromeScrollState();state.reset(4000)
        assertFalse(state.hidden);assertFalse(state.scroll(4001,true));assertTrue(state.scroll(4024,true))
        state.show();assertFalse(state.hidden)
    }
    @Test fun hugeScrollOffsetsCannotOverflowAccumulatedDistance() {
        val state=ChromeScrollState();assertTrue(state.scroll(Int.MAX_VALUE,true))
        assertFalse(state.scroll(Int.MAX_VALUE-1,true));state.reset();assertFalse(state.hidden)
    }
    @Test fun delayedFlingUpdatesCannotHideControlsAfterSmallReverseGesture() {
        val state=ChromeScrollState();assertTrue(state.scroll(1000,true))
        state.holdShown()
        assertFalse(state.scroll(1200,true));assertFalse(state.scroll(1800,true))
        assertFalse(state.scroll(2500,true));assertFalse(state.hidden)
        state.allowHiding()
        assertFalse(state.scroll(2523,true));assertTrue(state.scroll(2524,true))
    }
    @Test fun aNewPageDoesNotKeepThePreviousPagesRevealLock() {
        val state=ChromeScrollState();state.holdShown();state.reset(4000)
        assertFalse(state.scroll(4023,true));assertTrue(state.scroll(4024,true))
    }
}
