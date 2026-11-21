package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.catalog.GambianSlotTimeHelper
import com.example.data.catalog.LiveTurfBadgeState
import com.example.data.catalog.TurfLiveStatusInfo
import com.example.data.catalog.TurfPhotoCompressor
import com.example.data.model.GeoPoint
import com.example.data.model.TurfListing
import com.example.data.model.TurfSlot
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay

private enum class HomeFeedFilter(val label: String) {
  ALL("All"),
  FREE_NOW("Free now"),
  TOURNAMENTS("Tournaments"),
}

private val LiveGreen = Color(0xFF22C55E)
private val LiveOrange = Color(0xFFF97316)
private val LiveRed = Color(0xFFEF4444)
private val LiveAmber = Color(0xFFF59E0B)
private val LiveGrey = Color(0xFF9CA3AF)
private val VerifiedBlue = Color(0xFF38BDF8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  verifiedListingsState: UiState<List<TurfListing>>,
  todaySlotsByTurf: Map<String, List<TurfSlot>>,
  favoriteTurfIds: Set<String>,
  selectedCity: String,
  unreadNotificationsCount: Int,
  isRefreshing: Boolean,
  refreshNonce: Long,
  onSelectCity: (String) -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenTurfDetail: (TurfListing) -> Unit,
  onToggleFavoriteTurf: (TurfListing, Boolean) -> Unit,
  onCollectVisibleTurfSlots: suspend (String) -> Unit,
  onPrefetchFreeNowSlots: (List<TurfListing>) -> Unit,
  onRefresh: (Set<String>) -> Unit,
  onOpenCompetitions: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var cityMenuExpanded by remember { mutableStateOf(false) }
  var selectedAreaFilter by rememberSaveable { mutableStateOf("All Areas") }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var activeFilter by rememberSaveable { mutableStateOf(HomeFeedFilter.ALL) }
  var playerLocation by remember { mutableStateOf<GeoPoint?>(null) }

  var hasLocationPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    )
  }

  @SuppressLint("MissingPermission")
  fun fetchDeviceLocationIfPermitted() {
    val granted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    hasLocationPermission = granted
    if (!granted) {
      playerLocation = null
      return
    }
    runCatching {
      LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { loc ->
          if (loc != null) {
            playerLocation = GeoPoint(loc.latitude, loc.longitude)
          }
        }
    }
  }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasLocationPermission = granted
      if (granted) {
        fetchDeviceLocationIfPermitted()
      }
    }

  LaunchedEffect(refreshNonce) {
    fetchDeviceLocationIfPermitted()
  }

  // Single 1-second ticker for all visible "In use" countdowns
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000L)
      nowMillis = System.currentTimeMillis()
    }
  }

  val allVerifiedListings =
    remember(verifiedListingsState) {
      (verifiedListingsState as? UiState.Success)?.data
        ?.filter { it.verificationStatus == "verified" }
        .orEmpty()
    }

  // When "Free now" filter is active, fetch once for any turfs not yet in todaySlotsByTurf
  LaunchedEffect(activeFilter, allVerifiedListings) {
    if (activeFilter == HomeFeedFilter.FREE_NOW && allVerifiedListings.isNotEmpty()) {
      onPrefetchFreeNowSlots(allVerifiedListings)
    }
  }

  val distanceByTurfId =
    remember(allVerifiedListings, playerLocation) {
      val loc = playerLocation ?: return@remember emptyMap()
      allVerifiedListings.associate { turf ->
        turf.id to
          GambianFootballCatalog.haversineDistanceKm(
            loc.latitude,
            loc.longitude,
            turf.latitude,
            turf.longitude,
          )
      }
    }

  val liveStatusByTurfId =
    remember(allVerifiedListings, todaySlotsByTurf, nowMillis) {
      allVerifiedListings.associate { turf ->
        turf.id to
          GambianSlotTimeHelper.computeLiveStatusForTurf(
            openTimeRaw = turf.effectiveOpenTime(),
            closeTimeRaw = turf.effectiveCloseTime(),
            slotMinutesRaw = turf.effectiveSlotMinutes(),
            isOpen = turf.isOpen,
            todaySlots = todaySlotsByTurf[turf.id].orEmpty(),
            nowMillis = nowMillis,
          )
      }
    }

  val displayedListings =
    remember(
      allVerifiedListings,
      selectedAreaFilter,
      searchQuery,
      activeFilter,
      distanceByTurfId,
      liveStatusByTurfId,
    ) {
      val q = searchQuery.trim()
      val filtered =
        allVerifiedListings.filter { turf ->
          val matchesArea =
            selectedAreaFilter == "All Areas" ||
              turf.area.contains(selectedAreaFilter, ignoreCase = true) ||
              turf.city.contains(selectedAreaFilter, ignoreCase = true)
          val matchesQuery =
            q.isEmpty() ||
              turf.name.contains(q, ignoreCase = true) ||
              turf.area.contains(q, ignoreCase = true) ||
              turf.city.contains(q, ignoreCase = true)
          val matchesChip =
            when (activeFilter) {
              HomeFeedFilter.ALL -> true
              HomeFeedFilter.FREE_NOW ->
                liveStatusByTurfId[turf.id]?.state == LiveTurfBadgeState.FREE_NOW
              HomeFeedFilter.TOURNAMENTS -> false // Tournaments not yet linked to turfs in DB
            }
          matchesArea && matchesQuery && matchesChip
        }

      if (playerLocation != null && distanceByTurfId.isNotEmpty()) {
        filtered.sortedBy { distanceByTurfId[it.id] ?: Double.MAX_VALUE }
      } else {
        filtered.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
      }
    }

  val lazyListState = rememberLazyListState()
  val visibleTurfIds by
    remember {
      derivedStateOf {
        lazyListState.layoutInfo.visibleItemsInfo
          .mapNotNull { item ->
            val keyStr = item.key as? String
            if (keyStr != null && keyStr.startsWith("turf_")) keyStr.removePrefix("turf_")
            else null
          }
          .toSet()
      }
    }

  val pullToRefreshState = rememberPullToRefreshState()

  Column(
    modifier = modifier.fillMaxSize().background(ObsidianBlack).statusBarsPadding(),
  ) {
    // Fixed Top Controls: Location Header + Search Bar + Filter Chips
    Column(
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
    ) {
      // Row 1: Location dropdown + Notification Bell
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box {
          Row(
            modifier =
              Modifier.clip(RoundedCornerShape(12.dp))
                .clickable { cityMenuExpanded = true }
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .testTag("home_city_selector"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = "Selected area",
              tint = CrimsonRed,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text =
                when {
                  selectedAreaFilter != "All Areas" -> selectedAreaFilter
                  playerLocation != null -> "Near You"
                  else -> selectedCity.ifBlank { "Banjul" }
                },
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Filled.KeyboardArrowDown,
              contentDescription = "Change location",
              tint = TextSecondary,
              modifier = Modifier.size(20.dp),
            )
          }

          DropdownMenu(
            expanded = cityMenuExpanded,
            onDismissRequest = { cityMenuExpanded = false },
            modifier = Modifier.background(ObsidianCardElevated),
          ) {
            DropdownMenuItem(
              text = {
                Text(
                  text = "All of The Gambia",
                  color = if (selectedAreaFilter == "All Areas") CrimsonRed else TextPrimary,
                  fontWeight =
                    if (selectedAreaFilter == "All Areas") FontWeight.Bold else FontWeight.Normal,
                )
              },
              onClick = {
                cityMenuExpanded = false
                selectedAreaFilter = "All Areas"
              },
            )
            DropdownMenuItem(
              leadingIcon = {
                Icon(
                  imageVector = Icons.Filled.MyLocation,
                  contentDescription = null,
                  tint = CrimsonRed,
                  modifier = Modifier.size(18.dp),
                )
              },
              text = {
                Text(
                  text = if (hasLocationPermission) "Refresh GPS distance" else "Use my GPS location",
                  color = TextPrimary,
                )
              },
              onClick = {
                cityMenuExpanded = false
                selectedAreaFilter = "All Areas"
                if (hasLocationPermission) {
                  fetchDeviceLocationIfPermitted()
                } else {
                  locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
              },
            )
            HorizontalDivider(color = ObsidianBorder, thickness = 0.5.dp)
            GambianFootballCatalog.cities.forEach { city ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = city,
                    color = if (selectedAreaFilter == city) CrimsonRed else TextPrimary,
                    fontWeight =
                      if (selectedAreaFilter == city) FontWeight.Bold else FontWeight.Normal,
                  )
                },
                onClick = {
                  cityMenuExpanded = false
                  selectedAreaFilter = city
                  onSelectCity(city)
                },
              )
            }
          }
        }

        Box(modifier = Modifier.padding(end = 2.dp)) {
          IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier.testTag("home_notifications_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.NotificationsNone,
              contentDescription = "Notifications",
              tint = TextPrimary,
              modifier = Modifier.size(24.dp),
            )
          }
          if (unreadNotificationsCount > 0) {
            Box(
              modifier =
                Modifier.align(Alignment.TopEnd)
                  .padding(top = 6.dp, end = 6.dp)
                  .size(17.dp)
                  .clip(CircleShape)
                  .background(CrimsonRed),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = unreadNotificationsCount.coerceAtMost(9).toString(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Row 2: Search Bar
      Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(44.dp)
            .testTag("home_search_input"),
      ) {
        Row(
          modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search",
            tint = TextMuted,
            modifier = Modifier.size(19.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (searchQuery.isEmpty()) {
              Text(
                text = "Search turfs or areas...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
              )
            }
            BasicTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
              cursorBrush = SolidColor(CrimsonRed),
              modifier = Modifier.fillMaxWidth(),
            )
          }
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { searchQuery = "" },
              modifier = Modifier.size(28.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Clear search",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Row 3: Filter Chips (All, Free now, Tournaments)
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        HomeFilterChip(
          label = "All",
          selected = activeFilter == HomeFeedFilter.ALL,
          onClick = { activeFilter = HomeFeedFilter.ALL },
          testTag = "home_filter_all",
        )
        HomeFilterChip(
          label = "Free now",
          selected = activeFilter == HomeFeedFilter.FREE_NOW,
          onClick = { activeFilter = HomeFeedFilter.FREE_NOW },
          leadingDotColor = LiveGreen,
          testTag = "home_filter_free_now",
        )
        HomeFilterChip(
          label = "Tournaments",
          selected = activeFilter == HomeFeedFilter.TOURNAMENTS,
          onClick = { activeFilter = HomeFeedFilter.TOURNAMENTS },
          leadingTrophy = true,
          testTag = "home_filter_tournaments",
        )
      }
    }

    // Feed Content inside PullToRefreshBox
    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = { onRefresh(visibleTurfIds) },
      state = pullToRefreshState,
      indicator = {
        PullToRefreshDefaults.Indicator(
          state = pullToRefreshState,
          isRefreshing = isRefreshing,
          containerColor = ObsidianCardElevated,
          color = CrimsonRed,
          modifier = Modifier.align(Alignment.TopCenter),
        )
      },
      modifier = Modifier.fillMaxSize(),
    ) {
      when (verifiedListingsState) {
        is UiState.Loading -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 104.dp),
          ) {
            items(3) {
              HomeFeedSkeletonItem()
              HorizontalDivider(
                color = ObsidianBorder.copy(alpha = 0.45f),
                thickness = 0.5.dp,
              )
            }
          }
        }
        is UiState.Error -> {
          Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Filled.SportsSoccer,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(44.dp),
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Could not load turfs",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = verifiedListingsState.message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
              )
              Spacer(modifier = Modifier.height(14.dp))
              Button(
                onClick = { onRefresh(visibleTurfIds) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                shape = RoundedCornerShape(10.dp),
              ) {
                Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
        is UiState.Success -> {
          if (displayedListings.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
              contentAlignment = Alignment.Center,
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.testTag("home_empty_state"),
              ) {
                Icon(
                  imageVector =
                    if (activeFilter == HomeFeedFilter.TOURNAMENTS) Icons.Filled.EmojiEvents
                    else Icons.Filled.SportsSoccer,
                  contentDescription = null,
                  tint = if (activeFilter == HomeFeedFilter.TOURNAMENTS) GoldTrophy else TextMuted,
                  modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text =
                    when {
                      activeFilter == HomeFeedFilter.TOURNAMENTS ->
                        "No turf tournaments linked yet"
                      searchQuery.isNotBlank() ||
                        selectedAreaFilter != "All Areas" ||
                        activeFilter == HomeFeedFilter.FREE_NOW ->
                        "No matching turfs found"
                      else -> "No turfs near you yet"
                    },
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  color = TextPrimary,
                  textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text =
                    when {
                      activeFilter == HomeFeedFilter.TOURNAMENTS ->
                        "Tournaments are currently listed in Competitions until turfs link their events."
                      searchQuery.isNotBlank() ||
                        selectedAreaFilter != "All Areas" ||
                        activeFilter == HomeFeedFilter.FREE_NOW ->
                        "Try clearing your search or switching back to All turfs."
                      else -> "Verified turfs added in your area will appear here automatically."
                    },
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                  textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (activeFilter == HomeFeedFilter.TOURNAMENTS) {
                  Button(
                    onClick = onOpenCompetitions,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                    shape = RoundedCornerShape(10.dp),
                  ) {
                    Text("Open Competitions", color = Color.White, fontWeight = FontWeight.Bold)
                  }
                } else if (
                  searchQuery.isNotBlank() ||
                    selectedAreaFilter != "All Areas" ||
                    activeFilter == HomeFeedFilter.FREE_NOW
                ) {
                  Button(
                    onClick = {
                      searchQuery = ""
                      selectedAreaFilter = "All Areas"
                      activeFilter = HomeFeedFilter.ALL
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                    shape = RoundedCornerShape(10.dp),
                  ) {
                    Text("Show All Turfs", color = Color.White, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          } else {
            LazyColumn(
              state = lazyListState,
              modifier = Modifier.fillMaxSize().testTag("home_verified_feed"),
              contentPadding = PaddingValues(bottom = 104.dp),
            ) {
              items(
                items = displayedListings,
                key = { turf -> "turf_${turf.id}" },
              ) { turf ->
                val isVisible = visibleTurfIds.contains(turf.id)
                LaunchedEffect(turf.id, isVisible, refreshNonce) {
                  if (isVisible) {
                    onCollectVisibleTurfSlots(turf.id)
                  }
                }

                val statusInfo =
                  liveStatusByTurfId[turf.id]
                    ?: GambianSlotTimeHelper.computeLiveStatusForTurf(
                      openTimeRaw = turf.effectiveOpenTime(),
                      closeTimeRaw = turf.effectiveCloseTime(),
                      slotMinutesRaw = turf.effectiveSlotMinutes(),
                      isOpen = turf.isOpen,
                      todaySlots = emptyList(),
                      nowMillis = nowMillis,
                    )
                val todaySlots = todaySlotsByTurf[turf.id].orEmpty()

                InstagramStyleTurfFeedItem(
                  turf = turf,
                  distanceKm = distanceByTurfId[turf.id],
                  statusInfo = statusInfo,
                  todaySlots = todaySlots,
                  nowMillis = nowMillis,
                  isSaved = favoriteTurfIds.contains(turf.id),
                  onOpenDetail = { onOpenTurfDetail(turf) },
                  onToggleSave = {
                    onToggleFavoriteTurf(turf, favoriteTurfIds.contains(turf.id))
                  },
                )
                HorizontalDivider(
                  color = ObsidianBorder.copy(alpha = 0.45f),
                  thickness = 0.5.dp,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HomeFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
  leadingDotColor: Color? = null,
  leadingTrophy: Boolean = false,
) {
  Surface(
    color = if (selected) CrimsonRed else ObsidianCard,
    shape = RoundedCornerShape(50),
    border =
      if (selected) null
      else BorderStroke(1.dp, ObsidianBorder),
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (leadingDotColor != null) {
        Box(
          modifier =
            Modifier.size(8.dp)
              .clip(CircleShape)
              .background(if (selected) Color.White else leadingDotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
      } else if (leadingTrophy) {
        Icon(
          imageVector = Icons.Filled.EmojiEvents,
          contentDescription = null,
          tint = if (selected) Color.White else GoldTrophy,
          modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = label,
        style =
          MaterialTheme.typography.labelLarge.copy(
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
          ),
        color = if (selected) Color.White else TextPrimary,
      )
    }
  }
}

@Composable
private fun InstagramStyleTurfFeedItem(
  turf: TurfListing,
  distanceKm: Double?,
  statusInfo: TurfLiveStatusInfo,
  todaySlots: List<TurfSlot>,
  nowMillis: Long,
  isSaved: Boolean,
  onOpenDetail: () -> Unit,
  onToggleSave: () -> Unit,
) {
  // Decode ONLY cover photo (and logo if set) in feed to preserve slow/expensive mobile data
  val coverDataUri = turf.photos.firstOrNull()
  val coverBitmap =
    remember(coverDataUri) {
      coverDataUri?.let { TurfPhotoCompressor.decodeDataUriToImageBitmap(it) }
    }
  val avatarBitmap =
    remember(turf.logoUrl, coverBitmap) {
      if (turf.logoUrl.isNotBlank()) {
        TurfPhotoCompressor.decodeDataUriToImageBitmap(turf.logoUrl) ?: coverBitmap
      } else {
        coverBitmap
      }
    }

  val statusColor =
    when (statusInfo.state) {
      LiveTurfBadgeState.FREE_NOW -> LiveGreen
      LiveTurfBadgeState.IN_USE -> LiveOrange
      LiveTurfBadgeState.BOOKED_NOW -> LiveAmber
      LiveTurfBadgeState.FULLY_BOOKED -> LiveRed
      LiveTurfBadgeState.CLOSED -> LiveGrey
    }

  // Slide 1 (Cover Photo) + Slide 2 (Live Status). No Slide 3 until DB links tournaments to turfs.
  val pagerState = rememberPagerState(pageCount = { 2 })

  Column(
    modifier = Modifier.fillMaxWidth().background(ObsidianBlack).testTag("home_turf_item_${turf.id}"),
  ) {
    // 1. Instagram-Style Header
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier =
          Modifier.weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onOpenDetail)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // 40dp circular profile picture with thin red ring
        Box(
          modifier =
            Modifier.size(40.dp)
              .clip(CircleShape)
              .border(1.5.dp, CrimsonRed, CircleShape)
              .background(ObsidianCard),
          contentAlignment = Alignment.Center,
        ) {
          if (avatarBitmap != null) {
            Image(
              bitmap = avatarBitmap,
              contentDescription = turf.name,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
          } else {
            Image(
              painter = painterResource(id = R.drawable.img_turf_night),
              contentDescription = turf.name,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = turf.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(5.dp))
            VerifiedBadge(tint = VerifiedBlue, modifier = Modifier.size(15.dp))
          }
          Spacer(modifier = Modifier.height(1.dp))
          val locationSubtitle =
            if (distanceKm != null) "${turf.area} · $distanceKm km"
            else turf.area
          Text(
            text = locationSubtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Status dot + tag + Bookmark icon
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = statusInfo.headerTag,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary,
        )
        IconButton(
          onClick = onToggleSave,
          modifier = Modifier.testTag("home_bookmark_${turf.id}"),
        ) {
          Icon(
            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (isSaved) "Remove bookmark" else "Bookmark turf",
            tint = if (isSaved) CrimsonRed else TextPrimary,
            modifier = Modifier.size(21.dp),
          )
        }
      }
    }

    // 2. Full-Width Edge-to-Edge Horizontal Swiper (No border, no card frame)
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxWidth().height(228.dp),
    ) { page ->
      if (page == 0) {
        // SLIDE 1: Cover Photo + concise turf info overlay at bottom edge
        Box(
          modifier =
            Modifier.fillMaxSize()
              .clickable(onClick = onOpenDetail)
              .testTag("home_slide_turf_${turf.id}"),
        ) {
          if (coverBitmap != null) {
            Image(
              bitmap = coverBitmap,
              contentDescription = turf.name,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Image(
              painter = painterResource(id = R.drawable.img_turf_night),
              contentDescription = turf.name,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          }

          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(74.dp)
                .align(Alignment.BottomCenter)
                .background(
                  Brush.verticalGradient(
                    listOf(Color.Transparent, ObsidianBlack.copy(alpha = 0.84f))
                  )
                )
          )

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                color = ObsidianBlack.copy(alpha = 0.7f),
                shape = RoundedCornerShape(6.dp),
              ) {
                Text(
                  text = turf.primaryFormat,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
              TurfFacilityIconsRow(amenities = turf.amenities)
            }
            Text(
              text = "Swipe for live status →",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.8f),
            )
          }
        }
      } else {
        // SLIDE 2: Live Status Built From Today's Slots (PII-free)
        TurfLiveStatusSlide(
          turf = turf,
          statusInfo = statusInfo,
          statusColor = statusColor,
          todaySlots = todaySlots,
          nowMillis = nowMillis,
          onClick = onOpenDetail,
        )
      }
    }

    // 3. Under Swiper: Slide Dots + Price per hour + "View and Book" Button
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        repeat(2) { index ->
          val active = pagerState.currentPage == index
          Box(
            modifier =
              Modifier.padding(horizontal = 3.dp)
                .size(if (active) 7.dp else 6.dp)
                .clip(CircleShape)
                .background(if (active) Color.White else TextMuted.copy(alpha = 0.55f))
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text =
              buildAnnotatedString {
                withStyle(
                  SpanStyle(
                    color = CrimsonRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                  )
                ) {
                  append("D${turf.pricePerHour}")
                }
                withStyle(
                  SpanStyle(
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                  )
                ) {
                  append("/hr")
                }
              },
          )
        }

        Button(
          onClick = onOpenDetail,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = CrimsonRed,
              contentColor = Color.White,
            ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
          modifier = Modifier.height(42.dp).testTag("home_view_book_${turf.id}"),
        ) {
          Text(
            text = "View and Book",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun TurfFacilityIconsRow(amenities: List<String>) {
  val icons =
    amenities
      .mapNotNull { item ->
        when {
          item.contains("Flood", true) -> Icons.Filled.WbSunny to "Floodlights"
          item.contains("Park", true) -> Icons.Filled.LocalParking to "Parking"
          item.contains("Changing", true) -> Icons.Filled.Checkroom to "Changing rooms"
          item.contains("Water", true) -> Icons.Filled.WaterDrop to "Water"
          item.contains("Seat", true) -> Icons.Filled.Chair to "Seating"
          else -> null
        }
      }
      .distinctBy { it.second }
      .take(4)

  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    icons.forEach { (icon, desc) ->
      Box(
        modifier =
          Modifier.size(24.dp)
            .clip(CircleShape)
            .background(ObsidianBlack.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = desc,
          tint = Color.White,
          modifier = Modifier.size(14.dp),
        )
      }
    }
  }
}

@Composable
private fun TurfLiveStatusSlide(
  turf: TurfListing,
  statusInfo: TurfLiveStatusInfo,
  statusColor: Color,
  todaySlots: List<TurfSlot>,
  nowMillis: Long,
  onClick: () -> Unit,
) {
  val timeline =
    remember(turf.id, turf.openTime, turf.closeTime, turf.slotMinutes, todaySlots, nowMillis) {
      GambianSlotTimeHelper.buildTodayDisplayTimeline(
        openTimeRaw = turf.effectiveOpenTime(),
        closeTimeRaw = turf.effectiveCloseTime(),
        slotMinutesRaw = turf.effectiveSlotMinutes(),
        todaySlots = todaySlots,
        nowMillis = nowMillis,
      )
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              statusColor.copy(alpha = 0.16f),
              Color(0xFF111218),
              ObsidianBlack,
            )
          )
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 16.dp)
        .testTag("home_slide_live_${turf.id}"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = statusColor.copy(alpha = 0.2f),
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = statusInfo.slideTag,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = statusColor,
            )
          }
        }

        Text(
          text = "Live Today · GMT",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }

      Column {
        if (statusInfo.state == LiveTurfBadgeState.IN_USE && statusInfo.endsAtMillis != null) {
          val remaining = (statusInfo.endsAtMillis - nowMillis).coerceAtLeast(0L)
          Text(
            text = "${GambianSlotTimeHelper.formatCountdownColon(remaining)} left",
            style =
              MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
              ),
            color = TextPrimary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
              text = statusInfo.primaryDetail,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = statusColor,
            )
            Text(
              text = statusInfo.secondaryDetail,
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
            )
          }
        } else {
          Text(
            text = statusInfo.primaryDetail,
            style =
              MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
              ),
            color = TextPrimary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = statusInfo.secondaryDetail,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
      }

      Column {
        if (timeline.isNotEmpty()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            timeline.take(10).forEach { slot ->
              val barColor =
                when (slot.status) {
                  "available" -> LiveGreen
                  "in_progress" -> LiveOrange
                  "booked" -> LiveRed
                  else -> LiveGrey
                }
              Box(
                modifier =
                  Modifier.weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor.copy(alpha = 0.85f))
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${GambianSlotTimeHelper.formatHhMmTo12Hour(turf.effectiveOpenTime())} – ${GambianSlotTimeHelper.formatHhMmTo12Hour(turf.effectiveCloseTime())}",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
          )
          Text(
            text = "Tap for today's full schedule →",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = CrimsonRed,
          )
        }
      }
    }
  }
}

@Composable
private fun HomeFeedSkeletonItem() {
  val transition = rememberInfiniteTransition(label = "home_skeleton")
  val alpha by
    transition.animateFloat(
      initialValue = 0.35f,
      targetValue = 0.7f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 850),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "skeleton_alpha",
    )
  val shimmer = ObsidianCardElevated.copy(alpha = alpha)

  Column(modifier = Modifier.fillMaxWidth().background(ObsidianBlack)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(shimmer))
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Box(
          modifier =
            Modifier.width(150.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmer)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
          modifier =
            Modifier.width(100.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(shimmer)
        )
      }
      Box(
        modifier =
          Modifier.width(64.dp).height(14.dp).clip(RoundedCornerShape(50)).background(shimmer)
      )
    }

    Box(modifier = Modifier.fillMaxWidth().height(228.dp).background(shimmer))

    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.width(86.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).background(shimmer)
      )
      Box(
        modifier =
          Modifier.width(136.dp).height(40.dp).clip(RoundedCornerShape(10.dp)).background(shimmer)
      )
    }
  }
}
