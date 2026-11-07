package com.eymen.browser.telefon

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** First focus selects the address; later caret/IME edits remain ordinary edits. */
internal class AddressEditor(initial: String) {
    var value by mutableStateOf(TextFieldValue(initial))
        private set
    var focused by mutableStateOf(false)
        private set
    var focusGeneration by mutableIntStateOf(0)
        private set
    private var firstFocusSelection = false
    private var pointerGesture = false

    fun pointerDown() { pointerGesture = true }
    fun pointerUp() { pointerGesture = false;firstFocusSelection = false }

    fun focusChanged(hasFocus: Boolean) {
        if (hasFocus && !focused) {
            firstFocusSelection = true
            focusGeneration++
            value = value.copy(selection = TextRange(0, value.text.length))
        } else if (!hasFocus) firstFocusSelection = false
        focused = hasFocus
    }
    fun update(next: TextFieldValue) {
        // The initial tap can emit a caret update after the focus callback.
        // Do not let that same tap undo select-all. Never rewrite IME text.
        value = if (firstFocusSelection && next.text == value.text)
            next.copy(selection = TextRange(0, next.text.length))
        else next.also { firstFocusSelection = false }
    }
    fun finishFocusSelection(generation: Int) {
        if (generation == focusGeneration && !pointerGesture) firstFocusSelection = false
    }
    fun navigationChanged(address: String) {
        if (!focused && value.text != address) value = TextFieldValue(address)
    }
    fun clear() {
        firstFocusSelection = false
        value = TextFieldValue("")
    }
}

@Composable
internal fun rememberAddressEditor(address: String): AddressEditor {
    val editor = remember { AddressEditor(address) }
    LaunchedEffect(address, editor.focused) { editor.navigationChanged(address) }
    LaunchedEffect(editor.focusGeneration) {
        val generation = editor.focusGeneration
        withFrameNanos { }
        editor.finishFocusSelection(generation)
    }
    return editor
}
