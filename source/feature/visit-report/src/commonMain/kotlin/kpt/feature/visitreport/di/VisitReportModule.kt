package kpt.feature.visitreport.di

import kpt.feature.visitreport.ui.VisitEditorViewModel
import kpt.feature.visitreport.ui.VisitReportListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val VisitReportModule = module {
    viewModel { VisitReportListViewModel(visitRepository = get()) }
    viewModel { VisitEditorViewModel(visitRepository = get()) }
}
