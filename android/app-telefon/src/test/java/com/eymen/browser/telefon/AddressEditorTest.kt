package com.eymen.browser.telefon

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.*
import org.junit.Test

class AddressEditorTest {
    @Test fun firstTapSelectsWholeUrlAndItsCaretEventCannotUndoSelection() {
        val url = "https://example.com/uzun?arama=İstanbul"
        val editor = AddressEditor(url)
        editor.focusChanged(true)
        editor.update(TextFieldValue(url, selection = TextRange(8)))
        assertEquals(TextRange(0, url.length), editor.value.selection)
    }
    @Test fun newTypingReplacesSelectionWithoutReselectingTheNewQuery() {
        val editor = AddressEditor("https://example.com/old")
        editor.focusChanged(true)
        editor.update(TextFieldValue("yeni arama", selection = TextRange(10)))
        editor.finishFocusSelection(editor.focusGeneration)
        assertEquals("yeni arama", editor.value.text)
        assertEquals(TextRange(10), editor.value.selection)
    }
    @Test fun ordinaryCaretEditingWorksAfterInitialFocusFrame() {
        val editor = AddressEditor("abcd")
        editor.focusChanged(true)
        editor.finishFocusSelection(editor.focusGeneration)
        editor.update(TextFieldValue("abcd", selection = TextRange(2)))
        assertEquals(TextRange(2), editor.value.selection)
        editor.update(TextFieldValue("abXcd", selection = TextRange(3)))
        assertEquals("abXcd", editor.value.text)
    }
    @Test fun preservesImeCompositionAndTypedTextDuringRedirects() {
        val editor = AddressEditor("https://example.com")
        editor.focusChanged(true)
        val composing = TextFieldValue("İstanbul", TextRange(8), TextRange(0, 8))
        editor.update(composing)
        editor.navigationChanged("https://example.com/redirect")
        assertEquals(composing, editor.value)
        editor.focusChanged(false)
        editor.navigationChanged("https://example.com/redirect")
        assertEquals("https://example.com/redirect", editor.value.text)
    }
    @Test fun refocusSelectsAgainAndClearLeavesAnEmptyEditableField() {
        val editor = AddressEditor("old")
        editor.focusChanged(true)
        val oldGeneration = editor.focusGeneration
        editor.focusChanged(false)
        editor.focusChanged(true)
        editor.finishFocusSelection(oldGeneration)
        editor.update(TextFieldValue("old", TextRange(1)))
        assertEquals(TextRange(0, 3), editor.value.selection)
        editor.clear()
        assertTrue(editor.focused)
        assertEquals(TextFieldValue(""), editor.value)
    }
}
