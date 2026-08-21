# Integration Plan: Banker's Toolkit

## 1. Repository Evaluation Summary

| Repository | Current Language | Min/Target SDK | UI Framework | License | Last Commit / Release Date | Viable Base? |
| --- | --- | --- | --- | --- | --- | --- |
| **openMF/kmp-project-template** | Kotlin 2.4.0 (KMP) | Min SDK 26 / Target SDK 37 | Compose Multiplatform (CMP) | MPL 2.0 | Aug 15, 2026 | **Yes (Root Application Base)** |
| **tumesh007/Pdf-Redactor** | Python (Repo) / Kotlin (Release APK) | Min SDK 24 (Android APK) | PySide6 (Repo) / Jetpack Compose (APK) | MIT | Aug 14, 2026 (v1.0.0 Release) | **No as Base** (Android Engine Module Integration) |
| **tumesh007/Longbook-Merger** | Python 3.12+ | N/A (Desktop Python) | PySide6 (Qt) | MIT | Jul 25, 2026 | **No** (Feature Porting Only) |
| **ArtyomPanfutov/loan-amortization-calculator** | Java (Java 8) | N/A (Java 8+ Library) | None (Headless core logic) | MIT | Apr 20, 2023 | **No** (Library Module Integration Only) |

---

## 2. Repository Integration Decisions

### 1. `openMF/kmp-project-template`
- **License**: Mozilla Public License 2.0 (MPL 2.0)
- **Integration Method**: **Standalone Base (Root Application Template)**
- **Rationale**: Serves as the root architecture for **Banker's Toolkit**. Provides modern multi-module hierarchy (`:core:*`, `:feature:*`, `:cmp-*`), Koin dependency injection, Room 3 database, Store 5 state management, Ktor network stack, Compose Navigation, build-logic convention plugins, and pre-configured support for Android (minSdk 26 / targetSdk 37), iOS, Desktop, and Web.

### 2. `tumesh007/Pdf-Redactor`
- **License**: MIT License
- **Integration Method**: **Standalone Module (`:feature:pdf-redactor`)**
- **Rationale**: While the Git repository `main` branch contains Python desktop source code, the official GitHub release `v1.0.0` includes a pre-built Native Android debug binary (`PDFRedactor-debug.apk`) built with Kotlin, Jetpack Compose, and `pdfbox-android` (`com.tumesh007.pdfredactor`). The Kotlin architecture detailed in `HANDOFF.md` (`WatermarkRemover.kt`, `RedactionEngine.kt`, `WatermarkViewModel.kt`) will be integrated directly into a native Kotlin `:feature:pdf-redactor` module within the Banker's Toolkit KMP project.

### 3. `tumesh007/Longbook-Merger`
- **License**: MIT License
- **Integration Method**: **Rejected (Direct Source Merge)** / **Feature Porting**
- **Rationale**: Implemented in Python 3 using PySide6 and ReportLab for desktop. Direct source code merge into a Kotlin Android/KMP project is not possible. The CBSL/Finacle long book parsing algorithms and landscape PDF generation logic will be clean-room ported into Kotlin under a `:feature:longbook-merger` module.

### 4. `ArtyomPanfutov/loan-amortization-calculator`
- **License**: MIT License
- **Integration Method**: **Source Merge (Converted to Kotlin KMP Module)**
- **Rationale**: Headless Java 8 library for loan schedule calculations. Converting the Java classes and DTOs into pure Kotlin in `:core:domain` or `:feature:amortization` ensures full cross-platform compatibility across Android, iOS, Desktop, and Web without runtime Java VM dependencies on non-JVM platforms.

