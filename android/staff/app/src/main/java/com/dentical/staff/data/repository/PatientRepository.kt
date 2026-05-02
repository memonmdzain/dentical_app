package com.dentical.staff.data.repository

import android.util.Log
import com.dentical.staff.data.local.dao.PatientDao
import com.dentical.staff.data.local.entities.PatientEntity
import com.dentical.staff.data.remote.PatientDto
import com.dentical.staff.data.remote.SupabaseSyncHelper
import com.dentical.staff.data.remote.toDto
import com.dentical.staff.data.remote.toEntity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepository @Inject constructor(
    private val patientDao: PatientDao,
    private val sync: SupabaseSyncHelper
) {
    fun getAllPatients(): Flow<List<PatientEntity>> = patientDao.getAllPatients()

    fun searchPatients(query: String): Flow<List<PatientEntity>> =
        patientDao.searchPatients(query)

    suspend fun getPatientById(id: Long): PatientEntity? = patientDao.getPatientById(id)

    suspend fun addPatient(patient: PatientEntity): Long {
        val maxCode = patientDao.getMaxPatientCode() ?: 10000
        val nextCode = maxCode + 1
        val patientWithCode = patient.copy(patientCode = nextCode.toString())
        val id = patientDao.insertPatient(patientWithCode)
        sync.fireAndForget {
            sync.supabase.from("patients").upsert(patientWithCode.copy(id = id).toDto())
        }
        return id
    }

    suspend fun updatePatient(patient: PatientEntity) {
        val updated = patient.copy(updatedAt = System.currentTimeMillis())
        patientDao.updatePatient(updated)
        sync.fireAndForget { sync.supabase.from("patients").upsert(updated.toDto()) }
    }

    suspend fun deletePatient(patient: PatientEntity) {
        patientDao.deletePatient(patient)
        sync.delete("patients", patient.id)
    }

    suspend fun pullFromSupabase() {
        if (!sync.isConnected) return
        try {
            val dtos = sync.supabase.from("patients").select().decodeList<PatientDto>()
            patientDao.upsertAll(dtos.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Pull patients failed", e)
        }
    }
}
