/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package cmp.navigation.registry

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithPushTransitions
import kpt.core.base.ui.nav.popBackStackSafely
import kpt.core.data.demo.di.ProjectRepositoryModule
import kpt.core.database.demo.di.ProjectDatabaseModule
import kpt.core.network.demo.di.ProjectNetworkModule
import kpt.feature.calculators.di.CalculatorsModule
import kpt.feature.calculators.navigation.calculatorsGraph
import kpt.feature.emicalculator.di.EmiCalculatorModule
import kpt.feature.emicalculator.navigation.emiCalculatorDestination
import kpt.feature.gpscamera.di.GpsCameraModule
import kpt.feature.gpscamera.ui.GpsCameraScreen
import kpt.feature.gpscamera.ui.GpsCameraViewModel
import kpt.feature.visitreport.di.VisitReportModule
import kpt.feature.visitreport.ui.VisitEditorScreen
import kpt.feature.visitreport.ui.VisitEditorViewModel
import kpt.feature.visitreport.ui.VisitPreviewScreen
import kpt.feature.visitreport.ui.VisitReportListScreen
import kpt.feature.visitreport.ui.VisitReportListViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module

@Serializable
data object VisitReportRoute

@Serializable
data class VisitEditorRoute(val visitId: String = "new")

@Serializable
data class VisitPreviewRoute(val visitId: String = "")

@Serializable
data class GpsCameraRoute(val visitId: String = "")

@Serializable
object FeatureRegistry {
    val featureKoinModules: List<Module> = listOf(
        EmiCalculatorModule,
        CalculatorsModule,
        VisitReportModule,
        GpsCameraModule,
        ProjectRepositoryModule,
        ProjectNetworkModule,
        ProjectDatabaseModule,
    )

    val featureDestinations: NavGraphBuilder.(NavController) -> Unit = { navController ->
        emiCalculatorDestination(onBackClick = { navController.popBackStackSafely() })
        calculatorsGraph(navController)

        composableWithPushTransitions<VisitReportRoute> {
            val vm = koinViewModel<VisitReportListViewModel>()
            VisitReportListScreen(
                viewModel = vm,
                onNewVisit = { navController.navigate(VisitEditorRoute(visitId = "new")) },
                onEditVisit = { id -> navController.navigate(VisitEditorRoute(visitId = id)) },
                onPreviewVisit = { id -> navController.navigate(VisitPreviewRoute(visitId = id)) },
                onOpenStandaloneCamera = { navController.navigate(GpsCameraRoute()) },
                onNavigateBack = { navController.popBackStackSafely() }
            )
        }

        composableWithPushTransitions<VisitEditorRoute> { entry ->
            val route = entry.toRoute<VisitEditorRoute>()
            val vm = koinViewModel<VisitEditorViewModel>()
            androidx.compose.runtime.LaunchedEffect(route.visitId) {
                if (route.visitId != "new") vm.loadVisit(route.visitId)
            }
            // Reload from the database when the GPS camera reports newly captured photos.
            val photosChanged by entry.savedStateHandle
                .getStateFlow("gps_photos_changed", false)
                .collectAsState()
            androidx.compose.runtime.LaunchedEffect(photosChanged) {
                if (photosChanged && vm.uiState.value.id.isNotBlank()) {
                    vm.loadVisit(vm.uiState.value.id)
                    entry.savedStateHandle.set("gps_photos_changed", false)
                }
            }
            VisitEditorScreen(
                viewModel = vm,
                onLaunchCameraForVisit = { vId -> navController.navigate(GpsCameraRoute(visitId = vId)) },
                onPreviewVisit = { vId -> navController.navigate(VisitPreviewRoute(visitId = vId)) },
                onNavigateBack = { navController.popBackStackSafely() }
            )
        }

        composableWithPushTransitions<VisitPreviewRoute> { entry ->
            val route = entry.toRoute<VisitPreviewRoute>()
            val vm = koinViewModel<VisitEditorViewModel>()
            androidx.compose.runtime.LaunchedEffect(route.visitId) {
                if (route.visitId.isNotBlank()) vm.loadVisit(route.visitId)
            }
            VisitPreviewScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStackSafely() }
            )
        }

        composableWithPushTransitions<GpsCameraRoute> { entry ->
            val route = entry.toRoute<GpsCameraRoute>()
            val vm = koinViewModel<GpsCameraViewModel>()
            androidx.compose.runtime.LaunchedEffect(route.visitId) {
                vm.setVisitId(route.visitId.ifBlank { null })
            }
            GpsCameraScreen(
                viewModel = vm,
                onPhotoCaptured = { _ ->
                    if (route.visitId.isNotBlank()) {
                        // Photos are persisted by the camera ViewModel; signal the visit
                        // editor to reload the visit from the database on return.
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set("gps_photos_changed", true)
                    }
                },
                onNavigateBack = { navController.popBackStackSafely() }
            )
        }

    }
}
