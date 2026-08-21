# BANKER'S TOOLKIT

**Banker's Toolkit** is a single, integrated, local-first banking utility application built with Kotlin Multiplatform (KMP), Compose Multiplatform (CMP), and Room 3.

---

## 🌟 Core Modules & Features

### 1. FIELD TOOLS
- **Visit Reports (`:feature:visit-report`)**:
  - Full field visit creation, draft auto-saving, observations, recommendations, and search.
  - Multi-page searchable **A4 PDF Report Generation** (`VisitPdfExporter`) — **actual embedded photographs (JPEG/DCTDecode)**, GPS metadata tables, **offline vector location maps** (grid, N-arrow, marker, accuracy circle, scale bar).
  - Editable **DOCX Word Report Generation** (`VisitDocxExporter`) — **embedded photograph media parts**, GPS metadata, **offline 5×5 grid location maps** with center marker.
- **GPS Camera (`:feature:gps-camera`)**:
  - Standalone & Visit-tied GPS photo capture.
  - Photo category classification: *Property, Business Premises, Residence, Stock, Machinery, Documents, Signboard, Other*.
  - Structured metadata storage: latitude, longitude, accuracy, timestamp, address, category, and caption.
  - **100% Offline Resilience**: Photo capture is never blocked if GPS or network reverse geocoding is unavailable.
  - **Photo Persistence**: On visit attach, cache photos copied to persistent `filesDir/visit_photos/`; `renderedPath` updated in database.

### 2. FINANCIAL TOOLS
- **EMI Calculator (`:feature:emi-calculator`)**:
  - Equated Monthly Installment calculation: `EMI = P * r * (1+r)^n / ((1+r)^n - 1)`.
  - Full support for zero-interest loans ($r = 0$).
- **Amortization Schedule (`:feature:amortization` / `:core:domain`)**:
  - Month-by-month payment schedule table (Opening balance, EMI, Principal paid, Interest paid, Closing balance).
  - Floating-point rounding convergence handling ($B_n = 0.00$).

### 3. DOCUMENT TOOLS (Source Retained, Unwired from Android)
- **PDF Redactor (`:feature:pdf-redactor`)**:
  - Offline PDF watermark removal for portal download stamps (e.g. `PNB SAKSHAM <Employee ID> <Date/Time>`).
  - User-configurable Employee ID mode (**no hard-coded IDs**).
  - Rectangle coordinate pattern masking.
  - **Available as standalone module and separate project** (`/home/pnb/Projects/Pdf-Redactor`), but **not included in the Android app**.
- **Longbook Merger (`:feature:longbook-merger`)**:
  - Clean-room Kotlin port for Finacle/CBSL long book reports.
  - Generates print-ready **A4 Landscape PDF** merged documents.
  - **Commented out of Android build** (`settings.gradle.kts`).

---

## 🚀 Building & Testing

### Android Debug APK Build (Conservative Memory)
```bash
cd /home/pnb/BankersToolkit/source
./gradlew --no-daemon --no-parallel --no-configuration-cache --max-workers=1 \
  -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8 -XX:+UseParallelGC" \
  -Pkotlin.daemon.jvmargs="-Xmx1g -XX:+UseParallelGC" \
  :cmp-android:assembleDemoDebug
```
Output APK: `cmp-android/build/outputs/apk/demo/debug/cmp-android-demo-debug.apk` (**45.1 MB**)

### Run Unit Tests
```bash
./gradlew --no-daemon --no-parallel --no-configuration-cache --max-workers=1 \
  -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m" \
  -Pkotlin.daemon.jvmargs="-Xmx1g" \
  :feature:visit-report:desktopTest
```

---

## 🔒 Security & Privacy
- **Local-First**: All database persistence uses Room 3 SQLite on-device.
- **No Cloud Uploads**: Customer data and photographs remain strictly local.
- **Minimal Permissions**: Requests only Location and Camera. No SMS, Contacts, or Microphone permissions.
- **100% Offline**: No external APIs, no network dependencies, no API keys required.

---

## 📦 Final Android App Composition
| Category | Features |
|----------|----------|
| **FIELD TOOLS** | Visit Reports, GPS Camera |
| **FINANCIAL TOOLS** | EMI Calculator, Amortization |
| **REMOVED FROM APP** | PDF Reader, PDF Redactor, Longbook Merger |

---

## 📄 Open Source Compliance
See [`OPEN_SOURCE_NOTICES.md`](OPEN_SOURCE_NOTICES.md) and [`INTEGRATION_PLAN.md`](INTEGRATION_PLAN.md) for full licensing details.