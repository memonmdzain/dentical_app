package com.dentical.staff.ui.appointments

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dentical.staff.data.local.entities.AppointmentStatus
import com.dentical.staff.util.PhoneUtil
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailScreen(
    appointmentId: Long,
    onBack: () -> Unit,
    viewModel: AppointmentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(appointmentId) { viewModel.load(appointmentId) }

    val phone = uiState.patient?.phone ?: uiState.patient?.guardianPhone
    val hasPhone = !phone.isNullOrBlank()

    fun dial() = phone?.let {
        context.startActivity(Intent(Intent.ACTION_DIAL,
            Uri.parse("tel:${PhoneUtil.formatForDialing(it)}")))
    }

    fun openWhatsApp() = phone?.let {
        context.startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse(PhoneUtil.whatsAppUrl(it))))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appointment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val appt = uiState.appointment ?: return@Scaffold
        val dateTimeFormatter = SimpleDateFormat("EEEE, MMM d yyyy · hh:mm a", Locale.getDefault())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status badge + type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(appt.type.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                StatusBadge(appt.status)
            }

            // Date & time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(dateTimeFormatter.format(Date(appt.scheduledAt)),
                    style = MaterialTheme.typography.bodyLarge)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${appt.durationMinutes} minutes",
                    style = MaterialTheme.typography.bodyLarge)
            }

            HorizontalDivider()

            // Patient info
            Text("Patient", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(uiState.patient?.fullName ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text("ID: ${uiState.patient?.patientCode ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(12.dp))

                    // Call & WhatsApp buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { dial() },
                            enabled = hasPhone,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Call")
                        }
                        OutlinedButton(
                            onClick = { openWhatsApp() },
                            enabled = hasPhone,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Message, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("WhatsApp")
                        }
                    }
                    if (!hasPhone) {
                        Text("No phone number available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Dentist
            val dentist = uiState.dentist
            if (dentist != null) {
                Text("Dentist", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("Dr. ${dentist.fullName}",
                        style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Notes
            if (!appt.notes.isNullOrBlank()) {
                HorizontalDivider()
                Text("Notes", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text(appt.notes, style = MaterialTheme.typography.bodyLarge)
            }

            HorizontalDivider()

            // Status actions
            Text("Update Status", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)

            val nextStatuses = when (appt.status) {
                AppointmentStatus.SCHEDULED -> listOf(
                    AppointmentStatus.CONFIRMED,
                    AppointmentStatus.CANCELLED,
                    AppointmentStatus.NO_SHOW)
                AppointmentStatus.CONFIRMED -> listOf(
                    AppointmentStatus.IN_PROGRESS,
                    AppointmentStatus.CANCELLED,
                    AppointmentStatus.NO_SHOW)
                AppointmentStatus.IN_PROGRESS -> listOf(
                    AppointmentStatus.COMPLETED,
                    AppointmentStatus.CANCELLED)
                else -> emptyList()
            }

            if (nextStatuses.isEmpty()) {
                Text("No further status updates available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                nextStatuses.forEach { status ->
                    val (label, color) = when (status) {
                        AppointmentStatus.CONFIRMED -> "Confirm Appointment" to MaterialTheme.colorScheme.primary
                        AppointmentStatus.IN_PROGRESS -> "Mark In Progress" to MaterialTheme.colorScheme.tertiary
                        AppointmentStatus.COMPLETED -> "Mark Completed" to MaterialTheme.colorScheme.secondary
                        AppointmentStatus.CANCELLED -> "Cancel Appointment" to MaterialTheme.colorScheme.error
                        AppointmentStatus.NO_SHOW -> "Mark No Show" to MaterialTheme.colorScheme.error
                        else -> return@forEach
                    }
                    OutlinedButton(
                        onClick = { viewModel.updateStatus(appointmentId, status) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
                    ) { Text(label) }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
