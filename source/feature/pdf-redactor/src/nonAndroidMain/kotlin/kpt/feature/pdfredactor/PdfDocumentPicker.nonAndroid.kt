package kpt.feature.pdfredactor

import androidx.compose.runtime.Composable

actual object PdfDocumentPicker {
    @Composable actual fun Register(onDocumentsSelected: (List<String>) -> Unit) = Unit
    actual fun launch() = Unit
}
