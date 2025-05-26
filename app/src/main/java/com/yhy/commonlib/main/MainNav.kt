package com.yhy.commonlib.main

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/7 11:43
 **/
sealed class Screen(
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {
    companion object {
        const val DATA = "data"
    }

    data object Main : Screen("main")

    data object Waveform : Screen(
        route = "waveform/data={$DATA}",
        navArguments = listOf(
            navArgument(DATA) {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(data: String) = "waveform/data=${Uri.encode(data)}"
    }
}

@Composable
fun MainNav(
    viewModel: MainViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(viewModel) {
                navController.navigate(Screen.Waveform.createRoute(it)) {
                    launchSingleTop = true
                }
            }
        }

        composable(
            route = Screen.Waveform.route,
            arguments = Screen.Waveform.navArguments
        ) {
            val data = it.arguments?.getString(Screen.DATA) ?: ""
            WaveformScreen(data = data)
        }
    }
}