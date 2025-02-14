package com.lib.automix.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lib.automix.ui.home.AutoMixHome
import com.lib.automix.ui.setting.AutoMixSetting
import com.lib.automix.utils.isResumed

sealed class Screen(val route: String) {
    data object AutoMixHome : Screen("home")
    data object AutoMixSetting : Screen("setting")
}

@Composable
fun rememberAutoMixAppState(
    navController: NavHostController = rememberNavController()
) = remember(navController) {
    AutoMixAppState(navController)
}

class AutoMixAppState(val navController: NavHostController) {

    fun navigateToSetting(navBackStackEntry: NavBackStackEntry) {
        if (navBackStackEntry.lifecycle.isResumed()) {
            navController.navigate(Screen.AutoMixSetting.route)
        }
    }

    fun navigateBack(navBackStackEntry: NavBackStackEntry) {
        if (navBackStackEntry.lifecycle.isResumed()) {
            navController.popBackStack()
        }
    }
}

@Composable
fun AutoMixApp(
    requestPermission: () -> Unit = {},
    addItem: (Int) -> Unit = {}
) {
    val appState: AutoMixAppState = rememberAutoMixAppState()
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current
    NavHost(
        navController = appState.navController,
        startDestination = Screen.AutoMixHome.route,
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(200),
                initialOffsetX = { it }
            )
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = tween(200),
                targetOffsetX = { -it }
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(200),
                initialOffsetX = { -it }
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(200),
                targetOffsetX = { it }
            )
        }
    ) {
        composable(Screen.AutoMixHome.route) {
            AutoMixHome(
                onNavigationClick = {
                    backPressedDispatcher?.onBackPressedDispatcher?.onBackPressed()
                },
                onSettingClick = {
                    appState.navigateToSetting(it)
                },
                requestPermission = requestPermission,
                addItem = addItem
            )
        }

        composable(Screen.AutoMixSetting.route) {
            AutoMixSetting(
                onNavigationClick = {
                    appState.navigateBack(it)
                }
            )
        }
    }
}