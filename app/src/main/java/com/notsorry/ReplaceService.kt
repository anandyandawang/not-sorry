package com.notsorry

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.TimeUnit

const val BANNED_WORD = "sorry"

val ACTIVE_DURATION_MILLIS = TimeUnit.HOURS.toMillis(24)

val REPLACEMENTS = listOf(
    "pickle", "marshmallow", "noodle", "meatloaf", "beans",
    "waffle", "pancake", "muffin", "pretzel", "potato",
    "banana", "pudding", "taco", "cupcake", "donut",
    "burrito", "penguin", "hamster", "goose", "biscuit",
)

private const val PREFERENCES_NAME = "not_sorry"
private const val KEY_TURN_OFF_AT = "turn_off_at"
private const val NO_DEADLINE = 0L

class ReplaceService : AccessibilityService() {

    private val bannedWordRegex = Regex(Regex.escape(BANNED_WORD), RegexOption.IGNORE_CASE)

    private val mainHandler = Handler(Looper.getMainLooper())

    private val turnOff = Runnable { turnOffAndForgetDeadline() }

    private var lastInsertedText: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val turnOffAt = rememberedTurnOffAt().takeIf { it > now() } ?: rememberTurnOffAt(now() + ACTIVE_DURATION_MILLIS)
        mainHandler.postDelayed(turnOff, turnOffAt - now())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (activePeriodIsOver()) {
            turnOffAndForgetDeadline()
            return
        }
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
        node.performSetText(replacedText)
        node.performSetSelection(cursor + cursorShift)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        mainHandler.removeCallbacks(turnOff)
        forgetTurnOffAt()
        super.onDestroy()
    }

    private fun activePeriodIsOver(): Boolean {
        val turnOffAt = rememberedTurnOffAt()
        return turnOffAt != NO_DEADLINE && turnOffAt <= now()
    }

    private fun turnOffAndForgetDeadline() {
        mainHandler.removeCallbacks(turnOff)
        forgetTurnOffAt()
        disableSelf()
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun preferences() = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun rememberedTurnOffAt(): Long = preferences().getLong(KEY_TURN_OFF_AT, NO_DEADLINE)

    private fun rememberTurnOffAt(turnOffAt: Long): Long {
        preferences().edit().putLong(KEY_TURN_OFF_AT, turnOffAt).apply()
        return turnOffAt
    }

    private fun forgetTurnOffAt() {
        preferences().edit().remove(KEY_TURN_OFF_AT).apply()
    }

    private fun AccessibilityNodeInfo.performSetText(text: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun AccessibilityNodeInfo.performSetSelection(position: Int) {
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
