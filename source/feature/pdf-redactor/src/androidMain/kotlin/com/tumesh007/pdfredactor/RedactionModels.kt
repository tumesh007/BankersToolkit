package com.tumesh007.pdfredactor

import android.graphics.Color

data class NormalizedRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class RedactionConfig(
    val pageSelectionMode: String = "all",
    val pageSpec: String = "",
    val customRegexPatterns: List<String> = emptyList(),
    val coordinates: List<NormalizedRect> = emptyList(),
    val redactionColor: Int = Color.BLACK,
    val margin: Float = 0.005f,
    val removeMetadata: Boolean = true
)

data class RedactionResult(
    val success: Boolean,
    val pagesProcessed: Int,
    val redactionsApplied: Int,
    val error: String? = null
)
