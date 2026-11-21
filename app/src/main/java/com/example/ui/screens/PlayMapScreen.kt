package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.model.OsmPitch
import com.example.data.model.OsmPlaceSuggestion
import com.example.data.model.TurfVenue
import com.example.ui.components.FootballBrandLogo
import com.example.ui.components.FootballOsmMap
import com.example.ui.components.OpenStatusPill
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.rememberOsmMapController
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldOpen
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.OsmMapState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun PlayMapScreen(
  searchQuery: String,
  selectedFilter: String,
  selectedTurfId: String,
  favoriteTurfIds: Set<String>,
  unreadNotificationsCount: Int,
  osmState: OsmMapState,
  onSearchQueryChange: (String) -> Unit,
  onSelectFilter: (String) -> Unit,
  onSelectTurf: (String) -> Unit,
  onSelectOsmPitch: (String) -> Unit,
  onSelectOsmPlace: (OsmPlaceSuggestion) -> Unit,
  onViewportBoundsChanged: (south: Double, west: Double, north: Double, east: Double) -> Unit,
  onUseDeviceCoordinates: (lat: Double, lon: Double, label: String, isGps: Boolean) -> Unit,
  onRecenterAnchor: () -> Unit,
  onOpenTurfDetail: (TurfVenue) -> Unit,
  onHostChallengeAtPitch: (String) -> Unit,
  onToggleFavoriteTurf: (TurfVenue, Boolean) -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenTournaments: () -> Unit,
  onOpenTeams: () -> Unit,
  modifier: Modifier = Modifier,
  verifiedTurfs: List<TurfVenue> = GambianFootballCatalog.turfs,
) {
  val context = LocalContext.current
  val mapController = rememberOsmMapController()
  val filters = listOf("All Turfs", "Nearby", "OSM Pitches", "Academy", "Tournaments", "Teams")
  var showPlacesDropdown by remember { mutableStateOf(false) }

  @SuppressLint("MissingPermission")
  fun fetchCoarseLocationOrRecenter() {
    val hasCoarse =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (!hasCoarse) {
      onRecenterAnchor()
      return
    }
    runCatching {
      val client = LocationServices.getFusedLocationProviderClient(context)
      client
        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { location ->
          if (location != null) {
            onUseDeviceCoordinates(
              location.latitude,
              location.longitude,
              "Your Location",
              true,
            )
          } else {
            onRecenterAnchor()
          }
        }
        .addOnFailureListener { onRecenterAnchor() }
    }.onFailure { onRecenterAnchor() }
  }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        fetchCoarseLocationOrRecenter()
      } else {
        onRecenterAnchor()
      }
    }

  val filteredTurfs =
    remember(
      searchQuery,
      selectedFilter,
      osmState.playerAnchorLat,
      osmState.playerAnchorLon,
      verifiedTurfs,
    ) {
      if (selectedFilter == "OSM Pitches") {
        emptyList()
      } else {
        verifiedTurfs.filter { turf ->
          val matchesQuery =
            searchQuery.isBlank() ||
              turf.name.contains(searchQuery, ignoreCase = true) ||
              turf.area.contains(searchQuery, ignoreCase = true) ||
              turf.city.contains(searchQuery, ignoreCase = true)
          val distKm =
            GambianFootballCatalog.haversineDistanceKm(
              osmState.playerAnchorLat,
              osmState.playerAnchorLon,
              turf.latitude,
              turf.longitude,
            )
          val matchesFilter =
            when (selectedFilter) {
              "Nearby" -> distKm <= 5.0
              "Academy" -> turf.category == "Academy"
              else -> true
            }
          matchesQuery && matchesFilter
        }
      }
    }

  val filteredOsmPitches =
    remember(searchQuery, selectedFilter, osmState.osmPitches) {
      if (selectedFilter == "Academy") {
        emptyList()
      } else {
        osmState.osmPitches.filter { pitch ->
          searchQuery.isBlank() ||
            pitch.name.contains(searchQuery, ignoreCase = true) ||
            pitch.area.contains(searchQuery, ignoreCase = true) ||
            pitch.surface.contains(searchQuery, ignoreCase = true)
        }
      }
    }

  val activeOsmPitch: OsmPitch? =
    remember(osmState.selectedOsmPitchId, filteredOsmPitches, selectedFilter) {
      val explicit =
        osmState.selectedOsmPitchId?.let { id ->
          filteredOsmPitches.firstOrNull { it.id == id }
        }
      explicit ?: if (selectedFilter == "OSM Pitches") filteredOsmPitches.firstOrNull() else null
    }

  val activeTurf =
    remember(selectedTurfId, filteredTurfs, verifiedTurfs) {
      filteredTurfs.firstOrNull { it.id == selectedTurfId }
        ?: filteredTurfs.firstOrNull()
        ?: verifiedTurfs.first()
    }

  Column(
    modifier = modifier.fillMaxSize().background(ObsidianBlack).statusBarsPadding()
  ) {
    // Top Brand + Search Bar
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FootballBrandLogo(size = 34.dp)
      Spacer(modifier = Modifier.width(6.dp))
      Column {
        Text(
          text = "Football",
          style =
            MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              lineHeight = 14.sp,
            ),
          color = TextPrimary,
        )
        Text(
          text = "Hub",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              lineHeight = 12.sp,
            ),
          color = CrimsonRed,
        )
      }
      Spacer(modifier = Modifier.width(10.dp))

      // Search input pill with OSM Nominatim place autocomplete
      Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.weight(1f).height(42.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search turfs or Gambian places",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (searchQuery.isEmpty()) {
              Text(
                text = "Search turfs, Gambian areas...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            BasicTextField(
              value = searchQuery,
              onValueChange = {
                onSearchQueryChange(it)
                showPlacesDropdown = it.trim().length >= 2
              },
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
              cursorBrush = SolidColor(CrimsonRed),
              modifier = Modifier.fillMaxWidth().testTag("play_search_input"),
            )
          }
          if (osmState.isSearchingPlaces) {
            CircularProgressIndicator(
              color = CrimsonRed,
              strokeWidth = 2.dp,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
          }
          if (searchQuery.isNotEmpty()) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "Clear search",
              tint = TextSecondary,
              modifier =
                Modifier.size(18.dp)
                  .clip(CircleShape)
                  .clickable {
                    onSearchQueryChange("")
                    showPlacesDropdown = false
                  },
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(6.dp))
      Box {
        IconButton(onClick = onOpenNotifications, modifier = Modifier.size(38.dp)) {
          Icon(
            imageVector = Icons.Filled.NotificationsNone,
            contentDescription = "Notifications",
            tint = TextPrimary,
          )
        }
        if (unreadNotificationsCount > 0) {
          Box(
            modifier =
              Modifier.align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(CrimsonRed)
          )
        }
      }
    }

    // Filter Chips Row
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 14.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      filters.forEach { filter ->
        val isSelected = selectedFilter == filter
        val chipAccent =
          if (filter == "OSM Pitches" && isSelected) Color(0xFF16A34A)
          else if (isSelected) CrimsonRed
          else ObsidianCard
        val chipBorder =
          if (filter == "OSM Pitches" && isSelected) EmeraldOpen
          else if (isSelected) CrimsonGlow
          else ObsidianBorder
        Surface(
          color = chipAccent,
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, chipBorder),
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .clickable {
                when (filter) {
                  "Tournaments" -> onOpenTournaments()
                  "Teams" -> onOpenTeams()
                  else -> onSelectFilter(filter)
                }
              }
              .testTag("map_filter_$filter"),
        ) {
          Text(
            text = filter,
            style =
              MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              ),
            color = if (isSelected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Interactive OpenStreetMap MapLibre Container + Floating Overlays
    Box(modifier = Modifier.fillMaxSize()) {
      FootballOsmMap(
        turfs = filteredTurfs,
        osmPitches = filteredOsmPitches,
        selectedTurfId = activeTurf.id,
        selectedOsmPitchId = activeOsmPitch?.id,
        playerAnchorLat = osmState.playerAnchorLat,
        playerAnchorLon = osmState.playerAnchorLon,
        cameraTarget = osmState.cameraTarget,
        controller = mapController,
        onSelectTurf = onSelectTurf,
        onSelectOsmPitch = onSelectOsmPitch,
        onViewportBoundsChanged = onViewportBoundsChanged,
        modifier = Modifier.fillMaxSize().testTag("play_osm_map"),
      )

      // Top-left Live OpenStreetMap Data Badge + Attribution
      Row(
        modifier =
          Modifier.align(Alignment.TopStart)
            .padding(start = 12.dp, top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = ObsidianCard.copy(alpha = 0.90f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, ObsidianBorder),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (osmState.isLoadingPitches) {
              CircularProgressIndicator(
                color = EmeraldOpen,
                strokeWidth = 2.dp,
                modifier = Modifier.size(12.dp),
              )
            } else {
              Box(
                modifier =
                  Modifier.size(8.dp)
                    .clip(CircleShape)
                    .background(EmeraldOpen)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text =
                if (osmState.isLoadingPitches) "Syncing OSM pitches..."
                else "OSM • ${filteredOsmPitches.size} pitches",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = TextPrimary,
            )
          }
        }

        Surface(
          color = ObsidianBlack.copy(alpha = 0.78f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(0.5.dp, ObsidianBorder),
        ) {
          Text(
            text = "© OpenStreetMap",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          )
        }
      }

      // Nominatim Gambian Place Autocomplete Dropdown
      androidx.compose.animation.AnimatedVisibility(
        visible = showPlacesDropdown && osmState.placeSuggestions.isNotEmpty(),
        modifier =
          Modifier.align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
      ) {
        Surface(
          color = ObsidianCardElevated.copy(alpha = 0.97f),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(16.dp)),
        ) {
          Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
              text = "OpenStreetMap Places (The Gambia)",
              style = MaterialTheme.typography.labelSmall,
              color = GoldTrophy,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
            osmState.placeSuggestions.take(4).forEachIndexed { index, suggestion ->
              if (index > 0) {
                HorizontalDivider(color = ObsidianBorder, thickness = 0.5.dp)
              }
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable {
                      showPlacesDropdown = false
                      onSelectOsmPlace(suggestion)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.Public,
                  contentDescription = null,
                  tint = CrimsonRed,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = suggestion.shortName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "${suggestion.areaType} • ${suggestion.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }

      // Map zoom / recenter controls on right edge
      Column(
        modifier =
          Modifier.align(Alignment.CenterEnd)
            .padding(end = 12.dp)
            .offset(y = (-42).dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        MapControlButton(
          icon = Icons.Filled.MyLocation,
          desc = "Recenter on my location",
          onClick = {
            val hasCoarse =
              ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
              ) == PackageManager.PERMISSION_GRANTED
            if (hasCoarse) {
              fetchCoarseLocationOrRecenter()
            } else {
              locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
          },
        )
        MapControlButton(
          icon = Icons.Filled.Add,
          desc = "Zoom in",
          onClick = { mapController.zoomIn() },
        )
        MapControlButton(
          icon = Icons.Filled.Remove,
          desc = "Zoom out",
          onClick = { mapController.zoomOut() },
        )
      }

      // Bottom Floating Active Card (Verified Turf OR OpenStreetMap Community Pitch)
      Column(
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (activeOsmPitch != null) {
          val distKm =
            GambianFootballCatalog.haversineDistanceKm(
              osmState.playerAnchorLat,
              osmState.playerAnchorLon,
              activeOsmPitch.latitude,
              activeOsmPitch.longitude,
            )
          val walkMin = GambianFootballCatalog.estimateWalkMinutes(distKm)
          OsmPitchPreviewCard(
            pitch = activeOsmPitch,
            distanceKm = distKm,
            walkMinutes = walkMin,
            onHostChallenge = { onHostChallengeAtPitch(activeOsmPitch.name) },
            onSwitchToVerified = {
              onSelectFilter("All Turfs")
              onSelectTurf(activeTurf.id)
            },
          )
        } else {
          val isFav = favoriteTurfIds.contains(activeTurf.id)
          val distKm =
            GambianFootballCatalog.haversineDistanceKm(
              osmState.playerAnchorLat,
              osmState.playerAnchorLon,
              activeTurf.latitude,
              activeTurf.longitude,
            )
          val walkMin = GambianFootballCatalog.estimateWalkMinutes(distKm)

          VerifiedTurfPreviewCard(
            turf = activeTurf,
            distanceKm = distKm,
            walkMinutes = walkMin,
            isFavorited = isFav,
            onToggleFavorite = { onToggleFavoriteTurf(activeTurf, isFav) },
            onOpenDetail = { onOpenTurfDetail(activeTurf) },
          )
        }

        // Nearby Turfs + OSM summary bar
        val totalDisplayed = filteredTurfs.size + filteredOsmPitches.size
        Surface(
          color = ObsidianCard.copy(alpha = 0.94f),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, ObsidianBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "$totalDisplayed",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (activeOsmPitch != null) EmeraldOpen else CrimsonRed,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  "${filteredTurfs.size} Bookable • ${filteredOsmPitches.size} OSM around ${osmState.playerAnchorLabel}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            if (activeOsmPitch == null) {
              Text(
                text = "D${activeTurf.pricePerHour}/hr",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = EmeraldOpen,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VerifiedTurfPreviewCard(
  turf: TurfVenue,
  distanceKm: Double,
  walkMinutes: Int,
  isFavorited: Boolean,
  onToggleFavorite: () -> Unit,
  onOpenDetail: () -> Unit,
) {
  Surface(
    color = ObsidianCard.copy(alpha = 0.96f),
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.45f)),
    modifier =
      Modifier.fillMaxWidth()
        .shadow(16.dp, RoundedCornerShape(22.dp))
        .clickable(onClick = onOpenDetail)
        .testTag("map_active_turf_card"),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(92.dp, 86.dp).clip(RoundedCornerShape(14.dp))) {
          Image(
            painter = painterResource(id = R.drawable.img_turf_night),
            contentDescription = turf.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          OpenStatusPill(
            isOpen = turf.isOpen,
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Text(
                text = turf.name,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (turf.isVerified) {
                Spacer(modifier = Modifier.width(4.dp))
                VerifiedBadge()
              }
            }
            IconButton(
              onClick = onToggleFavorite,
              modifier = Modifier.size(32.dp),
            ) {
              Icon(
                imageVector =
                  if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite turf",
                tint = if (isFavorited) CrimsonRed else TextSecondary,
                modifier = Modifier.size(19.dp),
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Star,
              contentDescription = null,
              tint = GoldTrophy,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${turf.rating} (${turf.reviewCountLabel})",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }

          Spacer(modifier = Modifier.height(3.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = null,
              tint = TextMuted,
              modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "$distanceKm km • ${turf.area}",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
              color = TextSecondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
              contentDescription = null,
              tint = TextMuted,
              modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = "$walkMinutes min",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
              color = TextSecondary,
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              AmenityMiniPill(turf.primaryFormat)
              AmenityMiniPill("Floodlights")
            }
            Button(
              onClick = onOpenDetail,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = CrimsonRed,
                  contentColor = Color.White,
                ),
              shape = RoundedCornerShape(20.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.height(34.dp).testTag("map_view_details_button"),
            ) {
              Text(
                text = "View Details",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OsmPitchPreviewCard(
  pitch: OsmPitch,
  distanceKm: Double,
  walkMinutes: Int,
  onHostChallenge: () -> Unit,
  onSwitchToVerified: () -> Unit,
) {
  Surface(
    color = ObsidianCard.copy(alpha = 0.96f),
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, EmeraldOpen.copy(alpha = 0.55f)),
    modifier =
      Modifier.fillMaxWidth()
        .shadow(16.dp, RoundedCornerShape(22.dp))
        .testTag("map_active_osm_pitch_card"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = EmeraldOpen.copy(alpha = 0.16f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(0.5.dp, EmeraldOpen.copy(alpha = 0.5f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Public,
              contentDescription = null,
              tint = EmeraldOpen,
              modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "OpenStreetMap Pitch",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = EmeraldOpen,
            )
          }
        }
        Text(
          text = "Bookable Turfs →",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = CrimsonRed,
          modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onSwitchToVerified).padding(4.dp),
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.SportsSoccer,
          contentDescription = null,
          tint = EmeraldOpen,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = pitch.name,
          style = MaterialTheme.typography.titleLarge,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.LocationOn,
          contentDescription = null,
          tint = TextMuted,
          modifier = Modifier.size(13.dp),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          text = "$distanceKm km • ${pitch.area}",
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
          color = TextSecondary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
          contentDescription = null,
          tint = TextMuted,
          modifier = Modifier.size(13.dp),
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
          text = "$walkMinutes min",
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
          color = TextSecondary,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          AmenityMiniPill(pitch.surface)
          AmenityMiniPill(if (pitch.isFloodlit) "Floodlit" else pitch.accessLabel)
        }
        Button(
          onClick = onHostChallenge,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = CrimsonRed,
              contentColor = Color.White,
            ),
          shape = RoundedCornerShape(20.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.height(34.dp).testTag("map_osm_host_challenge_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Host Challenge",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
          )
        }
      }
    }
  }
}

@Composable
private fun MapControlButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  desc: String,
  onClick: () -> Unit,
) {
  Surface(
    color = ObsidianCardElevated.copy(alpha = 0.95f),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, ObsidianBorder),
    modifier =
      Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = desc,
        tint = TextPrimary,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun AmenityMiniPill(label: String) {
  Surface(
    color = ObsidianCardElevated,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(0.5.dp, ObsidianBorder),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
      color = TextSecondary,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
    )
  }
}
