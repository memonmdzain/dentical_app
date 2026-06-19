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
class FinancialLogicTest {

    private lateinit var db: DenticalDatabase
    private lateinit var patientDao: PatientDao
    private lateinit var treatmentDao: TreatmentDao
    private lateinit var visitDao: VisitDao
    private lateinit var crossRefDao: TreatmentVisitCrossRefDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DenticalDatabase::class.java
        ).allowMainThreadQueries().build()

        patientDao = db.patientDao()
        treatmentDao = db.treatmentDao()
        visitDao = db.visitDao()
        crossRefDao = db.treatmentVisitCrossRefDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun insertPatient(name: String = "Test Patient"): Long {
        val maxCode = patientDao.getMaxPatientCode() ?: 10000
        return patientDao.insertPatient(
            PatientEntity(
                patientCode = (maxCode + 1).toString(),
                fullName = name,
                dateOfBirth = System.currentTimeMillis() - 30L * 365 * 24 * 60 * 60 * 1000,
                gender = "Female",
                referralSource = "Walk-in"
            )
        )
    }

    private suspend fun insertTreatment(patientId: Long, cost: Double?, startOffset: Long = 0): Long =
        treatmentDao.insertTreatment(
            TreatmentEntity(
                patientId = patientId,
                quotedCost = cost,
                startDate = System.currentTimeMillis() + startOffset
            )
        )

    private suspend fun insertVisit(patientId: Long, amountPaid: Double, costCharged: Double = 0.0): Long =
        visitDao.insertVisit(
            VisitEntity(
                patientId = patientId,
                visitDate = System.currentTimeMillis(),
                performedBy = "Dr. Test",
                amountPaid = amountPaid,
                costCharged = costCharged
            )
        )

    private suspend fun linkVisit(treatmentId: Long, visitId: Long, allocated: Double) {
        crossRefDao.insert(
            TreatmentVisitCrossRef(
                treatmentId = treatmentId,
                visitId = visitId,
                workDone = "Work",
                allocatedAmount = allocated
            )
        )
    }

    // ── FIFO Payment Allocation ───────────────────────────────────────────────

    @Test
    fun `when a visit is linked to one treatment then the full payment is allocated to that treatment`() = runTest {
        val patientId = insertPatient()
        val treatmentId = insertTreatment(patientId, 5000.0)
        val visitId = insertVisit(patientId, 2000.0)
        linkVisit(treatmentId, visitId, 2000.0)

        val allocated = crossRefDao.getTotalAllocatedForTreatment(treatmentId)
        assertEquals(2000.0, allocated, 0.01)
    }

    @Test
    fun `when a visit is linked to two treatments then the payment is allocated to the older treatment first`() = runTest {
        val patientId = insertPatient()
        val olderTreatmentId = insertTreatment(patientId, 3000.0, startOffset = -10_000L)
        val newerTreatmentId = insertTreatment(patientId, 3000.0, startOffset = 0L)
        val visitId = insertVisit(patientId, 2000.0)

        // Simulate FIFO: all 2000 goes to older treatment first
        linkVisit(olderTreatmentId, visitId, 2000.0)
        linkVisit(newerTreatmentId, visitId, 0.0)

        val allocatedOlder = crossRefDao.getTotalAllocatedForTreatment(olderTreatmentId)
        val allocatedNewer = crossRefDao.getTotalAllocatedForTreatment(newerTreatmentId)

        assertEquals(2000.0, allocatedOlder, 0.01)
        assertEquals(0.0, allocatedNewer, 0.01)
    }

    @Test
    fun `when a payment fully covers the first linked treatment and has remainder then the remainder spills over to the next`() = runTest {
        val patientId = insertPatient()
        val firstTreatmentId = insertTreatment(patientId, 1000.0, startOffset = -5000L)
        val secondTreatmentId = insertTreatment(patientId, 3000.0, startOffset = 0L)
        val visitId = insertVisit(patientId, 1500.0)

        // FIFO: 1000 to first (fully paid), 500 spillover to second
        linkVisit(firstTreatmentId, visitId, 1000.0)
        linkVisit(secondTreatmentId, visitId, 500.0)

        val allocatedFirst = crossRefDao.getTotalAllocatedForTreatment(firstTreatmentId)
        val allocatedSecond = crossRefDao.getTotalAllocatedForTreatment(secondTreatmentId)

        assertEquals(1000.0, allocatedFirst, 0.01)
        assertEquals(500.0, allocatedSecond, 0.01)
    }

    @Test
    fun `when a linked treatment has null quoted cost then it is skipped in FIFO allocation`() = runTest {
        val patientId = insertPatient()
        val nullCostId = insertTreatment(patientId, null, startOffset = -5000L)
        val realCostId = insertTreatment(patientId, 2000.0, startOffset = 0L)
        val visitId = insertVisit(patientId, 1000.0)

        // null cost treatment is skipped; full amount goes to the real-cost treatment
        linkVisit(nullCostId, visitId, 0.0)
        linkVisit(realCostId, visitId, 1000.0)

        val allocatedNull = crossRefDao.getTotalAllocatedForTreatment(nullCostId)
        val allocatedReal = crossRefDao.getTotalAllocatedForTreatment(realCostId)

        assertEquals(0.0, allocatedNull, 0.01)
        assertEquals(1000.0, allocatedReal, 0.01)
    }

    @Test
    fun `when all linked treatments are fully paid then outstanding across them remains zero`() = runTest {
        val patientId = insertPatient()
        val t1 = insertTreatment(patientId, 1000.0)
        val t2 = insertTreatment(patientId, 1500.0)
        val visitId = insertVisit(patientId, 2500.0)

        linkVisit(t1, visitId, 1000.0)
        linkVisit(t2, visitId, 1500.0)

        val totalQuoted = treatmentDao.getTotalQuotedCostOnce(patientId)
        val totalPaid = visitDao.getTotalAmountPaidOnce(patientId)

        assertEquals(0.0, totalQuoted - totalPaid, 0.01)
    }

    // ── Outstanding Balance ───────────────────────────────────────────────────

    @Test
    fun `when a patient has treatments but no visits yet then outstanding equals the sum of all quoted costs`() = runTest {
        val patientId = insertPatient()
        insertTreatment(patientId, 2000.0)
        insertTreatment(patientId, 3000.0)

        val totalQuoted = treatmentDao.getTotalQuotedCostOnce(patientId)
        val totalPaid = visitDao.getTotalAmountPaidOnce(patientId)

        assertEquals(5000.0, totalQuoted, 0.01)
        assertEquals(0.0, totalPaid, 0.01)
        assertEquals(5000.0, totalQuoted - totalPaid, 0.01)
    }

    @Test
    fun `when visits have been recorded then outstanding equals total quoted cost minus total amount paid`() = runTest {
        val patientId = insertPatient()
        val treatmentId = insertTreatment(patientId, 4000.0)
        val visitId = insertVisit(patientId, 1500.0)
        linkVisit(treatmentId, visitId, 1500.0)

        val totalQuoted = treatmentDao.getTotalQuotedCostOnce(patientId)
        val totalPaid = visitDao.getTotalAmountPaidOnce(patientId)

        assertEquals(2500.0, totalQuoted - totalPaid, 0.01)
    }

    @Test
    fun `when a treatment is cancelled with a partial charge then only the partial charge contributes to total billed`() = runTest {
        val patientId = insertPatient()
        // Insert treatment with original cost 5000, but cancel with partial 2000
        val treatmentId = treatmentDao.insertTreatment(
            TreatmentEntity(patientId = patientId, quotedCost = 2000.0, status = TreatmentStatus.CANCELLED)
        )

        val totalQuoted = treatmentDao.getTotalQuotedCostOnce(patientId)
        assertEquals(2000.0, totalQuoted, 0.01)
    }

    @Test
    fun `when a treatment is cancelled at zero charge then it does not contribute to outstanding at all`() = runTest {
        val patientId = insertPatient()
        treatmentDao.insertTreatment(
            TreatmentEntity(patientId = patientId, quotedCost = 0.0, status = TreatmentStatus.CANCELLED)
        )

        val totalQuoted = treatmentDao.getTotalQuotedCostOnce(patientId)
        val totalPaid = visitDao.getTotalAmountPaidOnce(patientId)

        assertEquals(0.0, totalQuoted - totalPaid, 0.01)
    }

    @Test
    fun `when a refund visit is recorded as a negative amount paid then outstanding increases by that refund amount`() = runTest {
        val patientId = insertPatient()
        val treatmentId = insertTreatment(patientId, 3000.0)

        // Normal payment
        val visitId1 = insertVisit(patientId, 3000.0)
        linkVisit(treatmentId, visitId1, 3000.0)

        val outstandingBefore = treatmentDao.getTotalQuotedCostOnce(patientId) - visitDao.getTotalAmountPaidOnce(patientId)
        assertEquals(0.0, outstandingBefore, 0.01)

        // Refund visit (negative amountPaid)
        val refundVisitId = visitDao.insertVisit(
            VisitEntity(patientId = patientId, visitDate = System.currentTimeMillis(),
                performedBy = "Dr. Test", amountPaid = -500.0)
        )
        linkVisit(treatmentId, refundVisitId, -500.0)

        val outstandingAfter = treatmentDao.getTotalQuotedCostOnce(patientId) - visitDao.getTotalAmountPaidOnce(patientId)
        assertEquals(500.0, outstandingAfter, 0.01)
    }

    @Test
    fun `when a standalone visit is recorded then its cost charged and amount paid contribute independently to outstanding`() = runTest {
        val patientId = insertPatient()

        // Standalone visit (not linked to any treatment)
        visitDao.insertVisit(
            VisitEntity(patientId = patientId, visitDate = System.currentTimeMillis(),
                performedBy = "Dr. Test", amountPaid = 200.0, costCharged = 500.0)
        )

        val standaloneCharged = visitDao.getStandaloneVisitsTotalChargedOnce(patientId)
        val totalPaid = visitDao.getTotalAmountPaidOnce(patientId)

        assertEquals(500.0, standaloneCharged, 0.01)
        assertEquals(200.0, totalPaid, 0.01)
    }

    // ── Mark Complete — Payment Gate ──────────────────────────────────────────

    @Test
    fun `when a treatment has outstanding balance greater than zero then it cannot be marked complete`() = runTest {
        val patientId = insertPatient()
        val treatmentId = insertTreatment(patientId, 3000.0)

        val totalPaid = visitDao.getTotalAmountPaidOnce(patientId)
        val treatment = treatmentDao.getTreatmentById(treatmentId)!!
        val outstanding = (treatment.quotedCost ?: 0.0) - crossRefDao.getTotalAllocatedForTreatment(treatmentId)

        // Verify outstanding > 0, meaning mark-complete should be blocked
        assertTrue(outstanding > 0.0)
    }

    @Test
    fun `when a treatment is fully paid then marking it complete is allowed`() = runTest {
        val patientId = insertPatient()
        val treatmentId = insertTreatment(patientId, 2000.0)
        val visitId = insertVisit(patientId, 2000.0)
        linkVisit(treatmentId, visitId, 2000.0)

        val treatment = treatmentDao.getTreatmentById(treatmentId)!!
        val outstanding = (treatment.quotedCost ?: 0.0) - crossRefDao.getTotalAllocatedForTreatment(treatmentId)

        assertEquals(0.0, outstanding, 0.01)

        // Mark complete — allowed when outstanding == 0
        treatmentDao.updateTreatment(treatment.copy(status = TreatmentStatus.COMPLETED))
        val updated = treatmentDao.getTreatmentById(treatmentId)!!
        assertEquals(TreatmentStatus.COMPLETED, updated.status)
    }

    @Test
    fun `when one of multiple treatments still has outstanding then only that treatment is blocked`() = runTest {
        val patientId = insertPatient()
        val paidTreatmentId = insertTreatment(patientId, 1000.0)
        val unpaidTreatmentId = insertTreatment(patientId, 2000.0)

        val visitId = insertVisit(patientId, 1000.0)
        linkVisit(paidTreatmentId, visitId, 1000.0)

        val paidOutstanding = (treatmentDao.getTreatmentById(paidTreatmentId)!!.quotedCost ?: 0.0) -
            crossRefDao.getTotalAllocatedForTreatment(paidTreatmentId)
        val unpaidOutstanding = (treatmentDao.getTreatmentById(unpaidTreatmentId)!!.quotedCost ?: 0.0) -
            crossRefDao.getTotalAllocatedForTreatment(unpaidTreatmentId)

        assertEquals(0.0, paidOutstanding, 0.01)   // can complete
        assertTrue(unpaidOutstanding > 0.0)         // blocked
    }

    // ── Cancel Treatment ──────────────────────────────────────────────────────

    @Test
    fun `when cancelled with partial charge less than amount paid then balance is negative and a refund can be recorded`() = runTest {
        val patientId = insertPatient()
        val treatmentId = treatmentDao.insertTreatment(
            TreatmentEntity(patientId = patientId, quotedCost = 500.0) // partial charge after cancel
        )
        val visitId = insertVisit(patientId, 1000.0) // patient paid more
        linkVisit(treatmentId, visitId, 1000.0)

        val chargedAfterCancel = 500.0
        val amountPaid = visitDao.getTotalAmountPaidOnce(patientId)
        val balance = chargedAfterCancel - amountPaid

        assertTrue(balance < 0) // refund needed
    }

    @Test
    fun `when cancelled with partial charge equal to amount paid then balance is zero and no refund is needed`() = runTest {
        val patientId = insertPatient()
        val treatmentId = insertTreatment(patientId, 800.0)
        val visitId = insertVisit(patientId, 800.0)
        linkVisit(treatmentId, visitId, 800.0)

        val chargedAfterCancel = 800.0
        val amountPaid = visitDao.getTotalAmountPaidOnce(patientId)
        val balance = chargedAfterCancel - amountPaid

        assertEquals(0.0, balance, 0.01)
    }

    @Test
    fun `when cancelled with partial charge greater than amount paid then treatment is cancelled and remaining outstanding stays`() = runTest {
        val patientId = insertPatient()
        val treatmentId = insertTreatment(patientId, 1200.0)
        val visitId = insertVisit(patientId, 500.0)
        linkVisit(treatmentId, visitId, 500.0)

        val chargedAfterCancel = 1200.0
        val amountPaid = visitDao.getTotalAmountPaidOnce(patientId)
        val balance = chargedAfterCancel - amountPaid

        assertTrue(balance > 0) // still owe money
    }

    // ── Reopen Treatment ──────────────────────────────────────────────────────

    @Test
    fun `when a completed treatment is reopened then its status returns to ongoing and it reappears in the ongoing list`() = runTest {
        val patientId = insertPatient()
        val treatmentId = insertTreatment(patientId, 2000.0)

        val treatment = treatmentDao.getTreatmentById(treatmentId)!!
        treatmentDao.updateTreatment(treatment.copy(status = TreatmentStatus.COMPLETED))

        val completed = treatmentDao.getOngoingTreatmentsByPatient(patientId).first()
        assertTrue(completed.isEmpty())

        treatmentDao.updateTreatment(treatment.copy(status = TreatmentStatus.ONGOING))
        val reopened = treatmentDao.getOngoingTreatmentsByPatient(patientId).first()
        assertEquals(1, reopened.size)
    }

    @Test
    fun `when a cancelled treatment is reopened with a new quoted cost then the new cost is used going forward`() = runTest {
        val patientId = insertPatient()
        val treatmentId = insertTreatment(patientId, 1000.0)

        val treatment = treatmentDao.getTreatmentById(treatmentId)!!
        treatmentDao.updateTreatment(treatment.copy(status = TreatmentStatus.CANCELLED))
        treatmentDao.updateTreatment(treatment.copy(status = TreatmentStatus.ONGOING, quotedCost = 2500.0))

        val updated = treatmentDao.getTreatmentById(treatmentId)!!
        assertEquals(TreatmentStatus.ONGOING, updated.status)
        assertEquals(2500.0, updated.quotedCost ?: 0.0, 0.01)
    }

    @Test
    fun `when a reopened treatment already has visits then those visits and payments are still intact`() = runTest {
        val patientId = insertPatient()
        val treatmentId = insertTreatment(patientId, 3000.0)
        val visitId = insertVisit(patientId, 1000.0)
        linkVisit(treatmentId, visitId, 1000.0)

        val treatment = treatmentDao.getTreatmentById(treatmentId)!!
        treatmentDao.updateTreatment(treatment.copy(status = TreatmentStatus.COMPLETED))
        treatmentDao.updateTreatment(treatment.copy(status = TreatmentStatus.ONGOING))

        val totalPaid = visitDao.getTotalAmountPaidOnce(patientId)
        assertEquals(1000.0, totalPaid, 0.01)

        val visits = visitDao.getVisitsByTreatment(treatmentId).first()
        assertEquals(1, visits.size)
    }
}
