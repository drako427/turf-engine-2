package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.catalog.GambianSlotTimeHelper
import com.example.data.catalog.TurfPhotoCompressor
import com.example.data.model.TurfListing
import com.example.data.model.TurfSlot
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldOpen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun OwnerNewSessionScreen(
  turf: TurfListing,
  selectedDateIso: String,
  slotsForDay: List<TurfSlot>,
  initialStartTime: String?,
  isBusy: Boolean,
  onBack: () -> Unit,
  onSubmitSession: (
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
) {
  BackHandler(onBack = onBack)

  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(5000L)
      nowMillis = System.currentTimeMillis()
    }
  }

  val fallbackWindows =
    remember(turf.openTime, turf.closeTime, turf.slotMinutes) {
      GambianSlotTimeHelper.generateSlotWindows(
        turf.effectiveOpenTime(),
        turf.effectiveCloseTime(),
        turf.effectiveSlotMinutes(),
      )
    }

  val startTimeOptions =
    remember(slotsForDay, fallbackWindows) {
      if (slotsForDay.isNotEmpty()) slotsForDay.map { it.startTime }.distinct().sorted()
      else fallbackWindows.map { it.startTime }
    }
  val endTimeOptionsAll =
    remember(slotsForDay, fallbackWindows) {
      if (slotsForDay.isNotEmpty()) slotsForDay.map { it.endTime }.distinct().sorted()
      else fallbackWindows.map { it.endTime }
    }

  val defaultStart =
    remember(initialStartTime, slotsForDay, startTimeOptions) {
      if (!initialStartTime.isNullOrBlank() && startTimeOptions.contains(initialStartTime)) {
        initialStartTime
      } else {
        val nowHhMm = GambianSlotTimeHelper.currentHhMmGmt()
        val isToday = selectedDateIso == GambianSlotTimeHelper.todayIsoDateGmt()
        val bestOpen =
          slotsForDay
            .filter { it.effectiveStatus() == "available" && (!isToday || it.endTime > nowHhMm) }
            .minByOrNull { it.startTime }
            ?.startTime
        bestOpen ?: startTimeOptions.firstOrNull() ?: "16:00"
      }
    }

  var customerName by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var startTime by remember(defaultStart) { mutableStateOf(defaultStart) }
  var finishTime by remember(defaultStart, endTimeOptionsAll) {
    val startMins = GambianSlotTimeHelper.parseHhMmToMinutes(defaultStart) ?: 960
    val twoHourTarget = GambianSlotTimeHelper.minutesToHhMm(startMins + 120)
    val preferred =
      endTimeOptionsAll.firstOrNull { it == twoHourTarget }
        ?: endTimeOptionsAll.firstOrNull {
          (GambianSlotTimeHelper.parseHhMmToMinutes(it) ?: 0) > startMins
        }
        ?: twoHourTarget
    mutableStateOf(preferred)
  }

  val hoursSelected =
    remember(startTime, finishTime) {
      GambianSlotTimeHelper.hoursBetween(startTime, finishTime).coerceAtLeast(0.0)
    }
  val autoCalculatedPrice =
    remember(hoursSelected, turf.pricePerHour) {
      (hoursSelected * turf.pricePerHour).toLong().coerceAtLeast(0L)
    }

  var totalPriceInput by remember(autoCalculatedPrice) {
    mutableStateOf(autoCalculatedPrice.toString())
  }
  var paymentMethod by remember { mutableStateOf("cash") }
  var showStartMenu by remember { mutableStateOf(false) }
  var showFinishMenu by remember { mutableStateOf(false) }
  var formError by remember { mutableStateOf<String?>(null) }

  val coveredSlots =
    remember(slotsForDay, startTime, finishTime) {
      GambianSlotTimeHelper.findCoveredSlotsForRange(slotsForDay, startTime, finishTime)
    }
  val overlappingConflict =
    remember(coveredSlots, nowMillis) {
      coveredSlots.firstOrNull {
        it.effectiveStatus(nowMillis) in listOf("booked", "in_progress", "blocked")
      }
    }
  val isStartNow =
    remember(selectedDateIso, startTime, nowMillis) {
      GambianSlotTimeHelper.isStartTimeNow(selectedDateIso, startTime, nowMillis)
    }

  Box(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter),
    ) {
      // Header Bar
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(48.dp).testTag("new_session_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Back",
            tint = TextPrimary,
            modifier = Modifier.size(28.dp),
          )
        }
        Text(
          text = "New Session",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )
        Box(
          modifier = Modifier.size(48.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = CrimsonGlow,
            modifier = Modifier.size(22.dp),
          )
        }
      }

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Turf Info Header Card
        Surface(
          color = ObsidianCard,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, ObsidianBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val coverBitmap =
              remember(turf.photos) {
                turf.photos.firstOrNull()?.let { TurfPhotoCompressor.decodeDataUriToImageBitmap(it) }
              }
            Box(
              modifier =
                Modifier.size(width = 88.dp, height = 62.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(ObsidianCardElevated),
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
            }
            Spacer(modifier = Modifier.width(12.dp))
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
                if (turf.verificationStatus == "verified") {
                  Spacer(modifier = Modifier.width(6.dp))
                  VerifiedBadge(tint = Color(0xFF38BDF8))
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.LocationOn,
                  contentDescription = null,
                  tint = Color(0xFFF59E0B),
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = turf.city,
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        // Customer / Team Name *
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Groups,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Customer / Team Name",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              color = TextPrimary,
            )
            Text(" *", color = CrimsonGlow, fontWeight = FontWeight.Bold)
          }
          OutlinedTextField(
            value = customerName,
            onValueChange = {
              customerName = it
              formError = null
            },
            placeholder = { Text("Young Stars FC", color = TextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = sessionFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("new_session_customer_input"),
          )
        }

        // Phone Number *
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Phone,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Phone Number",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              color = TextPrimary,
            )
            Text(" *", color = CrimsonGlow, fontWeight = FontWeight.Bold)
          }
          OutlinedTextField(
            value = phone,
            onValueChange = {
              phone = it
              formError = null
            },
            placeholder = { Text("+220 7012345", color = TextMuted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(14.dp),
            colors = sessionFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("new_session_phone_input"),
          )
        }

        // Start Time * & Finish Time * side by side
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(modifier = Modifier.weight(1f)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.RadioButtonUnchecked,
                  contentDescription = null,
                  tint = TextSecondary,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Start Time",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                  color = TextPrimary,
                )
                Text(" *", color = CrimsonGlow, fontWeight = FontWeight.Bold)
              }
              Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier =
                  Modifier.fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showStartMenu = true }
                    .testTag("new_session_start_time_picker"),
              ) {
                Row(
                  modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = GambianSlotTimeHelper.formatHhMmTo12Hour(startTime),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary,
                  )
                  Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Select start time",
                    tint = TextSecondary,
                  )
                }
              }
            }
            DropdownMenu(
              expanded = showStartMenu,
              onDismissRequest = { showStartMenu = false },
              containerColor = ObsidianCardElevated,
            ) {
              startTimeOptions.forEach { hhMm ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = GambianSlotTimeHelper.formatHhMmTo12Hour(hhMm),
                      color = TextPrimary,
                    )
                  },
                  onClick = {
                    val currentDurMins =
                      ((GambianSlotTimeHelper.parseHhMmToMinutes(finishTime) ?: 0) -
                          (GambianSlotTimeHelper.parseHhMmToMinutes(startTime) ?: 0))
                        .coerceAtLeast(turf.effectiveSlotMinutes().toInt())
                    startTime = hhMm
                    val newStartMins = GambianSlotTimeHelper.parseHhMmToMinutes(hhMm) ?: 960
                    val desiredFinish = GambianSlotTimeHelper.minutesToHhMm(newStartMins + currentDurMins)
                    val matchingEnd =
                      endTimeOptionsAll.firstOrNull { it == desiredFinish }
                        ?: endTimeOptionsAll.lastOrNull {
                          (GambianSlotTimeHelper.parseHhMmToMinutes(it) ?: 0) > newStartMins &&
                            (GambianSlotTimeHelper.parseHhMmToMinutes(it) ?: 0) <=
                              newStartMins + currentDurMins
                        }
                        ?: endTimeOptionsAll.firstOrNull {
                          (GambianSlotTimeHelper.parseHhMmToMinutes(it) ?: 0) > newStartMins
                        }
                        ?: desiredFinish
                    finishTime = matchingEnd
                    formError = null
                    showStartMenu = false
                  },
                )
              }
            }
          }

          Box(modifier = Modifier.weight(1f)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.RadioButtonChecked,
                  contentDescription = null,
                  tint = TextSecondary,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Finish Time",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                  color = TextPrimary,
                )
                Text(" *", color = CrimsonGlow, fontWeight = FontWeight.Bold)
              }
              Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier =
                  Modifier.fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showFinishMenu = true }
                    .testTag("new_session_finish_time_picker"),
              ) {
                Row(
                  modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = GambianSlotTimeHelper.formatHhMmTo12Hour(finishTime),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary,
                  )
                  Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Select finish time",
                    tint = TextSecondary,
                  )
                }
              }
            }
            DropdownMenu(
              expanded = showFinishMenu,
              onDismissRequest = { showFinishMenu = false },
              containerColor = ObsidianCardElevated,
            ) {
              val sMins = GambianSlotTimeHelper.parseHhMmToMinutes(startTime) ?: 0
              endTimeOptionsAll
                .filter { (GambianSlotTimeHelper.parseHhMmToMinutes(it) ?: 0) > sMins }
                .forEach { hhMm ->
                  DropdownMenuItem(
                    text = {
                      Text(
                        text = GambianSlotTimeHelper.formatHhMmTo12Hour(hhMm),
                        color = TextPrimary,
                      )
                    },
                    onClick = {
                      finishTime = hhMm
                      formError = null
                      showFinishMenu = false
                    },
                  )
                }
            }
          }
        }

        // Quick duration chips (1h, 2h, 3h)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          listOf(1, 2, 3).forEach { hrs ->
            val startMins = GambianSlotTimeHelper.parseHhMmToMinutes(startTime) ?: 960
            val targetEnd = GambianSlotTimeHelper.minutesToHhMm(startMins + hrs * 60)
            val targetValid =
              endTimeOptionsAll.contains(targetEnd) ||
                (startMins + hrs * 60) <=
                  (GambianSlotTimeHelper.parseHhMmToMinutes(turf.effectiveCloseTime()) ?: 24 * 60)
            val isSelected = kotlin.math.abs(hoursSelected - hrs.toDouble()) < 0.05

            Surface(
              color = if (isSelected) CrimsonRed else ObsidianCard,
              shape = RoundedCornerShape(50),
              border = BorderStroke(1.dp, if (isSelected) CrimsonGlow else ObsidianBorder),
              modifier =
                Modifier.weight(1f)
                  .height(44.dp)
                  .clip(RoundedCornerShape(50))
                  .clickable(enabled = targetValid) {
                    finishTime = targetEnd
                    formError = null
                  }
                  .testTag("duration_chip_${hrs}h"),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "${hrs}h",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color =
                    when {
                      isSelected -> Color.White
                      targetValid -> TextPrimary
                      else -> TextMuted
                    },
                )
              }
            }
          }
        }

        // Price Summary card (auto-calculated + editable for discounts)
        Surface(
          color = ObsidianCard,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, ObsidianBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.LocalOffer,
                contentDescription = null,
                tint = CrimsonGlow,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Price Summary",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextSecondary,
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              val hoursLabel =
                if (hoursSelected % 1.0 == 0.0) {
                  val intH = hoursSelected.toInt()
                  "$intH ${if (intH == 1) "hour" else "hours"}"
                } else {
                  String.format(Locale.US, "%.1f hours", hoursSelected)
                }
              Text(
                text = "$hoursLabel x D${turf.pricePerHour}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
              )

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "= D",
                  style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                  color = CrimsonGlow,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Surface(
                  color = ObsidianCardElevated,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, ObsidianBorder),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    BasicTextField(
                      value = totalPriceInput,
                      onValueChange = { v -> totalPriceInput = v.filter { it.isDigit() }.take(6) },
                      singleLine = true,
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      textStyle =
                        TextStyle(
                          color = CrimsonGlow,
                          fontSize = 20.sp,
                          fontWeight = FontWeight.Bold,
                          textAlign = TextAlign.End,
                        ),
                      cursorBrush = SolidColor(CrimsonGlow),
                      modifier =
                        Modifier.widthIn(min = 48.dp, max = 96.dp)
                          .testTag("new_session_total_price_input"),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                      imageVector = Icons.Filled.Edit,
                      contentDescription = "Edit discount price",
                      tint = TextMuted,
                      modifier = Modifier.size(14.dp),
                    )
                  }
                }
              }
            }
          }
        }

        // Payment Method (Cash or Wave — recorded only)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Wallet,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Payment Method",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              color = TextPrimary,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            PaymentMethodCard(
              label = "Cash",
              selected = paymentMethod == "cash",
              isWave = false,
              onClick = { paymentMethod = "cash" },
              modifier = Modifier.weight(1f).testTag("payment_method_cash"),
            )
            PaymentMethodCard(
              label = "Wave",
              selected = paymentMethod == "wave",
              isWave = true,
              onClick = { paymentMethod = "wave" },
              modifier = Modifier.weight(1f).testTag("payment_method_wave"),
            )
          }
        }

        // Overlap Warning / Error Banner
        val conflictBannerText =
          when {
            overlappingConflict != null -> {
              val label =
                "${GambianSlotTimeHelper.formatHhMmTo12Hour(overlappingConflict.startTime)} – ${GambianSlotTimeHelper.formatHhMmTo12Hour(overlappingConflict.endTime)}"
              val stateWord =
                when (overlappingConflict.effectiveStatus(nowMillis)) {
                  "in_progress" -> "in use"
                  "booked" -> "already booked"
                  else -> "blocked"
                }
              "Overlaps a $stateWord slot ($label). Please select another time."
            }
            coveredSlots.isEmpty() ->
              "No open schedule slots found for $selectedDateIso in that range."
            else -> formError
          }

        if (conflictBannerText != null) {
          Surface(
            color = CrimsonRed.copy(alpha = 0.16f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().testTag("new_session_error_banner"),
          ) {
            Text(
              text = conflictBannerText,
              color = TextPrimary,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
      }

      // Bottom Submit Action ("Start Timer" when now, "Confirm Booking" when later)
      Surface(
        color = ObsidianBlack,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
      ) {
        Button(
          onClick = {
            val cleanName = customerName.trim()
            val cleanPhone = phone.trim()
            val parsedTotal = totalPriceInput.toLongOrNull() ?: autoCalculatedPrice
            when {
              cleanName.isEmpty() -> formError = "Customer or team name is required."
              cleanPhone.isEmpty() -> formError = "Phone number is required."
              hoursSelected <= 0.0 -> formError = "Finish time must be after start time."
              coveredSlots.isEmpty() ->
                formError = "No slots cover the selected time range on $selectedDateIso."
              overlappingConflict != null -> {
                val label =
                  "${GambianSlotTimeHelper.formatHhMmTo12Hour(overlappingConflict.startTime)}–${GambianSlotTimeHelper.formatHhMmTo12Hour(overlappingConflict.endTime)}"
                formError = "Selected time overlaps a ${overlappingConflict.effectiveStatus(nowMillis)} slot ($label)."
              }
              else -> {
                formError = null
                onSubmitSession(
                  selectedDateIso,
                  coveredSlots,
                  cleanName,
                  cleanPhone,
                  startTime,
                  finishTime,
                  hoursSelected,
                  parsedTotal,
                  paymentMethod,
                  isStartNow,
                ) { result ->
                  if (result.isFailure) {
                    formError =
                      result.exceptionOrNull()?.localizedMessage ?: "Could not save session."
                  }
                }
              }
            }
          },
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().height(56.dp).testTag("new_session_submit_button"),
        ) {
          if (isBusy) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.5.dp,
              modifier = Modifier.size(22.dp),
            )
          } else {
            Icon(
              imageVector =
                if (isStartNow) Icons.Filled.PlayArrow else Icons.Filled.CheckCircle,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isStartNow) "Start Timer" else "Confirm Booking",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PaymentMethodCard(
  label: String,
  selected: Boolean,
  isWave: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = ObsidianCard,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.5.dp, if (selected) CrimsonRed else ObsidianBorder),
    modifier = modifier.height(62.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
  ) {
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
      Row(
        modifier = Modifier.align(Alignment.CenterStart),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (isWave) {
          Surface(
            color = Color(0xFF1D4ED8).copy(alpha = 0.25f),
            shape = CircleShape,
            modifier = Modifier.size(30.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "W",
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
              )
            }
          }
        } else {
          Surface(
            color = EmeraldOpen.copy(alpha = 0.2f),
            shape = CircleShape,
            modifier = Modifier.size(30.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Payments,
                contentDescription = null,
                tint = EmeraldOpen,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary,
        )
      }
      if (selected) {
        Surface(
          color = CrimsonRed,
          shape = CircleShape,
          modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp).size(18.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(12.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun sessionFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CrimsonRed,
    unfocusedBorderColor = ObsidianBorder,
    focusedContainerColor = ObsidianCardElevated,
    unfocusedContainerColor = ObsidianCard,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = CrimsonRed,
  )
