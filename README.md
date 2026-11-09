# Mymipo Browser

Android browser built with Kotlin, Jetpack Compose and Android WebView.

## Features

- Home screen with search/address input and quick links
- Search with Google, Bing or DuckDuckGo
- Open websites in Android WebView
- Browser navigation: back and reload
- Settings for search engine, home page and dark theme
- Clear WebView browsing history and cache

## Requirements

- JDK 17
- Gradle 8.9
- Android SDK 35
- Minimum Android version: 8.0 (API 26)

## Build

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## CI

GitHub Actions builds the debug APK on every push and pull request to `main`.
The APK is uploaded as the `mymipo-debug-apk` workflow artifact.
