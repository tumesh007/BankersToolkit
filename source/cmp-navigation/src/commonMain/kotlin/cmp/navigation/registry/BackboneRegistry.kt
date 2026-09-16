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

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import kpt.core.base.ui.nav.popBackStackSafely
import kpt.feature.calculators.navigation.navigateToAmortization
import kpt.feature.emicalculator.navigation.navigateToEmiCalculator
import kpt.feature.home.demo.BankersToolkitDashboard
import kpt.feature.settings.navigateToSettings
import kpt.feature.settings.navigateToSyncAndDrafts
import kpt.feature.settings.notificationDestination
import kpt.feature.settings.settingsDestination
import kpt.feature.settings.syncAndDraftsDestination

object BackboneRegistry {
    val homeBody: @Composable (NavController) -> Unit = { navController ->
        BankersToolkitDashboard(
            onNavigateToVisitReport = { navController.navigate(VisitReportRoute) },
            onNavigateToGpsCamera = { navController.navigate(GpsCameraRoute()) },
            onNavigateToEmiCalculator = { navController.navigateToEmiCalculator() },
            onNavigateToAmortization = { navController.navigateToAmortization() },
            onNavigateToIdCardPrinter = { navController.navigate(IdCardPrinterRoute) },
            onNavigateToPdfRedactor = { navController.navigate(PdfRedactorRoute) },
        )
    }

    val navigateToSettings: (NavController) -> Unit = { it.navigateToSettings() }

    val backboneDestinations: NavGraphBuilder.(NavController) -> Unit = { navController ->
        notificationDestination(onBackClick = { navController.popBackStackSafely() })
        syncAndDraftsDestination(onBackClick = { navController.popBackStackSafely() })
        settingsDestination(
            onBackClick = { navController.popBackStackSafely() },
            onSyncAndDraftsClick = { navController.navigateToSyncAndDrafts() },
            devMenuEntries = ShowcaseRegistry.devSettingsEntries(navController),
        )
    }
}
