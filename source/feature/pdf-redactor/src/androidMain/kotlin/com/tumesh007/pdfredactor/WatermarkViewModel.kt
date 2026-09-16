package com.tumesh007.pdfredactor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BatchResultSummary(
    val totalFiles: Int,
    val successfulFiles: Int,
    val totalPagesCleaned: Int,
    val totalPagesUnmatched: Int,
    val failures: List<String>
)

sealed interface WatermarkUiState {
    data object Idle : WatermarkUiState

    data class FilesSelected(
        val uris: List<Uri>,
        val fileNames: List<String>
    ) : WatermarkUiState

    data class Processing(
        val currentFileName: String,
        val currentIndex: Int,
        val totalFiles: Int
    ) : WatermarkUiState

    data class Success(
        val result: BatchResultSummary
    ) : WatermarkUiState

    data class Error(
        val message: String
    ) : WatermarkUiState
}

class WatermarkViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<WatermarkUiState>(WatermarkUiState.Idle)
    val uiState: StateFlow<WatermarkUiState> = _uiState.asStateFlow()

    private val _employeeId = MutableStateFlow("")
    val employeeId: StateFlow<String> = _employeeId.asStateFlow()

    fun updateEmployeeId(newId: String) {
        _employeeId.value = newId
    }

    fun onFilesSelected(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val names = uris.map { uri ->
            uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
        }
        _uiState.value = WatermarkUiState.FilesSelected(uris, names)
    }

    fun processWatermarkRemoval(
        context: Context,
        targetUriProvider: suspend (String) -> Uri?
    ) {
        val currentState = _uiState.value
        if (currentState !is WatermarkUiState.FilesSelected) return

        val uris = currentState.uris
        val names = currentState.fileNames
        val currentEmpId = _employeeId.value

        viewModelScope.launch {
            var totalPagesCleaned = 0
            var totalPagesUnmatched = 0
            var successfulFiles = 0
            val failures = mutableListOf<String>()

            val sig = WatermarkDefaults.createSignature(currentEmpId)

            for (i in uris.indices) {
                val srcUri = uris[i]
                val srcName = names[i]
                _uiState.value = WatermarkUiState.Processing(srcName, i + 1, uris.size)

                val outName = srcName.removeSuffix(".pdf") + "_clean.pdf"
                val targetUri = targetUriProvider(outName)

                if (targetUri == null) {
                    failures.add("$srcName: Destination file creation canceled")
                    continue
                }

                val result = withContext(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(srcUri)
                            ?: return@withContext WatermarkRemovalResult(null, null, success = false, error = "Could not open input file")
                        val outputStream = context.contentResolver.openOutputStream(targetUri)
                            ?: return@withContext WatermarkRemovalResult(null, null, success = false, error = "Could not open output destination")

                        val res = WatermarkRemover.removeWatermarkStream(inputStream, outputStream, sig)
                        inputStream.close()
                        outputStream.close()
                        res
                    } catch (e: Exception) {
                        WatermarkRemovalResult(null, null, success = false, error = e.message ?: "Processing error")
                    }
                }

                if (result.success) {
                    successfulFiles++
                    totalPagesCleaned += result.pagesCleaned
                    totalPagesUnmatched += result.pagesUnmatched.size
                } else {
                    failures.add("$srcName: ${result.error ?: "Watermark removal failed"}")
                }
            }

            val summary = BatchResultSummary(
                totalFiles = uris.size,
                successfulFiles = successfulFiles,
                totalPagesCleaned = totalPagesCleaned,
                totalPagesUnmatched = totalPagesUnmatched,
                failures = failures
            )
            _uiState.value = WatermarkUiState.Success(summary)
        }
    }

    fun reset() {
        _uiState.value = WatermarkUiState.Idle
    }
}
