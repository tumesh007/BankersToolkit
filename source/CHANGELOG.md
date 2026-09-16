# Changelog

All notable changes to the Banker's Toolkit project will be documented in this file.

## [v3.0] - 2026-09-16

### Added
- **ID Card Printer Integration (`:feature:idcard-printer`):**
  - Full batch ID card formatting, multi-image gallery import, high-precision cropping, and direct wireless/system printer support.
  - Supports standard KYC card formats (Aadhaar, PAN, Voter ID, Driving License, Custom).
  - Enhanced `CardCropView` with full edge-dragging and entire box-dragging controls.
  - Integrated directly into the main app with unified Back navigation and app theme.
- **PDF Redactor Integration (`:feature:pdf-redactor`):**
  - Built-in PDF watermark remover and sensitive information redaction engine powered by `pdfbox-android`.
  - Configurable redaction presets, text blackout, coordinate masks, and multi-page processing.
- **Document & KYC Tools Suite:**
  - Added dedicated **"DOCUMENT & KYC TOOLS"** section to the Banker's Toolkit Dashboard.
  - Registered `PdfRedactorRoute` and `IdCardPrinterRoute` in navigation backbone.
- **Visit Report CASA Proforma:**
  - Added CASA / Current Account visit verification proforma template and export layout.

### Fixed
- **Navigation Flow**: Fixed double-back issue when exiting ID Card Printer; tapping Back (via header arrow, gesture, or system back key) returns immediately to the menu in a single action.
- **Crop Rotation**: Preserved crop boundary coordinates when rotating 90° using `CardQuad.rotated90()`.
- **Cache Lifecycle**: Automatically invalidate stale A4 exports and bitmaps when clearing slots or rotating cards.

## [v2.2] - 2026-09-09

### Added
- **Visit Report Proforma Templates:** Added drop-down support in the Visit Report Editor for selecting specific proforma formats (Part A: Inspection of Stocks, Part B: Project Implementation, Part C: Project Commercial, Part D: Immovable Property).
- **Proforma PDF/DOCX Export:** Generating PDF and DOCX reports will now dynamically inject the specific layout structure of the selected proforma before attaching the photographs.

## [v2.1] - 2026-08-31

### Added
- **Gallery EXIF Extraction:** "Upload from Gallery" now natively extracts original GPS coordinates and timestamps from the selected image's EXIF metadata instead of using current device location.
- **Standalone Camera Export:** Added "Share" and "Save to Gallery" buttons to standalone GPS camera interface, allowing easy export of securely stamped audit photos for external use or later reuse.
- **Reverse Geocoding on Import:** Automatically reverse-geocodes imported photo coordinates to determine the original physical address.
- **Visual Stamping for Imports:** Uploaded gallery photos are now actively stamped with the same tamper-proof visual watermark and OpenStreetMap thumbnail as live camera photos.

## [v2.0] - 2026-08-29

### Added
- **Secure GPS Verification Camera Module** (`:feature:gps-camera`)
  - **Anti-Spoofing:** Real-time mock/spoofed GPS location detection via `FusedLocationProviderClient`. App now blocks capture if location is determined to be from a mock provider.
  - **Embedded Audit Watermark:** Photos are permanently stamped on-device with a semi-transparent dark banner. Includes verified coordinates, physical address, UTC timestamp, Case ID, and Officer ID.
  - **Mini-Map Thumbnail Integration:** Fetches a 256x256 street-level static map tile from OpenStreetMap (via HTTP fallback) and renders it dynamically on the watermark with a location pin.
  - **EXIF Metadata Embedding:** Securely writes high-accuracy DMS coordinates, datetime, and audit details directly to the JPEG EXIF tags.
  - **Upload from Gallery:** Added support to select existing photos securely via Android's native `PickVisualMedia` contract.
  - **Enhanced UI Elements:** Live GPS lock indicator (🟢/🟡/🔴), front/back camera switch, flashlight toggle, and a rule-of-thirds grid overlay.
  - **Reverse Geocoding:** Integration with Android's native `Geocoder` with a reliable fallback to OpenStreetMap Nominatim.

### Changed
- Improved memory management in Gradle (`gradle.properties`) by adjusting daemon and worker memory limits for more stable builds on low-RAM systems.

