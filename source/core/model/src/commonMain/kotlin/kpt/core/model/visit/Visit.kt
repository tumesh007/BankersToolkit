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
    val photos: List<VisitPhoto> = emptyList()
)
