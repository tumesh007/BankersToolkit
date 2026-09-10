package kpt.feature.pdfredactor

import androidx.compose.runtime.Composable

/** Platform Storage Access Framework bridge. Android supplies real content URIs. */
expect object PdfDocumentPicker {
    @Composable
    fun Register(onDocumentsSelected: (List<String>) -> Unit)

    fun launch()
}
