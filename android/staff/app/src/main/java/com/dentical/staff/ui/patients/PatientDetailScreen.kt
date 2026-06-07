package com.dentical.staff.ui.patients

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
    onEditPatient: () -> Unit,
    onAddTreatment: () -> Unit,
    onAddVisit: () -> Unit,
    onEditVisit: (Long) -> Unit,
    onTreatmentClick: (Long) -> Unit,
    onScheduleAppointment: () -> Unit,
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
                    IconButton(onClick = onEditPatient) {
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
                    0 -> OverviewTab(patient = patient, dateFormatter = dateFormatter, onScheduleAppointment = onScheduleAppointment)
                    1 -> TreatmentsTab(
                        patient = patient,
                        treatments = uiState.treatments,
                        standaloneVisits = uiState.standaloneVisits,
                        treatmentOutstandings = uiState.treatmentOutstandings,
                        financialSummary = uiState.financialSummary,
                        selectedFilter = uiState.selectedFilter,
                        customFromMs = uiState.customFromMs,
                        customToMs = uiState.customToMs,
                        isFilterLoading = uiState.isFilterLoading,
                        filterError = uiState.filterError,
                        onFilterSelected = { filter, from, to -> viewModel.onFilterSelected(filter, from, to) },
                        onAddTreatment = onAddTreatment,
                        onAddVisit = onAddVisit,
                        onEditVisit = onEditVisit,
                        onTreatmentClick = onTreatmentClick
                    )
                    2 -> InvoicesTab()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentsTab(
    patient: PatientEntity,
    treatments: List<TreatmentEntity>,
    standaloneVisits: List<VisitEntity>,
    treatmentOutstandings: Map<Long, Double>,
    financialSummary: PatientFinancialSummary,
    selectedFilter: TreatmentDateFilter,
    customFromMs: Long,
    customToMs: Long,
    isFilterLoading: Boolean,
    filterError: String?,
    onFilterSelected: (TreatmentDateFilter, Long?, Long?) -> Unit,
    onAddTreatment: () -> Unit,
    onAddVisit: () -> Unit,
    onEditVisit: (Long) -> Unit,
    onTreatmentClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val phone = patient.phone ?: patient.guardianPhone

    // Custom date picker dialog state
    var showCustomPicker by remember { mutableStateOf(false) }

    if (showCustomPicker) {
        CustomDateRangePickerDialog(
            initialFromMs = customFromMs,
            initialToMs = customToMs,
            onConfirm = { from, to ->
                showCustomPicker = false
                onFilterSelected(TreatmentDateFilter.CUSTOM, from, to)
            },
            onDismiss = {
                showCustomPicker = false
                // If user cancels without confirming, revert chip selection to previous if needed
                if (selectedFilter == TreatmentDateFilter.CUSTOM) {
                    // Keep custom selected, just dismiss
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Financial summary card
        if (treatments.isNotEmpty() || standaloneVisits.isNotEmpty()) {
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

        // Ongoing treatments section — no filter applied
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

        // ── Date filter row + Past Treatments ─────────────────────────────────
        item {
            TreatmentDateFilterRow(
                selectedFilter = selectedFilter,
                customFromMs = customFromMs,
                customToMs = customToMs,
                onFilterSelected = { filter ->
                    if (filter == TreatmentDateFilter.CUSTOM) {
                        showCustomPicker = true
                    } else {
                        onFilterSelected(filter, null, null)
                    }
                },
                modifier = Modifier.padding(top = if (ongoingTreatments.isNotEmpty()) 4.dp else 0.dp)
            )
        }

        // Filter error message
        filterError?.let { error ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Filter loading indicator
        if (isFilterLoading) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
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
                StandaloneVisitCard(
                    visit = visit,
                    dateFormatter = dateFormatter,
                    onEdit = { onEditVisit(visit.id) }
                )
            }
        }

        // Empty state for filtered sections
        if (pastTreatments.isEmpty() && standaloneVisits.isEmpty() && filterError == null && !isFilterLoading) {
            item {
                Text(
                    text = when (selectedFilter) {
                        TreatmentDateFilter.LAST_1M -> "No past treatments in the last month."
                        TreatmentDateFilter.LAST_6M -> "No past treatments in the last 6 months."
                        TreatmentDateFilter.LAST_12M -> "No past treatments in the last 12 months."
                        TreatmentDateFilter.CUSTOM -> "No past treatments in the selected date range."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Full empty state (no ongoing AND no past)
        if (treatments.isEmpty() && standaloneVisits.isEmpty() && ongoingTreatments.isEmpty() && filterError == null && !isFilterLoading) {
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

/**
 * Horizontally scrollable filter chip row.
 * Shows 4 options: 1M (default), 6M, 12M, Custom.
 * Custom chip also shows the selected date range when active.
 */
@Composable
private fun TreatmentDateFilterRow(
    selectedFilter: TreatmentDateFilter,
    customFromMs: Long,
    customToMs: Long,
    onFilterSelected: (TreatmentDateFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val shortDateFmt = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }

    val customLabel = if (selectedFilter == TreatmentDateFilter.CUSTOM) {
        "${shortDateFmt.format(Date(customFromMs))} – ${shortDateFmt.format(Date(customToMs))}"
    } else {
        "Custom"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "History:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilterChip(
            selected = selectedFilter == TreatmentDateFilter.LAST_1M,
            onClick = { onFilterSelected(TreatmentDateFilter.LAST_1M) },
            label = { Text("1 Month") }
        )
        FilterChip(
            selected = selectedFilter == TreatmentDateFilter.LAST_6M,
            onClick = { onFilterSelected(TreatmentDateFilter.LAST_6M) },
            label = { Text("6 Months") }
        )
        FilterChip(
            selected = selectedFilter == TreatmentDateFilter.LAST_12M,
            onClick = { onFilterSelected(TreatmentDateFilter.LAST_12M) },
            label = { Text("12 Months") }
        )
        FilterChip(
            selected = selectedFilter == TreatmentDateFilter.CUSTOM,
            onClick = { onFilterSelected(TreatmentDateFilter.CUSTOM) },
            label = { Text(customLabel) },
            leadingIcon = if (selectedFilter == TreatmentDateFilter.CUSTOM) null else {
                { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
            }
        )
    }
}

/**
 * Simple custom date range dialog using two DatePicker dialogs in sequence.
 * First picks "from" date, then "to" date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangePickerDialog(
    initialFromMs: Long,
    initialToMs: Long,
    onConfirm: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var pickingTo by remember { mutableStateOf(false) }
    var fromMs by remember { mutableStateOf(initialFromMs) }

    val fromPickerState = rememberDatePickerState(initialSelectedDateMillis = initialFromMs)
    val toPickerState = rememberDatePickerState(initialSelectedDateMillis = initialToMs)

    if (!pickingTo) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    fromMs = fromPickerState.selectedDateMillis ?: initialFromMs
                    pickingTo = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = fromPickerState,
                title = { Text("From date", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                showModeToggle = false
            )
        }
    } else {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val toMs = toPickerState.selectedDateMillis ?: initialToMs
                    // Ensure to >= from
                    if (toMs >= fromMs) {
                        onConfirm(fromMs, toMs)
                    } else {
                        onConfirm(toMs, fromMs) // swap if user picked backwards
                    }
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { pickingTo = false }) { Text("Back") }
            }
        ) {
            DatePicker(
                state = toPickerState,
                title = { Text("To date", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                showModeToggle = false
            )
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
private fun StandaloneVisitCard(
    visit: VisitEntity,
    dateFormatter: SimpleDateFormat,
    onEdit: () -> Unit
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit visit",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
fun OverviewTab(patient: PatientEntity, dateFormatter: SimpleDateFormat, onScheduleAppointment: () -> Unit) {
    val context = LocalContext.current
    val phone = patient.phone?.takeIf { it.isNotBlank() }
        ?: patient.guardianPhone?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onScheduleAppointment) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Schedule appointment", tint = MaterialTheme.colorScheme.primary)
            }
            if (phone != null) {
                IconButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${PhoneUtil.formatForDialing(phone)}"))
                        )
                    }
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(PhoneUtil.whatsAppUrl(phone)))
                        )
                    }
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                }
            }
        }

        HorizontalDivider()

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
