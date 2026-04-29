package com.dentical.staff.data.repository

import com.dentical.staff.data.local.dao.PatientDao
import com.dentical.staff.data.local.entities.PatientEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepository @Inject constructor(
    private val patientDao: PatientDao
) {
    fun getAllPatients(): Flow<List<PatientEntity>> = patientDao.getAllPatients()

    fun searchPatients(query: String): Flow<List<PatientEntity>> =
        patientDao.searchPatients(query)

    suspend fun getPatientById(id: Long): PatientEntity? = patientDao.getPatientById(id)

    suspend fun addPatient(patient: PatientEntity): Long {
        val maxCode = patientDao.getMaxPatientCode() ?: 10000
        val nextCode = maxCode + 1
        val patientWithCode = patient.copy(patientCode = nextCode.toString())
        return patientDao.insertPatient(patientWithCode)
    }

    suspend fun updatePatient(patient: PatientEntity) {
        patientDao.updatePatient(patient.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deletePatient(patient: PatientEntity) = patientDao.deletePatient(patient)
}
