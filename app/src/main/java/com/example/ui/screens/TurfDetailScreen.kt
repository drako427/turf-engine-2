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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.catalog.GambianSlotTimeHelper
import com.example.data.catalog.TurfPhotoCompressor
import com.example.data.model.TurfSlot
import com.example.data.model.TurfVenue
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonSubtle
import com.example.ui.theme.EmeraldOpen
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun TurfDetailScreen(
  turf: TurfVenue,
  isFavorited: Boolean,
  isBusy: Boolean,
  onBack: () -> Unit,
  onToggleFavorite: () -> Unit,
  onBookNow: (dateLabel: String, timeSlot: String) -> Unit,
  modifier: Modifier = Modifier,
  liveSlotsToday: List<TurfSlot> = emptyList(),
  hasLiveSlots: Boolean = false,
) {
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(hasLiveSlots) {
    if (hasLiveSlots) {
      while (true) {
        delay(1000L)
        nowMillis = System.currentTimeMillis()
      }
    }
  }
  var selectedDate by remember { mutableStateOf(GambianFootballCatalog.dateOptions.first()) }
  var selectedSlot by remember(turf.id) {
    mutableStateOf(turf.timeSlots.getOrElse(2) { turf.timeSlots.first() })
  }

  Scaffold(
    containerColor = ObsidianBlack,
    bottomBar = {
      Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "D${turf.pricePerHour}/hr",
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
            )
            Text(
              text = "$selectedDate • $selectedSlot",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
              color = TextSecondary,
            )
          }

          Button(
            onClick = { onBookNow(selectedDate, selectedSlot) },
            enabled = !isBusy,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = CrimsonRed,
                contentColor = Color.White,
              ),
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            modifier = Modifier.height(52.dp).testTag("turf_book_now_button"),
          ) {
            if (isBusy) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
              )
            } else {
              Text(
                text = "Book Now",
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
    },
    modifier = modifier.fillMaxSize(),
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(bottom = 24.dp),
    ) {
      // Top Turf Hero Photo + Floating Action Bar (loads remaining photos only on detail screen)
      item {
        val photoCount = maxOf(turf.photos.size, 1)
        val photoPagerState = rememberPagerState(pageCount = { photoCount })
        Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
          HorizontalPager(
            state = photoPagerState,
            modifier = Modifier.fillMaxSize(),
          ) { pageIdx ->
            val rawPhoto = turf.photos.getOrNull(pageIdx)
            val decoded =
              remember(rawPhoto) {
                rawPhoto?.let { TurfPhotoCompressor.decodeDataUriToImageBitmap(it) }
              }
            if (decoded != null) {
              Image(
                bitmap = decoded,
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
          }
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.55f),
                    0.5f to Color.Transparent,
                    1.0f to ObsidianBlack,
                  )
                )
          )

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              color = ObsidianBlack.copy(alpha = 0.65f),
              shape = CircleShape,
            ) {
              IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("turf_detail_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  tint = Color.White,
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Surface(
                color = ObsidianBlack.copy(alpha = 0.65f),
                shape = CircleShape,
              ) {
                IconButton(
                  onClick = onToggleFavorite,
                  modifier = Modifier.testTag("turf_detail_favorite_button"),
                ) {
                  Icon(
                    imageVector =
                      if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Save turf",
                    tint = if (isFavorited) CrimsonRed else Color.White,
                  )
                }
              }
              Surface(
                color = ObsidianBlack.copy(alpha = 0.65f),
                shape = CircleShape,
              ) {
                IconButton(onClick = {}) {
                  Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share turf",
                    tint = Color.White,
                  )
                }
              }
            }
          }

          Surface(
            color = ObsidianBlack.copy(alpha = 0.78f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
          ) {
            Text(
              text = "${photoPagerState.currentPage + 1} / $photoCount",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }
      }

      // Turf Heading & Rating
      item {
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = turf.name,
              style = MaterialTheme.typography.headlineLarge,
              color = TextPrimary,
            )
            if (turf.isVerified) {
              Spacer(modifier = Modifier.width(8.dp))
              VerifiedBadge(tint = Color(0xFF38BDF8))
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = GoldTrophy,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "${turf.rating} (${turf.reviewCountLabel})",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(15.dp),
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "${turf.distanceKm} km",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = null,
              tint = CrimsonRed,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = turf.city,
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = turf.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
      }

      // Live Pitch Status + Today's Full Timeline (Read-Only, zero customer PII)
      if (hasLiveSlots) {
        item {
          Spacer(modifier = Modifier.height(16.dp))
          PlayerLivePitchStatusCard(
            turf = turf,
            liveSlotsToday = liveSlotsToday,
            nowMillis = nowMillis,
            modifier = Modifier.padding(horizontal = 18.dp),
          )
        }
        item {
          Spacer(modifier = Modifier.height(14.dp))
          PlayerTodayFullTimelineCard(
            turf = turf,
            liveSlotsToday = liveSlotsToday,
            nowMillis = nowMillis,
            modifier = Modifier.padding(horizontal = 18.dp),
          )
        }
      }

      // Amenities Row (4 icon tiles)
      item {
        Spacer(modifier = Modifier.height(18.dp))
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          turf.amenities.take(4).forEach { amenity ->
            val icon =
              when {
                amenity.contains("side", true) -> Icons.Filled.SportsSoccer
                amenity.contains("Flood", true) -> Icons.Filled.WbSunny
                amenity.contains("Changing", true) || amenity.contains("Showers", true) ->
                  Icons.Filled.Checkroom
                else -> Icons.Filled.LocalParking
              }
            AmenityIconTile(
              label = amenity,
              icon = icon,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Select Time Section
      item {
        Spacer(modifier = Modifier.height(22.dp))
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Select Time",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
            Text(
              text = "All slots GMT",
              style = MaterialTheme.typography.labelLarge,
              color = CrimsonRed,
            )
          }
          Spacer(modifier = Modifier.height(12.dp))

          // Date Cards
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            GambianFootballCatalog.dateOptions.take(3).forEach { dateLabel ->
              val isSelected = selectedDate == dateLabel
              val parts = dateLabel.split(" ")
              Surface(
                color = if (isSelected) CrimsonRed else ObsidianCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isSelected) CrimsonGlow else ObsidianBorder),
                modifier =
                  Modifier.weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { selectedDate = dateLabel }
                    .testTag("date_pill_$dateLabel"),
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = parts.firstOrNull().orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else TextSecondary,
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = parts.drop(1).joinToString(" "),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) Color.White else TextPrimary,
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Time Slot 3-Column Grid
          val rows = turf.timeSlots.chunked(3)
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { rowSlots ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                rowSlots.forEach { slot ->
                  val isSelected = selectedSlot == slot
                  Surface(
                    color = if (isSelected) CrimsonRed else ObsidianCard,
                    shape = RoundedCornerShape(12.dp),
                    border =
                      BorderStroke(
                        1.dp,
                        if (isSelected) CrimsonGlow else ObsidianBorder,
                      ),
                    modifier =
                      Modifier.weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedSlot = slot }
                        .testTag("slot_pill_$slot"),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = slot,
                        style =
                          MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight =
                              if (isSelected) FontWeight.Bold else FontWeight.Medium,
                          ),
                        color = if (isSelected) Color.White else TextPrimary,
                      )
                    }
                  }
                }
                repeat(3 - rowSlots.size) { Spacer(modifier = Modifier.weight(1f)) }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AmenityIconTile(
  label: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = ObsidianCard,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, ObsidianBorder),
    modifier = modifier.height(78.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = TextPrimary,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = TextSecondary,
        textAlign = TextAlign.Center,
        maxLines = 2,
      )
    }
  }
}

@Composable
private fun PlayerLivePitchStatusCard(
  turf: TurfVenue,
  liveSlotsToday: List<TurfSlot>,
  nowMillis: Long,
  modifier: Modifier = Modifier,
) {
  val statusInfo =
    remember(turf.openTime, turf.closeTime, turf.slotMinutes, turf.isOpen, liveSlotsToday, nowMillis) {
      GambianSlotTimeHelper.computeLiveStatusForTurf(
        openTimeRaw = turf.openTime,
        closeTimeRaw = turf.closeTime,
        slotMinutesRaw = turf.slotMinutes,
        isOpen = turf.isOpen,
        todaySlots = liveSlotsToday,
        nowMillis = nowMillis,
      )
    }
  val accent =
    when (statusInfo.state) {
      com.example.data.catalog.LiveTurfBadgeState.FREE_NOW -> EmeraldOpen
      com.example.data.catalog.LiveTurfBadgeState.IN_USE -> Color(0xFFF97316)
      com.example.data.catalog.LiveTurfBadgeState.BOOKED_NOW -> GoldTrophy
      com.example.data.catalog.LiveTurfBadgeState.FULLY_BOOKED -> CrimsonGlow
      com.example.data.catalog.LiveTurfBadgeState.CLOSED -> TextSecondary
    }
  val badgeBg = accent.copy(alpha = 0.16f)

  Surface(
    color = ObsidianCard,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
    modifier = modifier.fillMaxWidth().testTag("player_live_pitch_status_card"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(accent))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text =
              if (statusInfo.state == com.example.data.catalog.LiveTurfBadgeState.IN_USE) {
                "In use (${statusInfo.primaryDetail.lowercase()})"
              } else {
                statusInfo.slideTag
              },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
          )
        }
        Surface(color = badgeBg, shape = RoundedCornerShape(50)) {
          Text(
            text = "Live (GMT)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      if (statusInfo.endsAtMillis != null) {
        val remaining = (statusInfo.endsAtMillis - nowMillis).coerceAtLeast(0L)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${GambianSlotTimeHelper.formatCountdownColon(remaining)} left",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text =
          if (statusInfo.state == com.example.data.catalog.LiveTurfBadgeState.IN_USE) {
            statusInfo.secondaryDetail
          } else {
            "${statusInfo.primaryDetail} • ${statusInfo.secondaryDetail}"
          },
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
      )
    }
  }
}

@Composable
private fun PlayerTodayFullTimelineCard(
  turf: TurfVenue,
  liveSlotsToday: List<TurfSlot>,
  nowMillis: Long,
  modifier: Modifier = Modifier,
) {
  val timeline =
    remember(turf.openTime, turf.closeTime, turf.slotMinutes, liveSlotsToday, nowMillis) {
      GambianSlotTimeHelper.buildTodayDisplayTimeline(
        openTimeRaw = turf.openTime,
        closeTimeRaw = turf.closeTime,
        slotMinutesRaw = turf.slotMinutes,
        todaySlots = liveSlotsToday,
        nowMillis = nowMillis,
      )
    }

  Surface(
    color = ObsidianCard,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, ObsidianBorder),
    modifier = modifier.fillMaxWidth().testTag("player_today_timeline_card"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Today's Schedule",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )
        Text(
          text = "GMT",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      if (timeline.isEmpty()) {
        Text(
          text = "No schedule slots published for today yet.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          timeline.forEach { slot ->
            val (statusLabel, statusColor) =
              when (slot.status) {
                "available" -> "Available" to EmeraldOpen
                "in_progress" -> "In use" to Color(0xFFF97316)
                "booked" -> "Booked" to CrimsonGlow
                "blocked" -> "Booked" to Color(0xFF9CA3AF)
                else -> "Available" to EmeraldOpen
              }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text =
                    "${GambianSlotTimeHelper.formatHhMmTo12Hour(slot.startTime)} – ${GambianSlotTimeHelper.formatHhMmTo12Hour(slot.endTime)}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextPrimary,
                )
              }
              Surface(
                color = statusColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
              ) {
                Text(
                  text = statusLabel,
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

