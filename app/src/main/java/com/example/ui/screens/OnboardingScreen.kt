package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.model.OsmPlaceSuggestion
import com.example.data.model.PlayerProfile
import com.example.ui.components.FootballBrandLogo
import com.example.ui.components.GambianFlagBadge
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
  initialProfile: PlayerProfile?,
  googleDisplayName: String?,
  isBusy: Boolean,
  placeSuggestions: List<OsmPlaceSuggestion>,
  isSearchingPlaces: Boolean,
  onSearchPlaces: (String) -> Unit,
  onClearPlaceSuggestions: () -> Unit,
  onCompleteOnboarding: (
    displayName: String,
    handle: String,
    city: String,
    position: String,
    favoriteTeamId: String,
    customLatitude: Double?,
    customLongitude: Double?,
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  var step by remember { mutableIntStateOf(0) } // 0 = Location, 1 = Profile

  var cityInput by remember(initialProfile) {
    mutableStateOf(initialProfile?.city?.ifBlank { "Banjul" } ?: "Banjul")
  }
  var pickedLatitude by remember { mutableStateOf<Double?>(null) }
  var pickedLongitude by remember { mutableStateOf<Double?>(null) }

  val defaultName =
    initialProfile?.displayName?.takeIf { it.isNotBlank() }
      ?: googleDisplayName?.takeIf { it.isNotBlank() }
      ?: "Drako"
  var displayName by remember(defaultName) { mutableStateOf(defaultName) }
  var handle by remember(defaultName) {
    mutableStateOf(
      initialProfile?.handle?.takeIf { it.isNotBlank() }
        ?: ("@" +
          defaultName.lowercase().replace(Regex("[^a-z0-9]"), "").ifEmpty { "player" }.take(16) +
          "427")
    )
  }
  var selectedPosition by remember(initialProfile) {
    mutableStateOf(initialProfile?.position?.ifBlank { "Forward" } ?: "Forward")
  }
  var selectedTeamId by remember(initialProfile) {
    mutableStateOf(initialProfile?.favoriteTeamId?.ifBlank { "brikama_warriors" } ?: "brikama_warriors")
  }

  val locations = listOf("Banjul", "Serrekunda", "Brikama", "Bakau", "Kololi", "Kanifing", "Brusubi", "Sukuta")
  val positions = listOf("Forward", "Midfielder", "Defender", "Goalkeeper")

  if (step == 1) {
    BackHandler { step = 0 }
  }

  val matchingTurfsCount =
    remember(cityInput, pickedLatitude, pickedLongitude) {
      val anchorLat = pickedLatitude ?: GambianFootballCatalog.resolveAreaPoint(cityInput).latitude
      val anchorLon = pickedLongitude ?: GambianFootballCatalog.resolveAreaPoint(cityInput).longitude
      val nearbyCount =
        GambianFootballCatalog.turfs.count { turf ->
          GambianFootballCatalog.haversineDistanceKm(
            anchorLat,
            anchorLon,
            turf.latitude,
            turf.longitude,
          ) <= 6.5
        }
      if (nearbyCount > 0) nearbyCount else GambianFootballCatalog.turfs.size
    }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF160B0F), ObsidianBlack, ObsidianBlack)
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
      // Top Brand + Step Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (step == 1) {
            IconButton(
              onClick = { step = 0 },
              modifier = Modifier.size(38.dp).testTag("onboarding_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to location",
                tint = TextPrimary,
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
          } else {
            FootballBrandLogo(size = 34.dp)
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text(
            text = if (step == 0) "Step 1 of 2 • Location" else "Step 2 of 2 • Player Profile",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
          )
        }
        GambianFlagBadge()
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Progress indicator
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .height(5.dp)
              .clip(RoundedCornerShape(50))
              .background(CrimsonRed)
        )
        Box(
          modifier =
            Modifier.weight(1f)
              .height(5.dp)
              .clip(RoundedCornerShape(50))
              .background(if (step == 1) CrimsonRed else ObsidianCardElevated)
        )
      }

      Spacer(modifier = Modifier.height(22.dp))

      // Scrollable step body
      Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        AnimatedContent(targetState = step, label = "onboarding_step") { currentStep ->
          if (currentStep == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier =
                    Modifier.size(46.dp)
                      .clip(CircleShape)
                      .background(CrimsonRed.copy(alpha = 0.16f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = CrimsonRed,
                    modifier = Modifier.size(24.dp),
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "Where do you play?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                  )
                  Text(
                    text = "Set your home area in The Gambia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldTrophy,
                  )
                }
              }

              Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Filled.SportsSoccer,
                    contentDescription = null,
                    tint = CrimsonRed,
                    modifier = Modifier.size(22.dp),
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text =
                      "Adding your location helps you find nearby turfs, check live slot availability, and join local 7-a-side challenges near you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                  )
                }
              }

              OutlinedTextField(
                value = cityInput,
                onValueChange = {
                  cityInput = it
                  pickedLatitude = null
                  pickedLongitude = null
                  onSearchPlaces(it)
                },
                label = { Text("Your Town or Area") },
                placeholder = { Text("e.g. Serrekunda, Banjul, Brusubi") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = CrimsonRed,
                  )
                },
                trailingIcon = {
                  if (isSearchingPlaces) {
                    CircularProgressIndicator(
                      color = CrimsonRed,
                      strokeWidth = 2.dp,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonRed,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard,
                  ),
                modifier = Modifier.fillMaxWidth().testTag("onboarding_location_input"),
              )

              AnimatedVisibility(visible = placeSuggestions.isNotEmpty()) {
                Surface(
                  color = ObsidianCardElevated,
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.4f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                      text = "OpenStreetMap Results (The Gambia)",
                      style = MaterialTheme.typography.labelSmall,
                      color = GoldTrophy,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    placeSuggestions.take(4).forEachIndexed { idx, place ->
                      if (idx > 0) HorizontalDivider(color = ObsidianBorder, thickness = 0.5.dp)
                      Row(
                        modifier =
                          Modifier.fillMaxWidth()
                            .clickable {
                              cityInput = place.shortName
                              pickedLatitude = place.latitude
                              pickedLongitude = place.longitude
                              onClearPlaceSuggestions()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Public,
                          contentDescription = null,
                          tint = CrimsonRed,
                          modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = place.shortName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                          Text(
                            text = place.displayName,
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

              Text(
                text = "Popular Football Areas",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                locations.forEach { area ->
                  val selected = cityInput.trim().equals(area, ignoreCase = true)
                  Surface(
                    color = if (selected) CrimsonRed else ObsidianCard,
                    shape = RoundedCornerShape(50),
                    border =
                      BorderStroke(
                        1.dp,
                        if (selected) CrimsonGlow else ObsidianBorder,
                      ),
                    modifier =
                      Modifier.clip(RoundedCornerShape(50))
                        .clickable {
                          cityInput = area
                          pickedLatitude = null
                          pickedLongitude = null
                          onClearPlaceSuggestions()
                        }
                        .testTag("onboarding_city_chip_$area"),
                  ) {
                    Text(
                      text = area,
                      style = MaterialTheme.typography.labelLarge,
                      color = if (selected) Color.White else TextSecondary,
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                  }
                }
              }

              Surface(
                color = ObsidianCardElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text(
                      text = "Turfs matched for ${cityInput.ifBlank { "The Gambia" }}",
                      style = MaterialTheme.typography.titleMedium,
                      color = TextPrimary,
                    )
                    Text(
                      text = "Ready to browse & book instantly",
                      style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                      color = TextSecondary,
                    )
                  }
                  Text(
                    text = "$matchingTurfsCount turfs",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldOpen,
                  )
                }
              }
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Text(
                text = "Create your player profile",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
              )
              Text(
                text = "Choose your player name, position, and squad before hitting the turf.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
              )

              // Live Player Card Preview
              Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier =
                      Modifier.size(56.dp)
                        .clip(CircleShape)
                        .background(ObsidianCardElevated)
                        .border(2.dp, CrimsonRed, CircleShape),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Person,
                      contentDescription = null,
                      tint = TextPrimary,
                      modifier = Modifier.size(30.dp),
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = displayName.ifBlank { "Player" },
                      style = MaterialTheme.typography.titleLarge,
                      color = TextPrimary,
                    )
                    val formattedHandle =
                      handle.trim().let { if (it.startsWith("@") || it.isEmpty()) it else "@$it" }
                    Text(
                      text = "${formattedHandle.ifBlank { "@player" }} • ${cityInput.ifBlank { "Banjul" }}",
                      style = MaterialTheme.typography.bodyMedium,
                      color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = selectedPosition,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = GoldTrophy,
                    )
                  }
                }
              }

              OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Player Name / Nickname") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonRed,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard,
                  ),
                modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input"),
              )

              OutlinedTextField(
                value = handle,
                onValueChange = { handle = it },
                label = { Text("Player Handle") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonRed,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard,
                  ),
                modifier = Modifier.fillMaxWidth().testTag("onboarding_handle_input"),
              )

              Text(
                text = "Preferred Position",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                positions.forEach { pos ->
                  val selected = selectedPosition == pos
                  Surface(
                    color = if (selected) CrimsonRed else ObsidianCard,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, if (selected) CrimsonGlow else ObsidianBorder),
                    modifier =
                      Modifier.clip(RoundedCornerShape(50))
                        .clickable { selectedPosition = pos }
                        .testTag("onboarding_pos_$pos"),
                  ) {
                    Text(
                      text = pos,
                      style = MaterialTheme.typography.labelLarge,
                      color = if (selected) Color.White else TextSecondary,
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    )
                  }
                }
              }

              Text(
                text = "Your Squad",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                GambianFootballCatalog.teams.forEach { team ->
                  val selected = selectedTeamId == team.id
                  Surface(
                    color = if (selected) CrimsonRed else ObsidianCard,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, if (selected) CrimsonGlow else ObsidianBorder),
                    modifier =
                      Modifier.clip(RoundedCornerShape(50))
                        .clickable { selectedTeamId = team.id },
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = if (selected) Color.White else GoldTrophy,
                        modifier = Modifier.size(14.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = team.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else TextSecondary,
                      )
                    }
                  }
                }
                val isFreeAgent = selectedTeamId == "free_agent"
                Surface(
                  color = if (isFreeAgent) CrimsonRed else ObsidianCard,
                  shape = RoundedCornerShape(50),
                  border = BorderStroke(1.dp, if (isFreeAgent) CrimsonGlow else ObsidianBorder),
                  modifier =
                    Modifier.clip(RoundedCornerShape(50))
                      .clickable { selectedTeamId = "free_agent" },
                ) {
                  Text(
                    text = "Free Agent",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isFreeAgent) Color.White else TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Primary Bottom CTA
      Button(
        onClick = {
          if (step == 0) {
            if (cityInput.isBlank()) cityInput = "Banjul"
            step = 1
          } else {
            onCompleteOnboarding(
              displayName,
              handle,
              cityInput.ifBlank { "Banjul" },
              selectedPosition,
              selectedTeamId,
              pickedLatitude,
              pickedLongitude,
            )
          }
        },
        enabled = !isBusy && (step == 0 || displayName.isNotBlank()),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = CrimsonRed,
            contentColor = Color.White,
          ),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier =
          Modifier.fillMaxWidth()
            .height(56.dp)
            .testTag(
              if (step == 0) "onboarding_next_button" else "onboarding_complete_button"
            ),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.5.dp,
          )
        } else {
          Text(
            text = if (step == 0) "Continue to Profile" else "Enter Football Hub",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}
