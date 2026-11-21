package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.model.MatchChallenge
import com.example.data.model.SeedChallenge
import com.example.data.model.Tournament
import com.example.data.model.TournamentRegistration
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldOpen
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CompetitionsScreen(
  subTab: Int,
  selectedTournamentTab: String,
  selectedChallengeTab: String,
  registrations: List<TournamentRegistration>,
  challenges: List<MatchChallenge>,
  prefillChallengeTurf: String? = null,
  onConsumePrefillChallengeTurf: () -> Unit = {},
  onSelectSubTab: (Int) -> Unit,
  onSelectTournamentTab: (String) -> Unit,
  onSelectChallengeTab: (String) -> Unit,
  onRegisterTournament: (Tournament, String) -> Unit,
  onCreateChallenge: (hostTeam: String, turfName: String, stake: String, format: String, schedule: String) -> Unit,
  onJoinSeedChallenge: (SeedChallenge, String) -> Unit,
  onMarkChallengeCompleted: (String) -> Unit,
  onOpenNotifications: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var tournamentToRegister by remember { mutableStateOf<Tournament?>(null) }
  var showCreateChallengeDialog by remember { mutableStateOf(false) }
  var prefilledTurfName by remember { mutableStateOf("Paradise Turf") }

  androidx.compose.runtime.LaunchedEffect(prefillChallengeTurf) {
    if (!prefillChallengeTurf.isNullOrBlank()) {
      prefilledTurfName = prefillChallengeTurf
      showCreateChallengeDialog = true
      onConsumePrefillChallengeTurf()
    }
  }

  val registeredIds = remember(registrations) {
    registrations.filter { it.status == "registered" }.map { it.tournamentId }.toSet()
  }

  Column(
    modifier = modifier.fillMaxSize().background(ObsidianBlack).statusBarsPadding()
  ) {
    // Top Header + Segmented Switch (Tournaments | Challenges)
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (subTab == 0) Icons.Filled.EmojiEvents else Icons.Filled.Bolt,
          contentDescription = null,
          tint = GoldTrophy,
          modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (subTab == 0) "Tournaments" else "Challenges",
          style = MaterialTheme.typography.headlineLarge,
          color = TextPrimary,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (subTab == 1) {
          IconButton(
            onClick = { showCreateChallengeDialog = true },
            modifier = Modifier.testTag("challenges_header_add_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "Create Challenge",
              tint = TextPrimary,
            )
          }
        }
        IconButton(onClick = onOpenNotifications) {
          Icon(
            imageVector = Icons.Filled.NotificationsNone,
            contentDescription = "Notifications",
            tint = TextPrimary,
          )
        }
      }
    }

    // Sub-tab switcher pill
    Surface(
      color = ObsidianCard,
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, ObsidianBorder),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
      Row(modifier = Modifier.padding(4.dp)) {
        listOf("Tournaments", "Challenges").forEachIndexed { idx, label ->
          val selected = subTab == idx
          Surface(
            color = if (selected) CrimsonRed else Color.Transparent,
            shape = RoundedCornerShape(12.dp),
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelectSubTab(idx) }
                .testTag("comp_subtab_$label"),
          ) {
            Box(
              modifier = Modifier.padding(vertical = 10.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = label,
                style =
                  MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                  ),
                color = if (selected) Color.White else TextSecondary,
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (subTab == 0) {
      TournamentsContent(
        selectedTab = selectedTournamentTab,
        registeredIds = registeredIds,
        onSelectTab = onSelectTournamentTab,
        onRegisterClick = { tournamentToRegister = it },
      )
    } else {
      ChallengesContent(
        selectedTab = selectedChallengeTab,
        userChallenges = challenges,
        onSelectTab = onSelectChallengeTab,
        onOpenCreateDialog = { showCreateChallengeDialog = true },
        onJoinSeed = { seed -> onJoinSeedChallenge(seed, "Brikama Warriors") },
        onMarkCompleted = onMarkChallengeCompleted,
      )
    }
  }

  tournamentToRegister?.let { tournament ->
    RegisterTournamentDialog(
      tournament = tournament,
      onDismiss = { tournamentToRegister = null },
      onConfirm = { teamName ->
        onRegisterTournament(tournament, teamName)
        tournamentToRegister = null
      },
    )
  }

  if (showCreateChallengeDialog) {
    CreateChallengeDialog(
      initialTurfName = prefilledTurfName,
      onDismiss = { showCreateChallengeDialog = false },
      onCreate = { hostTeam, turfName, stake, format, schedule ->
        onCreateChallenge(hostTeam, turfName, stake, format, schedule)
        prefilledTurfName = "Paradise Turf"
        showCreateChallengeDialog = false
      },
    )
  }
}

@Composable
private fun TournamentsContent(
  selectedTab: String,
  registeredIds: Set<String>,
  onSelectTab: (String) -> Unit,
  onRegisterClick: (Tournament) -> Unit,
) {
  val tabs = listOf("Ongoing", "Upcoming", "Completed")
  val filtered =
    remember(selectedTab) {
      GambianFootballCatalog.tournaments.filter { it.category == selectedTab }
    }
  val hero = filtered.firstOrNull()
  val others = if (filtered.size > 1) filtered.drop(1) else emptyList()

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        tabs.forEach { tab ->
          val active = selectedTab == tab
          Surface(
            color = if (active) CrimsonRed else ObsidianCard,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, if (active) CrimsonGlow else ObsidianBorder),
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .clickable { onSelectTab(tab) }
                .testTag("tournament_tab_$tab"),
          ) {
            Text(
              text = tab,
              style = MaterialTheme.typography.labelLarge,
              color = if (active) Color.White else TextSecondary,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
          }
        }
      }
    }

    if (hero != null) {
      item {
        val isRegistered = registeredIds.contains(hero.id)
        Surface(
          color = ObsidianCard,
          shape = RoundedCornerShape(22.dp),
          border = BorderStroke(1.dp, ObsidianBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column {
            Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
              Image(
                painter = painterResource(id = R.drawable.img_trophy_cup),
                contentDescription = hero.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .background(
                      Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        1.0f to ObsidianCard,
                      )
                    )
              )
              Surface(
                color =
                  if (hero.category == "Ongoing") EmeraldSubtle else ObsidianCardElevated,
                shape = RoundedCornerShape(50),
                border =
                  BorderStroke(
                    1.dp,
                    if (hero.category == "Ongoing") EmeraldOpen else ObsidianBorder,
                  ),
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
              ) {
                Text(
                  text = hero.category,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (hero.category == "Ongoing") EmeraldOpen else TextPrimary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
              Text(
                text = hero.title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "${hero.teamCount} Teams • ${hero.prizePool}",
                style = MaterialTheme.typography.bodyMedium,
                color = GoldTrophy,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.CalendarToday,
                  contentDescription = null,
                  tint = TextMuted,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = hero.dateRange,
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                )
                Spacer(modifier = Modifier.width(14.dp))
                Icon(
                  imageVector = Icons.Filled.LocationOn,
                  contentDescription = null,
                  tint = TextMuted,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = hero.location,
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = hero.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
              Spacer(modifier = Modifier.height(14.dp))
              Button(
                onClick = { if (!isRegistered) onRegisterClick(hero) },
                enabled = !isRegistered && hero.category != "Completed",
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = if (isRegistered) EmeraldOpen else CrimsonRed,
                    contentColor = Color.White,
                    disabledContainerColor = ObsidianCardElevated,
                    disabledContentColor = EmeraldOpen,
                  ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("hero_tournament_cta"),
              ) {
                if (isRegistered) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Registered", style = MaterialTheme.typography.labelLarge)
                } else if (hero.category == "Completed") {
                  Text("Tournament Completed", style = MaterialTheme.typography.labelLarge)
                } else {
                  Text("Register Squad (${hero.entryFee})", style = MaterialTheme.typography.labelLarge)
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
      }
    }

    if (others.isNotEmpty()) {
      item {
        Text(
          text = "Other Tournaments",
          style = MaterialTheme.typography.titleLarge,
          color = TextPrimary,
        )
      }
      items(others, key = { it.id }) { tournament ->
        val isRegistered = registeredIds.contains(tournament.id)
        Surface(
          color = ObsidianCard,
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.dp, ObsidianBorder),
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .clickable { if (!isRegistered && tournament.category != "Completed") onRegisterClick(tournament) },
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_turf_night),
              contentDescription = tournament.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = tournament.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = "${tournament.teamCount} Teams • ${tournament.prizePool}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = GoldTrophy,
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = "${tournament.dateRange} • ${tournament.location}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextSecondary,
              )
            }
            if (isRegistered) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Registered",
                tint = EmeraldOpen,
                modifier = Modifier.size(22.dp),
              )
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Register",
                tint = CrimsonRed,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ChallengesContent(
  selectedTab: String,
  userChallenges: List<MatchChallenge>,
  onSelectTab: (String) -> Unit,
  onOpenCreateDialog: () -> Unit,
  onJoinSeed: (SeedChallenge) -> Unit,
  onMarkCompleted: (String) -> Unit,
) {
  val tabs = listOf("Open", "Accepted", "My Challenges")
  val joinedIds = remember(userChallenges) { userChallenges.map { it.id }.toSet() }
  val availableSeedChallenges =
    remember(joinedIds) {
      GambianFootballCatalog.seedChallenges.filterNot { joinedIds.contains(it.id) }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Sunset Hero Banner + Create Challenge CTA
    item {
      Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth().height(150.dp),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          Image(
            painter = painterResource(id = R.drawable.img_challenge_banner),
            contentDescription = "Find a team to play with",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.horizontalGradient(
                    0.0f to Color.Transparent,
                    0.45f to ObsidianBlack.copy(alpha = 0.72f),
                    1.0f to ObsidianBlack.copy(alpha = 0.95f),
                  )
                )
          )
          Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(18.dp),
            horizontalAlignment = Alignment.End,
          ) {
            Text(
              text = "Find a team\nto play with!",
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Create or join a 7s challenge",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }
        }
      }
    }

    item {
      Button(
        onClick = onOpenCreateDialog,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = CrimsonRed,
            contentColor = Color.White,
          ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("create_challenge_button"),
      ) {
        Text(
          text = "Create Challenge",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      }
    }

    // Tabs: Open | Accepted | My Challenges
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        tabs.forEach { tab ->
          val active = selectedTab == tab
          Surface(
            color = if (active) CrimsonRed else ObsidianCard,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, if (active) CrimsonGlow else ObsidianBorder),
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .clickable { onSelectTab(tab) }
                .testTag("challenge_tab_$tab"),
          ) {
            Text(
              text = tab,
              style = MaterialTheme.typography.labelLarge,
              color = if (active) Color.White else TextSecondary,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
          }
        }
      }
    }

    when (selectedTab) {
      "Open" -> {
        val openUserChallenges = userChallenges.filter { it.status == "open" }
        items(openUserChallenges, key = { "u_${it.id}" }) { challenge ->
          UserChallengeCard(
            challenge = challenge,
            actionLabel = "Complete",
            onAction = { onMarkCompleted(challenge.id) },
          )
        }
        items(availableSeedChallenges, key = { it.id }) { seed ->
          SeedChallengeCard(
            seed = seed,
            onJoin = { onJoinSeed(seed) },
          )
        }
      }
      "Accepted" -> {
        val accepted = userChallenges.filter { it.status == "accepted" }
        if (accepted.isEmpty()) {
          item {
            EmptyChallengeState("No accepted challenges yet. Join an open challenge to lock in kickoff!")
          }
        } else {
          items(accepted, key = { it.id }) { challenge ->
            UserChallengeCard(
              challenge = challenge,
              actionLabel = "Finish Match",
              onAction = { onMarkCompleted(challenge.id) },
            )
          }
        }
      }
      else -> {
        val mine = userChallenges.filter { it.isCreatedByUser }
        if (mine.isEmpty()) {
          item {
            EmptyChallengeState("You haven't posted a challenge yet. Tap Create Challenge above!")
          }
        } else {
          items(mine, key = { it.id }) { challenge ->
            UserChallengeCard(
              challenge = challenge,
              actionLabel = if (challenge.status == "completed") "Done" else "Complete",
              onAction = {
                if (challenge.status != "completed") onMarkCompleted(challenge.id)
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SeedChallengeCard(seed: SeedChallenge, onJoin: () -> Unit) {
  Surface(
    color = ObsidianCard,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, ObsidianBorder),
    modifier = Modifier.fillMaxWidth().testTag("seed_challenge_${seed.id}"),
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
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${seed.hostTeamName} vs ${seed.opponentName}",
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "${seed.stakeLabel} • ${seed.format} • ${seed.turfName}",
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
          color = GoldTrophy,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = seed.scheduleLabel,
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
          color = TextSecondary,
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        Button(
          onClick = onJoin,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = CrimsonRed,
              contentColor = Color.White,
            ),
          shape = RoundedCornerShape(18.dp),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
          modifier = Modifier.height(36.dp).testTag("join_challenge_${seed.id}"),
        ) {
          Text("Join", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Groups,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${seed.playersCount}/${seed.maxPlayers}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }
      }
    }
  }
}

@Composable
private fun UserChallengeCard(
  challenge: MatchChallenge,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Surface(
    color = ObsidianCard,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, ObsidianBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(48.dp)
            .clip(CircleShape)
            .background(CrimsonRed.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Shield,
          contentDescription = null,
          tint = CrimsonRed,
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${challenge.hostTeamName} vs ${challenge.opponentName}",
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "${challenge.stakeLabel} • ${challenge.format} • ${challenge.turfName}",
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
          color = GoldTrophy,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${challenge.scheduleLabel} • ${challenge.status.replaceFirstChar { it.uppercase() }}",
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
          color = if (challenge.status == "accepted") EmeraldOpen else TextSecondary,
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        Surface(
          color = ObsidianCardElevated,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, ObsidianBorder),
          modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onAction),
        ) {
          Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (challenge.status == "completed") EmeraldOpen else TextPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "${challenge.playersCount}/${challenge.maxPlayers} players",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }
    }
  }
}

@Composable
private fun EmptyChallengeState(message: String) {
  Surface(
    color = ObsidianCard,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, ObsidianBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = Icons.Filled.Bolt,
        contentDescription = null,
        tint = GoldTrophy,
        modifier = Modifier.size(32.dp),
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
      )
    }
  }
}

@Composable
private fun RegisterTournamentDialog(
  tournament: Tournament,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var teamName by remember { mutableStateOf("Brikama Warriors") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianCardElevated,
    title = {
      Text(
        text = "Register for ${tournament.title}",
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${tournament.location} • ${tournament.dateRange} • ${tournament.entryFee}",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
        OutlinedTextField(
          value = teamName,
          onValueChange = { teamName = it },
          label = { Text("Squad Name") },
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrimsonRed,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
            ),
          modifier = Modifier.fillMaxWidth().testTag("register_team_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(teamName) },
        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
      ) {
        Text("Confirm Entry")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
  )
}

@Composable
private fun CreateChallengeDialog(
  initialTurfName: String = "Paradise Turf",
  onDismiss: () -> Unit,
  onCreate: (hostTeam: String, turfName: String, stake: String, format: String, schedule: String) -> Unit,
) {
  var hostTeam by remember { mutableStateOf("Brikama Warriors") }
  var turfName by remember(initialTurfName) { mutableStateOf(initialTurfName) }
  var stake by remember { mutableStateOf("D500") }
  var format by remember { mutableStateOf("7-a-side") }
  var schedule by remember { mutableStateOf("Tonight • 7:00 PM") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianCardElevated,
    title = {
      Text("Create Match Challenge", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = hostTeam,
          onValueChange = { hostTeam = it },
          label = { Text("Your Team") },
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrimsonRed,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
            ),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = turfName,
          onValueChange = { turfName = it },
          label = { Text("Turf Venue") },
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrimsonRed,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
            ),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = stake,
            onValueChange = { stake = it },
            label = { Text("Stake") },
            singleLine = true,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonRed,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
              ),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = format,
            onValueChange = { format = it },
            label = { Text("Format") },
            singleLine = true,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonRed,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
              ),
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = schedule,
          onValueChange = { schedule = it },
          label = { Text("Kickoff Time") },
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrimsonRed,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
            ),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onCreate(hostTeam, turfName, stake, format, schedule) },
        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
        modifier = Modifier.testTag("submit_challenge_button"),
      ) {
        Text("Post Challenge")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
  )
}
