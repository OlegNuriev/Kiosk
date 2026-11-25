# Kiosk

Android-приложение для работы в режиме киоска: полноэкранное окно с WebView, фиксированным URL и ограничениями на выход из приложения.

Репозиторий: https://github.com/OlegNuriev/Kiosk

## Возможности

- Запуск в полноэкранном landscape-режиме без системных панелей.
- Загрузка одного заданного URL в `WebView` без кэша.
- Блокировка кнопок «Назад», «Меню», «Обзор задач» и поиска.
- Перехват системного жеста «Назад» на Android 16+.
- Автозапуск после перезагрузки, обновления приложения, включения экрана и снятия блокировки.
- Восстановление активности после ухода в фон.
- Работа в режиме домашнего экрана с поддержкой Device Admin и Lock Task.
- Отображение понятного сообщения при отсутствии сети и автоматическая загрузка страницы после восстановления соединения.
- Скрытие содержимого экрана в recent apps и предотвращение выключения экрана.

## Изменение URL

URL задаётся в файле:

`app/src/main/java/com/kiosk/app/utils/KioskConfig.kt`

```kotlin
object KioskConfig {
    const val URL_Test = "http://10.26.26.8"
}
```

## Требования

- Android Studio с Android SDK Platform 36.
- JDK 17.
- Устройство или эмулятор с Android 8.0 (API 26) и выше.
- Gradle wrapper 8.13 (используется автоматически через `gradlew`).

## Сборка

Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Linux / macOS:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

APK:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

Релизная сборка подписана keystore из `keystore/kiosk.keystore`.

## Установка

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Первичная настройка устройства

После первого запуска приложение покажет инструкцию. Для полноценного киоск-режима выполните:

1. Нажмите «Домой» и выберите это приложение как домашний экран, затем «Всегда».
2. В настройках батареи установите для приложения режим «Без ограничений».
3. Разрешите автозапуск, если такой пункт присутствует.
4. В «Настройках» → «Безопасность» → «Администраторы устройства» активируйте приложение.
5. Если приложение назначено Device Owner, используйте Lock Task, объявленный в манифесте через `lockTaskMode="if_whitelisted"`.

## Поведение без интернета

- При запуске без сети приложение не пытается пусто загружать страницу и показывает сообщение `Нет подключения к интернету. Ожидание сети...`.
- При ошибке загрузки показывается текст ошибки и номер попытки.
- Причина ошибки сохраняется после служебной страницы WebView, поэтому экран не остаётся чёрным.
- После восстановления сети тот же URL загружается автоматически.
- Повторные попытки выполняются с увеличивающейся задержкой, максимум 10 попыток.

## Проверка сборки

```powershell
.\gradlew.bat assembleDebug lintDebug
.\gradlew.bat assembleRelease
```

## Структура

- `app/src/main/java/com/kiosk/app/presentation` — активность, ViewModel и WebView.
- `app/src/main/java/com/kiosk/app/data/net/NetworkMonitor.kt` — отслеживание состояния сети.
- `app/src/main/java/com/kiosk/app/receiver` — автозапуск и Device Admin.
- `app/src/main/java/com/kiosk/app/data/repository` — хранение флага первого запуска.
- `app/src/main/java/com/kiosk/app/presentation/KioskController.kt` — скрытие системных панелей.
