package com.example

import com.example.data.catalog.GambianFootballCatalog
import com.example.data.catalog.GambianSlotTimeHelper
import com.example.data.model.TurfSlot
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun gambianCoordinates_resolveAndComputeReasonableDistance() {
    val banjul = GambianFootballCatalog.resolveAreaPoint("Banjul")
    val serrekunda = GambianFootballCatalog.resolveAreaPoint("Serrekunda")
    val distKm =
      GambianFootballCatalog.haversineDistanceKm(
        banjul.latitude,
        banjul.longitude,
        serrekunda.latitude,
        serrekunda.longitude,
      )
    assertTrue("Expected Banjul-Serrekunda distance between 8 and 15 km, got $distKm", distKm in 8.0..15.0)
    assertTrue("All turfs should have valid Gambian coordinates", GambianFootballCatalog.turfs.all {
      it.latitude in 13.0..14.0 && it.longitude in -17.0..-16.0
    })
  }

  @Test
  fun slotGeneration_60And90Minutes_producesExpectedGmtWindowsAndSevenDays() {
    val days = GambianSlotTimeHelper.nextSevenDaysGmt()
    assertEquals(7, days.size)
    assertEquals("Today", days.first().primaryLabel)

    val hourSlots = GambianSlotTimeHelper.generateSlotWindows("08:00", "23:00", 60L)
    assertEquals(15, hourSlots.size)
    assertEquals("08:00", hourSlots.first().startTime)
    assertEquals("09:00", hourSlots.first().endTime)
    assertEquals("22:00", hourSlots.last().startTime)
    assertEquals("23:00", hourSlots.last().endTime)

    val ninetyMinSlots = GambianSlotTimeHelper.generateSlotWindows("08:00", "23:00", 90L)
    assertEquals(10, ninetyMinSlots.size)
    assertEquals("08:00", ninetyMinSlots.first().startTime)
    assertEquals("09:30", ninetyMinSlots.first().endTime)
    assertEquals("21:30", ninetyMinSlots.last().startTime)
    assertEquals("23:00", ninetyMinSlots.last().endTime)

    assertEquals("6:00 PM", GambianSlotTimeHelper.formatHhMmTo12Hour("18:00"))
    assertEquals("12:00 AM", GambianSlotTimeHelper.formatHhMmTo12Hour("24:00"))

    val midnightSlots = GambianSlotTimeHelper.generateSlotWindows("16:00", "24:00", 60L)
    assertEquals(8, midnightSlots.size)
    assertEquals("23:00", midnightSlots.last().startTime)
    assertEquals("24:00", midnightSlots.last().endTime)
    assertEquals(2.0, GambianSlotTimeHelper.hoursBetween("18:00", "20:00"), 0.01)
  }

  @Test
  fun turfSlot_effectiveStatus_marksExpiredInProgressAsCompleted() {
    val now = 1_700_000_000_000L
    val expiredSlot =
      TurfSlot(
        id = "2026-11-15_17:00",
        date = "2026-11-15",
        startTime = "17:00",
        endTime = "18:00",
        status = "in_progress",
        endsAt = Timestamp(Date(now - 5_000L)),
      )
    assertEquals("completed", expiredSlot.effectiveStatus(now))

    val activeSlot = expiredSlot.copy(endsAt = Timestamp(Date(now + 60_000L)))
    assertEquals("in_progress", activeSlot.effectiveStatus(now))
  }

  @Test
  fun multiSlotSession_groupsActiveBookingAndFindsCoveredSlots() {
    val now = 1_700_000_000_000L
    val slots =
      listOf(
        TurfSlot(id = "2026-11-15_16:00", date = "2026-11-15", startTime = "16:00", endTime = "17:00", status = "available"),
        TurfSlot(id = "2026-11-15_17:00", date = "2026-11-15", startTime = "17:00", endTime = "18:00", status = "in_progress", bookingId = "b1", endsAt = Timestamp(Date(now + 120_000L))),
        TurfSlot(id = "2026-11-15_18:00", date = "2026-11-15", startTime = "18:00", endTime = "19:00", status = "in_progress", bookingId = "b1", endsAt = Timestamp(Date(now + 120_000L))),
        TurfSlot(id = "2026-11-15_19:00", date = "2026-11-15", startTime = "19:00", endTime = "20:00", status = "available"),
      )

    val active = GambianSlotTimeHelper.findActiveSession(slots, now)
    assertNotNull(active)
    assertEquals("17:00", active?.startTime)
    assertEquals("19:00", active?.endTime)
    assertEquals(2, active?.slots?.size)

    val covered = GambianSlotTimeHelper.findCoveredSlotsForRange(slots, "17:00", "19:00")
    assertEquals(2, covered.size)
  }

  @Test
  fun liveStatusForTurf_computesFreeInUseFullyBookedAndClosedCorrectly() {
    val gmt = GambianSlotTimeHelper.gmtTimeZone
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).apply { timeZone = gmt }
    val today = "2026-11-17"
    val now1730 = fmt.parse("$today 17:30")!!.time

    // 1. Free now -> shows Free until next booked slot
    val freeSlots =
      listOf(
        TurfSlot(id = "${today}_16:00", date = today, startTime = "16:00", endTime = "17:00", status = "available"),
        TurfSlot(id = "${today}_17:00", date = today, startTime = "17:00", endTime = "18:00", status = "available"),
        TurfSlot(id = "${today}_18:00", date = today, startTime = "18:00", endTime = "19:00", status = "available"),
        TurfSlot(id = "${today}_19:00", date = today, startTime = "19:00", endTime = "20:00", status = "booked"),
      )
    val freeStatus =
      GambianSlotTimeHelper.computeLiveStatusForTurf(
        openTimeRaw = "16:00",
        closeTimeRaw = "24:00",
        slotMinutesRaw = 60L,
        isOpen = true,
        todaySlots = freeSlots,
        nowMillis = now1730,
      )
    assertEquals(com.example.data.catalog.LiveTurfBadgeState.FREE_NOW, freeStatus.state)
    assertEquals("Free until 7:00 PM", freeStatus.primaryDetail)

    // 2. In use with back-to-back booked slot skips to first genuinely free slot (9:00 PM)
    val inUseSlots =
      listOf(
        TurfSlot(
          id = "${today}_17:00",
          date = today,
          startTime = "17:00",
          endTime = "18:00",
          status = "in_progress",
          bookingId = "b_active",
          endsAt = Timestamp(Date(now1730 + 30 * 60_000L)),
        ),
        TurfSlot(
          id = "${today}_18:00",
          date = today,
          startTime = "18:00",
          endTime = "19:00",
          status = "in_progress",
          bookingId = "b_active",
          endsAt = Timestamp(Date(now1730 + 90 * 60_000L)),
        ),
        TurfSlot(id = "${today}_19:00", date = today, startTime = "19:00", endTime = "20:00", status = "booked"),
        TurfSlot(id = "${today}_20:00", date = today, startTime = "20:00", endTime = "21:00", status = "blocked"),
        TurfSlot(id = "${today}_21:00", date = today, startTime = "21:00", endTime = "22:00", status = "available"),
      )
    val inUseStatus =
      GambianSlotTimeHelper.computeLiveStatusForTurf(
        openTimeRaw = "16:00",
        closeTimeRaw = "24:00",
        slotMinutesRaw = 60L,
        isOpen = true,
        todaySlots = inUseSlots,
        nowMillis = now1730,
      )
    assertEquals(com.example.data.catalog.LiveTurfBadgeState.IN_USE, inUseStatus.state)
    assertEquals("Until 7:00 PM", inUseStatus.primaryDetail)
    assertEquals("Next free: 9:00 PM", inUseStatus.secondaryDetail)

    // 3. Fully booked today -> Next free: tomorrow 4:00 PM
    val allBooked =
      GambianSlotTimeHelper.generateSlotWindows("16:00", "24:00", 60L).map { w ->
        TurfSlot(id = "${today}_${w.startTime}", date = today, startTime = w.startTime, endTime = w.endTime, status = "booked")
      }
    val bookedOutStatus =
      GambianSlotTimeHelper.computeLiveStatusForTurf(
        openTimeRaw = "16:00",
        closeTimeRaw = "24:00",
        slotMinutesRaw = 60L,
        isOpen = true,
        todaySlots = allBooked,
        nowMillis = now1730,
      )
    assertEquals(com.example.data.catalog.LiveTurfBadgeState.FULLY_BOOKED, bookedOutStatus.state)
    assertEquals("Next free: tomorrow 4:00 PM", bookedOutStatus.primaryDetail)

    // 4. Closed outside opening hours
    val now1000 = fmt.parse("$today 10:00")!!.time
    val closedStatus =
      GambianSlotTimeHelper.computeLiveStatusForTurf(
        openTimeRaw = "16:00",
        closeTimeRaw = "24:00",
        slotMinutesRaw = 60L,
        isOpen = true,
        todaySlots = emptyList(),
        nowMillis = now1000,
      )
    assertEquals(com.example.data.catalog.LiveTurfBadgeState.CLOSED, closedStatus.state)
    assertEquals("Opens 4:00 PM", closedStatus.primaryDetail)
  }
}
