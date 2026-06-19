package com.dentical.staff.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dentical.staff.data.local.DenticalDatabase
import com.dentical.staff.data.local.dao.*
import com.dentical.staff.data.local.entities.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: DenticalDatabase
    private lateinit var patientDao: PatientDao
    private lateinit var appointmentDao: AppointmentDao
    private lateinit var treatmentDao: TreatmentDao
    private lateinit var visitDao: VisitDao
    private lateinit var userDao: UserDao
    private lateinit var roleDao: RoleDao
    private lateinit var crossRefDao: TreatmentVisitCrossRefDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DenticalDatabase::class.java
        ).allowMainThreadQueries().build()

        patientDao = db.patientDao()
        appointmentDao = db.appointmentDao()
        treatmentDao = db.treatmentDao()
        visitDao = db.visitDao()
        userDao = db.userDao()
        roleDao = db.roleDao()
        crossRefDao = db.treatmentVisitCrossRefDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makePatient(name: String = "Test Patient"): PatientEntity =
        PatientEntity(
            fullName = name,
            dateOfBirth = System.currentTimeMillis() - 30L * 365 * 24 * 60 * 60 * 1000,
            gender = "Male",
            referralSource = "Walk-in"
        )

    private suspend fun insertPatient(name: String = "Test Patient"): Long {
        val maxCode = patientDao.getMaxPatientCode() ?: 10000
        val patient = makePatient(name).copy(patientCode = (maxCode + 1).toString())
        return patientDao.insertPatient(patient)
    }

    private fun makeUser(username: String = "user1"): UserEntity =
        UserEntity(username = username, passwordHash = "hash", fullName = "Test User")

    // ── Patient DAO ───────────────────────────────────────────────────────────

    @Test
    fun `when a patient is inserted then they can be retrieved by their ID`() = runTest {
        val id = insertPatient("Jane Doe")
        val retrieved = patientDao.getPatientById(id)
        assertNotNull(retrieved)
        assertEquals("Jane Doe", retrieved!!.fullName)
    }

    @Test
    fun `when the first patient is added to an empty database then their patient code is 10001`() = runTest {
        val maxCode = patientDao.getMaxPatientCode() ?: 10000
        val nextCode = maxCode + 1
        val patient = makePatient().copy(patientCode = nextCode.toString())
        patientDao.insertPatient(patient)
        val all = patientDao.getAllPatients().first()
        assertEquals("10001", all.first().patientCode)
    }

    @Test
    fun `when multiple patients are added sequentially then each gets a code one higher than the previous`() = runTest {
        val id1 = insertPatient("Patient A")
        val id2 = insertPatient("Patient B")
        val id3 = insertPatient("Patient C")

        val p1 = patientDao.getPatientById(id1)!!
        val p2 = patientDao.getPatientById(id2)!!
        val p3 = patientDao.getPatientById(id3)!!

        val code1 = p1.patientCode.toInt()
        val code2 = p2.patientCode.toInt()
        val code3 = p3.patientCode.toInt()

        assertEquals(code1 + 1, code2)
        assertEquals(code2 + 1, code3)
    }

    @Test
    fun `when searching by name then only patients whose name contains the search term are returned`() = runTest {
        insertPatient("Alice Smith")
        insertPatient("Bob Jones")

        val results = patientDao.searchPatients("Alice").first()
        assertEquals(1, results.size)
        assertEquals("Alice Smith", results.first().fullName)
    }

    @Test
    fun `when searching by phone then the matching patient is returned`() = runTest {
        val maxCode = patientDao.getMaxPatientCode() ?: 10000
        val patient = makePatient("Phone Patient").copy(
            patientCode = (maxCode + 1).toString(),
            phone = "9876543210"
        )
        patientDao.insertPatient(patient)

        val results = patientDao.searchPatients("9876543210").first()
        assertEquals(1, results.size)
        assertEquals("Phone Patient", results.first().fullName)
    }

    // ── Appointment DAO ───────────────────────────────────────────────────────

    @Test
    fun `when appointments are fetched for a specific date then only appointments on that date are returned`() = runTest {
        val patientId = insertPatient()

        val todayMidnight = System.currentTimeMillis() / 86_400_000 * 86_400_000
        val todayNoon = todayMidnight + 12 * 3600 * 1000L

        appointmentDao.insertAppointment(
            AppointmentEntity(patientId = patientId, scheduledAt = todayNoon)
        )

        val startOfDay = todayMidnight
        val endOfDay = todayMidnight + 86_400_000L
        val results = appointmentDao.getAppointmentsByDay(startOfDay, endOfDay).first()
        assertEquals(1, results.size)
    }

    @Test
    fun `when appointments from a different date exist then they are not included in a day query`() = runTest {
        val patientId = insertPatient()

        val todayMidnight = System.currentTimeMillis() / 86_400_000 * 86_400_000
        val yesterdayNoon = todayMidnight - 12 * 3600 * 1000L

        appointmentDao.insertAppointment(
            AppointmentEntity(patientId = patientId, scheduledAt = yesterdayNoon)
        )

        val startOfToday = todayMidnight
        val endOfToday = todayMidnight + 86_400_000L
        val results = appointmentDao.getAppointmentsByDay(startOfToday, endOfToday).first()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `when an appointment status is updated then the new status is reflected on retrieval`() = runTest {
        val patientId = insertPatient()
        val apptId = appointmentDao.insertAppointment(
            AppointmentEntity(patientId = patientId, scheduledAt = System.currentTimeMillis())
        )

        val appt = appointmentDao.getAppointmentById(apptId)!!
        appointmentDao.updateAppointment(appt.copy(status = AppointmentStatus.COMPLETED))

        val updated = appointmentDao.getAppointmentById(apptId)!!
        assertEquals(AppointmentStatus.COMPLETED, updated.status)
    }

    @Test
    fun `when a patient has multiple appointments then all of them are returned when querying by patient ID`() = runTest {
        val patientId = insertPatient()
        val now = System.currentTimeMillis()
        appointmentDao.insertAppointment(AppointmentEntity(patientId = patientId, scheduledAt = now))
        appointmentDao.insertAppointment(AppointmentEntity(patientId = patientId, scheduledAt = now + 3600_000))
        appointmentDao.insertAppointment(AppointmentEntity(patientId = patientId, scheduledAt = now + 7200_000))

        val results = appointmentDao.getAppointmentsByPatient(patientId).first()
        assertEquals(3, results.size)
    }

    // ── Treatment DAO ─────────────────────────────────────────────────────────

    @Test
    fun `when a treatment is inserted for a patient then it appears in that patient's treatment list`() = runTest {
        val patientId = insertPatient()
        treatmentDao.insertTreatment(
            TreatmentEntity(patientId = patientId, quotedCost = 1000.0)
        )

        val results = treatmentDao.getTreatmentsByPatient(patientId).first()
        assertEquals(1, results.size)
    }

    @Test
    fun `when a treatment status is changed to completed then it moves out of the ongoing treatments list`() = runTest {
        val patientId = insertPatient()
        val id = treatmentDao.insertTreatment(
            TreatmentEntity(patientId = patientId, status = TreatmentStatus.ONGOING)
        )

        val treatment = treatmentDao.getTreatmentById(id)!!
        treatmentDao.updateTreatment(treatment.copy(status = TreatmentStatus.COMPLETED))

        val ongoing = treatmentDao.getOngoingTreatmentsByPatient(patientId).first()
        assertTrue(ongoing.isEmpty())
    }

    @Test
    fun `when outstanding balance is queried then it equals total quoted cost minus total amount paid`() = runTest {
        val patientId = insertPatient()
        val treatmentId = treatmentDao.insertTreatment(
            TreatmentEntity(patientId = patientId, quotedCost = 5000.0)
        )

        val visitId = visitDao.insertVisit(
            VisitEntity(patientId = patientId, visitDate = System.currentTimeMillis(),
                performedBy = "Dr. Smith", amountPaid = 2000.0)
        )
        crossRefDao.insert(
            TreatmentVisitCrossRef(treatmentId = treatmentId, visitId = visitId,
                workDone = "Initial", allocatedAmount = 2000.0)
        )

        val totalQuoted = treatmentDao.getTotalQuotedCostOnce(patientId)
        val totalPaid = visitDao.getTotalAmountPaidOnce(patientId)
        val outstanding = totalQuoted - totalPaid

        assertEquals(3000.0, outstanding, 0.01)
    }

    // ── Visit DAO ─────────────────────────────────────────────────────────────

    @Test
    fun `when a visit is added with an amount paid then the live sum of amount paid increases accordingly`() = runTest {
        val patientId = insertPatient()

        val before = visitDao.getTotalAmountPaid(patientId).first()
        assertEquals(0.0, before, 0.01)

        visitDao.insertVisit(
            VisitEntity(patientId = patientId, visitDate = System.currentTimeMillis(),
                performedBy = "Dr. X", amountPaid = 1500.0)
        )

        val after = visitDao.getTotalAmountPaid(patientId).first()
        assertEquals(1500.0, after, 0.01)
    }

    @Test
    fun `when a visit is deleted then the live outstanding query returns the corrected higher value`() = runTest {
        val patientId = insertPatient()
        val treatmentId = treatmentDao.insertTreatment(
            TreatmentEntity(patientId = patientId, quotedCost = 3000.0)
        )
        val visitId = visitDao.insertVisit(
            VisitEntity(patientId = patientId, visitDate = System.currentTimeMillis(),
                performedBy = "Dr. X", amountPaid = 1000.0)
        )
        crossRefDao.insert(
            TreatmentVisitCrossRef(treatmentId = treatmentId, visitId = visitId,
                workDone = "Work", allocatedAmount = 1000.0)
        )

        val visit = visitDao.getVisitById(visitId)!!
        visitDao.deleteVisit(visit)

        val totalPaid = visitDao.getTotalAmountPaidOnce(patientId)
        val totalQuoted = treatmentDao.getTotalQuotedCostOnce(patientId)
        assertEquals(3000.0, totalQuoted - totalPaid, 0.01)
    }

    @Test
    fun `when multiple visits are added for the same patient then the total collected reflects all of them`() = runTest {
        val patientId = insertPatient()
        val now = System.currentTimeMillis()

        visitDao.insertVisit(VisitEntity(patientId = patientId, visitDate = now, performedBy = "Dr. A", amountPaid = 500.0))
        visitDao.insertVisit(VisitEntity(patientId = patientId, visitDate = now + 1000, performedBy = "Dr. A", amountPaid = 700.0))
        visitDao.insertVisit(VisitEntity(patientId = patientId, visitDate = now + 2000, performedBy = "Dr. A", amountPaid = 300.0))

        val total = visitDao.getTotalAmountPaid(patientId).first()
        assertEquals(1500.0, total, 0.01)
    }

    // ── User DAO ──────────────────────────────────────────────────────────────

    @Test
    fun `when a user is inserted then they can be retrieved by username`() = runTest {
        userDao.insertUser(makeUser("drsmith"))
        val retrieved = userDao.getUserByUsername("drsmith")
        assertNotNull(retrieved)
        assertEquals("drsmith", retrieved!!.username)
    }

    @Test
    fun `when a user is marked inactive then they do not appear in the active users list`() = runTest {
        val id = userDao.insertUser(makeUser("inactiveuser"))
        userDao.deactivateUser(id)

        val activeUsers = userDao.getAllActiveUsers().first()
        assertTrue(activeUsers.none { it.id == id })
    }

    @Test
    fun `when a user's roles are queried then all assigned roles and their permissions are returned together`() = runTest {
        val userId = userDao.insertUser(makeUser("staffuser"))
        val roleId = roleDao.insertRole(RoleEntity(name = "STAFF"))
        roleDao.upsertPermissions(listOf(
            PermissionEntity(roleId = roleId, resource = "patients",
                canCreate = true, canRead = true, canUpdate = false, canDelete = false)
        ))
        roleDao.insertUserRoleCrossRef(UserRoleCrossRef(userId = userId, roleId = roleId))

        val roles = roleDao.getRolesForUser(userId).first()
        assertEquals(1, roles.size)
        assertEquals("STAFF", roles.first().name)

        val permissions = roleDao.getPermissionsForRoleOnce(roleId)
        assertEquals(1, permissions.size)
        assertTrue(permissions.first().canRead)
    }

    // ── Role DAO ──────────────────────────────────────────────────────────────

    @Test
    fun `when a system role is queried then it is returned with isSystem true`() = runTest {
        val roleId = roleDao.insertRole(RoleEntity(name = "ADMIN", isSystem = true))
        val role = roleDao.getRoleById(roleId)
        assertNotNull(role)
        assertTrue(role!!.isSystem)
    }

    @Test
    fun `when a custom role is deleted then it no longer appears and the user-role mapping is also removed`() = runTest {
        val userId = userDao.insertUser(makeUser("customuser"))
        val roleId = roleDao.insertRole(RoleEntity(name = "CUSTOM_ROLE", isSystem = false))
        roleDao.insertUserRoleCrossRef(UserRoleCrossRef(userId = userId, roleId = roleId))

        val role = roleDao.getRoleById(roleId)!!
        roleDao.deleteRole(role)

        val allRoles = roleDao.getAllRoles().first()
        assertTrue(allRoles.none { it.id == roleId })

        val userRoles = roleDao.getRolesForUser(userId).first()
        assertTrue(userRoles.none { it.id == roleId })
    }

    @Test
    fun `when permissions are inserted for a role then they are returned when that role is fetched`() = runTest {
        val roleId = roleDao.insertRole(RoleEntity(name = "DENTIST"))
        roleDao.upsertPermissions(listOf(
            PermissionEntity(roleId = roleId, resource = "treatments",
                canCreate = true, canRead = true, canUpdate = true, canDelete = false),
            PermissionEntity(roleId = roleId, resource = "appointments",
                canCreate = true, canRead = true, canUpdate = true, canDelete = false)
        ))

        val permissions = roleDao.getPermissionsForRoleOnce(roleId)
        assertEquals(2, permissions.size)
        val resources = permissions.map { it.resource }.toSet()
        assertTrue(resources.contains("treatments"))
        assertTrue(resources.contains("appointments"))
    }
}
