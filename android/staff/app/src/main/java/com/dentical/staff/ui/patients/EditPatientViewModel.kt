package com.dentical.staff.ui.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentical.staff.data.local.entities.PatientEntity
import com.dentical.staff.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EditPatientViewModel @Inject constructor(
    private val repository: PatientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPatientUiState())
    val uiState: StateFlow<AddPatientUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var originalPatient: PatientEntity? = null
    private var loadedId = -1L

    fun load(patientId: Long) {
        if (loadedId == patientId) return
        loadedId = patientId
        viewModelScope.launch {
            val patient = repository.getPatientById(patientId) ?: return@launch
            originalPatient = patient
            val cal = Calendar.getInstance().apply { timeInMillis = patient.dateOfBirth }
            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - cal.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < cal.get(Calendar.DAY_OF_YEAR)) age--
            val isMinor = age < 18
            _uiState.update {
                it.copy(
                    fullName = patient.fullName,
                    dateOfBirth = patient.dateOfBirth,
                    dateOfBirthDisplay = dateFormatter.format(Date(patient.dateOfBirth)),
                    isMinor = isMinor,
                    gender = patient.gender,
                    phone = patient.phone ?: "",
                    isPhoneAvailable = patient.isPhoneAvailable,
                    guardianName = patient.guardianName ?: "",
                    guardianPhone = patient.guardianPhone ?: "",
                    email = patient.email ?: "",
                    address = patient.address ?: "",
                    referralSource = patient.referralSource,
                    referralDetail = patient.referralDetail ?: "",
                    medicalConditions = patient.medicalConditions ?: "",
                    allergies = patient.allergies ?: ""
                )
            }
        }
    }

    fun onFullNameChange(v: String) = _uiState.update { it.copy(fullName = v, fullNameError = null) }
    fun onGenderChange(v: String) = _uiState.update { it.copy(gender = v, genderError = null) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v, phoneError = null) }
    fun onGuardianNameChange(v: String) = _uiState.update { it.copy(guardianName = v, guardianNameError = null) }
    fun onGuardianPhoneChange(v: String) = _uiState.update { it.copy(guardianPhone = v, guardianPhoneError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v) }
    fun onAddressChange(v: String) = _uiState.update { it.copy(address = v) }
    fun onReferralDetailChange(v: String) = _uiState.update { it.copy(referralDetail = v, referralDetailError = null) }
    fun onMedicalConditionsChange(v: String) = _uiState.update { it.copy(medicalConditions = v) }
    fun onAllergiesChange(v: String) = _uiState.update { it.copy(allergies = v) }
    fun onShowDatePicker() = _uiState.update { it.copy(showDatePicker = true) }
    fun onDismissDatePicker() = _uiState.update { it.copy(showDatePicker = false) }

    fun onPhoneAvailableChange(available: Boolean) {
        _uiState.update {
            it.copy(isPhoneAvailable = available, phone = if (!available) "" else it.phone, phoneError = null)
        }
    }

    fun onReferralSourceChange(v: String) {
        _uiState.update {
            it.copy(referralSource = v, referralDetail = "", referralSourceError = null, referralDetailError = null)
        }
    }

    fun onDateSelected(millis: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - cal.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < cal.get(Calendar.DAY_OF_YEAR)) age--
        val isMinor = age < 18
        _uiState.update {
            it.copy(
                dateOfBirth = millis,
                dateOfBirthDisplay = dateFormatter.format(Date(millis)),
                isMinor = isMinor,
                showDatePicker = false,
                dobError = null,
                guardianName = if (!isMinor) "" else it.guardianName,
                guardianPhone = if (!isMinor) "" else it.guardianPhone
            )
        }
    }

    fun onSave() {
        val state = _uiState.value
        val original = originalPatient ?: return
        var hasError = false

        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(fullNameError = "Full name is required") }
            hasError = true
        }
        if (state.dateOfBirth == null) {
            _uiState.update { it.copy(dobError = "Date of birth is required") }
            hasError = true
        }
        if (state.gender.isBlank()) {
            _uiState.update { it.copy(genderError = "Gender is required") }
            hasError = true
        }
        if (state.isPhoneAvailable && state.phone.isBlank() && !state.isMinor) {
            _uiState.update { it.copy(phoneError = "Phone number is required") }
            hasError = true
        }
        if (state.isMinor) {
            if (state.guardianName.isBlank()) {
                _uiState.update { it.copy(guardianNameError = "Guardian name is required") }
                hasError = true
            }
            if (state.guardianPhone.isBlank()) {
                _uiState.update { it.copy(guardianPhoneError = "Guardian phone is required") }
                hasError = true
            }
        }
        if (state.referralSource.isBlank()) {
            _uiState.update { it.copy(referralSourceError = "Referral source is required") }
            hasError = true
        }
        if (state.referralSource.isNotEmpty() && state.referralSource != "Walk-in" && state.referralDetail.isBlank()) {
            _uiState.update { it.copy(referralDetailError = "${state.referralDetailLabel} is required") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                repository.updatePatient(
                    original.copy(
                        fullName = state.fullName.trim(),
                        dateOfBirth = state.dateOfBirth!!,
                        gender = state.gender,
                        phone = if (state.isPhoneAvailable && state.phone.isNotBlank()) state.phone else null,
                        isPhoneAvailable = state.isPhoneAvailable,
                        guardianName = state.guardianName.ifBlank { null },
                        guardianPhone = state.guardianPhone.ifBlank { null },
                        referralSource = state.referralSource,
                        referralDetail = state.referralDetail.ifBlank { null },
                        email = state.email.ifBlank { null },
                        address = state.address.ifBlank { null },
                        medicalConditions = state.medicalConditions.ifBlank { null },
                        allergies = state.allergies.ifBlank { null }
                    )
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = "Failed to update patient. Please try again.") }
            }
        }
    }
}
