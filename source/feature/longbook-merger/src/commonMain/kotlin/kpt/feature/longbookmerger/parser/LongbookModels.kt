package kpt.feature.longbookmerger.parser

data class LongbookRecord(
    val accountNumber: String,
    val partyName: String,
    val transactionDate: String,
    val reference: String,
    val debitAmount: Double = 0.0,
    val creditAmount: Double = 0.0,
    val balance: Double = 0.0
)

data class LongbookReport(
    val title: String,
    val branchName: String,
    val reportDate: String,
    val records: List<LongbookRecord>,
    val totalDebit: Double,
    val totalCredit: Double
)
