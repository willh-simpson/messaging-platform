package com.messaging.messagingplatform.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.messaging.messagingplatform.ui.auth.AuthScreen
import com.messaging.messagingplatform.ui.auth.AuthViewModel
import com.messaging.messagingplatform.ui.channels.ChannelListScreen
import com.messaging.messagingplatform.ui.chat.ChatScreen
import java.net.URLDecoder
import java.net.URLEncoder

private object Routes {
    const val AUTH = "auth"
    const val CHANNELS = "channels"
    const val CHAT = "chat/{channel_id}/{channel_name}"

    fun chat(channelId: String, channelName: String) = "chat/$channelId/${channelName.encodeUrl()}"

    private fun String.encodeUrl() =
        URLEncoder.encode(this, "UTF-8")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = if (authState.isAuthenticated) Routes.CHANNELS else Routes.AUTH

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                viewModel = hiltViewModel(),
                onAuthSuccess = {
                    navController.navigate(Routes.CHANNELS) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CHANNELS) {
            ChannelListScreen(
                viewModel = hiltViewModel(),
                onChannelClick = { channel ->
                    navController.navigate(Routes.chat(channel.channelId, channel.name))
                },
                onLogout = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType },
                navArgument("channelName") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
            val channelName = backStackEntry.arguments?.getString("channelName")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: return@composable

            ChatScreen(
                channelId = channelId,
                channelName = channelName,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}