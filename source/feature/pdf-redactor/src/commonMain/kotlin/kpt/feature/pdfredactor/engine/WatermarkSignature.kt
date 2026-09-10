package kpt.feature.pdfredactor.engine

data class WatermarkSignature(
    val employeeId: String? = null,
    val patternRegex: Regex = DEFAULT_DATETIME_REGEX
) {
    fun generateMatchTargets(): List<String> {
        val targets = mutableListOf<String>()
        if (!employeeId.isNullOrBlank()) {
            val id = employeeId.trim()
            targets.add(id)
            targets.add("EMP-$id")
            targets.add("EMP $id")
            targets.add("PNB SAKSHAM $id")
            targets.add("PNB $id")
        }
        return targets
    }

    companion object {
        val DEFAULT_DATETIME_REGEX = Regex("""\d{2}[-/]\d{2}[-/]\d{4}\s+\d{2}:\d{2}""")
    }
}
