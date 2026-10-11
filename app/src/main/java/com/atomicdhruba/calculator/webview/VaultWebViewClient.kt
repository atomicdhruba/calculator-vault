package com.atomicdhruba.calculator.webview

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Production-grade WebViewClient for the stealth vault.
 * Keeps navigation internal for web protocols (HTTP/HTTPS),
 * safely intercepts and suppresses non-HTTP schemes (intent:, market:, instagram:, etc.)
 * to prevent external app launch leaks and ERR_UNKNOWN_URL_SCHEME crashes,
 * and notifies listeners of page load lifecycle and error events.
 */
class VaultWebViewClient(
    var onLoadingChanged: ((Boolean) -> Unit)? = null,
    var onErrorReceived: ((String) -> Unit)? = null,
    var onExternalSchemeBlocked: ((String) -> Unit)? = null
) : WebViewClient() {

    /**
     * Determines whether the given URL should be overridden.
     * Returns false for standard web schemes (http, https, about, data, blob) so WebView loads them.
     * Returns true for non-HTTP external schemes to safely consume them in stealth mode.
     */
    fun shouldOverrideUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        val lower = trimmed.lowercase()

        // Standard web URLs remain inside WebView
        if (lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("about:") ||
            lower.startsWith("data:") ||
            lower.startsWith("blob:")
        ) {
            return false
        }

        // External non-HTTP schemes (intent:, market:, instagram:, mailto:, tel:, etc.)
        // Intercepted and consumed to prevent leaking vault activity or crashes
        onExternalSchemeBlocked?.invoke(trimmed)
        return true
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return shouldOverrideUrl(url)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return shouldOverrideUrl(url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onLoadingChanged?.invoke(true)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        try {
            if (view != null) {
                ScriptInjector.injectIfMatching(view, view.context, url)
            }
        } catch (_: Exception) {
        } finally {
            onLoadingChanged?.invoke(false)
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val description = error?.description?.toString() ?: "Navigation error"
            onErrorReceived?.invoke(description)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        onErrorReceived?.invoke(description ?: "Error code: $errorCode")
    }
}
