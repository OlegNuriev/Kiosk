package com.kiosk.app.presentation

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.kiosk.app.domain.repository.AppPreferencesRepository
import com.kiosk.app.presentation.ui.KioskWebView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferencesRepository: AppPreferencesRepository
    @Inject lateinit var kioskController: KioskController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        kioskController.applyWindow(this)

        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                }
            }
        )

        setContent {
            KioskWebView(hiltViewModel())
        }

        lifecycleScope.launch {
            if (appPreferencesRepository.isFirstLaunch()) {
                showKioskSetupInstructions()
                appPreferencesRepository.setLaunched()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        kioskController.applyWindow(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            kioskController.reapplyOnFocus(this)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_SEARCH -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onPause() {
        super.onPause()

        window.decorView.postDelayed({
            if (!isFinishing && !isDestroyed) {
                startActivity(
                    Intent(this, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                )
            }
        }, 500)
    }

    private fun showKioskSetupInstructions() {
        AlertDialog.Builder(this)
            .setTitle("Режим киоска — первичная настройка")
            .setMessage(
                "Для корректной работы приложения выполните шаги ОДИН РАЗ:\n\n" +

                        "1️⃣ Сделать приложение главным (Домой)\n" +
                        "• Нажмите кнопку «Домой»\n" +
                        "• В появившемся списке выберите это приложение\n" +
                        "• Нажмите «Всегда»\n\n" +

                        "2️⃣ Отключить экономию батареи\n" +
                        "• Откройте «Настройки»\n" +
                        "• Перейдите в «Приложения» → это приложение\n" +
                        "• Откройте «Батарея»\n" +
                        "• Выберите «Без ограничений»\n\n" +

                        "3️⃣ Разрешить автозапуск (если есть такой пункт)\n" +
                        "• «Настройки» → «Автозапуск»\n" +
                        "• Включите автозапуск для приложения\n\n" +

                        "4️⃣ (Рекомендуется) Администратор устройства\n" +
                        "• «Настройки» → «Безопасность»\n" +
                        "• «Администраторы устройства»\n" +
                        "• Выберите это приложение и нажмите «Активировать»\n\n" +

                        "После настройки приложение будет:\n" +
                        "• запускаться автоматически\n" +
                        "• работать в полноэкранном режиме\n" +
                        "• восстанавливаться после перезагрузки\n\n" +
                        "Закрывать приложение не требуется."
            )
            .setPositiveButton("Понятно") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }
}
