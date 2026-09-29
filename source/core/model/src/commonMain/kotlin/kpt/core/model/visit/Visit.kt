package kpt.core.model.visit

import kotlinx.serialization.Serializable

@Serializable
data class Visit(
    val id: String,
    val customerName: String,
    val accountNumber: String = "",
    val loanAccountNumber: String = "",
    val address: String = "",
    val visitDate: String = "",
    val visitTime: String = "",
    val purpose: String = "",
    val officerName: String = "",
    val branch: String = "",
    val referenceNumber: String = "",
    val observations: String = "",
    val recommendations: String = "",
    val remarks: String = "",
    val status: VisitStatus = VisitStatus.DRAFT,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val photos: List<VisitPhoto> = emptyList(),
    val reportFormat: ReportFormat = ReportFormat.GENERAL
)

@Serializable
enum class ReportFormat(val displayName: String) {
    GENERAL("General / Standard"),
    PART_A("Part A - Inspection of Stocks"),
    PART_B("Part B - Project Loans (Implementation)"),
    PART_C("Part C - Project Loans (Commercial)"),
    PART_D("Part D - Immovable Property"),
    PRE_SANCTION_UPTO_50L("Annexure I-A - Pre-Sanction (Upto 50L)"),
    POST_SANCTION_UPTO_50L("Annexure I-C - Post-Sanction (Upto 50L)"),
    CASA_CURRENT_ACCOUNT("CASA / Current Account Visit")
}
