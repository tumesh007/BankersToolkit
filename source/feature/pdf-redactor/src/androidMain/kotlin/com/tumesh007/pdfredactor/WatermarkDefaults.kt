package com.tumesh007.pdfredactor

object WatermarkDefaults {
    val defaultPatterns = listOf("""\d{2}[-/]\d{2}[-/]\d{4}\s+\d{2}:\d{2}""")

    fun createSignature(employeeId: String? = null): WatermarkSignature {
        val literals = mutableListOf<String>()
        if (!employeeId.isNullOrBlank()) {
            val trimmed = employeeId.trim()
            literals.add(trimmed)
            // If user entered just numbers, e.g., "5135265", also match "EMP-5135265"
            if (trimmed.all { it.isDigit() }) {
                literals.add("EMP-$trimmed")
            }
        }
        return WatermarkSignature(
            literals = literals,
            patterns = defaultPatterns
        )
    }
}
