package kpt.feature.longbookmerger.ui

import kpt.feature.longbookmerger.parser.LongbookReport

data class LongbookMergerState(
    val selectedFiles: List<String> = emptyList(),
    val rawCsvInput: String = "",
    val parsedReport: LongbookReport? = null,
    val isGenerating: Boolean = false,
    val outputDirectory: String = "/sdcard/BankersToolkit/MergedLongbooks",
    val statusMessage: String? = null
)
