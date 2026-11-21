package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.catalog.GambianSlotTimeHelper
import com.example.data.model.PlayerProfile
import com.example.data.model.TurfListing
import com.example.data.model.TurfSlot
import com.example.data.model.TurfVenueBooking
import com.example.ui.components.FootballBrandLogo
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldOpen
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState

enum class OwnerTab(val label: String, val icon: ImageVector) {
  TODAY("Today", Icons.Filled.Today),
  BOOKINGS("Bookings", Icons.AutoMirrored.Filled.EventNote),
  MY_TURF("My Turf", Icons.Filled.SportsSoccer),
  PROFILE("Profile", Icons.Filled.Person),
}

@Composable
fun OwnerPortalScreen(
  profile: PlayerProfile,
  turf: TurfListing,
  selectedDateIso: String,
  todayNowSlots: List<TurfSlot>,
  slotsState: UiState<List<TurfSlot>>,
  bookingsState: UiState<List<TurfVenueBooking>>,
  isBusy: Boolean,
  statusMessage: String?,
  onClearStatusMessage: () -> Unit,
  onInitializeDay: (TurfListing, String) -> Unit,
  onSelectDate: (String) -> Unit,
  onAutoCompleteExpired: (nowMillis: Long) -> Unit,
  onCreateSessionBooking: (
    dateIso: String,
    coveredSlots: List<TurfSlot>,
    customerName: String,
    phone: String,
    startTime: String,
    endTime: String,
    hours: Double,
    totalPrice: Long,
    paymentMethod: String,
    startNow: Boolean,
    onResult: (Result<String>) -> Unit,
  ) -> Unit,
  onBlockSlot: (TurfSlot) -> Unit,
  onUnblockSlot: (TurfSlot) -> Unit,
  onStartSession: (List<TurfSlot>, String?) -> Unit,
  onEndSession: (List<TurfSlot>, String?) -> Unit,
  onMarkNoShow: (List<TurfSlot>, String?) -> Unit,
  onSaveTurfDetails: (
    turfName: String,
    address: String,
    latitude: Double,
    longitude: Double,
    pricePerHour: Long,
    primaryFormat: String,
    facilities: List<String>,
    openTime: String,
    closeTime: String,
    slotMinutes: Long,
    photos: List<String>,
    description: String,
  ) -> Unit,
  onSignOut: () -> Unit,
) {
  var selectedTab by remember { mutableStateOf(OwnerTab.TODAY) }
  var showNewSession by remember { mutableStateOf(false) }
  var newSessionInitialStart by remember { mutableStateOf<String?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(turf.id, selectedDateIso, turf.openTime, turf.closeTime, turf.slotMinutes) {
    onInitializeDay(turf, selectedDateIso)
  }

  LaunchedEffect(statusMessage) {
    if (statusMessage != null) {
      snackbarHostState.showSnackbar(statusMessage)
      onClearStatusMessage()
    }
  }

  if (showNewSession) {
    val slotsForDay = (slotsState as? UiState.Success)?.data.orEmpty()
    OwnerNewSessionScreen(
      turf = turf,
      selectedDateIso = selectedDateIso,
      slotsForDay = slotsForDay,
      initialStartTime = newSessionInitialStart,
      isBusy = isBusy,
      onBack = { showNewSession = false },
      onSubmitSession = {
        dateIso,
        coveredSlots,
        customerName,
        phone,
        startTime,
        endTime,
        hours,
        totalPrice,
        paymentMethod,
        startNow,
        onResult ->
        onCreateSessionBooking(
          dateIso,
          coveredSlots,
          customerName,
          phone,
          startTime,
          endTime,
          hours,
          totalPrice,
          paymentMethod,
          startNow,
        ) { result ->
          if (result.isSuccess) {
            showNewSession = false
          }
          onResult(result)
        }
      },
    )
    return
  }

  if (selectedTab != OwnerTab.TODAY) {
    BackHandler { selectedTab = OwnerTab.TODAY }
  }

  val bookings = (bookingsState as? UiState.Success)?.data.orEmpty()

  Scaffold(
    containerColor = ObsidianBlack,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    bottomBar = {
      OwnerBottomNavBar(
        selectedTab = selectedTab,
        onSelectTab = { selectedTab = it },
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(ObsidianBlack)
          .statusBarsPadding()
          .padding(bottom = innerPadding.calculateBottomPadding()),
    ) {
      Box(
        modifier = Modifier.fillMaxSize().widthIn(max = 600.dp).align(Alignment.TopCenter),
      ) {
        when (selectedTab) {
          OwnerTab.TODAY -> {
            OwnerTodayTab(
              turf = turf,
              selectedDateIso = selectedDateIso,
              todayNowSlots = todayNowSlots,
              slotsState = slotsState,
              bookings = bookings,
              isBusy = isBusy,
              onSelectDate = onSelectDate,
              onOpenNewSession = { start ->
                newSessionInitialStart = start
                showNewSession = true
              },
              onOpenBookingsTab = { selectedTab = OwnerTab.BOOKINGS },
              onAutoCompleteExpired = onAutoCompleteExpired,
              onBlockSlot = onBlockSlot,
              onUnblockSlot = onUnblockSlot,
              onStartSession = onStartSession,
              onEndSession = onEndSession,
              onMarkNoShow = onMarkNoShow,
            )
          }
          OwnerTab.BOOKINGS -> {
            OwnerBookingsTab(bookingsState = bookingsState)
          }
          OwnerTab.MY_TURF -> {
            OwnerMyTurfTab(
              turf = turf,
              isBusy = isBusy,
              onSaveTurfDetails = { submission, slotMinutes ->
                onSaveTurfDetails(
                  submission.turfName,
                  submission.address,
                  submission.latitude,
                  submission.longitude,
                  submission.pricePerHour,
                  submission.primaryFormat,
                  submission.facilities,
                  submission.openTime,
                  submission.closeTime,
                  slotMinutes,
                  submission.photos,
                  submission.description,
                )
              },
            )
          }
          OwnerTab.PROFILE -> {
            OwnerProfileTab(
              profile = profile,
              turf = turf,
              onSignOut = onSignOut,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OwnerBottomNavBar(
  selectedTab: OwnerTab,
  onSelectTab: (OwnerTab) -> Unit,
) {
  Surface(
    color = ObsidianSurface,
    border = BorderStroke(1.dp, ObsidianBorder),
    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
  ) {
    NavigationBar(
      containerColor = ObsidianSurface,
      tonalElevation = 0.dp,
    ) {
      OwnerTab.entries.forEach { tab ->
        val isSelected = selectedTab == tab
        NavigationBarItem(
          selected = isSelected,
          onClick = { onSelectTab(tab) },
          icon = {
            Icon(
              imageVector = tab.icon,
              contentDescription = tab.label,
              modifier = Modifier.size(22.dp),
            )
          },
          label = {
            Text(
              text = tab.label,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
          },
          colors =
            NavigationBarItemDefaults.colors(
              selectedIconColor = CrimsonRed,
              selectedTextColor = CrimsonRed,
              unselectedIconColor = TextMuted,
              unselectedTextColor = TextMuted,
              indicatorColor = Color.Transparent,
            ),
          modifier = Modifier.testTag("owner_nav_${tab.name.lowercase()}"),
        )
      }
    }
  }
}

@Composable
private fun OwnerBookingsTab(bookingsState: UiState<List<TurfVenueBooking>>) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(
        text = "Bookings",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
      )
      Text(
        text = "Recorded walk-ins and upcoming pitch sessions.",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
      )
    }

    when (bookingsState) {
      UiState.Loading -> {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = CrimsonRed)
          }
        }
      }
      is UiState.Error -> {
        item {
          Text(
            text = bookingsState.message,
            color = CrimsonRed,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
      is UiState.Success -> {
        if (bookingsState.data.isEmpty()) {
          item {
            Surface(
              color = ObsidianCard,
              shape = RoundedCornerShape(18.dp),
              border = BorderStroke(1.dp, ObsidianBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(18.dp)) {
                Icon(
                  imageVector = Icons.Filled.CalendarMonth,
                  contentDescription = null,
                  tint = TextMuted,
                  modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "No sessions recorded yet",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Tap + New Session on Today to book or start a session.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                )
              }
            }
          }
        } else {
          items(bookingsState.data, key = { it.id }) { booking ->
            Surface(
              color = ObsidianCard,
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, ObsidianBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = booking.displayTeamName(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                  )
                  val timeRange =
                    if (booking.startTime.isNotBlank() && booking.endTime.isNotBlank()) {
                      val datePrefix = if (booking.date.isNotBlank()) "${booking.date} • " else ""
                      "$datePrefix${GambianSlotTimeHelper.formatHhMmTo12Hour(booking.startTime)}–${GambianSlotTimeHelper.formatHhMmTo12Hour(booking.endTime)}"
                    } else {
                      booking.slotId.replace("_", " • ")
                    }
                  Text(
                    text =
                      if (booking.phone.isNotBlank()) "$timeRange • ${booking.phone}"
                      else timeRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  val methodLabel = booking.paymentMethod.replaceFirstChar { it.uppercase() }
                  Text(
                    text = "D${booking.totalPrice} • $methodLabel",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = CrimsonGlow,
                  )
                }
                val (statusColor, statusBg) =
                  when (booking.status) {
                    "in_progress" -> Color(0xFFF59E0B) to Color(0xFF422607)
                    "completed" -> EmeraldOpen to EmeraldSubtle
                    "no_show", "cancelled" -> TextMuted to ObsidianCardElevated
                    else -> CrimsonGlow to CrimsonRed.copy(alpha = 0.16f)
                  }
                Surface(color = statusBg, shape = RoundedCornerShape(50)) {
                  Text(
                    text =
                      when (booking.status) {
                        "in_progress" -> "In use"
                        "confirmed" -> "Booked"
                        else -> booking.status.replace("_", " ").replaceFirstChar { it.uppercase() }
                      },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OwnerMyTurfTab(
  turf: TurfListing,
  isBusy: Boolean,
  onSaveTurfDetails: (TurfDetailsSubmission, slotMinutes: Long) -> Unit,
) {
  var slotMinutes by remember(turf.id, turf.slotMinutes) {
    mutableLongStateOf(turf.effectiveSlotMinutes())
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Spacer(modifier = Modifier.size(28.dp))
      Text(
        text = "Turf Details",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
      )
      Icon(
        imageVector = Icons.Filled.HomeWork,
        contentDescription = null,
        tint = CrimsonGlow,
        modifier = Modifier.size(24.dp),
      )
    }

    TurfDetailsFormContent(
      initialTurfName = turf.name,
      initialAddress = turf.city.ifBlank { turf.area },
      initialLatitude = turf.latitude,
      initialLongitude = turf.longitude,
      initialPricePerHour = turf.pricePerHour,
      initialFormat = turf.primaryFormat,
      initialFacilities = turf.amenities,
      initialOpenTime = turf.effectiveOpenTime(),
      initialCloseTime = turf.effectiveCloseTime(),
      initialPhotos = turf.photos,
      initialDescription = turf.description,
      isBusy = isBusy,
      submitButtonLabel = "Save and Publish",
      extraBeforeSubmit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Slot Length",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(60L, 90L).forEach { mins ->
              val selected = slotMinutes == mins
              Surface(
                color = if (selected) CrimsonRed else ObsidianCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (selected) CrimsonGlow else ObsidianBorder),
                modifier =
                  Modifier.weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { slotMinutes = mins }
                    .testTag("slot_length_$mins"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "$mins min",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) Color.White else TextPrimary,
                  )
                }
              }
            }
          }
        }
      },
      onSubmit = { submission ->
        onSaveTurfDetails(submission, slotMinutes)
      },
    )

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun OwnerProfileTab(
  profile: PlayerProfile,
  turf: TurfListing,
  onSignOut: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      FootballBrandLogo(size = 44.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = profile.displayName,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )
        Text(
          text = "${profile.city} • Verified Owner",
          style = MaterialTheme.typography.bodySmall,
          color = EmeraldOpen,
        )
      }
    }

    Surface(
      color = ObsidianCard,
      shape = RoundedCornerShape(18.dp),
      border = BorderStroke(1.dp, ObsidianBorder),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier.size(44.dp).clip(CircleShape).background(EmeraldSubtle),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = null,
            tint = EmeraldOpen,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = turf.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
          )
          Text(
            text = profile.contactPhone.ifBlank { turf.contactPhone.ifBlank { turf.city } },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
      }
    }

    OutlinedButton(
      onClick = onSignOut,
      border = BorderStroke(1.dp, ObsidianBorder),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier.fillMaxWidth().height(50.dp).testTag("owner_sign_out_button"),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
        contentDescription = null,
        tint = TextPrimary,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("Sign Out", color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
  }
}
