package kpt.feature.pdfredactor.engine

object WatermarkRemoverEngine {

    fun processStreamContent(content: String, signature: WatermarkSignature): String {
        val targets = signature.generateMatchTargets()
        var modified = content

        // Strategy 1: Target-based string literal stripping
        targets.forEach { target ->
            if (modified.contains(target, ignoreCase = true)) {
                modified = modified.replace(target, "", ignoreCase = true)
            }
        }

        // Strategy 2: Date/Time pattern watermark removal
        if (signature.patternRegex.containsMatchIn(modified)) {
            modified = signature.patternRegex.replace(modified, "")
        }

        return modified
    }

    fun decodePdfStringLiteral(raw: String): String {
        return raw.trim()
            .removePrefix("(")
            .removeSuffix(")")
            .replace("\\(", "(")
            .replace("\\)", ")")
    }
}
