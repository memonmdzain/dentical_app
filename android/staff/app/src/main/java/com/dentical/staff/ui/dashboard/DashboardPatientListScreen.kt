package com.dentical.staff.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.dentical.staff.util.PhoneUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardPatientListScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DashboardPatientListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.patients.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "No patients found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(uiState.patients) { dashboardPatient ->
                        DashboardPatientCard(
                            dashboardPatient = dashboardPatient,
                            onNavigate = onNavigate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardPatientCard(
    dashboardPatient: DashboardPatient,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val patient = dashboardPatient.patient
    val phone = patient.phone?.takeIf { it.isNotBlank() }
        ?: patient.guardianPhone?.takeIf { it.isNotBlank() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigate(com.dentical.staff.ui.navigation.Screen.PatientDetail.createRoute(patient.id))
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = patient.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${patient.patientCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${"%.2f".format(dashboardPatient.outstandingBalance)} due",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dashboardPatient.outstandingBalance > 0.0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(
                    onClick = {
                        onNavigate(com.dentical.staff.ui.navigation.Screen.AddAppointment.createRoute(patient.id))
                    }
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Schedule appointment",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (phone != null) {
                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${PhoneUtil.formatForDialing(phone)}"))
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(PhoneUtil.whatsAppUrl(phone)))
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}
