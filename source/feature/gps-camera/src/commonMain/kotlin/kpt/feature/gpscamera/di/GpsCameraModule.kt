package kpt.feature.gpscamera.di

import kpt.feature.gpscamera.ui.GpsCameraViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val GpsCameraModule = module {
    viewModel { GpsCameraViewModel(visitRepository = get()) }
}
