package com.mymipo.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private enum class Screen { HOME, BROWSER, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MymipoApp() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MymipoApp() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.HOME) }
    var address by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var homePage by remember { mutableStateOf("https://www.google.com") }
    var searchEngine by remember { mutableStateOf("Google") }
    var darkTheme by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("Mymipo") }
    var canGoBack by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun navigate(input: String) {
        val value = input.trim()
        if (value.isBlank()) return
        val url = if (value.startsWith("http://") || value.startsWith("https://")) value
        else if (value.contains(".") && !value.contains(" ")) "https://$value"
        else {
            val base = when (searchEngine) {
                "Bing" -> "https://www.bing.com/search?q="
                "DuckDuckGo" -> "https://duckduckgo.com/?q="
                else -> "https://www.google.com/search?q="
            }
            base + java.net.URLEncoder.encode(value, "UTF-8")
        }
        address = url
        screen = Screen.BROWSER
        webView?.loadUrl(url)
    }

    BackHandler(enabled = screen != Screen.HOME || canGoBack) {
        if (screen == Screen.SETTINGS) screen = Screen.HOME
        else if (screen == Screen.BROWSER && canGoBack) webView?.goBack()
        else screen = Screen.HOME
    }

    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            topBar = {
                if (screen == Screen.BROWSER) {
                    Column {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (canGoBack) webView?.goBack() else screen = Screen.HOME }) {
                                Icon(Icons.Default.ArrowBack, "Назад")
                            }
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text("Поиск или адрес сайта") },
                                trailingIcon = {
                                    IconButton(onClick = { navigate(address) }) { Icon(Icons.Default.Search, "Открыть") }
                                }
                            )
                            IconButton(onClick = { webView?.reload() }) { Icon(Icons.Default.Refresh, "Обновить") }
                            IconButton(onClick = { screen = Screen.SETTINGS }) { Icon(Icons.Default.MoreVert, "Меню") }
                        }
                        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                } else if (screen == Screen.SETTINGS) {
                    TopAppBarCompat("Настройки") { screen = Screen.HOME }
                }
            },
            bottomBar = {
                if (screen == Screen.BROWSER) {
                    NavigationBar {
                        NavigationBarItem(selected = false, onClick = { screen = Screen.HOME }, icon = { Icon(Icons.Default.Home, "Главная") }, label = { Text("Главная") })
                        NavigationBarItem(selected = true, onClick = { }, icon = { Icon(Icons.Default.Language, "Браузер") }, label = { Text("Браузер") })
                        NavigationBarItem(selected = false, onClick = { screen = Screen.SETTINGS }, icon = { Icon(Icons.Default.Settings, "Настройки") }, label = { Text("Настройки") })
                    }
                }
            }
        ) { padding ->
            when (screen) {
                Screen.HOME -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { navigate(query) },
                    onOpen = { navigate(it) },
                    onSettings = { screen = Screen.SETTINGS },
                    homePage = homePage
                )
                Screen.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    homePage = homePage,
                    onHomePageChange = { homePage = it },
                    searchEngine = searchEngine,
                    onSearchEngineChange = { searchEngine = it },
                    darkTheme = darkTheme,
                    onDarkThemeChange = { darkTheme = it },
                    onClearData = {
                        webView?.clearHistory()
                        webView?.clearCache(true)
                    }
                )
                Screen.BROWSER -> AndroidView(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    factory = {
                        WebView(it).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    loading = true
                                    url?.let { currentUrl = it; address = it }
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                    canGoBack = view?.canGoBack() == true
                                    pageTitle = view?.title ?: "Mymipo"
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) { loading = newProgress < 100 }
                            }
                            webView = this
                            if (address.isNotBlank()) loadUrl(address)
                        }
                    },
                    update = { webView = it }
                )
            }
        }
    }
}

@Composable
private fun TopAppBarCompat(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit,
    homePage: String
) {
    val shortcuts = listOf("Google" to "https://www.google.com", "YouTube" to "https://m.youtube.com", "Wikipedia" to "https://www.wikipedia.org", "GitHub" to "https://github.com")
    Column(modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(54.dp))
        Text("mymipo", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Ваш браузер. Ваш интернет.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = query, onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Поиск или адрес сайта") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { IconButton(onClick = onSearch) { Icon(Icons.Default.ArrowForward, "Найти") } }
        )
        Spacer(Modifier.height(28.dp))
        Text("Быстрый доступ", modifier = Modifier.align(Alignment.Start), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().height(190.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shortcuts) { (name, url) ->
                OutlinedButton(onClick = { onOpen(url) }, modifier = Modifier.height(64.dp)) { Text(name) }
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { onOpen(homePage) }) { Text("Открыть домашнюю страницу") }
        TextButton(onClick = onSettings) { Icon(Icons.Default.Settings, null); Spacer(Modifier.width(6.dp)); Text("Настройки") }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    homePage: String,
    onHomePageChange: (String) -> Unit,
    searchEngine: String,
    onSearchEngineChange: (String) -> Unit,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onClearData: () -> Unit
) {
    var homeDraft by remember(homePage) { mutableStateOf(homePage) }
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Поиск", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Поисковая система")
        listOf("Google", "Bing", "DuckDuckGo").forEach { engine ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = searchEngine == engine, onClick = { onSearchEngineChange(engine) })
                Text(engine)
            }
        }
        HorizontalDivider()
        Text("Главная страница", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = homeDraft, onValueChange = homeDraft.let { { value -> homeDraft = value } }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("URL домашней страницы") })
        Button(onClick = { onHomePageChange(homeDraft) }) { Text("Сохранить") }
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Тёмная тема", fontWeight = FontWeight.Medium)
                Text("Изменить оформление приложения", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange)
        }
        HorizontalDivider()
        Text("Конфиденциальность", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onClearData) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Очистить историю и кэш") }
        Text("Mymipo Browser · версия 1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
