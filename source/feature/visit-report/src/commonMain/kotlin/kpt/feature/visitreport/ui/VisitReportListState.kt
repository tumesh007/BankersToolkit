package kpt.feature.visitreport.ui

import kpt.core.model.visit.Visit

data class VisitReportListState(
    val visits: List<Visit> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
