package com.nanami.koishi.feature.tools.decision_maker.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

fun View.performClockTickHaptic(enabled: Boolean) {
    if (!enabled) return
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

fun View.performConfirmHaptic(enabled: Boolean) {
    if (!enabled) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
