package com.dentical.staff.ui.appointments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dentical.staff.data.local.entities.AppointmentType
import com.dentical.staff.ui.patients.DropdownField
import com.dentical.staff.ui.patients.FormField
import com.dentical.staff.ui.patients.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppointmentScreen(
    appointmentId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditAppointmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(appointmentId) { viewModel.load(appointmentId) }
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Appointment") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Patient — read only in edit mode
            SectionTitle("Patient")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(uiState.selectedPatient?.fullName ?: "Loading...",
                        style = MaterialTheme.typography.titleMedium)
                    Text("ID: ${uiState.selectedPatient?.patientCode ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Patient cannot be changed after booking.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Dentist
            SectionTitle("Dentist")
            FormField("Assign to dentist") {
                DropdownField(
                    value = uiState.selectedDentist?.fullName ?: "",
                    options = uiState.dentists.map { it.fullName },
                    onSelected = { name ->
                        uiState.dentists.find { it.fullName == name }
                            ?.let { viewModel.onDentistSelected(it) }
                    },
                    placeholder = "Select dentist",
                    isError = uiState.dentistError != null,
                    errorText = uiState.dentistError
                )
            }

            // Type
            SectionTitle("Appointment Details")
            FormField("Type *") {
                DropdownField(
                    value = uiState.appointmentType.displayName,
                    options = AppointmentType.values().map { it.displayName },
                    onSelected = { name ->
                        AppointmentType.values().find { it.displayName == name }
                            ?.let { viewModel.onTypeSelected(it) }
                    },
                    placeholder = "Select type"
                )
            }

            // Date & Time
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    FormField("Date *") {
                        OutlinedTextField(
                            value = uiState.dateDisplay,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                TextButton(onClick = viewModel::onShowDatePicker) { Text("Pick") }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.dateError != null,
                            supportingText = uiState.dateError?.let { { Text(it) } }
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    FormField("Time *") {
                        OutlinedTextField(
                            value = uiState.timeDisplay,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                TextButton(onClick = viewModel::onShowTimePicker) { Text("Pick") }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.timeError != null,
                            supportingText = uiState.timeError?.let { { Text(it) } }
                        )
                    }
                }
            }

            // Duration
            FormField("Duration *") {
                DropdownField(
                    value = "${uiState.durationMinutes} min",
                    options = DURATION_OPTIONS.map { "$it min" },
                    onSelected = { selected ->
                        val mins = selected.replace(" min", "").toIntOrNull() ?: 30
                        viewModel.onDurationSelected(mins)
                    },
                    placeholder = "Select duration"
                )
            }

            // Notes
            SectionTitle("Notes")
            FormField("Notes (optional)") {
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("Any special instructions...") }
                )
            }

            if (uiState.saveError != null) {
                Text(uiState.saveError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::onSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Update Appointment")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.dateMillis
        )
        DatePickerDialog(
            onDismissRequest = viewModel::onDismissDatePicker,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateSelected(it) }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDatePicker) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (uiState.showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.timeHour,
            initialMinute = uiState.timeMinute
        )
        AlertDialog(
            onDismissRequest = viewModel::onDismissTimePicker,
            title = { Text("Select Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTimeSelected(timePickerState.hour, timePickerState.minute)
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissTimePicker) { Text("Cancel") }
            }
        )
    }
}
