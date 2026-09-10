package kpt.feature.pdfredactor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kpt.feature.pdfredactor.engine.NormalizedRect
import kpt.feature.pdfredactor.engine.PdfRedactionProcessor
import kpt.feature.pdfredactor.engine.WatermarkSignature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfRedactorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PdfRedactorState())
    val uiState: StateFlow<PdfRedactorState> = _uiState.asStateFlow()

    fun setSelectedFiles(files: List<String>) {
        _uiState.update { it.copy(selectedFilePaths = files, statusMessage = null) }
    }

    fun updateEmployeeId(id: String) {
        _uiState.update { it.copy(employeeId = id) }
    }

    fun updateMode(mode: RedactionMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun addCustomRectangle(rect: NormalizedRect) {
        _uiState.update { it.copy(customRectangles = it.customRectangles + rect) }
    }

    fun clearCustomRectangles() {
        _uiState.update { it.copy(customRectangles = emptyList()) }
    }

    fun processRedaction() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.selectedFilePaths.isEmpty()) {
                _uiState.update { it.copy(statusMessage = "Please select at least one PDF file.") }
                return@launch
            }

            _uiState.update {
                it.copy(isProcessing = true, progressPercent = 0f, statusMessage = "Starting PDF redaction...")
            }

            val signature = WatermarkSignature(employeeId = state.employeeId.ifBlank { null })
            var totalWatermarks = 0
            var totalPages = 0

            for ((index, path) in state.selectedFilePaths.withIndex()) {
                val filename = path.substringAfterLast("/")
                _uiState.update {
                    it.copy(
                        statusMessage = "Processing $filename (${index + 1}/${state.selectedFilePaths.size})...",
                        progressPercent = index.toFloat() / state.selectedFilePaths.size,
                    )
                }

                val result = PdfRedactionProcessor.processFile(
                    inputUri = path,
                    outputDir = "",
                    signature = signature,
                )

                totalPages += result.pagesProcessed
                totalWatermarks += result.watermarksRemoved

                if (!result.success) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            statusMessage = "Error: ${result.error}",
                        )
                    }
                    return@launch
                }
            }

            val summary = buildString {
                append("Redacted ${state.selectedFilePaths.size} PDF(s)")
                if (totalWatermarks > 0) append(" — $totalWatermarks watermark(s) removed")
                append(". Saved to Downloads/BankersToolkit/Redacted")
            }

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    progressPercent = 1f,
                    processedFilesCount = state.selectedFilePaths.size,
                    statusMessage = summary,
                )
            }
        }
    }
}
