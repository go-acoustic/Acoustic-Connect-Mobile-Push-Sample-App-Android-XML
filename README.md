# Acoustic Connect Push Sample App — Android (XML)

Sample app demonstrating mobile push notification integration with the Acoustic Connect Android SDK using traditional XML layouts and Fragments.

Use this alongside the Integration Guide to see a working implementation of push registration, notification handling, and identity logging across both FCM (Firebase) and HMS (Huawei) providers.

---

## What's included

| Feature | Description |
|---|---|
| Push registration | Automatic provider detection (`strictProvider = null`) supporting both FCM and HMS |
| Notification authorization | Request and display push permission status (Android 13+) |
| SDK enable / disable | Toggle Connect SDK at runtime with custom app key and collector URL |
| Analytics capture | Enabled by default — events, screenshots, and screen visits out of the box |
| Identity logging | Log identity signals and view recent history (last 5 entries) |
| Dual provider support | FCM via Firebase, HMS via Huawei AppGallery Connect |

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

The app reads credentials from `SharedPreferences` at runtime, with fallback defaults baked into `MainActivity.kt`. To point the app at your own Acoustic environment, either:

**Option A — Edit the defaults in source:**

Open `app/src/main/java/.../MainActivity.kt` and replace the fallback values:

```kotlin
val appKey = prefs.getString(ConnectConstants.CLIENT_APP_ID_KEY, null)
    ?: "YOUR_APP_KEY"          // <-- your Acoustic app key
val collectorUrl = prefs.getString(ConnectConstants.COLLECTOR_URL_KEY, null)
    ?: "YOUR_COLLECTOR_URL"    // <-- your collector URL
```

**Option B — Enter credentials at runtime:**

Use the Notification screen in the running app to enter and save your app key and collector URL. Values are persisted to `SharedPreferences` and survive app restarts.

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

## FCM-only setup (no Huawei HMS)

If you do not need Huawei HMS and only want to use Firebase FCM, remove all Huawei dependencies and configuration. This avoids the Gradle error caused by a missing `agconnect-services.json`.

### 1. Remove the Huawei plugin resolution and repository from `settings.gradle.kts`

```kotlin
pluginManagement {
    // Remove the entire resolutionStrategy block below:
    // resolutionStrategy {
    //     eachPlugin {
    //         if (requested.id.id == "com.huawei.agconnect") {
    //             useModule("com.huawei.agconnect:agcp:${requested.version}")
    //         }
    //     }
    // }
    repositories {
        google { ... }
        mavenCentral()
        gradlePluginPortal()
        // maven { url = uri("https://developer.huawei.com/repo/") }  // <-- remove this line
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://developer.huawei.com/repo/") }  // <-- don't remove this line
    }
}
```

### 2. Remove the Huawei plugin declaration from `build.gradle.kts` (root)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.spotbugs) apply false
    // alias(libs.plugins.agconnect) apply false  // <-- remove this line
}
```

Also remove the Huawei `buildscript` block if present:

```kotlin
// Remove the block below entirely:
// buildscript {
//     repositories {
//         maven { url = uri("https://developer.huawei.com/repo/") }
//     }
//     dependencies {
//         classpath(libs.agcp)
//     }
// }
```

### 3. Remove the Huawei AGConnect plugin from `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    // id("com.huawei.agconnect")  // <-- remove this line
}
```

### 4. Keep Acoustic SDK dependencies unchanged

No changes needed to `libs.connect` or the Acoustic SDK dependencies in `app/build.gradle.kts`.

```kotlin
dependencies {
    // ...

    implementation(libs.connect)
    implementation(libs.tealeaf)
    implementation(libs.eocore)

    // Keep Firebase dependencies:
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
}
```

### 5. Remove `agconnect-services.json`

The `agconnect-services.json` file is not required when HMS is removed. You can delete it from `app/` or leave it in place — it will be ignored.

### 6. Force FCM provider

With HMS removed, set `strictProvider` to `MobileServiceType.FCM` so the SDK does not attempt HMS initialisation:

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

### 7. Ensure `google-services.json` is in place

Place your `google-services.json` in the `app/` directory. Obtain it from Firebase Console → Project settings → Your apps.

> After these changes the project builds and runs without `agconnect-services.json`. Only devices with Google Play Services will receive push notifications.

---

## HMS-only setup (no Firebase)

If you do not have a Firebase project and do not need FCM, you can remove all Firebase dependencies and run with Huawei HMS only. This avoids the Gradle build error caused by a missing `google-services.json`.

### 1. Remove the Google Services plugin

**`build.gradle.kts` (root)**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // alias(libs.plugins.google.services) apply false  // <-- remove this line
    alias(libs.plugins.spotbugs) apply false
    alias(libs.plugins.agconnect) apply false
}
```

Also remove the `google()` / `mavenCentral()` buildscript classpath for Google Services if present.

**`app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // alias(libs.plugins.google.services)  // <-- remove this line
    id("com.huawei.agconnect")
}
```

### 2. Remove Firebase dependencies and exclude transitive ones

In `app/build.gradle.kts`, remove the Firebase block and add `exclude` on `libs.connect` to prevent it from pulling Firebase in transitively:

```kotlin
dependencies {
    // ...

    implementation(libs.connect) {
        exclude(group = "com.google.firebase")
        exclude(group = "com.google.gms")
    }

    // Remove or comment out the three lines below:
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.messaging)
    // implementation(libs.firebase.analytics)
}
```

### 3. Force HMS provider

With Firebase removed, set `strictProvider` to `MobileServiceType.HMS` so the SDK does not attempt FCM initialisation:

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

### 4. Ensure `agconnect-services.json` is in place

The `agconnect-services.json` file must be present in `app/` and must contain your app's SHA-256 fingerprint. See step 5 of [Getting started](#getting-started) for instructions on obtaining and registering the fingerprint.

> After these changes the project builds and runs without `google-services.json`. Only Huawei devices or emulators with HMS Core will receive push notifications.

---

## Analytics-only setup (no push notifications)

If you only need analytics capture and do not require push notifications at all, you can remove both FCM and HMS entirely. No `google-services.json` or `agconnect-services.json` is needed.

### 1. Remove both push plugins from `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google { ... }
        mavenCentral()
        gradlePluginPortal()
        // maven { url = uri("https://developer.huawei.com/repo/") }  // <-- remove this line
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
         maven { url = uri("https://developer.huawei.com/repo/") }  // <-- don't remove this line
    }
}
```

### 2. Remove both push plugins from `build.gradle.kts` (root)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // alias(libs.plugins.google.services) apply false  // <-- remove this line
    alias(libs.plugins.spotbugs) apply false
    // alias(libs.plugins.agconnect) apply false        // <-- remove this line
}

// Remove the Huawei buildscript block entirely if present:
// buildscript {
//     repositories {
//         maven { url = uri("https://developer.huawei.com/repo/") }
//     }
//     dependencies {
//         classpath(libs.agcp)
//     }
// }
```

### 3. Remove both push plugins from `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // alias(libs.plugins.google.services)  // <-- remove this line
    // id("com.huawei.agconnect")           // <-- remove this line
}
```

### 4. Remove all Firebase and HMS dependencies

```kotlin
dependencies {
    // ...

    // Remove or comment out all Firebase and HMS dependencies:
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.messaging)
    // implementation(libs.firebase.analytics)
}
```

### 5. Remove `ConnectPushConfig` initialisation from `MainActivity.kt`

Do not call `Connect.enable(...)` with a `ConnectPushConfig`. Instead, initialise the SDK without push:

```kotlin
Connect.enable(application, appKey, collectorUrl)
```

> After these changes the project builds and runs without any push configuration files. The Connect SDK will capture analytics, user interactions, and screen visits as normal — push notifications will simply not be registered or delivered.

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

---

## Requirements

- Android Studio Hedgehog or later
- Android 7.0 (API 24) minimum
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