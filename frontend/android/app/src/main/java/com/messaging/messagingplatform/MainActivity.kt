package com.messaging.messagingplatform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.messaging.messagingplatform.ui.navigation.NavGraph
import com.messaging.messagingplatform.ui.theme.MessagingTheme

/**
 * Single activity Android app entry point.
 *
 * All other screens are treated as components handled in NavGraph().
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // called before onCreate() so splashscreen is displayed before Compose initializes
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MessagingTheme {
                NavGraph()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MessagingTheme {
        NavGraph()
    }
}