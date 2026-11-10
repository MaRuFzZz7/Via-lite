package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.adblock.AdBlockEngine
import com.example.model.WebTab
import com.example.viewmodel.BrowserViewModel
import java.io.ByteArrayInputStream

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    tab: WebTab,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNightMode by viewModel.repository.isNightModeEnabled.collectAsState()
    val isAdBlocker by viewModel.repository.isAdBlockerEnabled.collectAsState()
    val isTextOnly by viewModel.repository.isTextOnlyMode.collectAsState()

    val webView = remember(tab.id) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
        }
    }

    // Update settings dynamically
    LaunchedEffect(tab.isDesktopMode) {
        webView.settings.userAgentString = if (tab.isDesktopMode) {
            DESKTOP_USER_AGENT
        } else {
            null // reset to default
        }
    }

    LaunchedEffect(isTextOnly) {
        webView.settings.loadsImagesAutomatically = !isTextOnly
        webView.settings.blockNetworkImage = isTextOnly
    }

    LaunchedEffect(isNightMode) {
        if (isNightMode) {
            webView.evaluateJavascript(AdBlockEngine.DARK_MODE_JS, null)
        } else {
            webView.evaluateJavascript(AdBlockEngine.REMOVE_DARK_MODE_JS, null)
        }
    }

    // Attach Clients
    DisposableEffect(tab.id) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString() ?: return null
                if (isAdBlocker && AdBlockEngine.shouldBlock(reqUrl)) {
                    viewModel.recordBlockedUrl(tab.id, reqUrl)
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        ByteArrayInputStream(ByteArray(0))
                    )
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                val safeUrl = url ?: ""
                viewModel.updateTabProgress(tab.id, true, 15)
                viewModel.updateTabUrl(tab.id, safeUrl, safeUrl)
                viewModel.resetTabBlockedCount(tab.id)
                view?.evaluateJavascript(AdBlockEngine.COSMETIC_FILTER_JS, null)
                if (isNightMode) {
                    view?.evaluateJavascript(AdBlockEngine.DARK_MODE_JS, null)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val safeUrl = url ?: ""
                val title = view?.title ?: safeUrl
                viewModel.updateTabProgress(tab.id, false, 100)
                viewModel.updateTabNavigation(
                    tab.id,
                    view?.canGoBack() == true,
                    view?.canGoForward() == true
                )
                viewModel.updateTabUrl(tab.id, safeUrl, title)
                view?.evaluateJavascript(AdBlockEngine.COSMETIC_FILTER_JS, null)
                if (isNightMode) {
                    view?.evaluateJavascript(AdBlockEngine.DARK_MODE_JS, null)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                viewModel.updateTabProgress(tab.id, newProgress < 100, newProgress)
                viewModel.updateTabNavigation(
                    tab.id,
                    view?.canGoBack() == true,
                    view?.canGoForward() == true
                )
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                val curUrl = view?.url ?: tab.url
                viewModel.updateTabUrl(tab.id, curUrl, title ?: curUrl)
            }
        }

        // Initial load if tab has URL and hasn't loaded yet
        if (tab.url.isNotBlank() && webView.url == null) {
            webView.loadUrl(tab.url)
        }

        onDispose {
            webView.stopLoading()
        }
    }

    // Listen to ViewModel Web Actions
    LaunchedEffect(tab.id) {
        viewModel.webAction.collect { action ->
            when (action) {
                is BrowserViewModel.WebAction.LoadUrl -> {
                    if (action.tabId == tab.id) {
                        webView.loadUrl(action.url)
                    }
                }
                is BrowserViewModel.WebAction.GoBack -> {
                    if (action.tabId == tab.id && webView.canGoBack()) {
                        webView.goBack()
                    }
                }
                is BrowserViewModel.WebAction.GoForward -> {
                    if (action.tabId == tab.id && webView.canGoForward()) {
                        webView.goForward()
                    }
                }
                is BrowserViewModel.WebAction.Reload -> {
                    if (action.tabId == tab.id) {
                        webView.reload()
                    }
                }
                is BrowserViewModel.WebAction.FindInPage -> {
                    webView.findAllAsync(action.query)
                }
                is BrowserViewModel.WebAction.ClearFindMatches -> {
                    if (action.tabId == tab.id) {
                        webView.clearMatches()
                    }
                }
            }
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}
