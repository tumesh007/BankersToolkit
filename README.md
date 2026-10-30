# Banker's Toolkit 🏦💼

**Banker's Toolkit** is a unified, 100% offline-resilient, local-first banking utility application built with Kotlin Multiplatform (KMP), Compose Multiplatform (CMP), and Room 3. Designed specifically for branch banking, credit appraisal, and field asset inspections.

---

## 📱 Supported Platforms

- **Web Application (Wasm / JS)**: Live on GitHub Pages at [https://tumesh007.github.io/BankersToolkit/](https://tumesh007.github.io/BankersToolkit/) (runs in modern desktop and mobile browsers via WebAssembly).
- **Android** (API 26+): Mobile field kit with camera integration, GPS mock-detection, offline storage, and document processing.
- **Ubuntu Linux Desktop** (amd64): Native desktop application with bundled Java 21 runtime, native GNOME/Zenity file picker, and native PDF/Word document generation.

---

## 🌟 Modules & Features

### 1. Field Visit Reports (`:feature:visit-report`)
- **Inspection Formats**:
  - Standard Pre/Post-Sanction Formats (Part A: Inspection of Stocks, Part B: Project Implementation, Part C: Project Commercial, Part D: Immovable Property).
  - **Official Annexure I-A Pre-Sanction Inspection Proforma (Upto ₹ 50 Lakh)**:
    - 3-column Facilities Sought Grid (Term Loan, Working Capital, Non-Fund, Total).
    - Unit Assessment & Physical Verification checklist.
    - Market Verification & Credentials grid (min. 2 references).
    - Visiting Official sign-off block.
- **Dual High-Fidelity Document Generation**:
  - **Vector A4 PDF (`VisitPdfExporter`)**: Clean vector tables, GPS metadata, offline coordinate maps, and embedded photographs.
  - **Editable Word DOCX (`VisitDocxExporter`)**: Native `<w:tbl>` OpenXML tables, photo media parts, and structured headings.
- **Desktop Publishing**:
  - Automatically exports files to `~/Downloads/BankersToolkit/VisitReports/`.
  - Automatically launches the system default application (`xdg-open` / Document Viewer / LibreOffice Writer).

### 2. GPS Camera & Photo Verification (`:feature:gps-camera`)
- **Tamper-Proof Verification**: Mock/spoofed GPS detection blocks fraudulent location inputs.
- **Embedded Audit Watermark**: Photos are stamped with an audit banner containing coordinates, reverse-geocoded location, timestamp, Case/Officer IDs, and accuracy.
- **Gallery Upload**: Seamlessly pick photos on Ubuntu Desktop using native `/usr/bin/zenity` (GNOME/Wayland file dialog), `java.awt.FileDialog`, or `JFileChooser`.
- **Photo Persistence**: Stored safely in `~/.bankerstoolkit/` (Desktop) or app-isolated storage (Android).
- **Save & Share**: Save to `~/Pictures/BankersToolkit/` or launch directly in the default image viewer.

### 3. ID Card & Document Printer (`:feature:idcard-printer`)
- **Dual Mode**:
  - **ID Card Mode**: Pre-configured templates for Aadhaar, PAN Card, Voter ID, Driving License, and Custom card sizes.
  - **Full Document Mode**: Isolates single-page documents and renders directly to standard A4 sheets at 300 DPI.
- **Dynamic Quad Perspective Rectification**: Computes real-world physical geometry using corner Euclidean distances to eliminate distortion during document scanning.
- **Direct Printing**: Wireless/system printer output and PDF export.

### 4. PDF Redactor (`:feature:pdf-redactor`)
- **Watermark Removal**: Strips internal portal download stamps (e.g. `PNB SAKSHAM <Employee ID> <Date/Time>`).
- **Flexible Masking**: Configurable Employee ID pattern redaction and coordinate-based text blackout.

### 5. Financial Calculators (`:feature:emi-calculator`, `:core:domain`)
- **EMI Calculator**: Reducing-balance formula with full support for zero-interest schemes.
- **Amortization Schedule**: Month-by-month principal and interest schedule with floating-point rounding convergence.

---

## 💻 Ubuntu Desktop Installation

Pre-built binaries are located in `release/desktop/`:

### Option A: Install via Debian Package (.deb)
```bash
sudo dpkg -i release/desktop/bankers-toolkit_1.0.0_amd64.deb
```
Or double-click the `.deb` file in Ubuntu Files to install via Ubuntu Software.

### Option B: Portable Standalone Archive (.tar.gz)
No root or installation required:
```bash
cd release/desktop
./run-bankers-toolkit.sh
```
Or manually extract and run:
```bash
tar -xzf release/desktop/bankers-toolkit-linux-x64.tar.gz
./bankers-toolkit/bin/bankers-toolkit
```

---

## 📲 Android Installation

The compiled APKs are available in `release/` and `apk/`:
1. Transfer `BankersToolkit-v3-updated.apk` to your Android device.
2. Enable installation from unknown sources.
3. Tap the file to install.

---

## 🛠️ Building From Source

### Prerequisites
- JDK 21 (Temurin or OpenJDK recommended)
- Android SDK (for Android build)
- Ubuntu dependencies for desktop packaging: `fakeroot`, `alien` (optional)

### Build Desktop Packages
```bash
cd source
./gradlew --no-daemon --no-parallel --no-configuration-cache --max-workers=1 \
  -Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=384m -XX:+UseG1GC" \
  :cmp-desktop:packageDeb :cmp-desktop:createDistributable
```
- `.deb` output: `source/cmp-desktop/build/compose/binaries/main/deb/bankers-toolkit_1.0.0_amd64.deb`
- Portable binary: `source/cmp-desktop/build/compose/binaries/main/app/bankers-toolkit/bin/bankers-toolkit`

### Build Android APK
```bash
cd source
./gradlew --no-daemon --no-parallel --no-configuration-cache --max-workers=1 \
  -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m" \
  :cmp-android:assembleDemoDebug
```

---

## 🔒 Security & Privacy
- **100% Offline Resilience**: Operates entirely offline without external server dependencies or API keys.
- **Local-First Architecture**: All database records and photos reside strictly on the user's device.
- **Zero Tracking**: No telemetry, analytics, or background data collection.

---

## 📄 License
Licensed under the Apache License 2.0. See `source/LICENSE` for details.
