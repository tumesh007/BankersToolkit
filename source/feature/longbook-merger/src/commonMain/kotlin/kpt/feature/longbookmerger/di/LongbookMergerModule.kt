package kpt.feature.longbookmerger.di

import kpt.feature.longbookmerger.ui.LongbookMergerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val LongbookMergerModule = module {
    viewModel { LongbookMergerViewModel() }
}
