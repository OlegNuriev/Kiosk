package com.kiosk.app.presentation

import android.app.Activity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class KioskController {

    fun applyWindow(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        val window = activity.window
        val decorView = window.decorView

        val insetsController = WindowInsetsControllerCompat(window, decorView)

        insetsController.hide(WindowInsetsCompat.Type.statusBars())
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE


        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
            val statusBarsVisible = insets.isVisible(WindowInsetsCompat.Type.statusBars())
            val navigationBarsVisible = insets.isVisible(WindowInsetsCompat.Type.navigationBars())

            if (statusBarsVisible || navigationBarsVisible) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.hide(WindowInsetsCompat.Type.navigationBars())
            }

           insets
        }

        ViewCompat.requestApplyInsets(decorView)
    }

    fun reapplyOnFocus(activity: Activity) {
        applyWindow(activity)
    }
}