package com.stealth.calcbox.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

const val DEFAULT_VAULT_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"

/**
 * Production-grade stealth WebView container for Instagram / Decoy web sessions.
 * Features:
 * - Security hardening: disabled file access, mixed content blocked, accept cookies
 * - Media uploads for posts and stories via ActivityResultLauncher
 * - Fullscreen hardware-accelerated video for Reels and Stories
 * - BackHandler for in-WebView back navigation and instant lock
 * - Material 3 linear progress indicator for page loading
 * - Clean lifecycle management with anti-forensic zero-state disposal
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VaultWebView(
    url: String,
    onLockVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var lastLoadedUrl by remember { mutableStateOf("") }

    val chromeClient = remember {
        VaultChromeClient()
    }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK) {
            val parsed = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            if (!parsed.isNullOrEmpty()) {
                parsed
            } else {
                result.data?.data?.let { arrayOf(it) }
            }
        } else {
            null
        }
        chromeClient.onReceiveFiles(uris)
    }

    // Configure chrome client callbacks
    chromeClient.onOpenFileChooser = { _, fileChooserParams ->
        try {
            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            fileChooserLauncher.launch(intent)
        } catch (_: Exception) {
            chromeClient.resetFilePathCallback()
        }
    }

    chromeClient.onShowCustomViewListener = { view, _ ->
        customView = view
    }

    chromeClient.onHideCustomViewListener = {
        customView = null
    }

    chromeClient.onProgressUpdate = { newProgress ->
        progress = newProgress
    }

    val webViewClient = remember {
        VaultWebViewClient(
            onLoadingChanged = { loading ->
                if (!loading) {
                    progress = 100
                }
            }
        )
    }

    // Handle back button: CustomView -> WebView history -> Lock Vault
    BackHandler(enabled = true) {
        if (customView != null) {
            chromeClient.onHideCustomView()
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onLockVault()
        }
    }

    // React to external URL parameter updates
    LaunchedEffect(url) {
        if (url.isNotBlank() && url != lastLoadedUrl) {
            lastLoadedUrl = url
            webView?.loadUrl(url)
        }
    }

    // Cleanup when leaving composition
    DisposableEffect(Unit) {
        onDispose {
            chromeClient.resetFilePathCallback()
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                onPause()
                pauseTimers()
                destroy()
            }
            webView = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        @Suppress("DEPRECATION")
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        allowFileAccess = false
                        allowContentAccess = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = DEFAULT_VAULT_USER_AGENT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(true)
                        builtInZoomControls = false
                        displayZoomControls = false
                    }

                    this.webChromeClient = chromeClient
                    this.webViewClient = webViewClient

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    if (url.isNotBlank()) {
                        loadUrl(url)
                        lastLoadedUrl = url
                    }

                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading progress indicator
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        // Fullscreen Custom View (Stories / Reels video)
        if (customView != null) {
            key(customView) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { context ->
                            val cv = customView
                            (cv?.parent as? ViewGroup)?.removeView(cv)
                            cv ?: View(context)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
