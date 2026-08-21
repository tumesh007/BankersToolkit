package kpt.feature.visitreport.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kpt.core.data.visit.VisitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VisitReportListViewModel(
    private val visitRepository: VisitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitReportListState())
    val uiState: StateFlow<VisitReportListState> = _uiState.asStateFlow()

    init {
        loadVisits()
    }

    fun loadVisits() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            visitRepository.getAllVisits().collect { list ->
                _uiState.update { state ->
                    val filtered = if (state.searchQuery.isBlank()) {
                        list
                    } else {
                        list.filter {
                            it.customerName.contains(state.searchQuery, ignoreCase = true) ||
                                    it.accountNumber.contains(state.searchQuery, ignoreCase = true) ||
                                    it.purpose.contains(state.searchQuery, ignoreCase = true)
                        }
                    }
                    state.copy(visits = filtered, isLoading = false)
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadVisits()
    }

    fun deleteVisit(id: String) {
        viewModelScope.launch {
            visitRepository.deleteVisit(id)
        }
    }
}
