package com.dentical.staff.ui.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

val REFERRAL_SOURCES = listOf(
    "Walk-in",
    "Referral from Doctor",
    "Friend / Family",
    "Social Media",
    "Other"
)

val GENDER_OPTIONS = listOf("Male", "Female", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddPatientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Patient") },
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
            SectionTitle("Basic Information")

            FormField("Full Name *") {
                OutlinedTextField(
                    value = uiState.fullName,
                    onValueChange = viewModel::onFullNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.fullNameError != null,
                    supportingText = uiState.fullNameError?.let { { Text(it) } },
                    singleLine = true
                )
            }

            FormField("Date of Birth *") {
                OutlinedTextField(
                    value = uiState.dateOfBirthDisplay,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    placeholder = { Text("DD/MM/YYYY") },
                    trailingIcon = {
                        TextButton(onClick = viewModel::onShowDatePicker) {
                            Text("Select")
                        }
                    },
                    isError = uiState.dobError != null,
                    supportingText = uiState.dobError?.let { { Text(it) } }
                )
                if (uiState.isMinor) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "Minor patient — guardian details required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            FormField("Gender *") {
                DropdownField(
                    value = uiState.gender,
                    options = GENDER_OPTIONS,
                    onSelected = viewModel::onGenderChange,
                    placeholder = "Select gender",
                    isError = uiState.genderError != null,
                    errorText = uiState.genderError
                )
            }

            // Phone section
            SectionTitle("Contact Information")

            FormField("Phone Number ${if (!uiState.isPhoneAvailable) "" else "*"}") {
                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = viewModel::onPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isPhoneAvailable,
                    placeholder = { Text("e.g. 09123456789") },
                    isError = uiState.phoneError != null,
                    supportingText = uiState.phoneError?.let { { Text(it) } },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = !uiState.isPhoneAvailable,
                        onCheckedChange = { viewModel.onPhoneAvailableChange(!it) }
                    )
                    Text("Phone number not available",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Guardian fields — shown for minors
            if (uiState.isMinor) {
                SectionTitle("Parent / Guardian")

                FormField("Guardian Name *") {
                    OutlinedTextField(
                        value = uiState.guardianName,
                        onValueChange = viewModel::onGuardianNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.guardianNameError != null,
                        supportingText = uiState.guardianNameError?.let { { Text(it) } },
                        singleLine = true
                    )
                }

                FormField("Guardian Phone *") {
                    OutlinedTextField(
                        value = uiState.guardianPhone,
                        onValueChange = viewModel::onGuardianPhoneChange,
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.guardianPhoneError != null,
                        supportingText = uiState.guardianPhoneError?.let { { Text(it) } },
                        singleLine = true
                    )
                }
            }

            FormField("Email") {
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            FormField("Address") {
                OutlinedTextField(
                    value = uiState.address,
                    onValueChange = viewModel::onAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }

            // Referral
            SectionTitle("Referral *")

            FormField("How did the patient find us?") {
                DropdownField(
                    value = uiState.referralSource,
                    options = REFERRAL_SOURCES,
                    onSelected = viewModel::onReferralSourceChange,
                    placeholder = "Select referral source",
                    isError = uiState.referralSourceError != null,
                    errorText = uiState.referralSourceError
                )
            }

            if (uiState.referralSource.isNotEmpty() && uiState.referralSource != "Walk-in") {
                FormField(uiState.referralDetailLabel + " *") {
                    OutlinedTextField(
                        value = uiState.referralDetail,
                        onValueChange = viewModel::onReferralDetailChange,
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.referralDetailError != null,
                        supportingText = uiState.referralDetailError?.let { { Text(it) } },
                        singleLine = true
                    )
                }
            }

            // Medical
            SectionTitle("Medical Information")

            FormField("Medical Conditions") {
                OutlinedTextField(
                    value = uiState.medicalConditions,
                    onValueChange = viewModel::onMedicalConditionsChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Diabetes, Hypertension") },
                    minLines = 2,
                    maxLines = 3
                )
            }

            FormField("Allergies") {
                OutlinedTextField(
                    value = uiState.allergies,
                    onValueChange = viewModel::onAllergiesChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Penicillin") },
                    minLines = 2,
                    maxLines = 3
                )
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.saveError != null) {
                Text(uiState.saveError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = viewModel::onSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Patient")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Date Picker Dialog
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState()
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
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
    HorizontalDivider()
}

@Composable
fun FormField(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    placeholder: String,
    isError: Boolean = false,
    errorText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(placeholder) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            isError = isError,
            supportingText = errorText?.let { { Text(it) } }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}
