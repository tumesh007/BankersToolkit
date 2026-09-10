package kpt.feature.pdfredactor.di

import kpt.feature.pdfredactor.ui.PdfRedactorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val PdfRedactorModule = module {
    viewModel { PdfRedactorViewModel() }
}
