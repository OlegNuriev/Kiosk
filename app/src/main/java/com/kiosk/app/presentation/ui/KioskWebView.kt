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

@Composable
fun KioskWebView(vm: KioskViewModel) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }

    val handler = remember { Handler(Looper.getMainLooper()) }
    var timeoutRunnable by remember { mutableStateOf<Runnable?>(null) }

    LaunchedEffect(state.isOnline) {
        if (state.isOnline && state.lastError != null) {
            lastLoadedUrl = null
        }
    }

    val webView = remember {
        WebView(context).apply {
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
                    vm.onPageLoaded()

                    // Отменяем таймаут при успешной загрузке
                    timeoutRunnable?.let { handler.removeCallbacks(it) }

                    // Принудительный viewport
                    view?.evaluateJavascript("""
                        var meta = document.createElement('meta');
                        meta.name = 'viewport';
                        meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                        document.head.appendChild(meta);
                    """.trimIndent(), null)
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    handler?.cancel()
                    vm.onWebError("SSL ошибка")
                    timeoutRunnable?.let { handler?.removeCallbacks(it) }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        vm.onWebError("Ошибка загрузки: ${error?.description ?: "Неизвестно"}")
                    }
                    timeoutRunnable?.let { handler.removeCallbacks(it) }
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
            update = { frameLayout ->
                val currentUrl = state.url

                if (currentUrl != lastLoadedUrl ||
                    (state.isOnline && state.lastError != null) ||
                    (state.isOnline && lastLoadedUrl == null)
                ) {
                    lastLoadedUrl = currentUrl

                    // Сбрасываем предыдущий таймаут
                    timeoutRunnable?.let { handler.removeCallbacks(it) }

                    // Новый таймаут 10 секунд
                    timeoutRunnable = Runnable {
                        if (state.isPageLoading) {
                            vm.onWebError("Таймаут загрузки (10 сек)")
                        }
                    }
                    handler.postDelayed(timeoutRunnable!!, 10000)

                    webView.loadUrl(currentUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (state.lastError != null || state.isPageLoading) {
            OfflineOverlay(
                message = state.lastError ?: "Загрузка страницы...",
                retryCount = state.retryCount
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            timeoutRunnable?.let { handler.removeCallbacks(it) }
            webView.stopLoading()
            webView.destroy()
        }
    }
}

@Composable
private fun OfflineOverlay(message: String, retryCount: Int = 0) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                color = ComposeColor.White,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (retryCount > 0) {
                Text(
                    text = "Попытка ${retryCount + 1} из 10...",
                    color = ComposeColor.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}