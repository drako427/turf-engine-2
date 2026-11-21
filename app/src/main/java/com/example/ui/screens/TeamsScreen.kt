package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.model.FootballTeam
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldOpen
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TeamsScreen(
  selectedTeamId: String,
  followedTeamIds: Set<String>,
  onSelectTeam: (String) -> Unit,
  onToggleFollow: (FootballTeam, Boolean) -> Unit,
  onChallengeTeam: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val team =
    remember(selectedTeamId) {
      GambianFootballCatalog.teams.firstOrNull { it.id == selectedTeamId }
        ?: GambianFootballCatalog.teams.first()
    }
  val isFollowing = followedTeamIds.contains(team.id)
  var infoTab by remember { mutableIntStateOf(0) } // 0=About, 1=Matches, 2=Achievements

  LazyColumn(
    modifier = modifier.fillMaxSize().background(ObsidianBlack).statusBarsPadding(),
    contentPadding = PaddingValues(bottom = 100.dp),
  ) {
    // Squad Selector Chips
    item {
      Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(
          text = "Teams & Squads",
          style = MaterialTheme.typography.headlineLarge,
          color = TextPrimary,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          GambianFootballCatalog.teams.forEach { squad ->
            val selected = squad.id == team.id
            Surface(
              color = if (selected) CrimsonRed else ObsidianCard,
              shape = RoundedCornerShape(50),
              border = BorderStroke(1.dp, if (selected) CrimsonGlow else ObsidianBorder),
              modifier =
                Modifier.clip(RoundedCornerShape(50))
                  .clickable { onSelectTeam(squad.id) }
                  .testTag("team_chip_${squad.id}"),
            ) {
              Text(
                text = squad.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else TextSecondary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              )
            }
          }
        }
      }
    }

    // Team Hero Crest + Stats
    item {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .padding(top = 14.dp)
            .background(
              Brush.verticalGradient(
                listOf(
                  Color(team.badgeTintHex).copy(alpha = 0.14f),
                  ObsidianBlack,
                )
              )
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(
            modifier =
              Modifier.size(92.dp)
                .clip(CircleShape)
                .background(ObsidianCard)
                .border(2.5.dp, Color(team.badgeTintHex), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Shield,
              contentDescription = team.name,
              tint = Color(team.badgeTintHex),
              modifier = Modifier.size(48.dp),
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = team.name,
              style = MaterialTheme.typography.headlineLarge,
              color = TextPrimary,
            )
            if (team.isVerified) {
              Spacer(modifier = Modifier.width(6.dp))
              VerifiedBadge(tint = Color(0xFF38BDF8))
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${team.memberCount} Members • ${team.trophyCount} Trophies • ${team.city}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )

          Spacer(modifier = Modifier.height(18.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            TeamStatItem(team.matchesPlayed.toString(), "Matches")
            TeamStatItem(team.wins.toString(), "Wins")
            TeamStatItem(team.draws.toString(), "Draws")
            TeamStatItem(team.losses.toString(), "Losses")
          }

          Spacer(modifier = Modifier.height(18.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = { onToggleFollow(team, isFollowing) },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = if (isFollowing) ObsidianCardElevated else CrimsonRed,
                  contentColor = Color.White,
                ),
              shape = RoundedCornerShape(24.dp),
              modifier = Modifier.weight(1f).height(46.dp).testTag("team_follow_button"),
            ) {
              Text(
                text = if (isFollowing) "Following ✓" else "Follow",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              )
            }
            OutlinedButton(
              onClick = onChallengeTeam,
              border = BorderStroke(1.dp, ObsidianBorder),
              colors =
                ButtonDefaults.outlinedButtonColors(
                  containerColor = ObsidianCard,
                  contentColor = TextPrimary,
                ),
              shape = RoundedCornerShape(24.dp),
              modifier = Modifier.height(46.dp).testTag("team_challenge_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = "Challenge squad",
                tint = GoldTrophy,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Challenge", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }

    // Sub-tabs: About | Matches | Achievements
    item {
      Row(
        modifier =
          Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        listOf("About", "Matches", "Achievements").forEachIndexed { idx, label ->
          val active = infoTab == idx
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable { infoTab = idx }
                .padding(vertical = 8.dp),
          ) {
            Text(
              text = label,
              style =
                MaterialTheme.typography.labelLarge.copy(
                  fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                ),
              color = if (active) TextPrimary else TextSecondary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
              modifier =
                Modifier.fillMaxWidth(0.7f)
                  .height(2.5.dp)
                  .background(if (active) CrimsonRed else Color.Transparent)
            )
          }
        }
      }
    }

    when (infoTab) {
      0 -> {
        item {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
              text = team.motto,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = GoldTrophy,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = team.bio,
              style = MaterialTheme.typography.bodyLarge,
              color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
              text = "Recent Matches",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
          }
        }
        items(team.recentMatches) { match ->
          MatchResultCard(
            teamName = team.name,
            opponent = match.opponent,
            score = match.score,
            outcome = match.outcome,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
          )
        }
      }
      1 -> {
        items(team.recentMatches) { match ->
          MatchResultCard(
            teamName = team.name,
            opponent = match.opponent,
            score = match.score,
            outcome = match.outcome,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
          )
        }
      }
      else -> {
        items(team.achievements) { achievement ->
          Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = GoldTrophy,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = achievement,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TeamStatItem(value: String, label: String) {
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
private fun MatchResultCard(
  teamName: String,
  opponent: String,
  score: String,
  outcome: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = ObsidianCard,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, ObsidianBorder),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = score,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )
        Text(
          text = "$teamName vs $opponent",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }
      val isWin = outcome.equals("Win", ignoreCase = true)
      Surface(
        color = if (isWin) EmeraldSubtle else ObsidianCardElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isWin) EmeraldOpen else ObsidianBorder),
      ) {
        Text(
          text = outcome,
          style = MaterialTheme.typography.labelLarge,
          color = if (isWin) EmeraldOpen else TextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
      }
    }
  }
}
