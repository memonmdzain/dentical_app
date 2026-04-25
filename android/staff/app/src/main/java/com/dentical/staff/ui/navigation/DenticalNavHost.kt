package com.dentical.staff.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dentical.staff.ui.login.LoginScreen
import com.dentical.staff.ui.dashboard.DashboardScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Appointments : Screen("appointments")
    object NewAppointment : Screen("appointments/new")
    object AppointmentDetail : Screen("appointments/{appointmentId}") {
        fun createRoute(id: Long) = "appointments/$id"
    }
    object Patients : Screen("patients")
    object NewPatient : Screen("patients/new")
    object PatientDetail : Screen("patients/{patientId}") {
        fun createRoute(id: Long) = "patients/$id"
    }
    object Billing : Screen("billing")
    object InvoiceDetail : Screen("billing/{invoiceId}") {
        fun createRoute(id: Long) = "billing/$id"
    }
    object Reminders : Screen("reminders")
    object Settings : Screen("settings")
    object ManageStaff : Screen("settings/staff")
    object AddUser : Screen("settings/staff/new")
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

        // More screens will be added as features are built
    }
}
