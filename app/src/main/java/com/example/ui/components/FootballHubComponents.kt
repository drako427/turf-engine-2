package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TurfVenue
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldOpen
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FootballBrandLogo(size: Dp = 44.dp, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val w = this.size.width
      val h = this.size.height
      // Stylized Crimson "F" wing
      val fPath =
        Path().apply {
          moveTo(w * 0.16f, h * 0.14f)
          lineTo(w * 0.86f, h * 0.14f)
          lineTo(w * 0.74f, h * 0.32f)
          lineTo(w * 0.38f, h * 0.32f)
          lineTo(w * 0.34f, h * 0.46f)
          lineTo(w * 0.64f, h * 0.46f)
          lineTo(w * 0.56f, h * 0.62f)
          lineTo(w * 0.30f, h * 0.62f)
          lineTo(w * 0.22f, h * 0.88f)
          lineTo(w * 0.06f, h * 0.88f)
          close()
        }
      drawPath(
        path = fPath,
        brush = Brush.linearGradient(listOf(CrimsonGlow, CrimsonRed, CrimsonDark)),
      )
      // Soccer ball nested right
      val ballCenter = Offset(w * 0.71f, h * 0.68f)
      val ballRadius = w * 0.23f
      drawCircle(color = Color.White, radius = ballRadius, center = ballCenter)
      drawCircle(color = ObsidianBlack, radius = ballRadius * 0.42f, center = ballCenter)
    }
  }
}

@Composable
fun GambianFlagBadge(modifier: Modifier = Modifier) {
  Row(
    modifier =
      modifier
        .clip(RoundedCornerShape(4.dp))
        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
        .width(34.dp)
        .height(22.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Box(modifier = Modifier.fillMaxWidth().height(7.dp).background(Color(0xFFCE1126)))
      Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(Color.White))
      Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color(0xFF0C1C8C)))
      Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(Color.White))
      Box(modifier = Modifier.fillMaxWidth().height(7.dp).background(Color(0xFF3A7728)))
    }
  }
}

@Composable
fun VerifiedBadge(modifier: Modifier = Modifier, tint: Color = CrimsonRed) {
  Icon(
    imageVector = Icons.Filled.Verified,
    contentDescription = "Verified",
    tint = tint,
    modifier = modifier.size(16.dp),
  )
}

@Composable
fun OpenStatusPill(isOpen: Boolean, modifier: Modifier = Modifier) {
  Surface(
    color = if (isOpen) EmeraldSubtle else ObsidianCardElevated,
    shape = RoundedCornerShape(50),
    border =
      BorderStroke(
        width = 1.dp,
        color = if (isOpen) EmeraldOpen.copy(alpha = 0.4f) else ObsidianBorder,
      ),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(6.dp)
            .clip(CircleShape)
            .background(if (isOpen) EmeraldOpen else TextMuted)
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = if (isOpen) "Open" else "Closed",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = if (isOpen) EmeraldOpen else TextSecondary,
      )
    }
  }
}

@Composable
fun TurfCompactCard(
  turf: TurfVenue,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = ObsidianCard,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, ObsidianBorder),
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .testTag("turf_card_${turf.id}"),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_turf_night),
        contentDescription = turf.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Text(
              text = turf.name,
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (turf.isVerified) {
              Spacer(modifier = Modifier.width(4.dp))
              VerifiedBadge(tint = Color(0xFF38BDF8))
            }
          }
          Spacer(modifier = Modifier.width(6.dp))
          OpenStatusPill(isOpen = turf.isOpen)
        }
        Spacer(modifier = Modifier.height(4.dp))
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
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = TextSecondary,
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(13.dp),
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "${turf.area} • ${turf.distanceKm} km",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = TextSecondary,
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "D${turf.pricePerHour}/hr",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = EmeraldOpen,
          )
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open turf details",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

enum class HubTab(val label: String, val tag: String) {
  HOME("Home", "tab_home"),
  COMPETITIONS("Competitions", "tab_competitions"),
  PLAY("Play", "tab_play"),
  TEAMS("Teams", "tab_teams"),
  PROFILE("Profile", "tab_profile"),
}

@Composable
fun FootballBottomNavBar(
  selectedTab: HubTab,
  onTabSelected: (HubTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .background(ObsidianSurface.copy(alpha = 0.96f))
        .windowInsetsPadding(WindowInsets.navigationBars)
  ) {
    // Top subtle crimson hairline glow
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(1.dp)
          .background(
            Brush.horizontalGradient(
              listOf(
                ObsidianBorder,
                CrimsonRed.copy(alpha = 0.65f),
                ObsidianBorder,
              )
            )
          )
    )

    Row(
      modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 6.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      HubTab.entries.forEach { tab ->
        val isSelected = tab == selectedTab
        if (tab == HubTab.PLAY) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
              Modifier.offset(y = (-10).dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onTabSelected(tab) }
                .padding(horizontal = 8.dp)
                .testTag(tab.tag),
          ) {
            Box(
              modifier =
                Modifier.size(48.dp)
                  .shadow(
                    elevation = if (isSelected) 16.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = CrimsonGlow,
                    spotColor = CrimsonGlow,
                  )
                  .clip(CircleShape)
                  .background(
                    Brush.radialGradient(
                      colors =
                        if (isSelected) listOf(CrimsonGlow, CrimsonRed, CrimsonDark)
                        else listOf(ObsidianCardElevated, ObsidianCard)
                    )
                  )
                  .border(
                    width = 1.5.dp,
                    color = if (isSelected) CrimsonGlow else CrimsonRed.copy(alpha = 0.7f),
                    shape = CircleShape,
                  ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.SportsSoccer,
                contentDescription = tab.label,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = tab.label,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (isSelected) TextPrimary else TextSecondary,
            )
          }
        } else {
          val icon =
            when (tab) {
              HubTab.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
              HubTab.COMPETITIONS ->
                if (isSelected) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents
              HubTab.TEAMS -> if (isSelected) Icons.Filled.Groups else Icons.Outlined.Groups
              HubTab.PROFILE -> if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
              HubTab.PLAY -> Icons.Filled.SportsSoccer
            }
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
              Modifier.weight(1f)
                .height(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onTabSelected(tab) }
                .testTag(tab.tag),
          ) {
            Icon(
              imageVector = icon,
              contentDescription = tab.label,
              tint = if (isSelected) CrimsonRed else TextSecondary,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = tab.label,
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
              color = if (isSelected) CrimsonRed else TextSecondary,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}
