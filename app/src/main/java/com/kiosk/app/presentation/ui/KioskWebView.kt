package com.kiosk.app.presentation.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kiosk.app.presentation.KioskViewModel

private const val LOAD_TIMEOUT_MS = 10_000L

@Composable
fun KioskWebView(vm: KioskViewModel) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }

    val handler = remember { Handler(Looper.getMainLooper()) }
    val loadTimeout = remember {
        Runnable {
            if (vm.state.value.isPageLoading) {
                vm.onWebError("Таймаут загрузки (10 сек)")
            }
        }
    }

    val webView = remember(vm, handler, loadTimeout) {
        WebView(context.applicationContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            alpha = 0.99f
            setBackgroundColor(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                @Suppress("DEPRECATION")
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = false
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setInitialScale(100)
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    vm.onPageStarted()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    handler.removeCallbacks(loadTimeout)

                    if (vm.state.value.lastError == null) {
                        vm.onPageLoaded()

                        view?.evaluateJavascript(
                            """
                            var meta = document.createElement('meta');
                            meta.name = 'viewport';
                            meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                            document.head.appendChild(meta);
                            """.trimIndent(),
                            null
                        )
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    sslErrorHandler: SslErrorHandler?,
                    error: SslError?
                ) {
                    sslErrorHandler?.cancel()
                    handler.removeCallbacks(loadTimeout)
                    vm.onWebError("SSL ошибка")
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        handler.removeCallbacks(loadTimeout)
                        vm.onWebError("Ошибка загрузки: ${error?.description ?: "Неизвестно"}")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.bindReloadAction {
            lastLoadedUrl = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    addView(webView)
                }
            },
            update = {
                val currentUrl = state.url

                if (state.isOnline && currentUrl != lastLoadedUrl) {
                    lastLoadedUrl = currentUrl

                    handler.removeCallbacks(loadTimeout)
                    handler.postDelayed(loadTimeout, LOAD_TIMEOUT_MS)

                    webView.loadUrl(currentUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        val overlayMessage = when {
            !state.isOnline -> "Нет подключения к интернету\nОжидание сети..."
            state.lastError != null -> state.lastError
            state.isPageLoading -> "Загрузка страницы..."
            else -> null
        }

        if (overlayMessage != null) {
            OfflineOverlay(
                message = overlayMessage,
                retryCount = state.retryCount
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            handler.removeCallbacks(loadTimeout)
            webView.stopLoading()
            webView.destroy()
        }
    }
}
