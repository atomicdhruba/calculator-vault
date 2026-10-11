package com.stealth.calcbox.webview

import android.net.Uri
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Production-grade WebChromeClient for the stealth vault WebView.
 * Handles file choosing (media upload for posts and stories),
 * fullscreen custom views (video playback for Reels and Stories),
 * and page load progress tracking.
 */
class VaultChromeClient(
    var onOpenFileChooser: ((ValueCallback<Array<Uri>>, FileChooserParams?) -> Unit)? = null,
    var onShowCustomViewListener: ((View, CustomViewCallback) -> Unit)? = null,
    var onHideCustomViewListener: (() -> Unit)? = null,
    var onProgressUpdate: ((Int) -> Unit)? = null
) : WebChromeClient() {

    var pendingFilePathCallback: ValueCallback<Array<Uri>>? = null
        private set

    var customView: View? = null
        private set

    var customViewCallback: CustomViewCallback? = null
        private set

    val isCustomViewShowing: Boolean
        get() = customView != null

    val hasPendingFilePathCallback: Boolean
        get() = pendingFilePathCallback != null

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        // Cancel existing pending callback if present before replacing
        pendingFilePathCallback?.onReceiveValue(null)
        pendingFilePathCallback = null

        if (filePathCallback == null) {
            return false
        }

        val handler = onOpenFileChooser
        return if (handler != null) {
            pendingFilePathCallback = filePathCallback
            handler(filePathCallback, fileChooserParams)
            true
        } else {
            // No handler registered: resolve callback to avoid blocking subsequent chooser requests
            filePathCallback.onReceiveValue(null)
            false
        }
    }

    /**
     * Completes the pending file chooser request with the selected URIs.
     */
    fun onReceiveFiles(uris: Array<Uri>?) {
        val callback = pendingFilePathCallback
        pendingFilePathCallback = null
        callback?.onReceiveValue(uris)
    }

    /**
     * Cancels any pending file chooser request by passing null.
     */
    fun resetFilePathCallback() {
        val callback = pendingFilePathCallback
        pendingFilePathCallback = null
        callback?.onReceiveValue(null)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view == null || callback == null) return

        if (customView != null) {
            onHideCustomView()
        }

        customView = view
        customViewCallback = callback
        onShowCustomViewListener?.invoke(view, callback)
    }

    override fun onHideCustomView() {
        if (customView == null && customViewCallback == null) return

        val cb = customViewCallback
        customView = null
        customViewCallback = null

        cb?.onCustomViewHidden()
        onHideCustomViewListener?.invoke()
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressUpdate?.invoke(newProgress)
    }
}
