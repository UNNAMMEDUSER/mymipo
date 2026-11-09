package com.mymipo.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var homePage = "https://www.google.com"
    private var searchEngine = "Google"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            addJavascriptInterface(BrowserBridge(), "Mymipo")
        }
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    inner class BrowserBridge {
        @JavascriptInterface
        fun openUrl(url: String) {
            runOnUiThread {
                val value = url.trim()
                if (value.startsWith("https://") || value.startsWith("http://")) {
                    webView.loadUrl(value)
                } else {
                    Toast.makeText(this@MainActivity, "Неподдерживаемый адрес", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun goBack() {
            runOnUiThread { if (webView.canGoBack()) webView.goBack() }
        }

        @JavascriptInterface
        fun reload() {
            runOnUiThread { webView.reload() }
        }

        @JavascriptInterface
        fun clearData() {
            runOnUiThread {
                webView.clearHistory()
                webView.clearCache(true)
                Toast.makeText(this@MainActivity, "Кэш и история очищены", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun setHomePage(url: String) {
            if (url.startsWith("https://") || url.startsWith("http://")) homePage = url
        }

        @JavascriptInterface
        fun setSearchEngine(engine: String) {
            if (engine in listOf("Google", "Bing", "DuckDuckGo")) searchEngine = engine
        }
    }
}
