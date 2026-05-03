package com.dentical.staff.ui.patients

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dentical.staff.data.local.entities.PatientEntity
import com.dentical.staff.data.local.entities.PaymentMode
import com.dentical.staff.data.local.entities.TreatmentEntity
import com.dentical.staff.data.local.entities.TreatmentStatus
import com.dentical.staff.data.local.entities.VisitEntity
import com.dentical.staff.data.repository.PatientFinancialSummary
import com.dentical.staff.util.PhoneUtil
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: Long,
    onBack: () -> Unit,
    onAddTreatment: () -> Unit,
    onAddVisit: () -> Unit,
    onTreatmentClick: (Long) -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val canSync by viewModel.canSync.collectAsState()
    val context = LocalContext.current

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
                    IconButton(onClick = viewModel::onSyncClick, enabled = canSync) {
                        if (isSyncing)
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        else
                            Icon(Icons.Default.Sync, "Sync",
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
            val outstanding = uiState.financialSummary.totalOutstanding
            val phone = patient.phone ?: patient.guardianPhone

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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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

                        // Outstanding balance row
                        if (outstanding > 0) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Outstanding Balance",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        formatCurrency(outstanding),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                if (phone != null) {
                                    FilledTonalButton(
                                        onClick = {
                                            val message = "Dear ${patient.fullName}, you have an outstanding balance of ${formatCurrency(outstanding)} at our clinic. Kindly contact us to settle your dues. Thank you."
                                            val url = "${PhoneUtil.whatsAppUrl(phone)}?text=${Uri.encode(message)}"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFF25D366),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null,
                                            modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Remind", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
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

                // Debug error banner — remove before launch
                uiState.error?.let { errorMsg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "ERROR: $errorMsg",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                when (uiState.selectedTab) {
                    0 -> OverviewTab(patient = patient, dateFormatter = dateFormatter)
                    1 -> TreatmentsTab(
                        patient = patient,
                        treatments = uiState.treatments,
                        standaloneVisits = uiState.visits.filter {
                            uiState.visitCrossRefs[it.id].isNullOrEmpty()
                        },
                        treatmentOutstandings = uiState.treatmentOutstandings,
                        financialSummary = uiState.financialSummary,
                        onAddTreatment = onAddTreatment,
                        onAddVisit = onAddVisit,
                        onTreatmentClick = onTreatmentClick
                    )
                    2 -> InvoicesTab()
                }
            }
        }
    }
}

@Composable
fun TreatmentsTab(
    patient: PatientEntity,
    treatments: List<TreatmentEntity>,
    standaloneVisits: List<VisitEntity>,
    treatmentOutstandings: Map<Long, Double>,
    financialSummary: PatientFinancialSummary,
    onAddTreatment: () -> Unit,
    onAddVisit: () -> Unit,
    onTreatmentClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val phone = patient.phone ?: patient.guardianPhone

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Financial summary card
        if (treatments.isNotEmpty()) {
            item {
                FinancialSummaryCard(
                    financialSummary = financialSummary,
                    patient = patient,
                    phone = phone,
                    context = context
                )
            }
        }

        // Action buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onAddVisit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Visit")
                }
                Button(onClick = onAddTreatment, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Treatment")
                }
            }
        }

        // Ongoing treatments section
        val ongoingTreatments = treatments.filter { it.status == TreatmentStatus.ONGOING }
        if (ongoingTreatments.isNotEmpty()) {
            item { SectionHeader("Ongoing Treatments (${ongoingTreatments.size})", modifier = Modifier.padding(top = 4.dp)) }
            items(ongoingTreatments, key = { "t${it.id}" }) { treatment ->
                TreatmentCard(
                    treatment = treatment,
                    dateFormatter = dateFormatter,
                    outstanding = treatmentOutstandings[treatment.id],
                    onClick = { onTreatmentClick(treatment.id) }
                )
            }
        }

        // Past treatments section (Completed + Cancelled)
        val pastTreatments = treatments.filter { it.status != TreatmentStatus.ONGOING }
        if (pastTreatments.isNotEmpty()) {
            item { SectionHeader("Past Treatments (${pastTreatments.size})", modifier = Modifier.padding(top = 4.dp)) }
            items(pastTreatments, key = { "t${it.id}" }) { treatment ->
                TreatmentCard(treatment = treatment, dateFormatter = dateFormatter,
                    onClick = { onTreatmentClick(treatment.id) })
            }
        }

        // Standalone visits section
        if (standaloneVisits.isNotEmpty()) {
            item { SectionHeader("Standalone Visits (${standaloneVisits.size})", modifier = Modifier.padding(top = 4.dp)) }
            items(standaloneVisits, key = { "sv${it.id}" }) { visit ->
                StandaloneVisitCard(visit = visit, dateFormatter = dateFormatter)
            }
        }

        // Empty state
        if (treatments.isEmpty() && standaloneVisits.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No treatments yet. Tap Add Treatment to get started.",
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun StandaloneVisitCard(visit: VisitEntity, dateFormatter: SimpleDateFormat) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateFormatter.format(Date(visit.visitDate)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (visit.amountPaid > 0) {
                    Surface(shape = MaterialTheme.shapes.small, color = Color(0xFFE8F5E9)) {
                        Text(
                            buildString {
                                append("Paid ${formatCurrency(visit.amountPaid)}")
                                visit.paymentMode?.let { append(" · ${it.displayName}") }
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Text(
                "By ${visit.performedBy}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (visit.costCharged > 0) {
                Text(
                    "Charged: ${formatCurrency(visit.costCharged)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!visit.notes.isNullOrBlank()) {
                Text(
                    visit.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FinancialSummaryCard(
    financialSummary: PatientFinancialSummary,
    patient: PatientEntity,
    phone: String?,
    context: android.content.Context
) {
    val totalBilled = financialSummary.totalQuoted + financialSummary.standaloneCharged

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Financial Summary",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                FinancialFigure(
                    label = "Total Billed",
                    amount = totalBilled,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FinancialFigure(
                    label = "Paid",
                    amount = financialSummary.totalPaid,
                    color = Color(0xFF2E7D32)
                )
                FinancialFigure(
                    label = "Outstanding",
                    amount = financialSummary.totalOutstanding,
                    color = if (financialSummary.totalOutstanding > 0)
                        MaterialTheme.colorScheme.error
                    else Color(0xFF2E7D32)
                )
            }

            if (financialSummary.totalOutstanding > 0 && phone != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val message = "Dear ${patient.fullName}, you have an outstanding balance of ${formatCurrency(financialSummary.totalOutstanding)} at our clinic. Kindly contact us to settle your dues. Thank you."
                        val url = "${PhoneUtil.whatsAppUrl(phone)}?text=${Uri.encode(message)}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF25D366)
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Send Payment Reminder via WhatsApp")
                }
            }
        }
    }
}

@Composable
private fun FinancialFigure(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            formatCurrency(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TreatmentCard(
    treatment: TreatmentEntity,
    dateFormatter: SimpleDateFormat,
    outstanding: Double? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        treatment.procedure.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!treatment.toothNumber.isNullOrBlank()) {
                        Text(
                            "Tooth #${treatment.toothNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TreatmentStatusBadge(treatment.status)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Started ${dateFormatter.format(Date(treatment.startDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (treatment.quotedCost != null) {
                    Text(
                        "Quoted ${formatCurrency(treatment.quotedCost)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (treatment.visitsRequired != null) {
                Text(
                    "Est. ${treatment.visitsRequired} visit(s) required",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (outstanding != null && outstanding > 0.01 && treatment.status == TreatmentStatus.ONGOING) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        "Outstanding: ${formatCurrency(outstanding)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun TreatmentStatusBadge(status: TreatmentStatus) {
    val (bgColor, textColor, label) = when (status) {
        TreatmentStatus.ONGOING -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Ongoing"
        )
        TreatmentStatus.COMPLETED -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "Completed"
        )
        TreatmentStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Cancelled"
        )
    }
    Surface(shape = MaterialTheme.shapes.small, color = bgColor) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatCurrency(amount: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    fmt.maximumFractionDigits = 0
    return fmt.format(amount)
}

@Composable
fun OverviewTab(patient: PatientEntity, dateFormatter: SimpleDateFormat) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailRow("Date of Birth", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(patient.dateOfBirth)))
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
fun InvoicesTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Invoices coming soon", color = MaterialTheme.colorScheme.outline)
    }
}
