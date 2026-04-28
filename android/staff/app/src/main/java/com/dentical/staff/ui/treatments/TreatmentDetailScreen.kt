package com.dentical.staff.ui.treatments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dentical.staff.data.local.entities.TreatmentStatus
import com.dentical.staff.data.local.entities.TreatmentVisitCrossRef
import com.dentical.staff.data.local.entities.VisitEntity
import com.dentical.staff.ui.patients.TreatmentStatusBadge
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentDetailScreen(
    treatmentId: Long,
    onBack: () -> Unit,
    onAddVisit: () -> Unit,
    viewModel: TreatmentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(treatmentId) { viewModel.load(treatmentId) }

    var showCancelDialog by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.treatment?.procedure?.displayName ?: "Treatment") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddVisit,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Visit") }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.treatment == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Treatment not found")
            }
        } else {
            val treatment = uiState.treatment!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Treatment info card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    treatment.procedure.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                TreatmentStatusBadge(treatment.status)
                            }

                            if (!treatment.toothNumber.isNullOrBlank()) {
                                DetailChip("Tooth #${treatment.toothNumber}")
                            }

                            if (uiState.dentist != null) {
                                Text(
                                    "Dentist: ${uiState.dentist!!.fullName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Text(
                                "Started: ${dateFormatter.format(Date(treatment.startDate))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            if (treatment.completedDate != null) {
                                Text(
                                    "Completed: ${dateFormatter.format(Date(treatment.completedDate))}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                if (treatment.quotedCost != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            formatCurrency(treatment.quotedCost),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text("Quoted Cost",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${uiState.visitCount}${treatment.visitsRequired?.let { "/$it" } ?: ""}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("Visit(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }

                            if (!treatment.description.isNullOrBlank()) {
                                Text(treatment.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            if (!treatment.notes.isNullOrBlank()) {
                                Text("Note: ${treatment.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }

                // Status action buttons
                if (treatment.status == TreatmentStatus.ONGOING) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel Treatment")
                            }
                            Button(
                                onClick = { viewModel.markComplete(treatment.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Mark Complete")
                            }
                        }
                    }
                } else if (treatment.status == TreatmentStatus.CANCELLED) {
                    item {
                        OutlinedButton(
                            onClick = { viewModel.reactivateTreatment(treatment.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reactivate Treatment")
                        }
                    }
                }

                // Visits section header
                item {
                    Text(
                        "Visits (${uiState.visitCount})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (uiState.visits.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No visits recorded yet",
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    items(uiState.visits, key = { it.id }) { visit ->
                        val crossRef = uiState.crossRefs.find { it.visitId == visit.id }
                        TreatmentVisitCard(
                            visit = visit,
                            crossRef = crossRef,
                            dateFormatter = dateFormatter
                        )
                    }
                }

                // Bottom spacer for FAB
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Treatment?") },
            text = { Text("This will mark the treatment as cancelled. You can reactivate it later.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        uiState.treatment?.let { viewModel.cancelTreatment(it.id) }
                        showCancelDialog = false
                    }
                ) { Text("Cancel Treatment", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun DetailChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TreatmentVisitCard(
    visit: VisitEntity,
    crossRef: TreatmentVisitCrossRef?,
    dateFormatter: SimpleDateFormat
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
                if (visit.amountPaid > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFFE8F5E9)
                    ) {
                        val paidLabel = buildString {
                            append("Paid ${formatCurrency(visit.amountPaid)}")
                            visit.paymentMode?.let { append(" via ${it.displayName}") }
                        }
                        Text(
                            paidLabel,
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
            if (crossRef != null && crossRef.workDone.isNotBlank()) {
                Text(
                    crossRef.workDone,
                    style = MaterialTheme.typography.bodySmall
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

private fun formatCurrency(amount: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    fmt.maximumFractionDigits = 0
    return fmt.format(amount)
}
