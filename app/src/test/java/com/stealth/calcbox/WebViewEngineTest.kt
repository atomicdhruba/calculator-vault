package com.stealth.calcbox

import android.net.Uri
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import com.stealth.calcbox.webview.VaultChromeClient
import com.stealth.calcbox.webview.VaultWebViewClient
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class WebViewEngineTest {

    // --- Helper Fakes for WebKit Interfaces ---

    private class RecordingCallback : ValueCallback<Array<Uri>> {
        var callCount = 0
        var lastValue: Array<Uri>? = null

        override fun onReceiveValue(value: Array<Uri>?) {
            callCount++
            lastValue = value
        }
    }

    private class RecordingCustomViewCallback : WebChromeClient.CustomViewCallback {
        var hiddenCount = 0

        override fun onCustomViewHidden() {
            hiddenCount++
        }
    }

    // =========================================================================
    // VaultChromeClient Tests
    // =========================================================================

    @Test
    fun testFileChooserCallbackReplacementCancelsPreviousWithNull() {
        val client = VaultChromeClient()
        client.onOpenFileChooser = { _, _ -> /* active handler */ }

        val firstCallback = RecordingCallback()
        val firstHandled = client.onShowFileChooser(null, firstCallback, null)
        assertTrue(firstHandled)
        assertTrue(client.hasPendingFilePathCallback)
        assertEquals(0, firstCallback.callCount)

        // Second file chooser request arrives while first is still pending
        val secondCallback = RecordingCallback()
        val secondHandled = client.onShowFileChooser(null, secondCallback, null)
        assertTrue(secondHandled)
        assertTrue(client.hasPendingFilePathCallback)

        // First callback must have been safely cancelled with null
        assertEquals(1, firstCallback.callCount)
        assertNull(firstCallback.lastValue)

        // Second callback is now the active pending callback
        assertEquals(0, secondCallback.callCount)

        // Complete second callback
        val dummyUris = arrayOf<Uri>()
        client.onReceiveFiles(dummyUris)
        assertEquals(1, secondCallback.callCount)
        assertArrayEquals(dummyUris, secondCallback.lastValue)
        assertFalse(client.hasPendingFilePathCallback)
    }

    @Test
    fun testFileChooserResetCancelsPendingCallbackWithNull() {
        val client = VaultChromeClient()
        client.onOpenFileChooser = { _, _ -> }

        val callback = RecordingCallback()
        client.onShowFileChooser(null, callback, null)
        assertTrue(client.hasPendingFilePathCallback)

        client.resetFilePathCallback()
        assertEquals(1, callback.callCount)
        assertNull(callback.lastValue)
        assertFalse(client.hasPendingFilePathCallback)
    }

    @Test
    fun testFileChooserWithNoHandlerCancelsCallbackAndReturnsFalse() {
        val client = VaultChromeClient()
        client.onOpenFileChooser = null // No handler registered

        val callback = RecordingCallback()
        val handled = client.onShowFileChooser(null, callback, null)

        assertFalse(handled)
        assertEquals(1, callback.callCount)
        assertNull(callback.lastValue)
        assertFalse(client.hasPendingFilePathCallback)
    }

    @Test
    fun testFileChooserWithNullCallbackReturnsFalse() {
        val client = VaultChromeClient()
        client.onOpenFileChooser = { _, _ -> }

        val handled = client.onShowFileChooser(null, null, null)
        assertFalse(handled)
        assertFalse(client.hasPendingFilePathCallback)
    }

    @Test
    fun testProgressUpdatesTriggerListener() {
        val client = VaultChromeClient()
        var reportedProgress = -1
        client.onProgressUpdate = { progress ->
            reportedProgress = progress
        }

        client.onProgressChanged(null, 45)
        assertEquals(45, reportedProgress)

        client.onProgressChanged(null, 100)
        assertEquals(100, reportedProgress)
    }

    @Test
    fun testCustomViewLifecycle() {
        val client = VaultChromeClient()
        val dummyView = mock(View::class.java)
        val customViewCallback = RecordingCustomViewCallback()

        var showReportedView: View? = null
        var hideReported = false

        client.onShowCustomViewListener = { view, _ ->
            showReportedView = view
        }
        client.onHideCustomViewListener = {
            hideReported = true
        }

        assertFalse(client.isCustomViewShowing)

        // Show custom view
        client.onShowCustomView(dummyView, customViewCallback)
        assertTrue(client.isCustomViewShowing)
        assertEquals(dummyView, client.customView)
        assertEquals(dummyView, showReportedView)
        assertEquals(0, customViewCallback.hiddenCount)

        // Hide custom view
        client.onHideCustomView()
        assertFalse(client.isCustomViewShowing)
        assertNull(client.customView)
        assertNull(client.customViewCallback)
        assertEquals(1, customViewCallback.hiddenCount)
        assertTrue(hideReported)
    }

    @Test
    fun testSubsequentShowCustomViewHidesPreviousCustomView() {
        val client = VaultChromeClient()
        val view1 = mock(View::class.java)
        val callback1 = RecordingCustomViewCallback()
        val view2 = mock(View::class.java)
        val callback2 = RecordingCustomViewCallback()

        client.onShowCustomView(view1, callback1)
        assertEquals(view1, client.customView)

        // Showing view2 should safely hide view1 first and activate view2
        client.onShowCustomView(view2, callback2)
        assertEquals(1, callback1.hiddenCount)
        assertTrue(client.isCustomViewShowing)
        assertEquals(view2, client.customView)
        assertEquals(0, callback2.hiddenCount)
    }

    // =========================================================================
    // VaultWebViewClient Tests
    // =========================================================================

    @Test
    fun testInternalWebSchemesAreNotOverridden() {
        val client = VaultWebViewClient()

        // Standard web URLs must stay in the WebView (return false)
        assertFalse(client.shouldOverrideUrl("https://www.instagram.com/"))
        assertFalse(client.shouldOverrideUrl("http://www.instagram.com/accounts/login/"))
        assertFalse(client.shouldOverrideUrl("https://en.wikipedia.org/wiki/Main_Page"))
        assertFalse(client.shouldOverrideUrl("about:blank"))
        assertFalse(client.shouldOverrideUrl("data:text/html;charset=utf-8,<html></html>"))
        assertFalse(client.shouldOverrideUrl("blob:https://www.instagram.com/uuid-blob-123"))

        // Blank or null strings should return false
        assertFalse(client.shouldOverrideUrl(null))
        assertFalse(client.shouldOverrideUrl(""))
        assertFalse(client.shouldOverrideUrl("   "))
    }

    @Test
    fun testNonHttpSchemesAreInterceptedAndConsumed() {
        val client = VaultWebViewClient()
        val blockedUrls = mutableListOf<String>()
        client.onExternalSchemeBlocked = { url ->
            blockedUrls.add(url)
        }

        // Native app intents, Play Store, deep links, mail, and tel schemes
        // Must return true to consume and prevent leakage or ERR_UNKNOWN_URL_SCHEME
        assertTrue(client.shouldOverrideUrl("intent://instagram.com/#Intent;package=com.instagram.android;end"))
        assertTrue(client.shouldOverrideUrl("market://details?id=com.instagram.android"))
        assertTrue(client.shouldOverrideUrl("instagram://user?username=vault"))
        assertTrue(client.shouldOverrideUrl("mailto:support@instagram.com"))
        assertTrue(client.shouldOverrideUrl("tel:+1234567890"))
        assertTrue(client.shouldOverrideUrl("sms:12345"))
        assertTrue(client.shouldOverrideUrl("whatsapp://send?text=hello"))

        assertEquals(7, blockedUrls.size)
        assertTrue(blockedUrls.contains("market://details?id=com.instagram.android"))
        assertTrue(blockedUrls.contains("intent://instagram.com/#Intent;package=com.instagram.android;end"))
    }

    @Test
    fun testShouldOverrideUrlLoadingWithWebResourceRequest() {
        val client = VaultWebViewClient()
        val request = mock(WebResourceRequest::class.java)
        val dummyUri = mock(Uri::class.java)

        whenever(dummyUri.toString()).thenReturn("https://www.instagram.com/feed")
        whenever(request.url).thenReturn(dummyUri)

        // Web URL -> returns false
        assertFalse(client.shouldOverrideUrlLoading(null, request))

        // Non-HTTP deep link -> returns true
        whenever(dummyUri.toString()).thenReturn("intent://instagram.com/#Intent;package=com.instagram.android;end")
        assertTrue(client.shouldOverrideUrlLoading(null, request))
    }

    @Test
    fun testPageLifecycleCallbacks() {
        val client = VaultWebViewClient()
        val loadingStates = mutableListOf<Boolean>()
        client.onLoadingChanged = { loading ->
            loadingStates.add(loading)
        }

        client.onPageStarted(null, "https://www.instagram.com", null)
        client.onPageFinished(null, "https://www.instagram.com")

        assertEquals(listOf(true, false), loadingStates)
    }

    @Test
    fun testErrorCallbacks() {
        val client = VaultWebViewClient()
        var reportedError: String? = null
        client.onErrorReceived = { description ->
            reportedError = description
        }

        val request = mock(WebResourceRequest::class.java)
        val error = mock(WebResourceError::class.java)
        whenever(request.isForMainFrame).thenReturn(true)
        whenever(error.description).thenReturn("net::ERR_INTERNET_DISCONNECTED")

        client.onReceivedError(null, request, error)
        assertEquals("net::ERR_INTERNET_DISCONNECTED", reportedError)
    }
}
