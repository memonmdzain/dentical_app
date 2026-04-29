package com.dentical.staff.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dentical.staff.ui.appointments.*
import com.dentical.staff.ui.dashboard.DashboardPatientListScreen
import com.dentical.staff.ui.dashboard.DashboardScreen
import com.dentical.staff.ui.login.LoginScreen
import com.dentical.staff.ui.patients.*
import com.dentical.staff.ui.treatments.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Patients : Screen("patients")
    object AddPatient : Screen("patients/new")
    object PatientDetail : Screen("patients/{patientId}") {
        fun createRoute(id: Long) = "patients/$id"
    }
    object Appointments : Screen("appointments")
    object AddAppointment : Screen("appointments/new?patientId={patientId}") {
        fun createRoute(patientId: Long? = null) =
            if (patientId != null) "appointments/new?patientId=$patientId" else "appointments/new"
    }
    object PatientListDashboard : Screen("dashboard/patients/{filter}") {
        fun createRoute(filter: String) = "dashboard/patients/$filter"
    }
    object AppointmentDetail : Screen("appointments/{appointmentId}") {
        fun createRoute(id: Long) = "appointments/$id"
    }
    object EditAppointment : Screen("appointments/{appointmentId}/edit") {
        fun createRoute(id: Long) = "appointments/$id/edit"
    }
    object AddTreatment : Screen("patients/{patientId}/treatments/new") {
        fun createRoute(patientId: Long) = "patients/$patientId/treatments/new"
    }
    object TreatmentDetail : Screen("patients/{patientId}/treatments/{treatmentId}") {
        fun createRoute(patientId: Long, treatmentId: Long) =
            "patients/$patientId/treatments/$treatmentId"
    }
    object EditTreatment : Screen("patients/{patientId}/treatments/{treatmentId}/edit") {
        fun createRoute(patientId: Long, treatmentId: Long) =
            "patients/$patientId/treatments/$treatmentId/edit"
    }
    object AddVisit : Screen("patients/{patientId}/visits/new?preSelectedTreatmentId={preSelectedTreatmentId}") {
        fun createRoute(patientId: Long, preSelectedTreatmentId: Long = -1L) =
            "patients/$patientId/visits/new?preSelectedTreatmentId=$preSelectedTreatmentId"
    }
    object EditVisit : Screen("patients/{patientId}/visits/{visitId}/edit") {
        fun createRoute(patientId: Long, visitId: Long) =
            "patients/$patientId/visits/$visitId/edit"
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
            val patientId = it.arguments?.getLong("patientId") ?: return@composable
            PatientDetailScreen(
                patientId = patientId,
                onBack = { navController.popBackStack() },
                onAddTreatment = { navController.navigate(Screen.AddTreatment.createRoute(patientId)) },
                onAddVisit = { navController.navigate(Screen.AddVisit.createRoute(patientId)) },
                onTreatmentClick = { treatmentId ->
                    navController.navigate(Screen.TreatmentDetail.createRoute(patientId, treatmentId))
                }
            )
        }

        // Appointments
        composable(Screen.Appointments.route) {
            AppointmentsScreen(
                onAddAppointment = { navController.navigate(Screen.AddAppointment.createRoute()) },
                onAppointmentClick = { navController.navigate(Screen.AppointmentDetail.createRoute(it)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AddAppointment.route,
            arguments = listOf(navArgument("patientId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            AddAppointmentScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.PatientListDashboard.route,
            arguments = listOf(navArgument("filter") { type = NavType.StringType })
        ) {
            DashboardPatientListScreen(
                onNavigate = { navController.navigate(it) },
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

        // Treatments
        composable(
            route = Screen.AddTreatment.route,
            arguments = listOf(navArgument("patientId") { type = NavType.LongType })
        ) {
            val patientId = it.arguments?.getLong("patientId") ?: return@composable
            AddTreatmentScreen(
                patientId = patientId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TreatmentDetail.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.LongType },
                navArgument("treatmentId") { type = NavType.LongType }
            )
        ) {
            val patientId = it.arguments?.getLong("patientId") ?: return@composable
            val treatmentId = it.arguments?.getLong("treatmentId") ?: return@composable
            TreatmentDetailScreen(
                treatmentId = treatmentId,
                onBack = { navController.popBackStack() },
                onAddVisit = {
                    navController.navigate(Screen.AddVisit.createRoute(patientId, treatmentId))
                },
                onEditTreatment = {
                    navController.navigate(Screen.EditTreatment.createRoute(patientId, treatmentId))
                },
                onEditVisit = { visitId ->
                    navController.navigate(Screen.EditVisit.createRoute(patientId, visitId))
                }
            )
        }
        composable(
            route = Screen.EditTreatment.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.LongType },
                navArgument("treatmentId") { type = NavType.LongType }
            )
        ) {
            val treatmentId = it.arguments?.getLong("treatmentId") ?: return@composable
            EditTreatmentScreen(
                treatmentId = treatmentId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AddVisit.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.LongType },
                navArgument("preSelectedTreatmentId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            val patientId = it.arguments?.getLong("patientId") ?: return@composable
            val preSelected = it.arguments?.getLong("preSelectedTreatmentId") ?: -1L
            AddVisitScreen(
                patientId = patientId,
                preSelectedTreatmentId = preSelected,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditVisit.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.LongType },
                navArgument("visitId") { type = NavType.LongType }
            )
        ) {
            val visitId = it.arguments?.getLong("visitId") ?: return@composable
            EditVisitScreen(
                visitId = visitId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
