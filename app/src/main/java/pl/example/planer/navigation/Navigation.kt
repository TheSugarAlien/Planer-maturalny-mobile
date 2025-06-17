package pl.example.planer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pl.example.planer.ui.screens.LoginScreen
import pl.example.planer.ui.screens.DashboardStudentScreen
import pl.example.planer.ui.screens.DashboardTeacherScreen
import pl.example.planer.ui.screens.IndoorMapScreen
import pl.example.planer.ui.screens.AttendanceScreen

sealed class Screen(val route: String) {
    object Login            : Screen("login")
    object DashboardStudent : Screen("dashboardStudent")
    object DashboardTeacher : Screen("dashboardTeacher")
    object IndoorMap        : Screen("indoorMap")
    object Attendance       : Screen("attendance/{examId}") {
        fun createRoute(examId: String) = "attendance/$examId"
    }
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.DashboardStudent.route) {
            DashboardStudentScreen(navController)
        }
        composable(Screen.DashboardTeacher.route) {
            DashboardTeacherScreen(navController)
        }
        composable(Screen.IndoorMap.route) {
            IndoorMapScreen()
        }
        composable(
            route = Screen.Attendance.route,
            arguments = listOf(
                navArgument("examId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val examId = backStackEntry.arguments?.getString("examId") ?: ""
            AttendanceScreen(examId = examId, navController = navController)
        }
    }
}
