package kpt.feature.visitreport.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kpt.core.data.visit.VisitRepository
import kpt.core.model.visit.Visit
import kpt.core.model.visit.VisitPhoto
import kpt.core.model.visit.VisitStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class VisitEditorViewModel(
    private val visitRepository: VisitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitEditorState())
    val uiState: StateFlow<VisitEditorState> = _uiState.asStateFlow()

    fun loadVisit(id: String) {
        viewModelScope.launch {
            val visit = visitRepository.getVisitById(id)
            if (visit != null) {
                _uiState.update {
                    it.copy(
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
                        status = visit.status,
                        photos = visit.photos,
                        reportFormat = visit.reportFormat
                    )
                }
            }
        }
    }

    fun updateCustomerName(v: String) = _uiState.update { it.copy(customerName = v) }
    fun updateAccountNumber(v: String) = _uiState.update { it.copy(accountNumber = v) }
    fun updateLoanAccountNumber(v: String) = _uiState.update { it.copy(loanAccountNumber = v) }
    fun updateAddress(v: String) = _uiState.update { it.copy(address = v) }
    fun updateVisitDate(v: String) = _uiState.update { it.copy(visitDate = v) }
    fun updateVisitTime(v: String) = _uiState.update { it.copy(visitTime = v) }
    fun updatePurpose(v: String) = _uiState.update { it.copy(purpose = v) }
    fun updateOfficerName(v: String) = _uiState.update { it.copy(officerName = v) }
    fun updateBranch(v: String) = _uiState.update { it.copy(branch = v) }
    fun updateReferenceNumber(v: String) = _uiState.update { it.copy(referenceNumber = v) }
    fun updateReportFormat(f: kpt.core.model.visit.ReportFormat) = _uiState.update { it.copy(reportFormat = f) }
    fun updateObservations(v: String) = _uiState.update { it.copy(observations = v) }
    fun updateRecommendations(v: String) = _uiState.update { it.copy(recommendations = v) }
    fun updateRemarks(v: String) = _uiState.update { it.copy(remarks = v) }
    fun updateStatus(status: VisitStatus) = _uiState.update { it.copy(status = status) }

    fun addPhoto(photo: VisitPhoto) {
        _uiState.update { it.copy(photos = it.photos + photo) }
        autoSave()
    }

    fun removePhoto(photoId: String) {
        _uiState.update { it.copy(photos = it.photos.filterNot { p -> p.id == photoId }) }
        autoSave()
    }

    fun saveVisit(status: VisitStatus = _uiState.value.status): String {
        val visit = _uiState.value.copy(status = status).toVisit()
        _uiState.update { it.copy(id = visit.id, isSaved = true, message = "Visit saved successfully!") }
        viewModelScope.launch {
            visitRepository.saveVisit(visit)
        }
        return visit.id
    }

    private fun autoSave() {
        viewModelScope.launch {
            if (_uiState.value.customerName.isNotBlank()) {
                val visit = _uiState.value.toVisit()
                visitRepository.saveVisit(visit)
            }
        }
    }
}
