package com.tumesh007.pdfredactor

object PageSelectionResolver {
    fun resolve(selectionMode: String, spec: String, pageCount: Int): List<Int> {
        if (pageCount <= 0) return emptyList()
        val mode = selectionMode.lowercase().trim()
        return when (mode) {
            "all" -> (0 until pageCount).toList()
            "first" -> listOf(0)
            "last" -> listOf(pageCount - 1)
            "odd" -> (0 until pageCount).filter { (it + 1) % 2 == 1 }
            "even" -> (0 until pageCount).filter { (it + 1) % 2 == 0 }
            "range", "custom" -> parsePageSpec(spec, pageCount)
            else -> throw IllegalArgumentException("Unknown page selection mode: $selectionMode")
        }
    }

    private fun parsePageSpec(spec: String, pageCount: Int): List<Int> {
        if (spec.isBlank()) throw IllegalArgumentException("Page spec is empty.")
        val indices = mutableSetOf<Int>()
        for (chunk in spec.split(",")) {
            val trimmed = chunk.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.contains("-")) {
                val parts = trimmed.split("-", limit = 2)
                val start = parts[0].toInt()
                val end = parts[1].toInt()
                if (start < 1 || end > pageCount || start > end) {
                    throw IllegalArgumentException("Page range '$trimmed' out of bounds (1-$pageCount).")
                }
                for (i in start..end) {
                    indices.add(i - 1)
                }
            } else {
                val pageNum = trimmed.toInt()
                if (pageNum < 1 || pageNum > pageCount) {
                    throw IllegalArgumentException("Page '$trimmed' out of bounds (1-$pageCount).")
                }
                indices.add(pageNum - 1)
            }
        }
        return indices.sorted()
    }
}
