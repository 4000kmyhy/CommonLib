package com.yhy.commonlib.voice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.yhy.lib_compose.theme.BaseTheme

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/15 9:45
 **/
class VoiceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            BaseTheme {
                VoiceNavHost()
            }
        }
    }
}

@Composable
fun VoiceNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = VoiceGraph.VoiceValidate,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        }
    ) {
        voiceGraph(navController)
    }
}

private fun NavGraphBuilder.voiceGraph(navController: NavHostController) {
    composable<VoiceGraph.VoiceValidate> { entry ->
        VoiceValidateScreen(
            navigateToGenerate = {
                navController.navigate(
                    VoiceGraph.VoiceGenerate(Gson().toJson(it))
                )
            }
        )
    }

    composable<VoiceGraph.VoiceGenerate> { entry ->
        VoiceGenerateScreen()
    }
}