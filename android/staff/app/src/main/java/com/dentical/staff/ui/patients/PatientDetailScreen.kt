package com.dentical.staff.ui.patients

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: Long,
    onBack: () -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(patientId) { viewModel.loadPatient(patientId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.patient?.fullName ?: "Patient") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: edit */ }) {
                        Icon(Icons.Default.Edit, "Edit",
                            tint = MaterialTheme.colorScheme.onPrimary)
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
        } else if (uiState.patient == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Patient not found")
            }
        } else {
            val patient = uiState.patient!!
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val age = remember(patient.dateOfBirth) {
                val dob = Calendar.getInstance().apply { timeInMillis = patient.dateOfBirth }
                val today = Calendar.getInstance()
                var a = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) a--
                a
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = patient.fullName.first().uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(patient.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("ID: ${patient.patientCode} · Age $age · ${patient.gender}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                patient.phone ?: patient.guardianPhone?.let { "Guardian: $it" } ?: "No phone",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                // Tabs
                TabRow(selectedTabIndex = uiState.selectedTab) {
                    listOf("Overview", "Treatments", "Invoices").forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.onTabSelected(index) },
                            text = { Text(title) }
                        )
                    }
                }

                when (uiState.selectedTab) {
                    0 -> OverviewTab(patient = patient, dateFormatter = dateFormatter)
                    1 -> TreatmentsTab()
                    2 -> InvoicesTab()
                }
            }
        }
    }
}

@Composable
fun OverviewTab(patient: com.dentical.staff.data.local.entities.PatientEntity,
                dateFormatter: SimpleDateFormat) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailRow("Date of Birth", java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(patient.dateOfBirth)))
        if (patient.guardianName != null) DetailRow("Guardian", patient.guardianName)
        if (patient.guardianPhone != null) DetailRow("Guardian Phone", patient.guardianPhone)
        DetailRow("Referral", if (patient.referralDetail != null) "${patient.referralSource} — ${patient.referralDetail}" else patient.referralSource)
        if (patient.email != null) DetailRow("Email", patient.email)
        if (patient.address != null) DetailRow("Address", patient.address)
        DetailRow("Medical Conditions", patient.medicalConditions ?: "None")
        DetailRow("Allergies", patient.allergies ?: "None")
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun TreatmentsTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Treatments coming soon", color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun InvoicesTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Invoices coming soon", color = MaterialTheme.colorScheme.outline)
    }
}
