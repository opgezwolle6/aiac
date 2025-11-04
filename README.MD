# AIAC - YandexGPT Chat Application

Chat application with YandexGPT integration built with Compose Multiplatform.

## Setup

### 1. Configure YandexGPT API

Configure your YandexGPT API credentials by editing the configuration file:

Edit `sharedUI/src/commonMain/kotlin/com/raremartial/aiac/di/KoinInit.kt`:
```kotlin
object YandexConfig {
    const val API_KEY = "YOUR_API_KEY_HERE"
    const val FOLDER_ID = "YOUR_FOLDER_ID_HERE"
}
```

Replace `YOUR_API_KEY_HERE` and `YOUR_FOLDER_ID_HERE` with your actual credentials.

You can get your API key and Folder ID from [Yandex Cloud Console](https://console.cloud.yandex.ru/).

> **Note**: This file should be added to `.gitignore` if you don't want to commit your API credentials. The configuration is shared across all platforms (Android, iOS, Desktop).

### 1.1. Настройка прав доступа для сервисного аккаунта

Если вы получаете ошибку доступа `Permission to [resource-manager.folder...] denied`, необходимо настроить права для сервисного аккаунта:

1. **Откройте консоль Yandex Cloud**: https://console.cloud.yandex.ru/
2. **Перейдите в раздел "Сервисные аккаунты"**:
   - В меню слева выберите "Сервисные аккаунты"
   - Или используйте прямой путь: https://console.cloud.yandex.ru/cloud?service=iam&section=serviceAccounts
3. **Найдите сервисный аккаунт**, к которому привязан ваш API-ключ
4. **Назначьте необходимые роли**:
   - Нажмите на сервисный аккаунт
   - Перейдите на вкладку "Роли"
   - Нажмите "Назначить роли"
   - Выберите роль: **`ai.languageModels.user`** (или `ai.languageModels.editor`)
   - Убедитесь, что роль назначена на нужную папку (Folder ID из конфигурации)
5. **Проверьте доступ к папке**:
   - Убедитесь, что сервисный аккаунт имеет доступ к папке с ID из вашей конфигурации
   - При необходимости назначьте роль `viewer` или `editor` на уровне папки

**Подробная документация**: https://yandex.cloud/ru/docs/ai-studio/operations/get-api-key

После настройки прав подождите 1-2 минуты для применения изменений и повторите запрос.

### 2. Run the Application

#### Android
To run the application on android device/emulator:  
 - Open project in Android Studio and run imported android run configuration  

To build the application bundle:  
 - Run `./gradlew :androidApp:assembleDebug`  
 - Find `.apk` file in `androidApp/build/outputs/apk/debug/androidApp-debug.apk`  

#### Desktop
Run the desktop application: `./gradlew :desktopApp:run`  
Run the desktop **hot reload** application: `./gradlew :desktopApp:hotRun --auto`  

#### iOS
To run the application on iPhone device/simulator:  
 - Open `iosApp/iosApp.xcproject` in Xcode and run standard configuration  
 - Or use [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio

## Architecture

The project follows MVI (Model-View-Intent) architecture pattern:
- **Data Layer**: Models, Mappers, Repository
- **Network Layer**: YandexGPT API client using Ktor
- **Presentation Layer**: ViewModel, State, Actions, Events
- **UI Layer**: Jetpack Compose screens and components

## Tech Stack

- **Compose Multiplatform** - UI framework
- **Koin** - Dependency Injection
- **Ktor** - HTTP client
- **kotlinx.serialization** - JSON serialization
- **kotlinx.datetime** - Date/time handling
- **Room** - Local database (planned)
- **Material Design 3** - UI components

## Features

- ✅ Real-time chat with YandexGPT
- ✅ Minimalistic Material Design 3 UI
- ✅ Smooth animations and transitions
- ✅ Multiplatform support (Android, iOS, Desktop)
- ✅ MVI architecture pattern  

