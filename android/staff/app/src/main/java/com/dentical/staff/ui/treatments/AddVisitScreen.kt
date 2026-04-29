package com.dentical.staff.ui.treatments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dentical.staff.data.local.entities.PaymentMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVisitScreen(
    patientId: Long,
    preSelectedTreatmentId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddVisitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(patientId, preSelectedTreatmentId) {
        viewModel.load(patientId, preSelectedTreatmentId)
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDentistPicker by remember { mutableStateOf(false) }
    var showPaymentModePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val hasSelectedTreatments = uiState.treatmentSelections.any { it.isSelected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Visit") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Visit date
            OutlinedTextField(
                value = dateFormatter.format(Date(uiState.visitDateMillis)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Visit Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, "Pick date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Performed by (dentist dropdown)
            ExposedDropdownMenuBox(
                expanded = showDentistPicker,
                onExpandedChange = { showDentistPicker = it }
            ) {
                val selectedDentistName = uiState.dentists
                    .find { it.id == uiState.selectedDentistId }?.fullName ?: "Select Dentist *"
                OutlinedTextField(
                    value = selectedDentistName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Performed By *") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showDentistPicker,
                    onDismissRequest = { showDentistPicker = false }
                ) {
                    uiState.dentists.forEach { dentist ->
                        DropdownMenuItem(
                            text = { Text(dentist.fullName) },
                            onClick = {
                                viewModel.onDentistSelected(dentist.id)
                                showDentistPicker = false
                            }
                        )
                    }
                }
            }

            // Treatments section
            if (uiState.treatmentSelections.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Treatments Performed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Select treatments worked on during this visit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(4.dp))
                    uiState.treatmentSelections.forEach { selection ->
                        TreatmentSelectionItem(
                            selection = selection,
                            onToggle = { viewModel.onTreatmentToggle(selection.treatment.id, it) },
                            onWorkDoneChange = { viewModel.onWorkDoneChange(selection.treatment.id, it) }
                        )
                    }
                }
            } else {
                Text(
                    "No ongoing treatments — this will be recorded as a standalone visit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Cost charged (only for standalone visits)
            AnimatedVisibility(visible = !hasSelectedTreatments) {
                OutlinedTextField(
                    value = uiState.costCharged,
                    onValueChange = viewModel::onCostChargedChange,
                    label = { Text("Cost Charged ₹ *") },
                    supportingText = { Text("Required when no treatment is linked") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Amount paid
            OutlinedTextField(
                value = uiState.amountPaid,
                onValueChange = viewModel::onAmountPaidChange,
                label = { Text("Amount Paid ₹") },
                supportingText = { Text("Leave blank or 0 if not collected today") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Payment mode
            ExposedDropdownMenuBox(
                expanded = showPaymentModePicker,
                onExpandedChange = { showPaymentModePicker = it }
            ) {
                OutlinedTextField(
                    value = uiState.paymentMode?.displayName ?: "Select Payment Mode",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Mode") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showPaymentModePicker,
                    onDismissRequest = { showPaymentModePicker = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            viewModel.onPaymentModeChange(null)
                            showPaymentModePicker = false
                        }
                    )
                    PaymentMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.displayName) },
                            onClick = {
                                viewModel.onPaymentModeChange(mode)
                                showPaymentModePicker = false
                            }
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { viewModel.save(patientId) },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Visit")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.visitDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onVisitDateChange(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TreatmentSelectionItem(
    selection: TreatmentSelection,
    onToggle: (Boolean) -> Unit,
    onWorkDoneChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selection.isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = selection.isSelected,
                    onCheckedChange = onToggle
                )
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        selection.treatment.procedure.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (!selection.treatment.toothNumber.isNullOrBlank()) {
                        Text(
                            "Tooth #${selection.treatment.toothNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            AnimatedVisibility(visible = selection.isSelected) {
                OutlinedTextField(
                    value = selection.workDone,
                    onValueChange = onWorkDoneChange,
                    label = { Text("Work done for this treatment") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    minLines = 1,
                    maxLines = 3
                )
            }
        }
    }
}
