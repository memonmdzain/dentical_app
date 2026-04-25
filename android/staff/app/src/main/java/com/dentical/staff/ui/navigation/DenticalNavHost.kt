package com.dentical.staff.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dentical.staff.ui.dashboard.DashboardScreen
import com.dentical.staff.ui.login.LoginScreen
import com.dentical.staff.ui.patients.AddPatientScreen
import com.dentical.staff.ui.patients.PatientDetailScreen
import com.dentical.staff.ui.patients.PatientListScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Patients : Screen("patients")
    object AddPatient : Screen("patients/new")
    object PatientDetail : Screen("patients/{patientId}") {
        fun createRoute(id: Long) = "patients/$id"
    }
    object Appointments : Screen("appointments")
    object Billing : Screen("billing")
    object Reminders : Screen("reminders")
    object Settings : Screen("settings")
}

@Composable
fun DenticalNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(Screen.Patients.route) {
            PatientListScreen(
                onAddPatient = { navController.navigate(Screen.AddPatient.route) },
                onPatientClick = { id -> navController.navigate(Screen.PatientDetail.createRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddPatient.route) {
            AddPatientScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PatientDetail.route,
            arguments = listOf(navArgument("patientId") { type = NavType.LongType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getLong("patientId") ?: return@composable
            PatientDetailScreen(
                patientId = patientId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
