package com.mymipo.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : ComponentActivity() {
    private lateinit var shellView: WebView
    private lateinit var pageView: WebView
    private lateinit var root: LinearLayout
    private var pageOpen = false
    private var homePage = "https://www.google.com"
    private var searchEngine = "Google"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        shellView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            addJavascriptInterface(BrowserBridge(), "Mymipo")
        }

        pageView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            visibility = View.GONE
        }

        root.addView(shellView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ))
        root.addView(pageView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
        shellView.loadUrl("file:///android_asset/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    pageOpen && pageView.canGoBack() -> pageView.goBack()
                    pageOpen -> closePage()
                    else -> finish()
                }
            }
        })
    }

    private fun openPage(url: String) {
        val value = url.trim()
        if (!value.startsWith("https://") && !value.startsWith("http://")) {
            Toast.makeText(this, "Неподдерживаемый адрес", Toast.LENGTH_SHORT).show()
            return
        }
        pageOpen = true
        shellView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(190)
        )
        pageView.visibility = View.VISIBLE
        pageView.loadUrl(value)
    }

    private fun closePage() {
        pageOpen = false
        pageView.stopLoading()
        pageView.visibility = View.GONE
        shellView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        )
        shellView.evaluateJavascript("showView('home')", null)
    }

    inner class BrowserBridge {
        @JavascriptInterface
        fun openUrl(url: String) {
            runOnUiThread { openPage(url) }
        }

        @JavascriptInterface
        fun goBack() {
            runOnUiThread {
                if (pageView.canGoBack()) pageView.goBack() else closePage()
            }
        }

        @JavascriptInterface
        fun reload() {
            runOnUiThread { if (pageOpen) pageView.reload() }
        }

        @JavascriptInterface
        fun closePage() {
            runOnUiThread { this@MainActivity.closePage() }
        }

        @JavascriptInterface
        fun clearData() {
            runOnUiThread {
                pageView.clearHistory()
                pageView.clearCache(true)
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
