package kpt.feature.pdfredactor.ui

import androidx.compose.runtime.Composable

@Composable
expect fun PdfRedactorScreen(
    onNavigateBack: () -> Unit = {}
)
