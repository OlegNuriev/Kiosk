package com.kiosk.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kiosk.app.presentation.MainActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_USER_PRESENT
                -> startKioskActivity(context)
        }
    }

    private fun startKioskActivity(context: Context) {
        val i = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(i)
    }
}