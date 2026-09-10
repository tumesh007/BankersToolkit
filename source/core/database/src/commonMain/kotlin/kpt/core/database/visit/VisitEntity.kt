package kpt.core.database.visit

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kpt.core.model.visit.Visit
import kpt.core.model.visit.VisitStatus

@Entity(tableName = "banker_visits")
data class VisitEntity(
    @PrimaryKey
    val id: String,
    val customerName: String,
    val accountNumber: String,
    val loanAccountNumber: String,
    val address: String,
    val visitDate: String,
    val visitTime: String,
    val purpose: String,
    val officerName: String,
    val branch: String,
    val referenceNumber: String,
    val observations: String,
    val recommendations: String,
    val remarks: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    @androidx.room3.ColumnInfo(defaultValue = "GENERAL")
    val reportFormat: String = "GENERAL"
) {
    fun toDomain(photos: List<kpt.core.model.visit.VisitPhoto> = emptyList()): Visit {
        return Visit(
            id = id,
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
            status = try { VisitStatus.valueOf(status) } catch (e: Exception) { VisitStatus.DRAFT },
            createdAt = createdAt,
            updatedAt = updatedAt,
            photos = photos,
            reportFormat = try { kpt.core.model.visit.ReportFormat.valueOf(reportFormat) } catch (e: Exception) { kpt.core.model.visit.ReportFormat.GENERAL }
        )
    }

    companion object {
        fun fromDomain(visit: Visit): VisitEntity {
            return VisitEntity(
                id = visit.id,
                customerName = visit.customerName,
                accountNumber = visit.accountNumber,
                loanAccountNumber = visit.loanAccountNumber,
                address = visit.address,
                visitDate = visit.visitDate,
                visitTime = visit.visitTime,
                purpose = visit.purpose,
                officerName = visit.officerName,
                branch = visit.branch,
                referenceNumber = visit.referenceNumber,
                observations = visit.observations,
                recommendations = visit.recommendations,
                remarks = visit.remarks,
                status = visit.status.name,
                createdAt = visit.createdAt,
                updatedAt = visit.updatedAt,
                reportFormat = visit.reportFormat.name
            )
        }
    }
}
