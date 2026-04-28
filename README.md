# Acoustic Connect Push Sample App — Android (XML)

Sample app demonstrating mobile push notification integration with the Acoustic Connect Android SDK using traditional XML layouts and Fragments.

Use this alongside the Integration Guide to see a working implementation of push registration, notification handling, and identity logging across both FCM (Firebase) and HMS (Huawei) providers.

---

## What's included

| Feature | Description |
|---|---|
| Push registration | Automatic provider detection (`strictProvider = null`) supporting both FCM and HMS |
| Notification authorization | Request and display push permission status (Android 13+) |
| Analytics capture | Enabled by default — events, screenshots, and screen visits out of the box |
| Identity logging | Log identity signals and view recent history (last 5 entries) |
| Dual provider support | FCM via Firebase, HMS via Huawei AppGallery Connect |

---

## SDK dependencies — which artifact to use

The Connect SDK is published to Maven Central under `io.github.go-acoustic` as four separate artifacts. Pick the one that matches your push requirements — the push artifacts pull `connect` (and the matching vendor SDK) transitively, so you never need to declare `connect` yourself when using a push variant.

```kotlin
// No push at all — only core analytics / events
implementation("io.github.go-acoustic:connect:<version>")

// FCM only (Google Play Store builds)
implementation("io.github.go-acoustic:connect-push-fcm:<version>")

// HMS only (Huawei AppGallery builds)
implementation("io.github.go-acoustic:connect-push-hms:<version>")

// Both providers (single APK that auto-detects FCM or HMS at runtime)
implementation("io.github.go-acoustic:connect-push:<version>")
```

| Artifact | Pulls in | Use when |
|---|---|---|
| `connect` | Core SDK only | Analytics / events only, no push |
| `connect-push-fcm` | `connect` + Firebase Messaging | Google Play build, FCM push |
| `connect-push-hms` | `connect` + HMS Push Kit | Huawei AppGallery build, HMS push |
| `connect-push` | `connect-push-fcm` + `connect-push-hms` | Single APK supporting both providers |

---

## Getting started

For the complete step-by-step walkthrough — including Firebase project setup, FCM configuration, Huawei AppGallery Connect setup, HMS Push Kit enablement, SHA-256 fingerprint registration, and troubleshooting — see the Integration Guide.

The quick-start steps below assume you have completed the prerequisites in the guide.

### 1. Clone the repository

```bash
git clone https://github.com/go-acoustic/Acoustic-Connect-Mobile-Push-Sample-App-Android-XML.git
cd Acoustic-Connect-Mobile-Push-Sample-App-Android-XML
```

### 2. Open in Android Studio

Open the project root in Android Studio. Gradle will sync and resolve all dependencies automatically.

### 3. Add push provider configuration files

Place your provider config files in the `app/` directory before building:

| File | Provider | Where to obtain |
|---|---|---|
| `google-services.json` | FCM (Firebase) | Firebase Console → Project settings → Your apps |
| `agconnect-services.json` | HMS (Huawei) | AppGallery Connect → My apps → Download config |

> Both files are excluded from version control. The project ships with a placeholder `agconnect-services.json` — replace it with your own.

### 4. Configure your credentials

Open `app/src/main/java/.../AcousticCredentials.kt` and set your Acoustic app key and collector URL:

```kotlin
object AcousticCredentials {
    const val APP_KEY = "YOUR_APP_KEY"
    const val COLLECTOR_URL = "YOUR_COLLECTOR_URL"
}
```

`MainActivity.onCreate()` reads these constants directly when calling `Connect.enable(...)`.

### 5. Register SHA-256 fingerprint (HMS only)

HMS Push Kit requires your signing certificate fingerprint to be registered in AppGallery Connect.

Get the debug fingerprint:

```bash
./gradlew signingReport
```

Copy the **SHA-256** value from the `debug` variant and add it in AppGallery Connect → My apps → General information → SHA-256 certificate fingerprint. Then re-download `agconnect-services.json` and replace the file in `app/`.

### 6. Build and run

Select the `app` run configuration and run on a device or emulator.

- **FCM** — works on any device with Google Play Services
- **HMS** — works on Huawei devices or emulators with HMS Core

---

## Project structure

```
app/src/main/
  java/.../
    AcousticCredentials.kt            # APP_KEY and COLLECTOR_URL constants — edit before running
    MainActivity.kt                   # AppCompatActivity — SDK init, NavController, BottomNav
    notification/
      NotificationFragment.kt         # Push authorization UI
      NotificationViewModel.kt        # Push lifecycle, token fetch, SDK enable/disable
    identity/
      IdentityFragment.kt             # Identity logging UI
      IdentityViewModel.kt            # logIdentificationEvent, history (last 5)

  res/
    layout/
      activity_main.xml               # CoordinatorLayout with FragmentContainerView + BottomNavigationView
      fragment_notification.xml       # Notification authorization card
      fragment_identity.xml           # Identity input card + history card
      item_history_entry.xml          # Single history row
    navigation/
      nav_graph.xml                   # NavHostFragment destinations
    menu/
      bottom_nav_menu.xml             # Bottom navigation items

  assets/
    ConnectBasicConfig.properties     # App key, collector URL, session and image settings
    ConnectAdvancedConfig.json        # Connect SDK feature flags and capture behaviour
    ConnectLayoutConfig.json          # Per-screen capture rules and masking

  AndroidManifest.xml                 # INTERNET, NETWORK_STATE, POST_NOTIFICATIONS
  agconnect-services.json             # Huawei AppGallery Connect config (replace with yours)

app/
  google-services.json                # Firebase config (add your own — not in repo)
```

---

## Push provider selection

The sample app passes `strictProvider = null` to `ConnectPushConfig`, which lets the SDK auto-detect the best available provider at runtime:

- On devices with Google Play Services → **FCM**
- On Huawei devices without Play Services → **HMS**

To force a specific provider, set `strictProvider` explicitly:

```kotlin
ConnectPushConfig(
    application = application,
    iconRes = R.drawable.ic_notification,
    strictProvider = MobileServiceType.HMS,   // or MobileServiceType.FCM
    onTokenReady = { token -> ... },
    onFailure = { exception -> ... },
    onPermissionResult = { isGranted -> ... },
)
```

---

## Setup per scenario

Each of the four SDK artifacts has its own setup path. Pick the section that matches what you want to ship — every one is self-contained and tells you which Gradle plugins, repositories, config files, dependencies, and `ConnectPushConfig` to use.

The sample app in this repository ships configured for the **both-providers** scenario (single APK with FCM + HMS).

---

## Both-providers setup (`connect-push`)

Use `connect-push` when you want a single APK that auto-detects FCM on Google Play devices and HMS on Huawei devices at runtime.

### 1. `settings.gradle.kts`

Enable the Huawei repo for both plugin resolution and dependency resolution:

```kotlin
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.huawei.agconnect") {
                useModule("com.huawei.agconnect:agcp:${requested.version}")
            }
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}
```

### 2. `build.gradle.kts` (root)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.agconnect) apply false
}

buildscript {
    repositories {
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
    dependencies {
        classpath(libs.agcp)
    }
}
```

### 3. `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    id("com.huawei.agconnect")
}

dependencies {
    implementation("io.github.go-acoustic:connect-push:<version>")
    // connect-push pulls connect, connect-push-fcm, connect-push-hms and HMS Push transitively.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
}
```

### 4. Config files

Place both in `app/`:

| File | Where to obtain |
|---|---|
| `google-services.json` | Firebase Console → Project settings → Your apps |
| `agconnect-services.json` | AppGallery Connect → My apps → Download config (with SHA-256 fingerprint registered — see step 5 of [Getting started](#getting-started)) |

### 5. `ConnectPushConfig`

```kotlin
ConnectPushConfig(
    application = application,
    iconRes = R.drawable.ic_notification,
    strictProvider = null,   // auto-detect FCM or HMS at runtime 
    onTokenReady = { token -> ... },
    onFailure = { exception -> ... },
    onPermissionResult = { isGranted -> ... },
)
```

---

## FCM-only setup (`connect-push-fcm`)

Use `connect-push-fcm` for Google Play builds that do not need HMS. No Huawei plugin, no Huawei repo, no `agconnect-services.json`.

### 1. `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

### 2. `build.gradle.kts` (root)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
    // no agconnect plugin
}

buildscript {
    repositories {
        google()
        mavenCentral()
        // no maven { url = uri("https://developer.huawei.com/repo/") }
    }
    dependencies {
        classpath(libs.gradle)
        // no classpath(libs.agcp)
    }
}
```

### 3. `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    // no id("com.huawei.agconnect")
}

dependencies {
    implementation("io.github.go-acoustic:connect-push-fcm:<version>")
    
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    // Optional — add only if you want Firebase Analytics:
    // implementation("com.google.firebase:firebase-analytics-ktx")
}
```

### 4. Config files

Place in `app/`:

| File | Where to obtain |
|---|---|
| `google-services.json` | Firebase Console → Project settings → Your apps |

Do not ship `agconnect-services.json`.

### 5. `ConnectPushConfig`

```kotlin
ConnectPushConfig(
    application = application,
    iconRes = R.drawable.ic_notification,
    strictProvider = MobileServiceType.FCM,
    onTokenReady = { token -> ... },
    onFailure = { exception -> ... },
    onPermissionResult = { isGranted -> ... },
)
```

> Only devices with Google Play Services will receive push notifications.

---

## HMS-only setup (`connect-push-hms`)

Use `connect-push-hms` for AppGallery builds that do not need FCM. No Google Services plugin, no Firebase deps, no `google-services.json`.

### 1. `settings.gradle.kts`

```kotlin
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.huawei.agconnect") {
                useModule("com.huawei.agconnect:agcp:${requested.version}")
            }
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}
```

### 2. `build.gradle.kts` (root)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.agconnect) apply false
    // no google-services plugin
}

buildscript {
    repositories {
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
    dependencies {
        classpath(libs.agcp)
    }
}
```

### 3. `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.huawei.agconnect")
    // no alias(libs.plugins.google.services)
}

dependencies {
    implementation("io.github.go-acoustic:connect-push-hms:<version>")
    // connect-push-hms transitively pulls connect and HMS Push.
    // Do not add any Firebase dependencies.
}
```

### 4. Config files

Place in `app/`:

| File | Where to obtain |
|---|---|
| `agconnect-services.json` | AppGallery Connect → My apps → Download config (with SHA-256 fingerprint registered — see step 5 of [Getting started](#getting-started)) |

Do not ship `google-services.json`.

### 5. `ConnectPushConfig`

```kotlin
ConnectPushConfig(
    application = application,
    iconRes = R.drawable.ic_notification,
    strictProvider = MobileServiceType.HMS,
    onTokenReady = { token -> ... },
    onFailure = { exception -> ... },
    onPermissionResult = { isGranted -> ... },
)
```

---

## Analytics-only setup (`connect`)

Use `connect` when you only need analytics capture and do not want to register for push. No push plugins, no push config files, no `ConnectPushConfig`.

### 1. `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

### 2. `build.gradle.kts` (root)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // no google-services, no agconnect
}

buildscript {
    repositories {
        google()
        mavenCentral()
        // no maven { url = uri("https://developer.huawei.com/repo/") }
    }
    dependencies {
        classpath(libs.gradle)
        // no classpath(libs.agcp)
    }
}
```

> No push plugins, so the Huawei Maven repo and `agcp` classpath are not needed.

### 3. `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // no push plugins
}

dependencies {
    implementation("io.github.go-acoustic:connect:<version>")
    // No Firebase or HMS dependencies.
}
```

### 4. Config files

Neither `google-services.json` nor `agconnect-services.json` is needed.

### 5. Initialise the SDK without push

In `MainActivity.onCreate()`, call `Connect.init(application)` once and then `Connect.enable(...)` without a `ConnectPushConfig`:

```kotlin
Connect.init(application)
Connect.enable(
    appKey = AcousticCredentials.APP_KEY,
    postMessageUrl = AcousticCredentials.COLLECTOR_URL,
)
```

> The SDK will capture events, screen visits, and screenshots as normal — push notifications will simply not be registered or delivered.

---

## Analytics capture

The SDK captures user interactions, screen visits, and screenshots automatically with no additional configuration. `ConnectLayoutConfig.json` controls per-screen capture rules:

- User events (taps, text changes)
- Screen transition tracking
- Screenshots for session replay
- Sensitive data masking

To customise capture for specific screens or add masking rules, edit `app/src/main/assets/ConnectLayoutConfig.json`. See the Integration Guide for the full schema.

---

## Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `907135700: get scope error` | Push Kit not enabled in AppGallery Connect | Enable Push Kit: My apps → Develop → APIs enabled |
| `907135702: certificate fingerprint empty` | SHA-256 not registered in AppGallery Connect | Add fingerprint and re-download `agconnect-services.json` |
| `Failed to resolve: com.google.firebase:firebase-messaging:null` | `google-services.json` missing | Add your `google-services.json` to the `app/` directory |
| Push token never arrives | Notification permission denied (Android 13+) | Grant `POST_NOTIFICATIONS` permission when prompted |
| Push sent from dashboard but app receives nothing | Identity signal not yet flushed to collector when push was dispatched | Wait for the identity signal collector POST to return HTTP 200 (visible in logcat) before sending the push — the SDK flushes on a ~30s interval; a network interruption can delay the flush further |
| `RuntimeException: Using WebView from more than one process at once with the same data directory is not supported` crashing in `:pushservice` when a push arrives | HMS declares `HmsMsgService` with `android:process=":pushservice"`, so `Application.onCreate()` re-runs in that second process. If Connect is initialised there it constructs a WebView and collides with the main process's WebView data-dir lock | Do not initialise Connect in a custom `Application.onCreate()`. Call `Connect.init(application)` and `Connect.enable(...)` from `MainActivity.onCreate()` (or any activity) — activities only run in the main process. This demo follows that pattern and does not declare an `Application` subclass |

---

## Requirements

- Android Studio Hedgehog or later
- Android 8.0 (API 26) minimum
- Target SDK 36
- Kotlin 1.9.x (pinned — Huawei AGConnect 1.9.1.304 is not compatible with Kotlin 2.0)
- Acoustic Connect SDK `11.0.5`, Tealeaf `10.4.21`, EOCore `2.1.24-beta`
- For FCM: device or emulator with Google Play Services
- For HMS: Huawei device or emulator with HMS Core 5.0+

---

## Documentation

- Integration Guide — Full step-by-step guide covering Firebase and AppGallery Connect setup, SDK installation, push configuration, testing, and troubleshooting

---

## License

Copyright (C) 2026 Acoustic, L.P. All rights reserved.