package com.dentical.staff.ui.treatments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onEditTreatment: () -> Unit,
    onEditVisit: (visitId: Long) -> Unit,
    viewModel: TreatmentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(treatmentId) { viewModel.load(treatmentId) }

    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.treatment?.procedure?.displayName ?: "Treatment") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = onEditTreatment) {
                        Icon(Icons.Default.Edit, "Edit treatment",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
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
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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

                // Error card (payment outstanding)
                uiState.error?.let { errorMsg ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                errorMsg,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Status action buttons
                when (treatment.status) {
                    TreatmentStatus.ONGOING -> item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.openCancelDialog() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Cancel Treatment") }
                            Button(
                                onClick = { viewModel.markComplete(treatment.id) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Mark Complete") }
                        }
                    }
                    TreatmentStatus.COMPLETED, TreatmentStatus.CANCELLED -> item {
                        OutlinedButton(
                            onClick = { viewModel.openReopenDialog() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Reopen Treatment") }
                    }
                }

                // Visits header
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
                            dateFormatter = dateFormatter,
                            onEdit = { onEditVisit(visit.id) }
                        )
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (uiState.showCancelDialog) {
        val treatment = uiState.treatment
        val refundAmount = if (uiState.cancelBalance < -0.01) -uiState.cancelBalance else 0.0
        val balanceOwed = if (uiState.cancelBalance > 0.01) uiState.cancelBalance else 0.0
        val refundNeeded = refundAmount > 0.01

        AlertDialog(
            onDismissRequest = { viewModel.dismissCancelDialog() },
            title = { Text("Cancel Treatment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (treatment != null) {
                        val label = buildString {
                            append(treatment.procedure.displayName)
                            treatment.toothNumber?.let { append(" · Tooth #$it") }
                        }
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }

                    Text(
                        "Set the amount to charge for work completed so far:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = uiState.cancelPartialCharge,
                        onValueChange = { viewModel.onCancelPartialChargeChanged(it) },
                        label = { Text("Amount to charge ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    when {
                        refundNeeded -> Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Refund ₹${refundAmount.toLong()} to patient",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Patient has paid more than what will be billed after this cancellation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = uiState.cancelConfirmRefundDone,
                                        onCheckedChange = { viewModel.onCancelConfirmRefundToggle(it) }
                                    )
                                    Text(
                                        "I confirm I have refunded ₹${refundAmount.toLong()} to the patient",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        balanceOwed > 0.01 -> Text(
                            "Patient still has ₹${balanceOwed.toLong()} outstanding after this cancellation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        else -> Text(
                            "Patient balance is clear after this cancellation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    uiState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmCancelTreatment() },
                    enabled = !refundNeeded || uiState.cancelConfirmRefundDone
                ) { Text("Cancel Treatment", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCancelDialog() }) { Text("Keep") }
            }
        )
    }

    if (uiState.showReopenDialog) {
        val treatment = uiState.treatment
        AlertDialog(
            onDismissRequest = { viewModel.dismissReopenDialog() },
            title = { Text("Reopen Treatment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (treatment != null) {
                        val label = buildString {
                            append(treatment.procedure.displayName)
                            treatment.toothNumber?.let { append(" · Tooth #$it") }
                        }
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Update the quoted cost for this treatment:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = uiState.reopenQuotedCost,
                        onValueChange = { viewModel.onReopenQuotedCostChanged(it) },
                        label = { Text("Quoted cost ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Leave blank if not set") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmReopenTreatment() }) {
                    Text("Reopen")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissReopenDialog() }) { Text("Cancel") }
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        visit.amountPaid > 0 -> Surface(
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
                        visit.amountPaid < 0 -> Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                "Refunded ${formatCurrency(-visit.amountPaid)}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (visit.amountPaid >= 0) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, "Edit visit",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text("By ${visit.performedBy}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (crossRef != null && crossRef.workDone.isNotBlank()) {
                Text(crossRef.workDone, style = MaterialTheme.typography.bodySmall)
            }
            if (!visit.notes.isNullOrBlank()) {
                Text(visit.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    fmt.maximumFractionDigits = 0
    return fmt.format(amount)
}
