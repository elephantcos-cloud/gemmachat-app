package com.elephantcos.gemmachat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elephantcos.gemmachat.ui.screens.ChatScreen
import com.elephantcos.gemmachat.ui.screens.HomeScreen
import com.elephantcos.gemmachat.ui.screens.SetupScreen

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Home  : Screen("home")
    object Chat  : Screen("chat/{conversationId}") {
        fun createRoute(id: Long) = "chat/$id"
    }
}

@Composable
fun NavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val prefs = context.getSharedPreferences("gemmachat_prefs", 0)
    val startDestination = if (prefs.getString("model_path", null) != null)
        Screen.Home.route else Screen.Setup.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Setup.route) {
            SetupScreen(onModelSelected = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Setup.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(onOpenChat = { id ->
                navController.navigate(Screen.Chat.createRoute(id))
            })
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
        ) { back ->
            val convId = back.arguments?.getLong("conversationId") ?: return@composable
            ChatScreen(conversationId = convId, onBack = { navController.popBackStack() })
        }
    }
}
