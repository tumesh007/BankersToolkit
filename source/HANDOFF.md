# BANKER'S TOOLKIT — PROJECT HANDOFF DOCUMENTATION

**Project**: Banker's Toolkit (v2)  
**Root Framework**: Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP) based on `openMF/kmp-project-template`  
**Target Platform**: Android (Min SDK 26 / Target SDK 37), with cross-platform KMP readiness  
**Database**: Room 3 SQLite (Schema Version 12)  

---

## 1. Executive Summary

**Banker's Toolkit** is a single, unified, local-first banking utility application. It combines field visit reporting, GPS photography, EMI calculations, and loan amortization schedules into one cohesive, professional interface.

All features operate **100% offline** without mandatory network dependencies or external API keys.

**Removed from Android app (per requirements)**:
- PDF Reader
- PDF Redactor (module retained at `feature:pdf-redactor` and separate project `/home/pnb/Projects/Pdf-Redactor`)
- Longbook Merger (module commented out)

---

## 2. Integrated Feature Modules

### A. FIELD BANKING
1. **Visit Report (`:feature:visit-report`)**:
   - **Capabilities**: Complete visit report creation, draft auto-saving, observation/recommendation/remarks entry, visit history search, and report deletion.
   - **Report Export**:
     - **PDF Exporter (`VisitPdfExporter.kt`)**: Multi-page A4 PDF report generation with searchable text, headers, officer notes, **actual embedded photographs (JPEG/DCTDecode)**, GPS metadata tables, and **offline vector location maps** (grid, N-arrow, marker, accuracy circle, scale bar).
     - **DOCX Exporter (`VisitDocxExporter.kt`)**: Editable OpenXML Word document format with **embedded photograph media parts**, GPS metadata, and **offline 5×5 grid location maps** with center marker.

2. **GPS Camera (`:feature:gps-camera`)**:
   - **Capabilities**: Standalone and Visit-tied photo capture (`Visit → Camera → Capture`).
   - **Tamper-Proof Verification**: Mock/spoofed GPS detection (`isMockProvider`). Capture is safely blocked if mock locations are detected.
   - **Embedded Audit Watermark**: Photos are stamped directly with a semi-transparent dark audit banner displaying coordinates, physical address, UTC timestamp, and Case/Officer IDs.
   - **Mini-Map Thumbnail**: Fetches a 256x256 OSM static map tile and renders it on the watermark with a location pin.
   - **EXIF Metadata**: Writes high-accuracy DMS coordinates, datetime, and audit trails natively to the JPEG EXIF tags.
   - **Upload from Gallery**: Modern `PickVisualMedia` integration allows selection of existing photos, automatically tagged with current verified location.
   - **Photo Categories**: *Property, Business Premises, Residence, Stock, Machinery, Documents, Signboard, Other*.
   - **Offline Fallback**: Camera capture is never blocked if network reverse geocoding is unavailable.
   - **Photo Persistence**: On visit attach, photos copied from cache (`cacheDir/gps_camera/`) to persistent storage (`filesDir/visit_photos/`); `renderedPath` updated in database.

### B. FINANCIAL TOOLS
1. **EMI Calculator (`:feature:emi-calculator`)**:
   - **Capabilities**: Reducing-balance EMI calculation ($P \times r \times \frac{(1+r)^n}{(1+r)^n - 1}$).
   - **Zero-Interest Edge Case**: Handles 0% interest loans ($\text{EMI} = P / n$, 0 interest).
2. **Amortization Schedule (`:feature:amortization` / `:core:domain`)**:
   - **Capabilities**: Pure Kotlin amortization schedule engine (`amortizationSchedule`).
   - **Schedule Table**: Month-by-month breakdown of opening balance, EMI, principal paid, interest paid, and closing balance with final installment rounding convergence ($B_n = 0.00$).

---

## 3. Architecture & Project Structure

```text
BankersToolkit/
├── app-profile/                       # Application identity & branding (Banker's Toolkit)
├── cmp-android/                       # Android app launcher module & AGP config
├── cmp-navigation/                    # Compose Navigation graph & Registry seams
│   └── src/commonMain/.../registry/
│       ├── BackboneRegistry.kt        # Root dashboard host (renders BankersToolkitDashboard)
│       └── FeatureRegistry.kt         # Feature DI modules & type-safe routes
├── core/
│   ├── common/                        # Shared utility extensions & formatting
│   ├── data/                          # Repositories (VisitRepositoryImpl + PhotoPersistence)
│   ├── database/                      # Room 3 AppDatabase & migrations (v11 -> v12)
│   ├── domain/                        # Pure Kotlin calculation engine (EmiCalculator)
│   └── model/                         # Core domain data models (Visit, VisitPhoto)
├── feature/
│   ├── visit-report/                  # Visit Report UI, state, viewmodels & export
│   ├── gps-camera/                    # GPS Camera UI, location capture & metadata
│   ├── pdf-redactor/                  # PDF Redactor (RETAINED SOURCE, UNWIRED FROM ANDROID)
│   ├── longbook-merger/               # Longbook Merger (COMMENTED OUT IN SETTINGS)
│   ├── emi-calculator/                # EMI Calculator UI & Navigation
│   ├── amortization/                  # Amortization Schedule UI & Navigation
│   └── home/                          # Home shell & BankersToolkitDashboard UI
├── CALCULATION_VALIDATION.md          # Formula & Excel comparison reference
├── INTEGRATION_PLAN.md                # Authoritative repo integration plan
├── OPEN_SOURCE_NOTICES.md             # Open source library licenses & notices
├── PHASE1_ANDROID_VERIFICATION.md     # Phase 1A verification report
└── WRONG_DASHBOARD_ROOT_CAUSE.md      # Root cause analysis & startup fixes
```

---

## 4. Root Cause Fixes & Database Migration

1. **Room 3 Database Migration (Launch Crash Fix)**:
   - Preserved original shipped `11.json` schema without editing in place.
   - Set `ForkDatabaseConfig.VERSION_OFFSET = 1` (raising effective DB version from 11 to 12).
   - Added `AutoMigration(from = 11, to = 12)` in `AppDatabase.kt`. Room generated `12.json`, resolving the same-version identity hash mismatch and enabling seamless auto-migration for existing on-device databases.

2. **Background Sync Worker Fix (`DataSyncWorker.kt`)**:
   - Neutralized `DataSyncWorker.kt` to a persister-only worker, removing dangling Koin DI references for demo repositories (`CurrencyRepository` / `MacroIndicatorsRepository`).

3. **Type-Safe Navigation Routes**:
   - Converted feature routes in `FeatureRegistry.kt` and `BackboneRegistry.kt` to `@Serializable` KMP-compliant objects and data classes (`VisitReportRoute`, `VisitEditorRoute`, `VisitPreviewRoute`, `GpsCameraRoute`).

4. **App Branding**:
   - Set `appDisplayName = "Banker's Toolkit"` in `libs.versions.toml` and `app.yaml`.
   - Escaped single quotes (`appDisplayName.replace("'", "\\'")`) in `cmp-android/build.gradle.kts` for Android XML resource generation.

5. **PDF Redactor Removal from Android**:
   - Removed dashboard card (already absent in `BankersToolkitDashboard`)
   - Removed navigation entry from `FeatureRegistry` (route, module, composable block)
   - Removed navigation callback from `BackboneRegistry`
   - Removed DI module from `FeatureRegistry.featureKoinModules`
   - Removed build dependency from `feature-deps.gradle.kts`
   - Commented out `include(":feature:pdf-redactor")` in `settings.gradle.kts`
   - Module source retained at `feature/pdf-redactor/`; separate project `/home/pnb/Projects/Pdf-Redactor` untouched

6. **Visit Report Photo Embedding & Offline Maps**:
   - Added `PhotoBytesLoader` expect/actual (content://, file://, absolute file paths)
   - Added `JpegParser` shared SOF marker dimension parser (pure Kotlin)
   - PDF: JPEG image XObjects with DCTDecode filter, aspect-ratio preserving frame fit
   - DOCX: JPEG media parts + relationships + `wp:inline` drawing with EMU sizing
   - Offline location maps: PDF vector panel + DOCX 5×5 grid table (no external APIs)

7. **Photo Persistence (Cache → FilesDir)**:
   - Added `PhotoPersistence` expect/actual abstraction
   - Android: copies cacheDir photos to `filesDir/visit_photos/` on `VisitRepository.addPhotoToVisit`
   - Updates `renderedPath` in database to persistent copy
   - Survives OS cache eviction; photos available for export anytime

---

## 5. Build, Test & Installation Instructions

### Environmental Requirements
- JDK 17+
- Android SDK (Location: `/home/pnb/android-sdk` or configured in `local.properties`)

### Build Android Debug APK (Conservative Memory)
```bash
cd /home/pnb/BankersToolkit/source
./gradlew --no-daemon --no-parallel --no-configuration-cache --max-workers=1 \
  -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8 -XX:+UseParallelGC" \
  -Pkotlin.daemon.jvmargs="-Xmx1g -XX:+UseParallelGC" \
  :cmp-android:assembleDemoDebug
```

**Output APK Path**:  
`/home/pnb/BankersToolkit/source/cmp-android/build/outputs/apk/demo/debug/cmp-android-demo-debug.apk` (45.1 MB)

**Release Copy**: `/home/pnb/BankersToolkit/release/BankersToolkit-debug.apk`

### Run Unit Test Suite
```bash
cd /home/pnb/BankersToolkit/source
./gradlew --no-daemon --no-parallel --no-configuration-cache --max-workers=1 \
  -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m" \
  -Pkotlin.daemon.jvmargs="-Xmx1g" \
  :feature:visit-report:desktopTest
```

### Install & Launch on Android Device via ADB
```bash
# Install APK
adb -s <device_id> install -r /home/pnb/BankersToolkit/release/BankersToolkit-debug.apk

# Launch Application
adb -s <device_id> shell am start -n com.example.app/cmp.android.app.MainActivity
```

---

## 6. Physical Device Verification Handoff

- **Build Verified**: `2026-08-21` (APK 45.1 MB, demoDebug)
- **DEX Inspection**: VisitReport, GpsCamera, EmiCalculator, Amortization classes present
- **Removed Features Confirmed Absent**: PDF Redactor, LongbookMerger classes not in DEX
- **Dashboard Verified**: FIELD TOOLS (Visit Report, GPS Camera) + FINANCIAL TOOLS (EMI, Amortization)
- **Exporters Functional**: PDF/DOCX generate with placeholders when photos unavailable; GPS metadata + offline maps rendered

---

## 7. Final Android App Composition

| Category | Features |
|----------|----------|
| **FIELD TOOLS** | Visit Reports, GPS Camera |
| **FINANCIAL TOOLS** | EMI Calculator, Amortization |
| **REMOVED FROM APP** | PDF Reader, PDF Redactor, Longbook Merger |