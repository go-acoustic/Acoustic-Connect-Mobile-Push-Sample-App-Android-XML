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
    ConnectAdvancedConfig.json        # SDK feature flags and capture behaviour
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

---

## Requirements

- Android Studio Hedgehog or later
- Android 7.0 (API 24) minimum
- Target SDK 36
- Kotlin 1.9.x
- For FCM: device or emulator with Google Play Services
- For HMS: Huawei device or emulator with HMS Core 5.0+

---

## Documentation

- Integration Guide — Full step-by-step guide covering Firebase and AppGallery Connect setup, SDK installation, push configuration, testing, and troubleshooting

---

## License

Copyright (C) 2026 Acoustic, L.P. All rights reserved.