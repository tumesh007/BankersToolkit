package kpt.feature.pdfredactor.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tumesh007.pdfredactor.WatermarkAppScreen
import com.tumesh007.pdfredactor.WatermarkViewModel

@Composable
actual fun PdfRedactorScreen(
    onNavigateBack: () -> Unit
) {
    val vm: WatermarkViewModel = viewModel()
    WatermarkAppScreen(
        viewModel = vm,
        onNavigateBack = onNavigateBack
    )
}
