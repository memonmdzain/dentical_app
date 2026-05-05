package com.dentical.staff.data.remote

import android.util.Log
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.dentical.staff.data.repository.AppointmentRepository
import com.dentical.staff.data.repository.PatientRepository
import com.dentical.staff.data.repository.RoleRepository
import com.dentical.staff.data.repository.TreatmentRepository
import com.dentical.staff.data.repository.UserRepository
import com.dentical.staff.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val patientRepository: PatientRepository,
    private val appointmentRepository: AppointmentRepository,
    private val treatmentRepository: TreatmentRepository,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _canSync = MutableStateFlow(true)
    val canSync: StateFlow<Boolean> = _canSync.asStateFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_START) forceSync()
            }
        )
    }

    private fun forceSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        _canSync.value = false
        scope.launch {
            try {
                roleRepository.pullFromSupabase()
                userRepository.pullFromSupabase()
                patientRepository.pullFromSupabase()
                appointmentRepository.pullFromSupabase()
                treatmentRepository.pullAll()
            } catch (e: Exception) {
                Log.e("SyncManager", "Full sync failed", e)
            } finally {
                _isSyncing.value = false
            }
            delay(30_000L)
            _canSync.value = true
        }
    }

    fun syncAll() {
        if (!_canSync.value) return
        forceSync()
    }
}
