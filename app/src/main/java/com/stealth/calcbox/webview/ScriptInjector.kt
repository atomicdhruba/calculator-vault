package com.stealth.calcbox.webview

import android.content.Context
import android.webkit.WebView
import java.io.InputStream
import java.net.URI

/**
 * Handles automated script injection for Instagram web sessions.
 * Caches the JavaScript file in-memory to prevent repeated disk I/O,
 * verifies domains before injecting to avoid running scripts on decoy sites,
 * and executes modern DOM automation rules (banner suppression, popup auto-dismiss, dark theme).
 */
object ScriptInjector {

    const val SCRIPT_ASSET_NAME = "inject_instagram.js"

    @Volatile
    private var cachedScript: String? = null

    /**
     * Checks if the given URL belongs to the Instagram domain (*.instagram.com).
     * Rejects empty/null URLs, decoy sites (e.g., wikipedia.org), and malicious lookalikes.
     */
    fun isInstagramDomain(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        val host = try {
            val uri = URI(trimmed)
            uri.host?.lowercase()
        } catch (_: Exception) {
            null
        } ?: extractHostFallback(trimmed)

        if (host.isNullOrBlank()) return false
        return host == "instagram.com" || host.endsWith(".instagram.com")
    }

    private fun extractHostFallback(url: String): String? {
        val withoutScheme = if (url.contains("://")) {
            url.substringAfter("://")
        } else {
            url
        }
        val hostPart = withoutScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':')
            .trim()
        return hostPart.lowercase().takeIf { it.isNotEmpty() }
    }

    /**
     * Reads a UTF-8 script string from the provided InputStream.
     */
    fun loadScript(inputStream: InputStream): String {
        return inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    /**
     * Loads the injection script, utilizing an in-memory cache to prevent repeated asset reads.
     */
    fun getOrLoadScript(context: Context): String {
        cachedScript?.let { return it }
        synchronized(this) {
            cachedScript?.let { return it }
            val script = try {
                context.assets.open(SCRIPT_ASSET_NAME).use { loadScript(it) }
            } catch (_: Exception) {
                ""
            }
            cachedScript = script
            return script
        }
    }

    /**
     * Injects the automation script into the WebView if script content is available.
     */
    fun inject(webView: WebView, context: Context) {
        val script = getOrLoadScript(context)
        if (script.isNotBlank()) {
            webView.evaluateJavascript(script, null)
        }
    }

    /**
     * Injects the automation script only if the URL matches an Instagram domain.
     */
    fun injectIfMatching(webView: WebView, context: Context, url: String?) {
        if (isInstagramDomain(url)) {
            inject(webView, context)
        }
    }

    /**
     * Clears the in-memory cache (primarily used for unit testing).
     */
    fun clearCache() {
        synchronized(this) {
            cachedScript = null
        }
    }

    /**
     * Explicitly sets the cached script (primarily used for unit testing).
     */
    fun setCachedScript(script: String?) {
        synchronized(this) {
            cachedScript = script
        }
    }

    /**
     * Returns the currently cached script, if any.
     */
    fun getCachedScript(): String? = cachedScript
}
