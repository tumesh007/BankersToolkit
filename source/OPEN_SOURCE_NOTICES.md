# Open Source Notices & Licenses

**Banker's Toolkit** includes open-source software and components from third-party repositories and projects under their respective open-source licenses.

---

## Included Projects & Libraries

### 1. KMP Project Template (Root Application Shell)
- **Author / Organization**: openMF / Mifos Initiative
- **Repository**: https://github.com/openMF/kmp-project-template
- **License**: Mozilla Public License 2.0 (MPL 2.0)
- **Modifications**: Customized root branding, module structure, and feature additions for Banker's Toolkit application framework.

### 2. PDF Redactor
- **Author**: tumesh007
- **Repository**: https://github.com/tumesh007/Pdf-Redactor
- **License**: MIT License
- **Modifications**: Native Kotlin / Jetpack Compose PDF watermark removal & redaction engine (`pdfbox-android`) integrated into `:feature:pdf-redactor`.

### 3. Longbook Merger
- **Author**: tumesh007
- **Repository**: https://github.com/tumesh007/Longbook-Merger
- **License**: MIT License
- **Modifications**: CBSL/Finacle long book parsing algorithms and landscape PDF generation clean-room ported to Kotlin under `:feature:longbook-merger`.

### 4. Loan Amortization Calculator
- **Author**: Artyom Panfutov
- **Repository**: https://github.com/ArtyomPanfutov/loan-amortization-calculator
- **License**: MIT License
- **Modifications**: Java 8 amortization calculation engine converted to pure Kotlin KMP calculation module (`:core:domain` / `:feature:amortization`).

---

## Full License Texts

### Mozilla Public License Version 2.0 (MPL 2.0)
For `kmp-project-template` source files governed by MPL 2.0:
https://www.mozilla.org/en-US/MPL/2.0/

### MIT License
For `Pdf-Redactor`, `Longbook-Merger`, and `loan-amortization-calculator`:

```text
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
