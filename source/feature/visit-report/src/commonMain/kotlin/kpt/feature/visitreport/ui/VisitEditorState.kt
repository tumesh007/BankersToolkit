package kpt.feature.visitreport.ui

import kpt.core.model.visit.Visit
import kpt.core.model.visit.VisitPhoto
import kpt.core.model.visit.VisitStatus
import kotlin.time.Clock

data class VisitEditorState(
    val id: String = "",
    val customerName: String = "",
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
    val photos: List<VisitPhoto> = emptyList(),
    val reportFormat: kpt.core.model.visit.ReportFormat = kpt.core.model.visit.ReportFormat.GENERAL,
    val isSaved: Boolean = false,
    val message: String? = null
) {
    fun toVisit(): Visit {
        val now = Clock.System.now().toEpochMilliseconds()
        return Visit(
            id = id.ifBlank { "visit_" + now },
            customerName = customerName,
            accountNumber = accountNumber,
            loanAccountNumber = loanAccountNumber,
            address = address,
            visitDate = visitDate,
            visitTime = visitTime,
            purpose = purpose,
            officerName = officerName,
            branch = branch,
            referenceNumber = referenceNumber,
            observations = observations,
            recommendations = recommendations,
            remarks = remarks,
            status = status,
            createdAt = now,
            updatedAt = now,
            photos = photos,
            reportFormat = reportFormat
        )
    }
}
