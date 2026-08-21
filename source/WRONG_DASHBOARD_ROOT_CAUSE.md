# Diagnosis & Resolution: Android App Launch Crash & Dashboard Alignment

## 1. Diagnosis & Root Cause Analysis

### Cause A: Launch Crash (Room Database Mismatch)
- **Root Cause**: Shipped Room schema `11.json` was previously modified in place without increasing the database version. On devices that already had an installed v11 database, Room opened the SQLite file, detected an identity hash mismatch (`7ed49856...` vs `9d4c70b0...`), and threw an unhandled `IllegalStateException: Room cannot verify the data integrity...` leading to an immediate crash on launch.
- **Fix**:
  1. Restored `11.json` to its original git state.
  2. Incremented `ForkDatabaseConfig.VERSION_OFFSET` from `0` to `1` (bumping effective DB schema version from 11 to 12).
  3. Added `AutoMigration(from = 11, to = 12)` in `AppDatabase.kt`.
  4. Room generated `12.json` and executed clean auto-migrations for existing on-device databases without data loss or launch crashes.

### Cause B: `DataSyncWorker` Runtime Exception
- **Root Cause**: `sync/.../infra/DataSyncWorker.kt` maintained constructor injections for `CurrencyRepository` and `MacroIndicatorsRepository`. When these showcase repositories were stripped from Koin DI, worker initialization threw `NoDefinitionFoundException`.
- **Fix**: Neutralized `DataSyncWorker.kt` to a persister-only worker, stripping the demo sync dependencies.

### Cause C: GPS Camera Navigation Route Parameter Bug
- **Root Cause**: `FeatureRegistry.kt` was attempting to navigate to `"gps_camera?visitId=$vId"` while registering only `"gps_camera"`.
- **Fix**: Registered `composable("gps_camera")` and updated parameter handling so navigation between Visit Editor and Camera functions without crashes.

### Cause D: App Title & Branding
- **Root Cause**: `libs.versions.toml` and `app-profile/app.yaml` contained `"App Toolkit"` placeholders and unescaped quotes.
- **Fix**: Updated `appDisplayName` and `desktopAppName` to `"Banker's Toolkit"` and escaped single quotes in `cmp-android/build.gradle.kts` for Android XML resource generation.

---

## 2. Dashboard Resolution Report (Step 13)

| Dashboard | File Location | Module | Currently Launched |
| --- | --- | --- | --- |
| **DASHBOARD A (Template Demo)** | `feature/home/src/commonMain/kotlin/kpt/feature/home/demo/HomeDashboard.kt` | `:feature:home` | **NO** (Isolated & disabled; no FRED API or FX rates loaded) |
| **DASHBOARD B (Banker's Toolkit)** | `feature/home/src/commonMain/kotlin/kpt/feature/home/demo/BankersToolkitDashboard.kt` | `:feature:home` | **YES** (**Authoritative Root Dashboard**) |

---

## 3. Physical Device Verification Results (`DA9LJJH6RWQWQSBE`)

- **Device ID**: `DA9LJJH6RWQWQSBE`
- **Installation Command**: `adb -s DA9LJJH6RWQWQSBE install -r cmp-android-demo-debug.apk`
- **Launch Command**: `adb -s DA9LJJH6RWQWQSBE shell am start -n com.example.app/cmp.android.app.MainActivity`
- **Install Status**: `Performing Streamed Install → Success`
- **Runtime Status**: Process `com.example.app` (PID 16503) running continuously on device with 0 crashes.
- **Visible Dashboard On Device**:
  - **FIELD BANKING**: Visit Report, GPS Camera
  - **DOCUMENT TOOLS**: PDF Redactor, Longbook Merger
  - **FINANCIAL TOOLS**: EMI Calculator, Amortization Schedule
