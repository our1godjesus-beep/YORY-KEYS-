package app.yory.keys.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.graphics.Color
import android.view.Gravity

/**
 * Minimal real IME vertical slice.
 * Replace this deliberately small baseline with the shared keyboard renderer,
 * layout engine, theme engine, and prediction adapter in subsequent milestones.
 */
class YoryInputMethodService : InputMethodService() {
    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            setBackgroundColor(Color.rgb(24, 25, 34))
        }

        val rows = listOf("qwertyuiop", "asdfghjkl", "⇧zxcvbnm⌫")
        rows.forEach { row ->
            val line = LinearLayout(this).apply {
                gravity = Gravity.CENTER
                orientation = LinearLayout.HORIZONTAL
            }
            row.forEach { symbol ->
                val key = Button(this).apply {
                    text = symbol.toString()
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.rgb(54, 57, 72))
                    setOnClickListener { handleKey(symbol) }
                }
                line.addView(key, LinearLayout.LayoutParams(0, 52, 1f).apply {
                    setMargins(2, 2, 2, 2)
                })
            }
            root.addView(line)
        }

        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER }
        listOf("123", "☺", "space", "↵").forEach { label ->
            val key = Button(this).apply {
                text = label
                isAllCaps = false
                setOnClickListener {
                    when (label) {
                        "space" -> currentInputConnection?.commitText(" ", 1)
                        "↵" -> sendDefaultEditorAction(true)
                        else -> { /* reserved for keyboard mode / emoji picker */ }
                    }
                }
            }
            bottom.addView(key, LinearLayout.LayoutParams(0, 52, if (label == "space") 4f else 1f))
        }
        root.addView(bottom)
        return root
    }

    private fun handleKey(key: Char) {
        val connection: InputConnection = currentInputConnection ?: return
        when (key) {
            '⌫' -> connection.deleteSurroundingText(1, 0)
            '⇧' -> { /* shift state is implemented in the next keyboard-core milestone */ }
            else -> connection.commitText(key.toString(), 1)
        }
    }
}
