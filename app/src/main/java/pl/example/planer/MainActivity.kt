package pl.example.planer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import pl.example.planer.ui.screens.SplashScreen
import pl.example.planer.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showSplash by remember { mutableStateOf(true) }

            if (showSplash) {
                SplashScreen()
                LaunchedEffect(Unit) {
                    // czekaj na zakończenie animacji (sumarycznie ~600+100+800+700 = 2200ms)
                    delay(2200)
                    showSplash = false
                }
            } else {
                val navController = rememberNavController()
                AppNavGraph(navController)
            }
        }
    }
}
