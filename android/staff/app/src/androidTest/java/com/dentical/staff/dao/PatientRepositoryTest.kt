package com.dentical.staff.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dentical.staff.data.local.DenticalDatabase
import com.dentical.staff.data.local.dao.PatientDao
import com.dentical.staff.data.local.entities.PatientEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Module 5 — Patient Repository logic tested via PatientDao (in-memory Room).
 * Mirrors PatientRepository.addPatient() patientCode logic directly in the DAO layer.
 */
@RunWith(AndroidJUnit4::class)
class PatientRepositoryTest {

    private lateinit var db: DenticalDatabase
    private lateinit var patientDao: PatientDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DenticalDatabase::class.java
        ).allowMainThreadQueries().build()
        patientDao = db.patientDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun basePatient(
        name: String = "Test Patient",
        phone: String? = "9999999999",
        dob: Long = System.currentTimeMillis() - 30L * 365 * 24 * 60 * 60 * 1000,
        guardianName: String? = null,
        guardianPhone: String? = null,
        referralSource: String = "Walk-in",
        referralDetail: String? = null
    ) = PatientEntity(
        fullName = name,
        dateOfBirth = dob,
        gender = "Female",
        phone = phone,
        referralSource = referralSource,
        referralDetail = referralDetail,
        guardianName = guardianName,
        guardianPhone = guardianPhone
    )

    /** Replicates PatientRepository.addPatient() code-assignment logic */
    private suspend fun addPatient(patient: PatientEntity): Long {
        val maxCode = patientDao.getMaxPatientCode() ?: 10000
        val nextCode = maxCode + 1
        return patientDao.insertPatient(patient.copy(patientCode = nextCode.toString()))
    }

    // ── Patient Code Auto-Increment ───────────────────────────────────────────

    @Test
    fun `when the first patient is added to an empty database then their patient code is 10001`() = runTest {
        val id = addPatient(basePatient("First"))
        val patient = patientDao.getPatientById(id)!!
        assertEquals("10001", patient.patientCode)
    }

    @Test
    fun `when multiple patients are added sequentially then each gets a code one higher than the previous`() = runTest {
        val id1 = addPatient(basePatient("Patient A"))
        val id2 = addPatient(basePatient("Patient B"))
        val id3 = addPatient(basePatient("Patient C"))

        val code1 = patientDao.getPatientById(id1)!!.patientCode.toInt()
        val code2 = patientDao.getPatientById(id2)!!.patientCode.toInt()
        val code3 = patientDao.getPatientById(id3)!!.patientCode.toInt()

        assertEquals(code1 + 1, code2)
        assertEquals(code2 + 1, code3)
    }

    @Test
    fun `when a patient is deleted from the database directly then the next patient does not reuse the deleted code`() = runTest {
        val id1 = addPatient(basePatient("Patient A"))
        addPatient(basePatient("Patient B")) // code 10002
        val p1 = patientDao.getPatientById(id1)!!
        patientDao.deletePatient(p1) // delete 10001

        val id3 = addPatient(basePatient("Patient C"))
        val code3 = patientDao.getPatientById(id3)!!.patientCode.toInt()

        // Next code is MAX+1 = 10002+1 = 10003, not 10001
        assertEquals(10003, code3)
    }

    // ── Add Patient ───────────────────────────────────────────────────────────

    @Test
    fun `when a patient is added with all required fields then they are retrievable from the repository`() = runTest {
        val id = addPatient(basePatient("Full Fields Patient"))
        val patient = patientDao.getPatientById(id)
        assertNotNull(patient)
        assertEquals("Full Fields Patient", patient!!.fullName)
        assertEquals("9999999999", patient.phone)
    }

    @Test
    fun `when a patient is added without a phone number then the record is saved with phone as null`() = runTest {
        val id = addPatient(basePatient("No Phone", phone = null))
        val patient = patientDao.getPatientById(id)!!
        assertNull(patient.phone)
    }

    @Test
    fun `when a patient is under 18 based on their date of birth then guardian fields are stored alongside the record`() = runTest {
        val minorDob = System.currentTimeMillis() - 10L * 365 * 24 * 60 * 60 * 1000 // 10 years old
        val id = addPatient(
            basePatient(
                name = "Minor Patient",
                dob = minorDob,
                guardianName = "Parent Name",
                guardianPhone = "8888888888"
            )
        )
        val patient = patientDao.getPatientById(id)!!
        assertEquals("Parent Name", patient.guardianName)
        assertEquals("8888888888", patient.guardianPhone)
    }

    @Test
    fun `when a patient is added with a referral source then the referral detail is stored correctly`() = runTest {
        val id = addPatient(
            basePatient(
                name = "Referred Patient",
                referralSource = "Referral from Doctor",
                referralDetail = "Dr. Sharma"
            )
        )
        val patient = patientDao.getPatientById(id)!!
        assertEquals("Referral from Doctor", patient.referralSource)
        assertEquals("Dr. Sharma", patient.referralDetail)
    }

    // ── Update Patient ────────────────────────────────────────────────────────

    @Test
    fun `when a patient's details are updated then the latest values are returned on next retrieval`() = runTest {
        val id = addPatient(basePatient("Original Name"))
        val original = patientDao.getPatientById(id)!!
        patientDao.updatePatient(original.copy(fullName = "Updated Name", address = "123 New Street"))

        val updated = patientDao.getPatientById(id)!!
        assertEquals("Updated Name", updated.fullName)
        assertEquals("123 New Street", updated.address)
    }

    @Test
    fun `when a patient's phone number is removed on update then it is stored as null`() = runTest {
        val id = addPatient(basePatient("Has Phone", phone = "7777777777"))
        val original = patientDao.getPatientById(id)!!
        patientDao.updatePatient(original.copy(phone = null))

        val updated = patientDao.getPatientById(id)!!
        assertNull(updated.phone)
    }

    // ── Search Patient ────────────────────────────────────────────────────────

    @Test
    fun `when searching by partial name then all patients whose name contains that string are returned`() = runTest {
        addPatient(basePatient("Anjali Mehta"))
        addPatient(basePatient("Ananya Singh"))
        addPatient(basePatient("Rohit Kumar"))

        val results = patientDao.searchPatients("An").first()
        assertEquals(2, results.size)
        assertTrue(results.all { it.fullName.contains("An", ignoreCase = true) })
    }

    @Test
    fun `when searching by phone number then the matching patient is returned`() = runTest {
        addPatient(basePatient("Phone Match", phone = "9876543210"))
        addPatient(basePatient("Other Patient", phone = "1234567890"))

        val results = patientDao.searchPatients("9876543210").first()
        assertEquals(1, results.size)
        assertEquals("Phone Match", results.first().fullName)
    }

    @Test
    fun `when searching by patient code then the exact matching patient is returned`() = runTest {
        val id = addPatient(basePatient("Code Search Patient"))
        val patient = patientDao.getPatientById(id)!!
        val code = patient.patientCode

        val results = patientDao.searchPatients(code).first()
        assertEquals(1, results.size)
        assertEquals(code, results.first().patientCode)
    }

    @Test
    fun `when the search term matches no patient then an empty result is returned`() = runTest {
        addPatient(basePatient("Someone"))
        addPatient(basePatient("Somebody Else"))

        val results = patientDao.searchPatients("ZZZNOMATCH").first()
        assertTrue(results.isEmpty())
    }
}
