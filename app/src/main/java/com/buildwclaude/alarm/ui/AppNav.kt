package com.buildwclaude.alarm.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.buildwclaude.alarm.ui.edit.AlarmEditScreen
import com.buildwclaude.alarm.ui.list.AlarmListScreen
import com.buildwclaude.alarm.ui.settings.SettingsScreen

object Routes {
    const val LIST = "list"
    const val EDIT = "edit"        // edit?id={id}; id = -1 means "new"
    const val SETTINGS = "settings"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            AlarmListScreen(
                onAddAlarm = { nav.navigate("${Routes.EDIT}?id=-1") },
                onEditAlarm = { id -> nav.navigate("${Routes.EDIT}?id=$id") },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = "${Routes.EDIT}?id={id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 }),
        ) { entry ->
            val id = entry.arguments?.getInt("id") ?: -1
            AlarmEditScreen(alarmId = id, onDone = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
