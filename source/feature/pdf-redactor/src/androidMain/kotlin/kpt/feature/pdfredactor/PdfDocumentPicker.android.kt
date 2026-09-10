package kpt.feature.pdfredactor

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual object PdfDocumentPicker {
    private var launchPicker: (() -> Unit)? = null

    @Composable
    actual fun Register(onDocumentsSelected: (List<String>) -> Unit) {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            onDocumentsSelected(uris.map { uri ->
                // Keep access across process recreation when the provider allows it.
                runCatching {
                    // The contract has already granted read access. Persisting is best-effort.
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                uri.toString()
            })
        }
        launchPicker = { launcher.launch(arrayOf("application/pdf")) }
    }

    actual fun launch() { launchPicker?.invoke() }
}
