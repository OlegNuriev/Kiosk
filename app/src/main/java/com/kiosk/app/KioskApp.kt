package com.kiosk.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import dagger.hilt.android.HiltAndroidApp
import kotlin.system.exitProcess

@HiltAndroidApp
class KioskApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Toast.makeText(
                this,
                "Краш приложения: ${throwable.message}\n${throwable.stackTraceToString().take(500)}",
                Toast.LENGTH_LONG
            ).show()

            Handler(Looper.getMainLooper()).postDelayed({
                defaultHandler?.uncaughtException(thread, throwable) ?: exitProcess(1)
            }, 1000)
        }
    }
}
