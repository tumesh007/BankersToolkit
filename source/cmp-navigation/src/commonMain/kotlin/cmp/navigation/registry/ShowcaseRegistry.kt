package cmp.navigation.registry

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import kpt.feature.settings.DevMenuEntry

object ShowcaseRegistry {
    fun devSettingsEntries(navController: NavController): List<DevMenuEntry> {
        return emptyList()
    }

    val devDestinations: NavGraphBuilder.(NavController) -> Unit = { navController ->
        // No showcase destinations
    }
}
