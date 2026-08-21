package kpt.feature.longbookmerger.parser

object FinacleParser {

    fun parseRawExport(rawText: String): LongbookReport {
        val lines = rawText.lines()
        val records = mutableListOf<LongbookRecord>()

        var totalDebit = 0.0
        var totalCredit = 0.0
        var branchName = "Main Branch"
        var reportDate = "2026-08-16"

        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("BRANCH:", ignoreCase = true)) {
                branchName = trimmed.substringAfter(":").trim()
            } else if (trimmed.startsWith("DATE:", ignoreCase = true)) {
                reportDate = trimmed.substringAfter(":").trim()
            } else if (trimmed.contains(",")) {
                val parts = trimmed.split(",")
                if (parts.size >= 5) {
                    val acc = parts[0].trim()
                    val name = parts[1].trim()
                    val debit = parts[2].toDoubleOrNull() ?: 0.0
                    val credit = parts[3].toDoubleOrNull() ?: 0.0
                    val bal = parts[4].toDoubleOrNull() ?: 0.0

                    if (acc.isNotBlank() && acc != "Account") {
                        totalDebit += debit
                        totalCredit += credit
                        records.add(
                            LongbookRecord(
                                accountNumber = acc,
                                partyName = name,
                                transactionDate = reportDate,
                                reference = "TXN_" + records.size,
                                debitAmount = debit,
                                creditAmount = credit,
                                balance = bal
                            )
                        )
                    }
                }
            }
        }

        return LongbookReport(
            title = "Finacle Long Book Merged Report",
            branchName = branchName,
            reportDate = reportDate,
            records = records,
            totalDebit = totalDebit,
            totalCredit = totalCredit
        )
    }
}
