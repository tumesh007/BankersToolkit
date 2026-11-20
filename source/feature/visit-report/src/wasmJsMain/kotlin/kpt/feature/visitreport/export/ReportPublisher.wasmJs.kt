package kpt.feature.visitreport.export

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("""
(base64Data, mimeType, fileName) => {
    try {
        const byteCharacters = atob(base64Data);
        const byteNumbers = new Array(byteCharacters.length);
        for (let i = 0; i < byteCharacters.length; i++) {
            byteNumbers[i] = byteCharacters.charCodeAt(i);
        }
        const byteArray = new Uint8Array(byteNumbers);
        const blob = new Blob([byteArray], { type: mimeType });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    } catch (e) {
        console.error("Report export failed", e);
    }
}
""")
private external fun jsDownloadBase64(base64Data: String, mimeType: String, fileName: String)

actual object ReportPublisher {

    @OptIn(ExperimentalEncodingApi::class)
    actual fun publish(fileName: String, mimeType: String, bytes: ByteArray): String? {
        return try {
            val base64 = Base64.encode(bytes)
            jsDownloadBase64(base64, mimeType, fileName)
            fileName
        } catch (e: Throwable) {
            println("ReportPublisher export error: ${e.message}")
            null
        }
    }
}
