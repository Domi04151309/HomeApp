package io.github.domi04151309.home.custom

import android.text.Editable
import android.text.TextWatcher

class TextWatcher(private val lambda: (text: String) -> Unit) : TextWatcher {
    override fun beforeTextChanged(
        p0: CharSequence,
        p1: Int,
        p2: Int,
        p3: Int,
    ) {
        // Do nothing.
    }

    override fun onTextChanged(
        p0: CharSequence,
        p1: Int,
        p2: Int,
        p3: Int,
    ) {
        // Do nothing.
    }

    override fun afterTextChanged(text: Editable) {
        lambda(text.toString())
    }
}
