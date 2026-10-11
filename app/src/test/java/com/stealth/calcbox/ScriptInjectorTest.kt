package com.stealth.calcbox

import android.content.Context
import android.content.res.AssetManager
import android.webkit.WebView
import com.stealth.calcbox.webview.DEFAULT_VAULT_USER_AGENT
import com.stealth.calcbox.webview.ScriptInjector
import com.stealth.calcbox.webview.VaultWebViewClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class ScriptInjectorTest {

    @Before
    fun setUp() {
        ScriptInjector.clearCache()
    }

    @After
    fun tearDown() {
        ScriptInjector.clearCache()
    }

    // =========================================================================
    // Domain Matching Tests
    // =========================================================================

    @Test
    fun testIsInstagramDomainMatchesValidUrls() {
        assertTrue(ScriptInjector.isInstagramDomain("https://www.instagram.com/"))
        assertTrue(ScriptInjector.isInstagramDomain("https://instagram.com/"))
        assertTrue(ScriptInjector.isInstagramDomain("https://m.instagram.com/direct/"))
        assertTrue(ScriptInjector.isInstagramDomain("http://instagram.com"))
        assertTrue(ScriptInjector.isInstagramDomain("https://sub.instagram.com/explore/"))
        assertTrue(ScriptInjector.isInstagramDomain("https://www.instagram.com:443/feed"))
        assertTrue(ScriptInjector.isInstagramDomain("   https://www.instagram.com/   "))
    }

    @Test
    fun testIsInstagramDomainRejectsInvalidAndDecoyUrls() {
        assertFalse(ScriptInjector.isInstagramDomain("https://en.wikipedia.org/"))
        assertFalse(ScriptInjector.isInstagramDomain("https://google.com/"))
        assertFalse(ScriptInjector.isInstagramDomain("https://notinstagram.com/"))
        assertFalse(ScriptInjector.isInstagramDomain("https://fakeinstagram.com/"))
        assertFalse(ScriptInjector.isInstagramDomain("https://instagram.com.attacker.com/"))
        assertFalse(ScriptInjector.isInstagramDomain("https://instagram.com:secret@attacker.com/test"))
        assertFalse(ScriptInjector.isInstagramDomain("ftp://instagram.com/"))
        assertFalse(ScriptInjector.isInstagramDomain("custom://instagram.com/"))
        assertFalse(ScriptInjector.isInstagramDomain("about:blank"))
        assertFalse(ScriptInjector.isInstagramDomain("data:text/html,test"))
        assertFalse(ScriptInjector.isInstagramDomain(null))
        assertFalse(ScriptInjector.isInstagramDomain(""))
        assertFalse(ScriptInjector.isInstagramDomain("   "))
    }

    // =========================================================================
    // Script Loading and In-Memory Caching Tests
    // =========================================================================

    @Test
    fun testLoadScriptFromStream() {
        val sampleCode = "(function() { console.log('calcbox_test'); })();"
        val inputStream = sampleCode.byteInputStream(Charsets.UTF_8)

        val result = ScriptInjector.loadScript(inputStream)
        assertEquals(sampleCode, result)
    }

    @Test
    fun testScriptCacheStoresAndClearsValue() {
        assertNull(ScriptInjector.getCachedScript())

        val script = "var test = 42;"
        ScriptInjector.setCachedScript(script)
        assertEquals(script, ScriptInjector.getCachedScript())

        ScriptInjector.clearCache()
        assertNull(ScriptInjector.getCachedScript())
    }

    @Test
    fun testGetOrLoadScriptCachesAssetContent() {
        val mockContext = mock(Context::class.java)
        val mockAssets = mock(AssetManager::class.java)
        whenever(mockContext.assets).thenReturn(mockAssets)

        val assetContent = "(function() { window.__loaded = true; })();"
        whenever(mockAssets.open(ScriptInjector.SCRIPT_ASSET_NAME))
            .thenReturn(assetContent.byteInputStream(Charsets.UTF_8))

        // First call loads from asset manager
        val firstResult = ScriptInjector.getOrLoadScript(mockContext)
        assertEquals(assetContent, firstResult)
        assertEquals(assetContent, ScriptInjector.getCachedScript())

        // Second call must return cached value without reading assets again
        val secondResult = ScriptInjector.getOrLoadScript(mockContext)
        assertEquals(assetContent, secondResult)

        verify(mockAssets, times(1)).open(ScriptInjector.SCRIPT_ASSET_NAME)
    }

    // =========================================================================
    // Injection Execution Tests
    // =========================================================================

    @Test
    fun testInjectExecutesJavascriptWhenScriptIsAvailable() {
        val mockWebView = mock(WebView::class.java)
        val mockContext = mock(Context::class.java)

        val script = "(function() { window.__calcbox = true; })();"
        ScriptInjector.setCachedScript(script)

        ScriptInjector.inject(mockWebView, mockContext)

        verify(mockWebView).evaluateJavascript(eq(script), anyOrNull())
    }

    @Test
    fun testInjectHandlesWebViewExceptionsGracefully() {
        val mockWebView = mock(WebView::class.java)
        val mockContext = mock(Context::class.java)

        val script = "(function() { window.__calcbox = true; })();"
        ScriptInjector.setCachedScript(script)

        whenever(mockWebView.evaluateJavascript(eq(script), anyOrNull()))
            .thenThrow(IllegalStateException("WebView is destroyed"))

        // Should not throw or crash
        ScriptInjector.inject(mockWebView, mockContext)
    }

    @Test
    fun testInjectIfMatchingExecutesOnlyForInstagram() {
        val mockWebView = mock(WebView::class.java)
        val mockContext = mock(Context::class.java)

        val script = "(function() { window.__calcbox = true; })();"
        ScriptInjector.setCachedScript(script)

        // Non-Instagram URL: Should NOT evaluate javascript
        ScriptInjector.injectIfMatching(mockWebView, mockContext, "https://en.wikipedia.org/wiki/Main_Page")
        verify(mockWebView, never()).evaluateJavascript(any(), anyOrNull())

        // Instagram URL: Should evaluate javascript
        ScriptInjector.injectIfMatching(mockWebView, mockContext, "https://www.instagram.com/")
        verify(mockWebView).evaluateJavascript(eq(script), anyOrNull())
    }

    @Test
    fun testVaultWebViewClientInvokesInjectionOnPageFinished() {
        val client = VaultWebViewClient()
        val mockWebView = mock(WebView::class.java)
        val mockContext = mock(Context::class.java)
        whenever(mockWebView.context).thenReturn(mockContext)

        val script = "console.log('injected');"
        ScriptInjector.setCachedScript(script)

        // Decoy load -> no injection
        client.onPageFinished(mockWebView, "https://en.wikipedia.org/")
        verify(mockWebView, never()).evaluateJavascript(any(), anyOrNull())

        // Instagram load -> injection triggered
        client.onPageFinished(mockWebView, "https://www.instagram.com/feed")
        verify(mockWebView).evaluateJavascript(eq(script), anyOrNull())
    }

    // =========================================================================
    // User Agent Verification
    // =========================================================================

    @Test
    fun testVaultUserAgentMatchesExpectedMobileFormat() {
        val expected = "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36 Instagram 365.0.0.0"
        assertEquals(expected, DEFAULT_VAULT_USER_AGENT)
    }
}
