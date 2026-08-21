package kpt.feature.longbookmerger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kpt.feature.longbookmerger.generator.LandscapePdfGenerator
import kpt.feature.longbookmerger.parser.FinacleParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class LongbookMergerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LongbookMergerState())
    val uiState: StateFlow<LongbookMergerState> = _uiState.asStateFlow()

    init {
        val sampleCsv = """
        BRANCH: Connaught Place Branch
        DATE: 2026-08-16
        Account,Party,Debit,Credit,Balance
        100102001,M/s Sharma & Sons,50000.00,0.00,450000.00
        100102002,Global Tech Solutions,0.00,120000.00,780000.00
        100102003,Verma Traders,25000.00,0.00,15000.00
        """.trimIndent()

        _uiState.update {
            it.copy(
                rawCsvInput = sampleCsv,
                parsedReport = FinacleParser.parseRawExport(sampleCsv)
            )
        }
    }

    fun updateRawInput(csv: String) {
        _uiState.update {
            it.copy(
                rawCsvInput = csv,
                parsedReport = FinacleParser.parseRawExport(csv)
            )
        }
    }

    fun setSelectedFiles(files: List<String>) {
        _uiState.update { it.copy(selectedFiles = files) }
    }

    fun generateMergedPdf() {
        viewModelScope.launch {
            val report = _uiState.value.parsedReport ?: return@launch
            _uiState.update { it.copy(isGenerating = true, statusMessage = "Generating A4 Landscape PDF...") }

            val now = Clock.System.now().toEpochMilliseconds()
            val outputPath = "${_uiState.value.outputDirectory}/Merged_Longbook_$now.pdf"
            LandscapePdfGenerator.generateA4LandscapePdf(report, outputPath)

            _uiState.update {
                it.copy(
                    isGenerating = false,
                    statusMessage = "A4 Landscape PDF generated at: $outputPath"
                )
            }
        }
    }
}
