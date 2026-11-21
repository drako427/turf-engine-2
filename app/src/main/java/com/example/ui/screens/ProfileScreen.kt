package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.model.MatchChallenge
import com.example.data.model.PlayerProfile
import com.example.data.model.TournamentRegistration
import com.example.data.model.TurfBooking
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

@Composable
fun ProfileScreen(
  profile: PlayerProfile?,
  bookings: List<TurfBooking>,
  registrations: List<TournamentRegistration>,
  challenges: List<MatchChallenge>,
  onOpenMyTeam: () -> Unit,
  onOpenNotifications: () -> Unit,
  onCancelBooking: (String) -> Unit,
  onUpdateProfile: (displayName: String, handle: String, city: String) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showBookingsDialog by remember { mutableStateOf(false) }
  var showTournamentsDialog by remember { mutableStateOf(false) }
  var showHistoryDialog by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }

  val displayName = profile?.displayName ?: "Drako"
  val handle = profile?.handle ?: "@drako427"
  val city = profile?.city ?: "Banjul"
  val position = profile?.position ?: "Forward"
  val myTeam =
    remember(profile?.favoriteTeamId) {
      GambianFootballCatalog.teams.firstOrNull { it.id == profile?.favoriteTeamId }
    }
  val matches = profile?.matchesPlayed ?: 24L
  val wins = profile?.wins ?: 16L
  val trophies = profile?.trophies ?: 4L
  val activeBookingsCount = bookings.count { it.status == "confirmed" }

  LazyColumn(
    modifier = modifier.fillMaxSize().background(ObsidianBlack).statusBarsPadding(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Top settings icon
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Player Hub",
          style = MaterialTheme.typography.titleLarge,
          color = TextPrimary,
        )
        IconButton(
          onClick = { showEditDialog = true },
          modifier = Modifier.testTag("profile_settings_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Edit profile settings",
            tint = TextPrimary,
          )
        }
      }
    }

    // Player Avatar + Name + Stats
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier.size(88.dp)
              .clip(CircleShape)
              .background(ObsidianCardElevated)
              .border(2.5.dp, CrimsonRed, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = displayName,
            tint = TextPrimary,
            modifier = Modifier.size(44.dp),
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = displayName,
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = "Captain crown",
            tint = GoldTrophy,
            modifier = Modifier.size(22.dp),
          )
        }
        Text(
          text = "$handle • $city • $position",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(18.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          ProfileStat(matches.toString(), "Matches")
          ProfileStat(wins.toString(), "Wins")
          ProfileStat(trophies.toString(), "Trophies")
        }
      }
    }

    // My Team Card
    item {
      Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenMyTeam)
            .testTag("profile_my_team_card"),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(48.dp)
                .clip(CircleShape)
                .background(GoldTrophy.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Shield,
              contentDescription = null,
              tint = GoldTrophy,
              modifier = Modifier.size(26.dp),
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "My Team",
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted,
            )
            Text(
              text = myTeam?.name ?: "Free Agent",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
            )
            Text(
              text = myTeam?.let { "${it.memberCount} Members" } ?: "Open for squad invites",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
              color = TextSecondary,
            )
          }
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open team",
            tint = TextSecondary,
          )
        }
      }
    }

    // Profile Menu Rows
    item {
      Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          ProfileMenuRow(
            icon = Icons.Filled.CalendarMonth,
            label = "My Bookings",
            badgeCount = activeBookingsCount,
            onClick = { showBookingsDialog = true },
            tag = "menu_my_bookings",
          )
          ProfileMenuRow(
            icon = Icons.Filled.EmojiEvents,
            label = "My Tournaments",
            badgeCount = registrations.size,
            onClick = { showTournamentsDialog = true },
            tag = "menu_my_tournaments",
          )
          ProfileMenuRow(
            icon = Icons.Filled.History,
            label = "Match History",
            badgeCount = challenges.count { it.status == "completed" },
            onClick = { showHistoryDialog = true },
            tag = "menu_match_history",
          )
          ProfileMenuRow(
            icon = Icons.Filled.NotificationsNone,
            label = "Notifications",
            badgeCount = GambianFootballCatalog.notifications.count { it.isUnread },
            onClick = onOpenNotifications,
            tag = "menu_notifications",
          )
          ProfileMenuRow(
            icon = Icons.Filled.Settings,
            label = "Edit Profile & City",
            badgeCount = 0,
            onClick = { showEditDialog = true },
            tag = "menu_edit_profile",
          )
          ProfileMenuRow(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            label = "Sign Out",
            badgeCount = 0,
            tint = CrimsonRed,
            onClick = onSignOut,
            tag = "menu_sign_out",
          )
        }
      }
    }
  }

  if (showBookingsDialog) {
    AlertDialog(
      onDismissRequest = { showBookingsDialog = false },
      containerColor = ObsidianCardElevated,
      title = { Text("My Turf Bookings", color = TextPrimary) },
      text = {
        if (bookings.isEmpty()) {
          Text(
            "No turf bookings yet. Pick a turf on Play and tap Book Now!",
            color = TextSecondary,
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            bookings.take(5).forEach { booking ->
              Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      booking.turfName,
                      style = MaterialTheme.typography.titleMedium,
                      color = TextPrimary,
                    )
                    Text(
                      "${booking.dateLabel} • ${booking.timeSlot}",
                      style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                      color = TextSecondary,
                    )
                    Text(
                      "D${booking.pricePerHour} • ${booking.status.uppercase()}",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (booking.status == "confirmed") EmeraldOpen else TextMuted,
                    )
                  }
                  if (booking.status == "confirmed") {
                    TextButton(onClick = { onCancelBooking(booking.id) }) {
                      Text("Cancel", color = CrimsonRed)
                    }
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showBookingsDialog = false }) {
          Text("Close", color = TextPrimary)
        }
      },
    )
  }

  if (showTournamentsDialog) {
    AlertDialog(
      onDismissRequest = { showTournamentsDialog = false },
      containerColor = ObsidianCardElevated,
      title = { Text("My Tournaments", color = TextPrimary) },
      text = {
        if (registrations.isEmpty()) {
          Text(
            "You haven't registered a squad for any tournaments yet.",
            color = TextSecondary,
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            registrations.forEach { reg ->
              Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                  Text(reg.tournamentTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                  Text("Squad: ${reg.teamName}", style = MaterialTheme.typography.bodyMedium, color = GoldTrophy)
                  Text("${reg.dateRange} • ${reg.location}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showTournamentsDialog = false }) {
          Text("Close", color = TextPrimary)
        }
      },
    )
  }

  if (showHistoryDialog) {
    AlertDialog(
      onDismissRequest = { showHistoryDialog = false },
      containerColor = ObsidianCardElevated,
      title = { Text("Match History", color = TextPrimary) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          val completedChallenges = challenges.filter { it.status == "completed" }
          if (completedChallenges.isNotEmpty()) {
            completedChallenges.forEach { c ->
              Text(
                "• ${c.hostTeamName} vs ${c.opponentName} @ ${c.turfName}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
          GambianFootballCatalog.teams.first().recentMatches.forEach { m ->
            Text(
              "• Brikama Warriors ${m.score} ${m.opponent} (${m.outcome})",
              color = TextSecondary,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showHistoryDialog = false }) {
          Text("Close", color = TextPrimary)
        }
      },
    )
  }

  if (showEditDialog) {
    var editName by remember(displayName) { mutableStateOf(displayName) }
    var editHandle by remember(handle) { mutableStateOf(handle) }
    var editCity by remember(city) { mutableStateOf(city) }

    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      containerColor = ObsidianCardElevated,
      title = { Text("Edit Player Profile", color = TextPrimary) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Player Name") },
            singleLine = true,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonRed,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
              ),
          )
          OutlinedTextField(
            value = editHandle,
            onValueChange = { editHandle = it },
            label = { Text("Handle") },
            singleLine = true,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonRed,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
              ),
          )
          OutlinedTextField(
            value = editCity,
            onValueChange = { editCity = it },
            label = { Text("Home City") },
            singleLine = true,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonRed,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
              ),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateProfile(editName, editHandle, editCity)
            showEditDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
    )
  }
}

@Composable
private fun ProfileStat(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      color = TextPrimary,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
      color = TextSecondary,
    )
  }
}

@Composable
private fun ProfileMenuRow(
  icon: ImageVector,
  label: String,
  badgeCount: Int,
  onClick: () -> Unit,
  tag: String,
  tint: Color = TextPrimary,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = tint,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(14.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = tint,
      modifier = Modifier.weight(1f),
    )
    if (badgeCount > 0) {
      Surface(
        color = CrimsonRed,
        shape = CircleShape,
      ) {
        Text(
          text = badgeCount.toString(),
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
    }
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = TextMuted,
      modifier = Modifier.size(18.dp),
    )
  }
}
