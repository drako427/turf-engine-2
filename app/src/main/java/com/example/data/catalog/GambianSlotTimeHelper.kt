package com.example.data.catalog

import com.example.data.model.TurfSlot
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class GmtDayOption(
  val isoDate: String,
  val primaryLabel: String,
  val secondaryLabel: String,
)

data class SlotWindow(
  val startTime: String,
  val endTime: String,
)

data class ActiveSlotSession(
  val bookingId: String?,
  val slots: List<TurfSlot>,
  val startTime: String,
  val endTime: String,
  val endsAtMillis: Long,
)

object GambianSlotTimeHelper {
  const val DEFAULT_OPEN_TIME = "08:00"
  const val DEFAULT_CLOSE_TIME = "23:00"
  const val DEFAULT_SLOT_MINUTES = 60L

  val gmtTimeZone: TimeZone = TimeZone.getTimeZone("GMT")

  fun todayIsoDateGmt(nowMillis: Long = System.currentTimeMillis()): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = gmtTimeZone }
    return fmt.format(Date(nowMillis))
  }

  fun currentHhMmGmt(nowMillis: Long = System.currentTimeMillis()): String {
    val fmt = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = gmtTimeZone }
    return fmt.format(Date(nowMillis))
  }

  fun nextSevenDaysGmt(nowMillis: Long = System.currentTimeMillis()): List<GmtDayOption> {
    val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = gmtTimeZone }
    val weekdayFmt = SimpleDateFormat("EEE", Locale.US).apply { timeZone = gmtTimeZone }
    val dateFmt = SimpleDateFormat("d MMM", Locale.US).apply { timeZone = gmtTimeZone }
    val cal = Calendar.getInstance(gmtTimeZone, Locale.US).apply { timeInMillis = nowMillis }

    return (0 until 7).map { offset ->
      val date = cal.time
      val option =
        GmtDayOption(
          isoDate = isoFmt.format(date),
          primaryLabel = if (offset == 0) "Today" else weekdayFmt.format(date),
          secondaryLabel = dateFmt.format(date),
        )
      cal.add(Calendar.DAY_OF_MONTH, 1)
      option
    }
  }

  fun slotDocId(dateIso: String, startTime: String): String = "${dateIso}_$startTime"

  fun normalizeTimeOrDefault(raw: String?, fallback: String): String {
    val clean = raw?.trim().orEmpty()
    val match = Regex("^(([01][0-9]|2[0-3]):[0-5][0-9]|24:00)$").matchEntire(clean)
    return if (match != null) clean else fallback
  }

  fun normalizeSlotMinutes(raw: Long?): Long = if (raw == 90L) 90L else 60L

  fun parseHhMmToMinutes(hhMm: String): Int? {
    val trimmed = hhMm.trim()
    if (trimmed == "24:00") return 24 * 60
    val parts = trimmed.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
  }

  fun minutesToHhMm(totalMinutes: Int): String {
    val clamped = totalMinutes.coerceIn(0, 24 * 60)
    if (clamped == 24 * 60) return "24:00"
    val h = clamped / 60
    val m = clamped % 60
    return String.format(Locale.US, "%02d:%02d", h, m)
  }

  fun hoursBetween(startTime: String, endTime: String): Double {
    val s = parseHhMmToMinutes(startTime) ?: return 0.0
    val e = parseHhMmToMinutes(endTime) ?: return 0.0
    if (e <= s) return 0.0
    return (e - s) / 60.0
  }

  fun generateSlotWindows(
    openTime: String,
    closeTime: String,
    slotMinutes: Long,
  ): List<SlotWindow> {
    val safeOpen = normalizeTimeOrDefault(openTime, DEFAULT_OPEN_TIME)
    val safeClose = normalizeTimeOrDefault(closeTime, DEFAULT_CLOSE_TIME)
    val step = normalizeSlotMinutes(slotMinutes).toInt()

    val openMins = parseHhMmToMinutes(safeOpen) ?: (8 * 60)
    val rawCloseMins = parseHhMmToMinutes(safeClose) ?: (23 * 60)
    val closeMins =
      if (rawCloseMins > openMins) rawCloseMins.coerceAtMost(24 * 60)
      else (openMins + step).coerceAtMost(24 * 60)

    val windows = mutableListOf<SlotWindow>()
    var cursor = openMins
    while (cursor + step <= closeMins && cursor + step <= 24 * 60 && windows.size < 24) {
      windows +=
        SlotWindow(
          startTime = minutesToHhMm(cursor),
          endTime = minutesToHhMm(cursor + step),
        )
      cursor += step
    }
    return windows
  }

  fun formatHhMmTo12Hour(hhMm: String): String {
    val mins = parseHhMmToMinutes(hhMm) ?: return hhMm
    if (mins == 24 * 60 || mins == 0) return "12:00 AM"
    val hour24 = mins / 60
    val minute = mins % 60
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 =
      when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
      }
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
  }

  fun formatTimestampTo12HourGmt(timestamp: Timestamp?): String? {
    val date = timestamp?.toDate() ?: return null
    val fmt = SimpleDateFormat("h:mm a", Locale.US).apply { timeZone = gmtTimeZone }
    return fmt.format(date)
  }

  fun computeSlotStartEpochMillis(dateIso: String, startTime: String): Long {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = gmtTimeZone }
    return runCatching { fmt.parse("$dateIso $startTime")?.time }.getOrNull()
      ?: System.currentTimeMillis()
  }

  fun computeSlotEndEpochMillis(dateIso: String, endTime: String): Long {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = gmtTimeZone }
    if (endTime.trim() == "24:00") {
      val dayStart = runCatching { fmt.parse("$dateIso 00:00")?.time }.getOrNull()
      if (dayStart != null) return dayStart + 24L * 60L * 60L * 1000L
    }
    val parsed = runCatching { fmt.parse("$dateIso $endTime")?.time }.getOrNull()
    return parsed ?: (System.currentTimeMillis() + 60L * 60L * 1000L)
  }

  fun formatCountdownColon(remainingMillis: Long): String {
    val safeSeconds = (remainingMillis / 1000L).coerceAtLeast(0L)
    val hours = safeSeconds / 3600L
    val minutes = (safeSeconds % 3600L) / 60L
    val seconds = safeSeconds % 60L
    return if (hours > 0L) {
      String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
  }

  fun formatCountdown(remainingMillis: Long): String = formatCountdownColon(remainingMillis)

  fun isStartTimeNow(dateIso: String, startTime: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (dateIso != todayIsoDateGmt(nowMillis)) return false
    val slotStart = computeSlotStartEpochMillis(dateIso, startTime)
    // "Now" if we are inside or within 30 mins of the slot's kickoff time
    return slotStart <= nowMillis + 30L * 60L * 1000L
  }

  fun findCoveredSlotsForRange(
    slots: List<TurfSlot>,
    startTime: String,
    endTime: String,
  ): List<TurfSlot> {
    val startMins = parseHhMmToMinutes(startTime) ?: return emptyList()
    val endMins = parseHhMmToMinutes(endTime) ?: return emptyList()
    if (endMins <= startMins) return emptyList()

    return slots
      .filter { slot ->
        val s = parseHhMmToMinutes(slot.startTime) ?: return@filter false
        val e = parseHhMmToMinutes(slot.endTime) ?: return@filter false
        // Overlaps [startMins, endMins)
        s < endMins && e > startMins
      }
      .sortedBy { it.startTime }
  }

  fun findActiveSession(slots: List<TurfSlot>, nowMillis: Long): ActiveSlotSession? {
    val activeSlots = slots.filter { it.effectiveStatus(nowMillis) == "in_progress" }.sortedBy { it.startTime }
    if (activeSlots.isEmpty()) return null
    val primaryBookingId = activeSlots.first().bookingId
    val grouped =
      if (!primaryBookingId.isNullOrBlank()) {
        activeSlots.filter { it.bookingId == primaryBookingId }
      } else {
        listOf(activeSlots.first())
      }
    val endsAtMillis =
      grouped.mapNotNull { it.endsAt?.toDate()?.time }.maxOrNull()
        ?: computeSlotEndEpochMillis(grouped.last().date, grouped.last().endTime)
    return ActiveSlotSession(
      bookingId = primaryBookingId,
      slots = grouped,
      startTime = grouped.first().startTime,
      endTime = grouped.last().endTime,
      endsAtMillis = endsAtMillis,
    )
  }

  fun findExpiredInProgressSessions(slots: List<TurfSlot>, nowMillis: Long): List<ActiveSlotSession> {
    val expired =
      slots.filter {
        it.status == "in_progress" &&
          (it.endsAt?.toDate()?.time?.let { endMs -> endMs <= nowMillis } == true)
      }
    if (expired.isEmpty()) return emptyList()
    return expired
      .groupBy { it.bookingId ?: it.id }
      .values
      .map { group ->
        val sorted = group.sortedBy { it.startTime }
        ActiveSlotSession(
          bookingId = sorted.first().bookingId,
          slots = sorted,
          startTime = sorted.first().startTime,
          endTime = sorted.last().endTime,
          endsAtMillis = sorted.mapNotNull { it.endsAt?.toDate()?.time }.maxOrNull() ?: nowMillis,
        )
      }
  }

  fun findActiveInProgressSlot(slots: List<TurfSlot>, nowMillis: Long): TurfSlot? =
    findActiveSession(slots, nowMillis)?.slots?.firstOrNull()

  fun findNextBookedSlot(slots: List<TurfSlot>, nowMillis: Long): TurfSlot? {
    val nowHhMm = currentHhMmGmt(nowMillis)
    return slots
      .filter { it.effectiveStatus(nowMillis) == "booked" && it.endTime > nowHhMm }
      .sortedBy { it.startTime }
      .firstOrNull()
  }

  fun findNextAvailableSlotToday(slots: List<TurfSlot>, nowMillis: Long): TurfSlot? {
    val nowHhMm = currentHhMmGmt(nowMillis)
    return slots
      .filter { it.effectiveStatus(nowMillis) == "available" && it.endTime > nowHhMm }
      .sortedBy { it.startTime }
      .firstOrNull()
  }

  fun computeLiveStatusForTurf(
    openTimeRaw: String?,
    closeTimeRaw: String?,
    slotMinutesRaw: Long?,
    isOpen: Boolean,
    todaySlots: List<TurfSlot>,
    nowMillis: Long = System.currentTimeMillis(),
  ): TurfLiveStatusInfo {
    val openTime = normalizeTimeOrDefault(openTimeRaw, DEFAULT_OPEN_TIME)
    val closeTime = normalizeTimeOrDefault(closeTimeRaw, DEFAULT_CLOSE_TIME)
    val slotMinutes = normalizeSlotMinutes(slotMinutesRaw)
    val openMins = parseHhMmToMinutes(openTime) ?: (8 * 60)
    val closeMins = parseHhMmToMinutes(closeTime) ?: (23 * 60)
    val todayIso = todayIsoDateGmt(nowMillis)
    val nowHhMm = currentHhMmGmt(nowMillis)
    val nowMins = parseHhMmToMinutes(nowHhMm) ?: 0

    val todayDocs = todaySlots.filter { it.date.isBlank() || it.date == todayIso }
    val slotByStart = todayDocs.associateBy { it.startTime }
    val timeline = buildTodayDisplayTimeline(openTime, closeTime, slotMinutes, todayDocs, nowMillis)

    val activeSession = findActiveSession(todayDocs, nowMillis)
    if (activeSession != null) {
      val activeEndMins = parseHhMmToMinutes(activeSession.endTime) ?: closeMins
      val laterSlots =
        timeline.filter { (parseHhMmToMinutes(it.startTime) ?: 0) >= activeEndMins }
      val nextFreeSlot = laterSlots.firstOrNull { it.status == "available" }
      val nextFreeLabel =
        when {
          nextFreeSlot != null -> formatHhMmTo12Hour(nextFreeSlot.startTime)
          laterSlots.isEmpty() && activeEndMins < closeMins -> formatHhMmTo12Hour(activeSession.endTime)
          else -> "tomorrow ${formatHhMmTo12Hour(openTime)}"
        }
      return TurfLiveStatusInfo(
        state = LiveTurfBadgeState.IN_USE,
        headerTag = "In use",
        slideTag = "In use",
        primaryDetail = "Until ${formatHhMmTo12Hour(activeSession.endTime)}",
        secondaryDetail = "Next free: $nextFreeLabel",
        endsAtMillis = activeSession.endsAtMillis,
      )
    }

    if (!isOpen || nowMins < openMins || nowMins >= closeMins) {
      val opens12 = formatHhMmTo12Hour(openTime)
      return TurfLiveStatusInfo(
        state = LiveTurfBadgeState.CLOSED,
        headerTag = "Closed",
        slideTag = "Closed",
        primaryDetail = if (nowMins >= closeMins) "Opens tomorrow $opens12" else "Opens $opens12",
        secondaryDetail = "Hours: $opens12 – ${formatHhMmTo12Hour(closeTime)} GMT",
      )
    }

    val remaining =
      timeline.filter { (parseHhMmToMinutes(it.endTime) ?: 0) > nowMins }
    val currentSlot =
      remaining.firstOrNull {
        val s = parseHhMmToMinutes(it.startTime) ?: 0
        val e = parseHhMmToMinutes(it.endTime) ?: 0
        s <= nowMins && nowMins < e
      } ?: remaining.firstOrNull()

    val firstFree = remaining.firstOrNull { it.status == "available" }
    if (firstFree == null) {
      return TurfLiveStatusInfo(
        state = LiveTurfBadgeState.FULLY_BOOKED,
        headerTag = "Fully booked",
        slideTag = "Fully booked today",
        primaryDetail = "Next free: tomorrow ${formatHhMmTo12Hour(openTime)}",
        secondaryDetail = "All remaining slots today are reserved",
      )
    }

    if (currentSlot != null && currentSlot.status == "available") {
      val currentStartMins = parseHhMmToMinutes(currentSlot.startTime) ?: nowMins
      val nextOccupied =
        remaining.firstOrNull {
          (parseHhMmToMinutes(it.startTime) ?: 0) >= currentStartMins && it.status != "available"
        }
      val freeUntil = nextOccupied?.startTime ?: closeTime
      val extra =
        slotByStart.values
          .count { it.effectiveStatus(nowMillis) == "available" && (parseHhMmToMinutes(it.endTime) ?: 0) > nowMins }
      return TurfLiveStatusInfo(
        state = LiveTurfBadgeState.FREE_NOW,
        headerTag = "Free now",
        slideTag = "Free now",
        primaryDetail = "Free until ${formatHhMmTo12Hour(freeUntil)}",
        secondaryDetail =
          if (extra > 0) "$extra open slots left today"
          else "Open ${formatHhMmTo12Hour(openTime)} – ${formatHhMmTo12Hour(closeTime)} GMT",
      )
    }

    val nextFree12 = formatHhMmTo12Hour(firstFree.startTime)
    val isBlocked = currentSlot?.status == "blocked"
    return TurfLiveStatusInfo(
      state = LiveTurfBadgeState.BOOKED_NOW,
      headerTag = if (isBlocked) "Blocked" else "Booked",
      slideTag = if (isBlocked) "Blocked now" else "Booked now",
      primaryDetail = "Until $nextFree12",
      secondaryDetail = "Next free: $nextFree12",
    )
  }

  fun buildTodayDisplayTimeline(
    openTimeRaw: String?,
    closeTimeRaw: String?,
    slotMinutesRaw: Long?,
    todaySlots: List<TurfSlot>,
    nowMillis: Long = System.currentTimeMillis(),
  ): List<DisplaySlotTimelineItem> {
    val openTime = normalizeTimeOrDefault(openTimeRaw, DEFAULT_OPEN_TIME)
    val closeTime = normalizeTimeOrDefault(closeTimeRaw, DEFAULT_CLOSE_TIME)
    val slotMinutes = normalizeSlotMinutes(slotMinutesRaw)
    val windows = generateSlotWindows(openTime, closeTime, slotMinutes)
    val byStart = todaySlots.associateBy { it.startTime }

    val merged =
      windows.map { w ->
        val slot = byStart[w.startTime]
        val rawStatus = slot?.effectiveStatus(nowMillis) ?: "available"
        val normalizedStatus = if (rawStatus == "completed") "available" else rawStatus
        DisplaySlotTimelineItem(
          startTime = w.startTime,
          endTime = w.endTime,
          status = normalizedStatus,
        )
      }

    // Include any extra slot docs outside standard grid cleanly (status & times only)
    val extraSlots =
      todaySlots
        .filter { slot -> windows.none { it.startTime == slot.startTime } }
        .map { slot ->
          val rawStatus = slot.effectiveStatus(nowMillis)
          DisplaySlotTimelineItem(
            startTime = slot.startTime,
            endTime = slot.endTime,
            status = if (rawStatus == "completed") "available" else rawStatus,
          )
        }
    return (merged + extraSlots).sortedBy { it.startTime }
  }
}

enum class LiveTurfBadgeState {
  FREE_NOW,
  IN_USE,
  BOOKED_NOW,
  FULLY_BOOKED,
  CLOSED,
}

data class TurfLiveStatusInfo(
  val state: LiveTurfBadgeState,
  val headerTag: String,
  val slideTag: String,
  val primaryDetail: String,
  val secondaryDetail: String,
  val endsAtMillis: Long? = null,
)

data class DisplaySlotTimelineItem(
  val startTime: String,
  val endTime: String,
  val status: String,
)

