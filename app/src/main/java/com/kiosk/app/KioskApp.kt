package com.kiosk.app

import android.app.Application
import android.widget.Toast
import dagger.hilt.android.HiltAndroidApp
import kotlin.system.exitProcess

@HiltAndroidApp
class KioskApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Toast.makeText(
                this,
                "Краш приложения: ${throwable.message}\n${throwable.stackTraceToString().take(500)}",
                Toast.LENGTH_LONG
            ).show()

            android.os.Handler(mainLooper).postDelayed({
                exitProcess(1)
            }, 5000)
        }
    }
}