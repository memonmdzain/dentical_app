package com.dentical.staff.ui.treatments

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dentical.staff.data.local.entities.AppointmentType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTreatmentScreen(
    treatmentId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditTreatmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(treatmentId) { viewModel.load(treatmentId) }
    LaunchedEffect(uiState.saved) { if (uiState.saved) onSaved() }

    var showDatePicker by remember { mutableStateOf(false) }
    var showProcedurePicker by remember { mutableStateOf(false) }
    var showDentistPicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Treatment") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Procedure type
                ExposedDropdownMenuBox(
                    expanded = showProcedurePicker,
                    onExpandedChange = { showProcedurePicker = it }
                ) {
                    OutlinedTextField(
                        value = uiState.procedure.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Procedure Type *") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showProcedurePicker,
                        onDismissRequest = { showProcedurePicker = false }
                    ) {
                        AppointmentType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = { viewModel.onProcedureChange(type); showProcedurePicker = false }
                            )
                        }
                    }
                }

                // Tooth number
                OutlinedTextField(
                    value = uiState.toothNumber,
                    onValueChange = viewModel::onToothNumberChange,
                    label = { Text("Tooth Number (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Dentist dropdown
                ExposedDropdownMenuBox(
                    expanded = showDentistPicker,
                    onExpandedChange = { showDentistPicker = it }
                ) {
                    val selectedDentistName = uiState.dentists
                        .find { it.id == uiState.selectedDentistId }?.fullName ?: "Select Dentist"
                    OutlinedTextField(
                        value = selectedDentistName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dentist (optional)") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showDentistPicker,
                        onDismissRequest = { showDentistPicker = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = { viewModel.onDentistSelected(null); showDentistPicker = false }
                        )
                        uiState.dentists.forEach { dentist ->
                            DropdownMenuItem(
                                text = { Text(dentist.fullName) },
                                onClick = { viewModel.onDentistSelected(dentist.id); showDentistPicker = false }
                            )
                        }
                    }
                }

                // Start date
                OutlinedTextField(
                    value = dateFormatter.format(Date(uiState.startDateMillis)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "Pick date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quoted cost
                OutlinedTextField(
                    value = uiState.quotedCost,
                    onValueChange = viewModel::onQuotedCostChange,
                    label = { Text("Quoted Cost ₹ (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Visits required
                OutlinedTextField(
                    value = uiState.visitsRequired,
                    onValueChange = viewModel::onVisitsRequiredChange,
                    label = { Text("Estimated Visits Required (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

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
                    onClick = { viewModel.save(treatmentId) },
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
                        Text("Save Changes")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.startDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onStartDateChange(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}
