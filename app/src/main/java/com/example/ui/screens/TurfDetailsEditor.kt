package com.example.ui.screens

import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.catalog.GambianSlotTimeHelper
import com.example.data.catalog.TurfPhotoCompressor
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.backgroundColor
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.lineColor

val PITCH_SIZE_OPTIONS = listOf("5-a-side", "7-a-side", "11-a-side")
val FACILITY_OPTIONS = listOf("Floodlights", "Parking", "Changing rooms", "Water", "Seating")

private val OPENING_TIME_OPTIONS: List<String> =
  (6..22).map { h -> String.format(java.util.Locale.US, "%02d:00", h) }
private val CLOSING_TIME_OPTIONS: List<String> =
  (8..24).map { h -> String.format(java.util.Locale.US, "%02d:00", h) }

data class TurfDetailsSubmission(
  val turfName: String,
  val address: String,
  val latitude: Double,
  val longitude: Double,
  val pricePerHour: Long,
  val primaryFormat: String,
  val facilities: List<String>,
  val openTime: String,
  val closeTime: String,
  val photos: List<String>,
  val description: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TurfDetailsFormContent(
  initialTurfName: String,
  initialAddress: String,
  initialLatitude: Double,
  initialLongitude: Double,
  initialPricePerHour: Long,
  initialFormat: String,
  initialFacilities: List<String>,
  initialOpenTime: String,
  initialCloseTime: String,
  initialPhotos: List<String>,
  initialDescription: String,
  isBusy: Boolean,
  submitButtonLabel: String = "Save and Publish",
  externalErrorMessage: String? = null,
  extraBeforeError: @Composable () -> Unit = {},
  extraBeforeSubmit: @Composable () -> Unit = {},
  validateExtraBeforeSubmit: () -> String? = { null },
  onSubmit: (TurfDetailsSubmission) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var turfName by remember(initialTurfName) { mutableStateOf(initialTurfName) }
  var address by remember(initialAddress) { mutableStateOf(initialAddress) }
  var latitude by remember(initialLatitude) { mutableDoubleStateOf(initialLatitude) }
  var longitude by remember(initialLongitude) { mutableDoubleStateOf(initialLongitude) }
  var hasExplicitPin by remember { mutableStateOf(false) }
  var priceInput by remember(initialPricePerHour) {
    mutableStateOf(if (initialPricePerHour > 0L) initialPricePerHour.toString() else "450")
  }
  var primaryFormat by remember(initialFormat) {
    mutableStateOf(
      PITCH_SIZE_OPTIONS.firstOrNull { it.equals(initialFormat, ignoreCase = true) } ?: "7-a-side"
    )
  }
  var selectedFacilities by remember(initialFacilities) {
    val matched =
      FACILITY_OPTIONS.filter { opt ->
        initialFacilities.any { it.equals(opt, ignoreCase = true) }
      }.toSet()
    mutableStateOf(if (matched.isEmpty() && initialFacilities.isEmpty()) setOf("Floodlights") else matched)
  }
  var openTime by remember(initialOpenTime) {
    mutableStateOf(GambianSlotTimeHelper.normalizeTimeOrDefault(initialOpenTime, "08:00"))
  }
  var closeTime by remember(initialCloseTime) {
    mutableStateOf(GambianSlotTimeHelper.normalizeTimeOrDefault(initialCloseTime, "24:00"))
  }
  var photos by remember(initialPhotos) { mutableStateOf(initialPhotos.take(6)) }
  var description by remember(initialDescription) { mutableStateOf(initialDescription) }

  var isCompressingPhotos by remember { mutableStateOf(false) }
  var showMapPicker by remember { mutableStateOf(false) }
  var showOpenMenu by remember { mutableStateOf(false) }
  var showCloseMenu by remember { mutableStateOf(false) }
  var localError by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris ->
      if (uris.isEmpty()) return@rememberLauncherForActivityResult
      scope.launch {
        isCompressingPhotos = true
        val remaining = (6 - photos.size).coerceAtLeast(0)
        val compressed = mutableListOf<String>()
        for (uri in uris.take(remaining)) {
          val encoded = TurfPhotoCompressor.compressUriToDataUri(context, uri)
          if (encoded != null) {
            compressed += encoded
          }
        }
        photos = (photos + compressed).take(6)
        isCompressingPhotos = false
      }
    }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Photo Grid (up to 6 photos, Cover photo badge + x button + Add more tile)
    TurfPhotoGridRow(
      photos = photos,
      isCompressing = isCompressingPhotos,
      onRemovePhoto = { index ->
        photos = photos.filterIndexed { i, _ -> i != index }
      },
      onAddPhotos = {
        photoPickerLauncher.launch(
          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
      },
    )

    extraBeforeError()

    val shownError = localError ?: externalErrorMessage
    if (shownError != null) {
      Surface(
        color = CrimsonRed.copy(alpha = 0.16f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().testTag("turf_details_error_banner"),
      ) {
        Text(
          text = shownError,
          color = TextPrimary,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    // Turf Name *
    LabeledDarkTextField(
      icon = Icons.Filled.SportsSoccer,
      label = "Turf Name",
      required = true,
      value = turfName,
      onValueChange = {
        turfName = it
        localError = null
      },
      placeholder = "e.g., Gambia Sports Arena",
      testTag = "turf_details_name_input",
    )

    // Address * + Pin on map
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      FieldIconHeader(icon = Icons.Filled.LocationOn, label = "Address", required = true)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = address,
          onValueChange = {
            address = it
            localError = null
          },
          placeholder = { Text("Banjul, Gambia", color = TextMuted) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = darkFieldColors(),
          modifier = Modifier.weight(1f).testTag("turf_details_address_input"),
        )
        Surface(
          color = CrimsonRed.copy(alpha = 0.14f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.7f)),
          modifier =
            Modifier.height(54.dp)
              .clip(RoundedCornerShape(14.dp))
              .clickable { showMapPicker = true }
              .testTag("turf_pin_on_map_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = "Pin on map",
              tint = CrimsonGlow,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Pin on map",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              color = CrimsonGlow,
            )
          }
        }
      }
      if (hasExplicitPin) {
        Text(
          text =
            "Pinned at ${String.format(java.util.Locale.US, "%.4f, %.4f", latitude, longitude)}",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }
    }

    // Price per hour (D) *
    LabeledDarkTextField(
      icon = Icons.Filled.Payments,
      label = "Price per hour (D)",
      required = true,
      value = priceInput,
      onValueChange = { value ->
        priceInput = value.filter { it.isDigit() }
        localError = null
      },
      placeholder = "450",
      keyboardType = KeyboardType.Number,
      testTag = "turf_details_price_input",
    )

    // Pitch Size
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      FieldIconHeader(icon = Icons.Filled.SportsSoccer, label = "Pitch Size", required = false)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PITCH_SIZE_OPTIONS.forEach { sizeOption ->
          val selected = primaryFormat == sizeOption
          Surface(
            color = if (selected) CrimsonRed else ObsidianCard,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, if (selected) CrimsonGlow else ObsidianBorder),
            modifier =
              Modifier.weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(50))
                .clickable { primaryFormat = sizeOption }
                .testTag("pitch_size_$sizeOption"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = sizeOption,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) Color.White else TextSecondary,
              )
            }
          }
        }
      }
    }

    // Facilities (multi-select)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      FieldIconHeader(icon = Icons.Filled.Build, label = "Facilities", required = false)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FACILITY_OPTIONS.forEach { facility ->
          val selected = selectedFacilities.contains(facility)
          Surface(
            color = if (selected) CrimsonRed else ObsidianCard,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, if (selected) CrimsonGlow else ObsidianBorder),
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .clickable {
                  selectedFacilities =
                    if (selected) selectedFacilities - facility else selectedFacilities + facility
                }
                .testTag("facility_chip_$facility"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = if (selected) Color.White else TextSecondary,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = facility,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else TextSecondary,
              )
            }
          }
        }
      }
    }

    // Opening Time & Closing Time side by side
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(modifier = Modifier.weight(1f)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          FieldIconHeader(icon = Icons.Filled.Schedule, label = "Opening Time", required = false)
          Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier =
              Modifier.fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { showOpenMenu = true }
                .testTag("turf_open_time_picker"),
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = GambianSlotTimeHelper.formatHhMmTo12Hour(openTime),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary,
              )
              Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Select opening time",
                tint = TextSecondary,
              )
            }
          }
        }
        DropdownMenu(
          expanded = showOpenMenu,
          onDismissRequest = { showOpenMenu = false },
          containerColor = ObsidianCardElevated,
        ) {
          OPENING_TIME_OPTIONS.forEach { hhMm ->
            DropdownMenuItem(
              text = {
                Text(
                  text = GambianSlotTimeHelper.formatHhMmTo12Hour(hhMm),
                  color = TextPrimary,
                )
              },
              onClick = {
                openTime = hhMm
                val oMins = GambianSlotTimeHelper.parseHhMmToMinutes(hhMm) ?: (8 * 60)
                val cMins = GambianSlotTimeHelper.parseHhMmToMinutes(closeTime) ?: (23 * 60)
                if (cMins <= oMins) {
                  closeTime = GambianSlotTimeHelper.minutesToHhMm((oMins + 120).coerceAtMost(24 * 60))
                }
                showOpenMenu = false
              },
            )
          }
        }
      }

      Box(modifier = Modifier.weight(1f)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Closing Time",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
          )
          Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier =
              Modifier.fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { showCloseMenu = true }
                .testTag("turf_close_time_picker"),
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = GambianSlotTimeHelper.formatHhMmTo12Hour(closeTime),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary,
              )
              Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Select closing time",
                tint = TextSecondary,
              )
            }
          }
        }
        DropdownMenu(
          expanded = showCloseMenu,
          onDismissRequest = { showCloseMenu = false },
          containerColor = ObsidianCardElevated,
        ) {
          val openMins = GambianSlotTimeHelper.parseHhMmToMinutes(openTime) ?: (8 * 60)
          CLOSING_TIME_OPTIONS.filter {
            (GambianSlotTimeHelper.parseHhMmToMinutes(it) ?: 0) > openMins
          }.forEach { hhMm ->
            DropdownMenuItem(
              text = {
                Text(
                  text = GambianSlotTimeHelper.formatHhMmTo12Hour(hhMm),
                  color = TextPrimary,
                )
              },
              onClick = {
                closeTime = hhMm
                showCloseMenu = false
              },
            )
          }
        }
      }
    }

    // Short Description
    LabeledDarkTextField(
      icon = Icons.Filled.Description,
      label = "Short Description",
      required = false,
      value = description,
      onValueChange = { description = it },
      placeholder = "Modern turf with great atmosphere. Come play!",
      singleLine = false,
      testTag = "turf_details_description_input",
    )

    extraBeforeSubmit()

    Button(
      onClick = {
        val parsedPrice = priceInput.toLongOrNull() ?: 0L
        val openMins = GambianSlotTimeHelper.parseHhMmToMinutes(openTime) ?: (8 * 60)
        val closeMins = GambianSlotTimeHelper.parseHhMmToMinutes(closeTime) ?: (23 * 60)
        val extraError = validateExtraBeforeSubmit()
        when {
          extraError != null -> localError = extraError
          turfName.trim().length < 2 -> localError = "Please enter the turf name."
          address.trim().length < 2 -> localError = "Please enter the turf address."
          parsedPrice < 50L -> localError = "Price per hour must be at least D50."
          closeMins <= openMins -> localError = "Closing time must be after opening time."
          else -> {
            localError = null
            onSubmit(
              TurfDetailsSubmission(
                turfName = turfName.trim(),
                address = address.trim(),
                latitude = latitude,
                longitude = longitude,
                pricePerHour = parsedPrice,
                primaryFormat = primaryFormat,
                facilities = selectedFacilities.toList(),
                openTime = openTime,
                closeTime = closeTime,
                photos = photos,
                description = description.trim(),
              )
            )
          }
        }
      },
      enabled = !isBusy && !isCompressingPhotos,
      colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().height(54.dp).testTag("turf_save_publish_button"),
    ) {
      if (isBusy) {
        CircularProgressIndicator(
          color = Color.White,
          strokeWidth = 2.5.dp,
          modifier = Modifier.size(22.dp),
        )
      } else {
        Icon(
          imageVector = Icons.Filled.Save,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = submitButtonLabel,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
      }
    }
  }

  if (showMapPicker) {
    PinOnMapDialog(
      initialLatitude = latitude,
      initialLongitude = longitude,
      onDismiss = { showMapPicker = false },
      onConfirm = { pickedLat, pickedLng ->
        latitude = pickedLat
        longitude = pickedLng
        hasExplicitPin = true
        showMapPicker = false
      },
    )
  }
}

@Composable
private fun TurfPhotoGridRow(
  photos: List<String>,
  isCompressing: Boolean,
  onRemovePhoto: (Int) -> Unit,
  onAddPhotos: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    photos.forEachIndexed { index, dataUri ->
      val bitmap = remember(dataUri) { TurfPhotoCompressor.decodeDataUriToImageBitmap(dataUri) }
      Box(
        modifier =
          Modifier.size(width = 96.dp, height = 104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianCardElevated),
      ) {
        if (bitmap != null) {
          Image(
            bitmap = bitmap,
            contentDescription = if (index == 0) "Cover photo" else "Turf photo ${index + 1}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
        Surface(
          color = ObsidianBlack.copy(alpha = 0.78f),
          shape = CircleShape,
          modifier =
            Modifier.align(Alignment.TopEnd)
              .padding(5.dp)
              .size(24.dp)
              .clip(CircleShape)
              .clickable { onRemovePhoto(index) }
              .testTag("remove_turf_photo_$index"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "Remove photo",
              tint = Color.White,
              modifier = Modifier.size(14.dp),
            )
          }
        }
        if (index == 0) {
          Surface(
            color = CrimsonRed,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
          ) {
            Text(
              text = "Cover photo",
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
      }
    }

    if (photos.size < 6) {
      Box(
        modifier =
          Modifier.size(width = 104.dp, height = 104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianCard)
            .clickable(enabled = !isCompressing, onClick = onAddPhotos)
            .testTag("add_turf_photo_tile"),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawRoundRect(
            color = Color(0xFF4A4A5A),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
            style =
              Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
              ),
          )
        }
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(8.dp),
        ) {
          if (isCompressing) {
            CircularProgressIndicator(
              color = CrimsonRed,
              strokeWidth = 2.dp,
              modifier = Modifier.size(22.dp),
            )
          } else {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "Add more photos",
              tint = TextPrimary,
              modifier = Modifier.size(22.dp),
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Add more\n(${photos.size}/6 photos)",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
          )
        }
      }
    }
  }
}

@Composable
private fun FieldIconHeader(
  icon: ImageVector,
  label: String,
  required: Boolean,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = TextSecondary,
      modifier = Modifier.size(16.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
      color = TextPrimary,
    )
    if (required) {
      Text(
        text = " *",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = CrimsonGlow,
      )
    }
  }
}

@Composable
private fun LabeledDarkTextField(
  icon: ImageVector,
  label: String,
  required: Boolean,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  testTag: String,
  singleLine: Boolean = true,
  keyboardType: KeyboardType = KeyboardType.Text,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    FieldIconHeader(icon = icon, label = label, required = required)
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = { Text(placeholder, color = TextMuted) },
      singleLine = singleLine,
      minLines = if (singleLine) 1 else 3,
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
      shape = RoundedCornerShape(14.dp),
      colors = darkFieldColors(),
      modifier = Modifier.fillMaxWidth().testTag(testTag),
    )
  }
}

@Composable
private fun darkFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CrimsonRed,
    unfocusedBorderColor = ObsidianBorder,
    focusedContainerColor = ObsidianCardElevated,
    unfocusedContainerColor = ObsidianCard,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = CrimsonRed,
  )

@Composable
fun PinOnMapDialog(
  initialLatitude: Double,
  initialLongitude: Double,
  onDismiss: () -> Unit,
  onConfirm: (latitude: Double, longitude: Double) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var pinnedLat by remember { mutableDoubleStateOf(initialLatitude) }
  var pinnedLng by remember { mutableDoubleStateOf(initialLongitude) }

  val mapInitialized = remember(context) {
    runCatching {
      MapLibre.getInstance(context.applicationContext)
      true
    }.getOrDefault(false)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = ObsidianBlack,
      shape = RoundedCornerShape(20.dp),
      border = BorderStroke(1.dp, ObsidianBorder),
      modifier = Modifier.fillMaxSize().padding(14.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Pin Turf Location",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
            )
            Text(
              text = "Move map or tap to position the pin on your pitch",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Close map picker", tint = TextPrimary)
          }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
          if (mapInitialized) {
            val mapView = remember(context) { MapView(context).apply { onCreate(null) } }
            DisposableEffect(lifecycleOwner, mapView) {
              val observer = LifecycleEventObserver { _, event ->
                when (event) {
                  Lifecycle.Event.ON_START -> mapView.onStart()
                  Lifecycle.Event.ON_RESUME -> mapView.onResume()
                  Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                  Lifecycle.Event.ON_STOP -> mapView.onStop()
                  Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                  else -> Unit
                }
              }
              lifecycleOwner.lifecycle.addObserver(observer)
              onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView.onDestroy()
              }
            }

            AndroidView(
              modifier = Modifier.fillMaxSize(),
              factory = {
                mapView.apply {
                  addOnDidFailLoadingMapListener {
                    getMapAsync { m ->
                      if (m.style == null) {
                        m.setStyle(
                          Style.Builder()
                            .fromJson(
                              """{"version":8,"sources":{},"layers":[{"id":"bg","type":"background","paint":{"background-color":"#0A0A0E"}}]}"""
                            )
                        )
                      }
                    }
                  }
                  getMapAsync { m ->
                    m.uiSettings.isLogoEnabled = false
                    m.uiSettings.isAttributionEnabled = false
                    m.cameraPosition =
                      CameraPosition.Builder()
                        .target(LatLng(initialLatitude, initialLongitude))
                        .zoom(14.5)
                        .build()
                    m.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/dark")) {
                      style ->
                      style.layers.forEach { layer ->
                        when (layer) {
                          is BackgroundLayer ->
                            layer.setProperties(backgroundColor(AndroidColor.parseColor("#0A0A0E")))
                          is FillLayer ->
                            if (layer.id.contains("water", true)) {
                              layer.setProperties(fillColor(AndroidColor.parseColor("#070B14")))
                            }
                          is LineLayer ->
                            if (layer.id.contains("road", true) || layer.id.contains("highway", true)) {
                              layer.setProperties(lineColor(AndroidColor.parseColor("#2A2F42")))
                            }
                        }
                      }
                    }
                    m.addOnCameraIdleListener {
                      val center = m.cameraPosition.target ?: return@addOnCameraIdleListener
                      pinnedLat = center.latitude
                      pinnedLng = center.longitude
                    }
                    m.addOnMapClickListener { latLng ->
                      pinnedLat = latLng.latitude
                      pinnedLng = latLng.longitude
                      m.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.newLatLng(latLng),
                        250,
                      )
                      true
                    }
                  }
                }
              },
            )
          }

          // Center crosshair pin
          Surface(
            color = CrimsonRed,
            shape = CircleShape,
            border = BorderStroke(2.5.dp, Color.White),
            modifier = Modifier.align(Alignment.Center).size(36.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Selected turf pin",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
              )
            }
          }

          Surface(
            color = ObsidianBlack.copy(alpha = 0.85f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
          ) {
            Text(
              text =
                String.format(java.util.Locale.US, "%.5f, %.5f", pinnedLat, pinnedLng),
              style = MaterialTheme.typography.labelMedium,
              color = TextPrimary,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onDismiss,
            border = BorderStroke(1.dp, ObsidianBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Text("Cancel", color = TextPrimary)
          }
          Button(
            onClick = { onConfirm(pinnedLat, pinnedLng) },
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1.4f).height(48.dp).testTag("confirm_map_pin_button"),
          ) {
            Text("Use This Location", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
