package com.dentical.staff.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dentical.staff.ui.appointments.*
import com.dentical.staff.ui.dashboard.DashboardScreen
import com.dentical.staff.ui.login.LoginScreen
import com.dentical.staff.ui.patients.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Patients : Screen("patients")
    object AddPatient : Screen("patients/new")
    object PatientDetail : Screen("patients/{patientId}") {
        fun createRoute(id: Long) = "patients/$id"
    }
    object Appointments : Screen("appointments")
    object AddAppointment : Screen("appointments/new")
    object AppointmentDetail : Screen("appointments/{appointmentId}") {
        fun createRoute(id: Long) = "appointments/$id"
    }
    object EditAppointment : Screen("appointments/{appointmentId}/edit") {
        fun createRoute(id: Long) = "appointments/$id/edit"
    }
    object Billing : Screen("billing")
    object Reminders : Screen("reminders")
    object Settings : Screen("settings")
}

@Composable
fun DenticalNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(onNavigate = { navController.navigate(it) })
        }

        // Patients
        composable(Screen.Patients.route) {
            PatientListScreen(
                onAddPatient = { navController.navigate(Screen.AddPatient.route) },
                onPatientClick = { navController.navigate(Screen.PatientDetail.createRoute(it)) },
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
        ) {
            val id = it.arguments?.getLong("patientId") ?: return@composable
            PatientDetailScreen(patientId = id, onBack = { navController.popBackStack() })
        }

        // Appointments
        composable(Screen.Appointments.route) {
            AppointmentsScreen(
                onAddAppointment = { navController.navigate(Screen.AddAppointment.route) },
                onAppointmentClick = { navController.navigate(Screen.AppointmentDetail.createRoute(it)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddAppointment.route) {
            AddAppointmentScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AppointmentDetail.route,
            arguments = listOf(navArgument("appointmentId") { type = NavType.LongType })
        ) {
            val id = it.arguments?.getLong("appointmentId") ?: return@composable
            AppointmentDetailScreen(
                appointmentId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.EditAppointment.createRoute(it)) }
            )
        }
        composable(
            route = Screen.EditAppointment.route,
            arguments = listOf(navArgument("appointmentId") { type = NavType.LongType })
        ) {
            val id = it.arguments?.getLong("appointmentId") ?: return@composable
            EditAppointmentScreen(
                appointmentId = id,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
