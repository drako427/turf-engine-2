package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.catalog.ActiveSlotSession
import com.example.data.catalog.GambianSlotTimeHelper
import com.example.data.model.TurfListing
import com.example.data.model.TurfSlot
import com.example.data.model.TurfVenueBooking
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonSubtle
import com.example.ui.theme.EmeraldOpen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay

private val AmberInUse = Color(0xFFF59E0B)
private val AmberInUseBg = Color(0xFF422607)
private val SlateBlocked = Color(0xFF8B8B99)
private val SlateBlockedBg = Color(0xFF23232C)

private data class TimelineScheduleEntry(
  val key: String,
  val slots: List<TurfSlot>,
  val startTime: String,
  val endTime: String,
  val effectiveStatus: String,
  val booking: TurfVenueBooking?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerTodayTab(
  turf: TurfListing,
  selectedDateIso: String,
  todayNowSlots: List<TurfSlot>,
  slotsState: UiState<List<TurfSlot>>,
  bookings: List<TurfVenueBooking>,
  isBusy: Boolean,
  onSelectDate: (String) -> Unit,
  onOpenNewSession: (initialStartTime: String?) -> Unit,
  onOpenBookingsTab: () -> Unit,
  onAutoCompleteExpired: (nowMillis: Long) -> Unit,
  onBlockSlot: (slot: TurfSlot) -> Unit,
  onUnblockSlot: (slot: TurfSlot) -> Unit,
  onStartSession: (slots: List<TurfSlot>, bookingId: String?) -> Unit,
  onEndSession: (slots: List<TurfSlot>, bookingId: String?) -> Unit,
  onMarkNoShow: (slots: List<TurfSlot>, bookingId: String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000L)
      nowMillis = System.currentTimeMillis()
    }
  }

  // Auto-mark expired in-progress sessions completed & free slots when timer reaches zero
  LaunchedEffect(todayNowSlots, nowMillis / 2000L) {
    if (todayNowSlots.isNotEmpty()) {
      onAutoCompleteExpired(nowMillis)
    }
  }

  val days = remember(nowMillis / 60_000L) { GambianSlotTimeHelper.nextSevenDaysGmt(nowMillis) }
  val bookingsById = remember(bookings) { bookings.associateBy { it.id } }

  var selectedEntryForActions by remember { mutableStateOf<TimelineScheduleEntry?>(null) }

  val activeNowSession =
    remember(todayNowSlots, nowMillis) {
      GambianSlotTimeHelper.findActiveSession(todayNowSlots, nowMillis)
    }
  val activeNowTeam =
    remember(activeNowSession, bookingsById) {
      activeNowSession?.bookingId?.let { bookingsById[it]?.displayTeamName() }
    }
  val nextBookedSlot =
    remember(todayNowSlots, nowMillis) {
      GambianSlotTimeHelper.findNextBookedSlot(todayNowSlots, nowMillis)
    }
  val nextBookedTeam =
    remember(nextBookedSlot, bookingsById) {
      nextBookedSlot?.bookingId?.let { bookingsById[it]?.displayTeamName() }
    }

  val daySlots = (slotsState as? UiState.Success)?.data.orEmpty()
  val timelineEntries =
    remember(daySlots, bookingsById, nowMillis / 5_000L) {
      buildTimelineEntries(daySlots, bookingsById, nowMillis)
    }

  Column(
    modifier = modifier.fillMaxSize().background(ObsidianBlack),
  ) {
    // Top Header ("Today" + Bookings quick icon with badge count)
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Spacer(modifier = Modifier.size(40.dp))
      Text(
        text = "Today",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
      )
      val activeCount =
        remember(bookings) {
          bookings.count { it.status == "in_progress" || it.status == "confirmed" }
        }
      Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        IconButton(
          onClick = onOpenBookingsTab,
          modifier = Modifier.size(40.dp).testTag("owner_today_bookings_shortcut"),
        ) {
          Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = "Open bookings",
            tint = TextPrimary,
          )
        }
        if (activeCount > 0) {
          Surface(
            color = CrimsonRed,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp).size(16.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = activeCount.coerceAtMost(9).toString(),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxWidth().weight(1f),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Hero "Now" Countdown / Free Card
      item {
        OwnerNowHeroCard(
          activeSession = activeNowSession,
          teamName = activeNowTeam,
          nextBookedSlot = nextBookedSlot,
          nextBookedTeam = nextBookedTeam,
          nowMillis = nowMillis,
          isBusy = isBusy,
          onEndSession = { session ->
            onEndSession(session.slots, session.bookingId)
          },
        )
      }

      // 7-day GMT date switcher strip
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          days.forEach { day ->
            val selected = day.isoDate == selectedDateIso
            Surface(
              color = if (selected) CrimsonRed else ObsidianCard,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, if (selected) CrimsonGlow else ObsidianBorder),
              modifier =
                Modifier.clip(RoundedCornerShape(12.dp))
                  .clickable { onSelectDate(day.isoDate) }
                  .testTag("owner_date_chip_${day.isoDate}"),
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = day.primaryLabel,
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (selected) Color.White else TextPrimary,
                )
                Text(
                  text = day.secondaryLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (selected) Color.White.copy(alpha = 0.9f) else TextSecondary,
                )
              }
            }
          }
        }
      }

      // "Today's Schedule" header
      item {
        val selectedDayLabel =
          days.firstOrNull { it.isoDate == selectedDateIso }?.let { day ->
            if (day.primaryLabel == "Today") "Today's Schedule"
            else "${day.primaryLabel}, ${day.secondaryLabel} Schedule"
          } ?: "Today's Schedule"

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = selectedDayLabel,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
          )
        }
      }

      when (slotsState) {
        UiState.Loading -> {
          item {
            Box(
              modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(color = CrimsonRed)
            }
          }
        }
        is UiState.Error -> {
          item {
            Surface(
              color = CrimsonSubtle,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = slotsState.message,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
              )
            }
          }
        }
        is UiState.Success -> {
          if (timelineEntries.isEmpty()) {
            item {
              Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  CircularProgressIndicator(
                    color = CrimsonRed,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "Preparing schedule for ${turf.name}...",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                  )
                }
              }
            }
          } else {
            itemsIndexed(timelineEntries, key = { _, e -> e.key }) { index, entry ->
              ScheduleTimelineItem(
                entry = entry,
                isFirst = index == 0,
                isLast = index == timelineEntries.lastIndex,
                onClick = { selectedEntryForActions = entry },
              )
            }
          }
        }
      }
    }

    // Persistent Crimson "+ New Session" CTA above Bottom Navigation Bar
    Surface(
      color = ObsidianBlack,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
      Button(
        onClick = { onOpenNewSession(null) },
        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("owner_new_session_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "New Session",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
      }
    }
  }

  val sheetEntry = selectedEntryForActions
  if (sheetEntry != null) {
    val primarySlot = sheetEntry.slots.first()
    ModalBottomSheet(
      onDismissRequest = { selectedEntryForActions = null },
      containerColor = ObsidianCardElevated,
      contentColor = TextPrimary,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text =
            "${GambianSlotTimeHelper.formatHhMmTo12Hour(sheetEntry.startTime)} – ${GambianSlotTimeHelper.formatHhMmTo12Hour(sheetEntry.endTime)} GMT",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )
        if (sheetEntry.booking != null) {
          val phoneLabel =
            sheetEntry.booking.phone.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()
          Text(
            text = "${sheetEntry.booking.displayTeamName()}$phoneLabel • D${sheetEntry.booking.totalPrice} (${sheetEntry.booking.paymentMethod.replaceFirstChar { it.uppercase() }})",
            style = MaterialTheme.typography.bodyMedium,
            color = AmberInUse,
          )
        } else {
          Text(
            text = "Status: ${slotStatusDisplayLabel(sheetEntry.effectiveStatus)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (sheetEntry.effectiveStatus) {
          "available" -> {
            Button(
              onClick = {
                selectedEntryForActions = null
                onOpenNewSession(primarySlot.startTime)
              },
              enabled = !isBusy,
              colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("slot_action_walkin"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("New Session / Walk-in", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = {
                onBlockSlot(primarySlot)
                selectedEntryForActions = null
              },
              enabled = !isBusy,
              border = BorderStroke(1.dp, ObsidianBorder),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("slot_action_block"),
            ) {
              Icon(
                Icons.Filled.Block,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Block (Maintenance)", color = TextPrimary)
            }
          }
          "booked" -> {
            Button(
              onClick = {
                onStartSession(sheetEntry.slots, sheetEntry.booking?.id ?: primarySlot.bookingId)
                selectedEntryForActions = null
              },
              enabled = !isBusy,
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldOpen, contentColor = ObsidianBlack),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("slot_action_start"),
            ) {
              Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Start session", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = {
                onMarkNoShow(sheetEntry.slots, sheetEntry.booking?.id ?: primarySlot.bookingId)
                selectedEntryForActions = null
              },
              enabled = !isBusy,
              border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("slot_action_noshow"),
            ) {
              Icon(
                Icons.Filled.PersonOff,
                contentDescription = null,
                tint = CrimsonRed,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Mark no-show", color = CrimsonRed)
            }
          }
          "in_progress" -> {
            Button(
              onClick = {
                onEndSession(sheetEntry.slots, sheetEntry.booking?.id ?: primarySlot.bookingId)
                selectedEntryForActions = null
              },
              enabled = !isBusy,
              colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("slot_action_end"),
            ) {
              Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("End session", fontWeight = FontWeight.Bold)
            }
          }
          "blocked" -> {
            Button(
              onClick = {
                onUnblockSlot(primarySlot)
                selectedEntryForActions = null
              },
              enabled = !isBusy,
              colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("slot_action_unblock"),
            ) {
              Icon(
                Icons.Filled.EventAvailable,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Unblock", fontWeight = FontWeight.Bold)
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

private fun buildTimelineEntries(
  slots: List<TurfSlot>,
  bookingsById: Map<String, TurfVenueBooking>,
  nowMillis: Long,
): List<TimelineScheduleEntry> {
  if (slots.isEmpty()) return emptyList()
  val sorted = slots.sortedBy { it.startTime }
  val entries = mutableListOf<TimelineScheduleEntry>()
  var index = 0
  while (index < sorted.size) {
    val first = sorted[index]
    val firstEff = first.effectiveStatus(nowMillis)
    val bId = first.bookingId
    if (!bId.isNullOrBlank() && (firstEff == "booked" || firstEff == "in_progress")) {
      val group = mutableListOf(first)
      var nextIdx = index + 1
      while (nextIdx < sorted.size && sorted[nextIdx].bookingId == bId) {
        group += sorted[nextIdx]
        nextIdx++
      }
      val booking = bookingsById[bId]
      entries +=
        TimelineScheduleEntry(
          key = "${bId}_${first.id}",
          slots = group,
          startTime = group.first().startTime,
          endTime = group.last().endTime,
          effectiveStatus = firstEff,
          booking = booking,
        )
      index = nextIdx
    } else {
      entries +=
        TimelineScheduleEntry(
          key = first.id,
          slots = listOf(first),
          startTime = first.startTime,
          endTime = first.endTime,
          effectiveStatus = if (firstEff == "completed") "available" else firstEff,
          booking = null,
        )
      index++
    }
  }
  return entries
}

@Composable
private fun OwnerNowHeroCard(
  activeSession: ActiveSlotSession?,
  teamName: String?,
  nextBookedSlot: TurfSlot?,
  nextBookedTeam: String?,
  nowMillis: Long,
  isBusy: Boolean,
  onEndSession: (ActiveSlotSession) -> Unit,
) {
  val isLive = activeSession != null
  val borderColor = if (isLive) Color(0xFF5C2618) else EmeraldOpen.copy(alpha = 0.45f)

  Surface(
    color = ObsidianCard,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier.fillMaxWidth().testTag("owner_now_card"),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.horizontalGradient(
              colors =
                if (isLive) {
                  listOf(Color(0xFF1C1311), Color(0xFF2B1014))
                } else {
                  listOf(Color(0xFF111A16), Color(0xFF13131A))
                }
            )
          )
    ) {
      // Subtle pitch / soccer icon silhouette on right side like the mockup
      Icon(
        imageVector = Icons.Filled.SportsSoccer,
        contentDescription = null,
        tint = (if (isLive) CrimsonRed else EmeraldOpen).copy(alpha = 0.14f),
        modifier =
          Modifier.align(Alignment.CenterEnd)
            .padding(end = 10.dp)
            .size(110.dp)
            .alpha(0.75f),
      )

      Column(modifier = Modifier.padding(16.dp)) {
        if (activeSession == null) {
          Surface(
            color = EmeraldOpen.copy(alpha = 0.16f),
            shape = RoundedCornerShape(50),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldOpen))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Free now",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = EmeraldOpen,
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Free now",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          val nextBookedText =
            if (nextBookedSlot != null) {
              val teamSuffix = nextBookedTeam?.let { " • $it" }.orEmpty()
              "Next booked: ${GambianSlotTimeHelper.formatHhMmTo12Hour(nextBookedSlot.startTime)}$teamSuffix"
            } else {
              "No upcoming bookings scheduled today"
            }
          Text(
            text = nextBookedText,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        } else {
          val remainingMillis = (activeSession.endsAtMillis - nowMillis).coerceAtLeast(0L)
          val finishLabel = GambianSlotTimeHelper.formatHhMmTo12Hour(activeSession.endTime)

          Surface(
            color = AmberInUseBg,
            shape = RoundedCornerShape(50),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AmberInUse))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "In use",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AmberInUse,
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = GambianSlotTimeHelper.formatCountdownColon(remainingMillis),
              fontSize = 40.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
              letterSpacing = 0.5.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "left",
              style = MaterialTheme.typography.titleMedium,
              color = TextSecondary,
              modifier = Modifier.padding(bottom = 7.dp),
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Groups,
              contentDescription = null,
              tint = TextPrimary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = teamName ?: "Active Session",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = TextPrimary,
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Schedule,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Finish: $finishLabel",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = { onEndSession(activeSession) },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("now_card_end_session_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Stop,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("End session", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun ScheduleTimelineItem(
  entry: TimelineScheduleEntry,
  isFirst: Boolean,
  isLast: Boolean,
  onClick: () -> Unit,
) {
  val dotColor =
    when (entry.effectiveStatus) {
      "available" -> EmeraldOpen
      "in_progress" -> AmberInUse
      "booked" -> CrimsonRed
      else -> SlateBlocked
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(IntrinsicSize.Min)
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 4.dp)
        .testTag("owner_slot_row_${entry.slots.first().id}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Timeline vertical track + dot
    Box(
      modifier = Modifier.width(24.dp).fillMaxHeight(),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier.width(2.dp)
              .weight(1f)
              .background(if (isFirst) Color.Transparent else ObsidianBorder)
        )
        Box(
          modifier =
            Modifier.size(10.dp)
              .clip(CircleShape)
              .background(dotColor)
        )
        Box(
          modifier =
            Modifier.width(2.dp)
              .weight(1f)
              .background(if (isLast) Color.Transparent else ObsidianBorder)
        )
      }
    }

    Spacer(modifier = Modifier.width(10.dp))

    // Time Range + Team/Subtitle
    Column(
      modifier = Modifier.weight(1f).padding(vertical = 10.dp),
    ) {
      Text(
        text =
          "${GambianSlotTimeHelper.formatHhMmTo12Hour(entry.startTime)} – ${GambianSlotTimeHelper.formatHhMmTo12Hour(entry.endTime)}",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = TextPrimary,
      )
      val subtitle =
        when (entry.effectiveStatus) {
          "in_progress", "booked" -> entry.booking?.displayTeamName()
          "blocked" -> "Blocked (Maintenance)"
          else -> null
        }
      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }
    }

    // Status label / rounded badge matching mockup
    when (entry.effectiveStatus) {
      "available" -> {
        Text(
          text = "Available",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          color = EmeraldOpen,
          modifier = Modifier.padding(horizontal = 6.dp),
        )
      }
      "in_progress" -> {
        Surface(
          color = AmberInUseBg,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, AmberInUse.copy(alpha = 0.55f)),
        ) {
          Text(
            text = "In use",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = AmberInUse,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }
      }
      "booked" -> {
        Surface(
          color = CrimsonRed.copy(alpha = 0.18f),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.65f)),
        ) {
          Text(
            text = "Booked",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = CrimsonGlow,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }
      }
      else -> {
        Surface(
          color = SlateBlockedBg,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, ObsidianBorder),
        ) {
          Text(
            text = "Blocked",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = SlateBlocked,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }
      }
    }
  }
}

private fun slotStatusDisplayLabel(status: String): String =
  when (status) {
    "available" -> "Available"
    "booked" -> "Booked"
    "in_progress" -> "In use"
    "completed" -> "Completed"
    "blocked" -> "Blocked"
    else -> status
  }
