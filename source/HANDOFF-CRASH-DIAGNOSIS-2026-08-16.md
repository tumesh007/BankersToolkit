# Banker's Toolkit — Crash Diagnosis Handoff

**Date:** 2026-08-16  
**Diagnosed by:** Codex agent session  
**Status:** Root cause identified, fix not yet applied

---

## 1. Executive Summary

The Android APK crashes immediately at startup on a physical device. The crash is **NOT** a Room database issue (previous hypothesis). The crash is caused by **Firebase Performance Monitoring auto-initializing against a placeholder API key** in `google-services.json`.

---

## 2. Current APK Identity

| Field | Value |
|---|---|
| **applicationId** | `com.example.app` |
| **versionCode** | `1` |
| **versionName** | Dynamic (reckon plugin) |
| **APK module** | `:cmp-android` |
| **APK output path** | `cmp-android/build/outputs/apk/prod/debug/cmp-android-prod-debug.apk` |
| **Build flavor** | `prodDebug` |
| **Namespace** | `cmp.android.app` |
| **App display name** | `Banker's Toolkit` (from `libs.versions.toml → appDisplayName`) |

---

## 3. Device Under Test

| Field | Value |
|---|---|
| **Manufacturer** | realme |
| **Model** | RMX2001 |
| **Android version** | 11 |
| **ADB serial** | DA9LJJH6RWQWQSBE |

---

## 4. Startup Path

```
AndroidApp.onCreate()                          (cmp-android/.../AndroidApp.kt)
  └─ initKoin { androidContext(this) }         (cmp-shared/.../KoinExt.kt)
       └─ startKoin { modules(KoinModules.allModules) }
       └─ WorkerKmpAuto.install()
       └─ initSyncNotifier()
       └─ AppInitializers.runAll()
  └─ restoreSavedLanguage()                    (runBlocking — reads UserDataRepository)
       
MainActivity.onCreate()                       (cmp-android/.../MainActivity.kt)
  └─ installSplashScreen()
  └─ setContent { SharedApp(...) }
       └─ ComposeApp(...)
            └─ BackboneRegistry.homeBody       (cmp-navigation/.../BackboneRegistry.kt)
                 └─ BankersToolkitDashboard()  (feature/home/.../BankersToolkitDashboard.kt)

[BACKGROUND — immediate, parallel to above]
  Firebase Performance SDK auto-initializes on pool-9-thread-1
    └─ TransportManager.syncLog()
         └─ FirebaseInstallations.getId()
              └─ preConditionChecks()
                   └─ THROWS IllegalArgumentException (invalid API key)
                        └─ PROCESS CRASH
```

---

## 5. The Crash — Full Stack Trace

```
08-16 20:04:36.236 E/AndroidRuntime( 4810): FATAL EXCEPTION: pool-9-thread-1
08-16 20:04:36.236 E/AndroidRuntime( 4810): Process: com.example.app, PID: 4810
08-16 20:04:36.236 E/AndroidRuntime( 4810): java.lang.IllegalArgumentException: Please set a valid API key. A Firebase API key is required to communicate with Firebase server APIs: It authenticates your project with Google.Please refer to https://firebase.google.com/support/privacy/init-options.
08-16 20:04:36.236 E/AndroidRuntime( 4810): 	at com.google.android.gms.common.internal.Preconditions.checkArgument(com.google.android.gms:play-services-basement@@18.9.0:2)
08-16 20:04:36.236 E/AndroidRuntime( 4810): 	at com.google.firebase.installations.FirebaseInstallations.preConditionChecks(FirebaseInstallations.java:179)
08-16 20:04:36.236 E/AndroidRuntime( 4810): 	at com.google.firebase.installations.FirebaseInstallations.getId(FirebaseInstallations.java:236)
08-16 20:04:36.236 E/AndroidRuntime( 4810): 	at com.google.firebase.perf.transport.TransportManager.updateFirebaseInstallationIdIfPossibleAndNeeded(TransportManager.java:566)
08-16 20:04:36.236 E/AndroidRuntime( 4810): 	at com.google.firebase.perf.transport.TransportManager.setApplicationInfoAndBuild(TransportManager.java:502)
08-16 20:04:36.236 E/AndroidRuntime( 4810): 	at com.google.firebase.perf.transport.TransportManager.syncLog(TransportManager.java:379)
08-16 20:04:36.236 E/AndroidRuntime( 4810): 	at com.google.firebase.perf.transport.TransportManager.lambda$log$2$com-google-firebase-perf-transport-TransportManager(TransportManager.java:301)
08-16 20:04:36.236 E/AndroidRuntime( 4810): 	at com.google.firebase.perf.transport.TransportManager$$ExternalSyntheticLambda2.run(D8$$SyntheticClass:0)
08-16 20:04:36.236 E/AndroidRuntime( 4810): 	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
08-16 20:04:36.236 E/AndroidRuntime( 4810): 	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
08-16 20:04:36.236 E/AndroidRuntime( 4810): 	at java.lang.Thread.run(Thread.java:923)
```

**Thread:** `pool-9-thread-1` (background, not main thread)  
**Exception:** `java.lang.IllegalArgumentException`  
**Message:** "Please set a valid API key."  
**First app-level source frame:** None — the entire trace is within Firebase/GMS SDK internals.  
**Trigger file:** `cmp-android/google-services.json` (placeholder API key)

---

## 6. Root Cause Analysis

### The Bad File

`cmp-android/google-services.json` contains placeholder values:

```json
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "com.google.samples.apps.template",
    "storage_bucket": "com.google.samples.apps.template.appspot.com"
  },
  "client": [{
    "client_info": {
      "mobilesdk_app_id": "1:000000000000:android:placeholder",
      "android_client_info": { "package_name": "com.example.app" }
    },
    "oauth_client": [],
    "api_key": [{ "current_key": "AIza-placeholder-android-api-key" }],
    "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
  }],
  "configuration_version": "1"
}
```

### The Dependency Chain

```
cmp-android
  └─ projects.core.firebase                     (core/firebase/build.gradle.kts)
       └─ projects.coreBase.firebase            (core-base/firebase/build.gradle.kts)
            └─ libs.cmp.firebase                (published cmp-firebase engine)
                 └─ dev.gitlive:firebase-perf:3.0.0-alpha01
                      └─ com.google.firebase:firebase-perf:22.0.6
```

`firebase-perf` auto-initializes via a `ContentProvider` at app startup. It runs `TransportManager.syncLog()` on a background thread pool, which calls `FirebaseInstallations.getId()`, which validates the API key. The placeholder key `AIza-placeholder-android-api-key` fails validation → `IllegalArgumentException` → uncaught on the thread pool → process crash.

### Why Analytics/Crashlytics Don't Crash First

The main manifest (`cmp-android/src/main/AndroidManifest.xml`) disables analytics and crashlytics collection:

```xml
<meta-data android:name="firebase_analytics_collection_deactivated" android:value="true" />
<meta-data android:name="firebase_crashlytics_collection_enabled" android:value="false" />
```

**But there is no equivalent disable for Firebase Performance.** The `prod` flavor manifest (`cmp-android/src/prod/AndroidManifest.xml`) actually **enables** all collection, overriding the defaults. Neither manifest sets `firebase_performance_collection_enabled` to `false`.

---

## 7. Room Database Status — NOT the Current Crash

| Field | Value |
|---|---|
| **AppDatabase.TEMPLATE_BASE_VERSION** | `11` |
| **ForkDatabaseConfig.VERSION_OFFSET** | `1` |
| **Effective VERSION** | `12` |
| **Schema files present** | `1.json` through `8.json`, `10.json`, `11.json`, `12.json` |
| **Missing schema files** | `9.json` (intentional — migration skips 8→10) |
| **AutoMigrations declared** | 3→4, 4→5, 5→6, 6→7, 7→8, 8→10 (with spec), 11→12 |
| **Is Room the current crash?** | **NO** |

### Latent Room Concern (NOT the current crash)

There is no `AutoMigration(from = 10, to = 11)` declared. The migration chain is: `…→ 8→10 → [gap] → 11→12`. If a device has an existing database at version 10, Room will fail to migrate to 12 because there's no 10→11 path. This would only matter for devices that previously had the app installed at schema version 10. **This is a separate issue from the current crash and should not be conflated.**

---

## 8. Template/Demo Code Status

The following template/demo artifacts exist in the codebase:

| Artifact | Location | Runs at startup? |
|---|---|---|
| `FredApiConfig` | `core/network/src/commonMain/kotlin/.../FredApiConfig.kt` | No — lazy, only when FRED feature is accessed |
| `CurrencyRepository` | `feature/currency-rates/` | No — only when currency rates screen is navigated to |
| `MacroIndicator` | `core/model/.../MacroIndicator.kt` | No — data class only |
| `BankersToolkitDashboard` | `feature/home/.../BankersToolkitDashboard.kt` | Yes — it IS the home screen body via `BackboneRegistry.homeBody` |

**None of the template/demo code causes the current crash.** The crash occurs before any Composable is rendered.

---

## 9. BankersToolkitDashboard Status

| Check | Result |
|---|---|
| Compiles? | **YES** (build succeeded) |
| Referenced by launcher Activity? | Yes, via `BackboneRegistry.homeBody` → `SharedApp` → `ComposeApp` |
| Reachable through navigation? | Yes — it's the home tab content |
| Actually displayed? | **NO** — app crashes before rendering due to Firebase |

---

## 10. Proposed Fixes (Smallest to Largest)

### Fix A — Disable Firebase Performance collection (smallest diff, immediate)

Add one `<meta-data>` to `cmp-android/src/main/AndroidManifest.xml` inside the `<application>` block:

```xml
<meta-data
    android:name="firebase_performance_collection_enabled"
    android:value="false" />
```

This prevents `firebase-perf` from auto-initializing and calling `FirebaseInstallations.getId()`. The placeholder key remains inert. Analytics and Crashlytics are already disabled in the non-prod manifest.

**Pros:** One line. No new dependencies. No structural changes.  
**Cons:** Firebase Performance is disabled even in prod (acceptable until a real `google-services.json` is provided).

### Fix B — Replace google-services.json (proper fix, requires Firebase project)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create/open a project
3. Register Android app with package name `com.example.app`
4. Download `google-services.json`
5. Replace `cmp-android/google-services.json` with the real one

**Pros:** All Firebase features work correctly.  
**Cons:** Requires a Firebase project and Google Cloud billing.

### Fix C — Disable Firebase Performance auto-init globally (alternative to A)

Add to `cmp-android/src/main/AndroidManifest.xml`:

```xml
<provider
    android:name="com.google.firebase.perf.provider.FirebasePerfProvider"
    android:authorities="${applicationId}.firebaseperfprovider"
    tools:node="remove" />
```

This removes the `ContentProvider` that bootstraps Firebase Perf entirely.

**Pros:** Perf SDK never loads at all.  
**Cons:** More aggressive; harder to re-enable later.

### Recommended Path

Apply **Fix A** now (unblocks development), then apply **Fix B** before any release build.

---

## 11. Reproduction Steps

```bash
# 1. Build
cd /home/pnb/BankersToolkit
./gradlew :cmp-android:assembleDebug

# 2. Install prod debug APK (matches installed flavor)
adb install -r cmp-android/build/outputs/apk/prod/debug/cmp-android-prod-debug.apk

# 3. Clear logcat
adb logcat -c

# 4. Launch
adb shell am start -n com.example.app/cmp.android.app.MainActivity

# 5. Wait ~4-10 seconds for background thread crash

# 6. Capture crash
adb logcat -d -v time AndroidRuntime:E *:S
```

---

## 12. What Was Ruled Out

| Hypothesis | Verdict | Evidence |
|---|---|---|
| Room schema mismatch | **Ruled out** | Build succeeds; schemas 11.json and 12.json exist; AutoMigration(11→12) declared; crash trace has zero Room frames |
| Koin dependency failure | **Ruled out** | No Koin exceptions in logcat; app initializes past `initKoin()` |
| Missing generated classes | **Ruled out** | Build succeeds with no KSP/codegen errors |
| Compose initialization failure | **Ruled out** | Crash is on `pool-9-thread-1`, not main thread; no Compose frames in trace |
| Navigation misconfiguration | **Ruled out** | `BackboneRegistry` wires `BankersToolkitDashboard` correctly; build compiles |
| Template code interference | **Ruled out** | FRED/Currency/Macro code is lazy-loaded, not startup |
| Native library crash | **Ruled out** | No SIGABRT/SIGSEGV in logcat |
| WorkManager initialization | **Ruled out** | `WorkerKmpAuto.install()` runs after `startKoin` without error |

---

## 13. Files of Interest

| File | Role |
|---|---|
| `cmp-android/google-services.json` | **THE PROBLEM** — placeholder Firebase config |
| `cmp-android/src/main/AndroidManifest.xml` | Launcher, application class, Firebase meta-data |
| `cmp-android/src/prod/AndroidManifest.xml` | Enables Firebase collection for prod flavor |
| `cmp-android/src/main/kotlin/cmp/android/app/AndroidApp.kt` | Application class — `initKoin()` + `restoreSavedLanguage()` |
| `cmp-android/src/main/kotlin/cmp/android/app/MainActivity.kt` | Launcher Activity — `SharedApp()` |
| `cmp-shared/src/commonMain/kotlin/cmp/shared/utils/KoinExt.kt` | `initKoin()` — Koin + workers + notifier + AppInitializers |
| `cmp-navigation/.../BackboneRegistry.kt` | Wires `BankersToolkitDashboard` as home body |
| `feature/home/.../BankersToolkitDashboard.kt` | The actual dashboard Composable |
| `core/database/.../AppDatabase.kt` | Room database (version 12, NOT the current crash) |
| `core/database/.../ForkDatabaseConfig.kt` | VERSION_OFFSET = 1 |
| `core-base/firebase/build.gradle.kts` | Pulls in `cmp-firebase` → transitively `firebase-perf` |
| `gradle/libs.versions.toml` | `appId = "com.example.app"`, `appDisplayName = "Banker's Toolkit"` |
| `secrets/sample/android/firebase/google-services.json` | Sample placeholder (also fake) |

---

## 14. Open Questions for Project Owner

1. **Is there a real Firebase project for this app?** If so, download `google-services.json` from Firebase Console and replace `cmp-android/google-services.json`.

2. **Is `com.example.app` the intended final applicationId?** The version catalog still has `appId = "com.example.app"` which is a placeholder.

3. **Is Firebase Performance Monitoring wanted?** It's pulled in transitively via `dev.gitlive:firebase-perf`. If not needed, it could be excluded from the dependency tree entirely.

4. **Room migration gap 10→11:** No `AutoMigration(from = 10, to = 11)` exists. Is this intentional? If any device has a database at version 10, it cannot migrate to 12.

---

*End of handoff document.*
