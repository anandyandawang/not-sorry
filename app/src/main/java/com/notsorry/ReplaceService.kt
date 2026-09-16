package com.notsorry

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

const val BANNED_WORD = "sorry"

val REPLACEMENTS = listOf(
    "pickle", "marshmallow", "noodle", "meatloaf", "beans",
    "waffle", "pancake", "muffin", "pretzel", "potato",
    "banana", "pudding", "taco", "cupcake", "donut",
    "burrito", "penguin", "hamster", "goose", "biscuit",
)

class ReplaceService : AccessibilityService() {

    private val bannedWordRegex = Regex(
        """(?<![\p{L}\p{N}_])${Regex.escape(BANNED_WORD)}(?=[\s\p{P}])""",
        RegexOption.IGNORE_CASE,
    )

    private var lastInsertedText: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        val node = event.source ?: return
        if (!node.isEditable) return
        val text = node.text?.toString() ?: return
        if (text == lastInsertedText) return

        val cursor = node.textSelectionStart.takeIf { it >= 0 } ?: text.length
        var cursorShift = 0
        val replacedText = bannedWordRegex.replace(text) { match ->
            val replacement = REPLACEMENTS.random().withCasingOf(match.value)
            if (match.range.last < cursor) cursorShift += replacement.length - match.value.length
            replacement
        }
        if (replacedText == text) return

        lastInsertedText = replacedText
        node.setText(replacedText)
        node.setCursor(cursor + cursorShift)
    }

    override fun onInterrupt() {}

    private fun AccessibilityNodeInfo.setText(text: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun AccessibilityNodeInfo.setCursor(position: Int) {
        val arguments = Bundle()
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, position)
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, position)
        performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments)
    }

    private fun String.withCasingOf(typed: String): String = when {
        typed.all { it.isUpperCase() } -> uppercase()
        typed.first().isUpperCase() -> replaceFirstChar { it.uppercase() }
        else -> this
    }
}
