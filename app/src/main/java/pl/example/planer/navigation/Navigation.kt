package pl.example.planer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import pl.example.planer.ui.screens.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object DashboardStudent : Screen("dashboard_student")
    object DashboardTeacher : Screen("dashboard_teacher")
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.DashboardStudent.route) {
            DashboardStudentScreen()
        }
        composable(Screen.DashboardTeacher.route) {
            DashboardTeacherScreen()
        }
    }
}
