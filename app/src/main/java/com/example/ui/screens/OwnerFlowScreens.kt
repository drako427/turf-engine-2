package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.model.PlayerProfile
import com.example.data.model.TurfListing
import com.example.ui.components.FootballBrandLogo
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldOpen
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState

@Composable
fun OwnerOnboardingScreen(
  googleDisplayName: String?,
  initialMembershipCode: String,
  isBusy: Boolean,
  statusErrorMessage: String?,
  onSubmitOwnerOnboarding: (
    membershipCode: String,
    ownerName: String,
    contactPhone: String,
    turfName: String,
    area: String,
    pricePerHour: Long,
    primaryFormat: String,
    hasFloodlights: Boolean,
    description: String,
    latitude: Double,
    longitude: Double,
    facilities: List<String>,
    openTime: String,
    closeTime: String,
    photos: List<String>,
  ) -> Unit,
  onSignOut: () -> Unit,
) {
  var membershipCode by remember(initialMembershipCode) { mutableStateOf(initialMembershipCode) }
  var showCodeEditor by remember { mutableStateOf(false) }
  var ownerName by remember(googleDisplayName) {
    mutableStateOf(googleDisplayName?.takeIf { it.isNotBlank() }.orEmpty())
  }
  var contactPhone by remember { mutableStateOf("") }
  val defaultPoint = remember { GambianFootballCatalog.resolveAreaPoint("Banjul") }

  Box(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp)
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          FootballBrandLogo(size = 40.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Turf Details",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
            )
            Text(
              text = "Admin verification required before going live",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
          }
        }
        IconButton(
          onClick = onSignOut,
          modifier = Modifier.size(48.dp).testTag("owner_onboarding_sign_out"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = "Sign out",
            tint = TextSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (statusErrorMessage != null && statusErrorMessage.contains("code", ignoreCase = true) && !showCodeEditor) {
        Surface(
          color = CrimsonRed.copy(alpha = 0.16f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = statusErrorMessage,
              color = TextPrimary,
              style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Tap to update membership code",
              color = CrimsonRed,
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.clickable { showCodeEditor = true },
            )
          }
        }
      }

      TurfDetailsFormContent(
        initialTurfName = "",
        initialAddress = "Banjul, The Gambia",
        initialLatitude = defaultPoint.latitude,
        initialLongitude = defaultPoint.longitude,
        initialPricePerHour = 450L,
        initialFormat = "7-a-side",
        initialFacilities = listOf("Floodlights"),
        initialOpenTime = "16:00",
        initialCloseTime = "24:00",
        initialPhotos = emptyList(),
        initialDescription = "",
        isBusy = isBusy,
        submitButtonLabel = "Save and Publish",
        externalErrorMessage = statusErrorMessage,
        extraBeforeError = {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OwnerTextField(
              value = ownerName,
              onValueChange = { ownerName = it },
              label = "Owner Full Name",
              placeholder = "e.g., Lamin Jallow",
              tag = "owner_name_input",
            )
            OwnerTextField(
              value = contactPhone,
              onValueChange = { contactPhone = it },
              label = "Business Phone",
              placeholder = "e.g., +220 712 3456",
              keyboardType = KeyboardType.Phone,
              tag = "owner_phone_input",
            )
          }
        },
        extraBeforeSubmit = {
          Column {
            TextButton(
              onClick = { showCodeEditor = !showCodeEditor },
              modifier = Modifier.align(Alignment.Start),
            ) {
              Text(
                text = if (showCodeEditor) "Hide membership code" else "Edit membership code",
                color = TextSecondary,
                style = MaterialTheme.typography.labelLarge,
              )
            }
            if (showCodeEditor) {
              OwnerTextField(
                value = membershipCode,
                onValueChange = { membershipCode = it },
                label = "Membership Code",
                placeholder = "Admin code",
                tag = "owner_code_override_input",
              )
            }
          }
        },
        validateExtraBeforeSubmit = {
          when {
            ownerName.trim().length < 2 -> "Please enter the owner name."
            membershipCode.trim().length < 4 -> "Valid membership code is required."
            else -> null
          }
        },
        onSubmit = { submission ->
          onSubmitOwnerOnboarding(
            membershipCode.trim(),
            ownerName.trim(),
            contactPhone.trim(),
            submission.turfName,
            submission.address,
            submission.pricePerHour,
            submission.primaryFormat,
            submission.facilities.any { it.equals("Floodlights", ignoreCase = true) },
            submission.description,
            submission.latitude,
            submission.longitude,
            submission.facilities,
            submission.openTime,
            submission.closeTime,
            submission.photos,
          )
        },
      )

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
fun OwnerPendingDashboardScreen(
  profile: PlayerProfile,
  ownerTurfsState: UiState<List<TurfListing>>,
  isBusy: Boolean = false,
  onSavePendingTurfDetails: ((turfId: String, submission: TurfDetailsSubmission, slotMinutes: Long) -> Unit)? = null,
  onSignOut: () -> Unit,
) {
  val isVerifiedOwner = profile.role == "owner"
  var editingTurf by remember { mutableStateOf<TurfListing?>(null) }

  val targetEditTurf = editingTurf
  if (targetEditTurf != null && onSavePendingTurfDetails != null) {
    BackHandler { editingTurf = null }
    Box(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .widthIn(max = 600.dp)
            .align(Alignment.TopCenter),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = { editingTurf = null }, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary,
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Turf Details",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
          )
        }
        TurfDetailsFormContent(
          initialTurfName = targetEditTurf.name,
          initialAddress = targetEditTurf.city.ifBlank { targetEditTurf.area },
          initialLatitude = targetEditTurf.latitude,
          initialLongitude = targetEditTurf.longitude,
          initialPricePerHour = targetEditTurf.pricePerHour,
          initialFormat = targetEditTurf.primaryFormat,
          initialFacilities = targetEditTurf.amenities,
          initialOpenTime = targetEditTurf.effectiveOpenTime(),
          initialCloseTime = targetEditTurf.effectiveCloseTime(),
          initialPhotos = targetEditTurf.photos,
          initialDescription = targetEditTurf.description,
          isBusy = isBusy,
          submitButtonLabel = "Save and Publish",
          onSubmit = { submission ->
            onSavePendingTurfDetails(targetEditTurf.id, submission, targetEditTurf.effectiveSlotMinutes())
            editingTurf = null
          },
        )
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
    return
  }

  Box(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            FootballBrandLogo(size = 42.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Turf Owner Hub",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
              )
              Text(
                text = profile.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
            }
          }
          OutlinedButton(
            onClick = onSignOut,
            border = BorderStroke(1.dp, ObsidianBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(48.dp).testTag("owner_sign_out_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ExitToApp,
              contentDescription = null,
              tint = TextPrimary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sign Out", color = TextPrimary)
          }
        }
      }

      item {
        Surface(
          color = ObsidianCard,
          shape = RoundedCornerShape(20.dp),
          border =
            BorderStroke(
              1.dp,
              if (isVerifiedOwner) EmeraldOpen.copy(alpha = 0.45f)
              else GoldTrophy.copy(alpha = 0.45f),
            ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.size(46.dp)
                  .clip(CircleShape)
                  .background(if (isVerifiedOwner) EmeraldSubtle else GoldSubtle),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector =
                  if (isVerifiedOwner) Icons.Filled.VerifiedUser else Icons.Filled.HourglassTop,
                contentDescription = null,
                tint = if (isVerifiedOwner) EmeraldOpen else GoldTrophy,
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text =
                  if (isVerifiedOwner) "Owner Account Verified"
                  else "Pending Admin Verification",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text =
                  if (isVerifiedOwner) {
                    "Your owner account is active. Full turf management tools will appear here."
                  } else {
                    "Your turf submission is awaiting verification in the admin panel. Players will see your turf once approved."
                  },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
            }
          }
        }
      }

      item {
        Text(
          text = "Your Submitted Turfs",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary,
        )
      }

      when (ownerTurfsState) {
        UiState.Loading -> {
          item {
            Box(
              modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(color = CrimsonRed)
            }
          }
        }
        is UiState.Error -> {
          item {
            Text(
              text = ownerTurfsState.message,
              color = CrimsonRed,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
        is UiState.Success -> {
          if (ownerTurfsState.data.isEmpty()) {
            item {
              Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "No turfs submitted yet.",
                  color = TextSecondary,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(16.dp),
                )
              }
            }
          } else {
            items(ownerTurfsState.data, key = { it.id }) { turf ->
              OwnerTurfStatusCard(
                turf = turf,
                onEditClick =
                  if (onSavePendingTurfDetails != null) {
                    { editingTurf = turf }
                  } else {
                    null
                  },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OwnerTurfStatusCard(
  turf: TurfListing,
  onEditClick: (() -> Unit)? = null,
) {
  val statusColor =
    when (turf.verificationStatus) {
      "verified" -> EmeraldOpen
      "rejected" -> CrimsonRed
      else -> GoldTrophy
    }
  val statusLabel =
    when (turf.verificationStatus) {
      "verified" -> "Verified • Live to Players"
      "rejected" -> "Rejected"
      else -> "Pending Verification"
    }

  Surface(
    color = ObsidianCard,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, ObsidianBorder),
    modifier = Modifier.fillMaxWidth().testTag("owner_turf_card_${turf.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            imageVector = Icons.Filled.SportsSoccer,
            contentDescription = null,
            tint = CrimsonRed,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = turf.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
          )
        }
        Surface(
          color = statusColor.copy(alpha = 0.16f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.5f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (turf.verificationStatus == "verified") {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(12.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
              text = statusLabel,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = statusColor,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.LocationOn,
          contentDescription = null,
          tint = TextMuted,
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "${turf.city} • ${turf.primaryFormat} • D${turf.pricePerHour}/hr",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }

      if (onEditClick != null) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = onEditClick,
          border = BorderStroke(1.dp, ObsidianBorder),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(42.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = null,
            tint = CrimsonRed,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Edit Turf Details", color = TextPrimary)
        }
      }
    }
  }
}

@Composable
fun PlayerAccountInOwnerModeScreen(
  onSignOut: () -> Unit,
  onContinueAsPlayer: () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize().background(ObsidianBlack).padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      color = ObsidianCard,
      shape = RoundedCornerShape(22.dp),
      border = BorderStroke(1.dp, ObsidianBorder),
      modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        FootballBrandLogo(size = 52.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Player Account Detected",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text =
            "This Google account is already registered as a Player. Sign out and use a separate Google account to register as a Turf Owner.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = onSignOut,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = CrimsonRed,
              contentColor = Color.White,
            ),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Text("Sign Out", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = onContinueAsPlayer,
          border = BorderStroke(1.dp, ObsidianBorder),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Text("Continue to Player App", color = TextPrimary)
        }
      }
    }
  }
}

@Composable
private fun OwnerTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  tag: String,
  singleLine: Boolean = true,
  keyboardType: KeyboardType = KeyboardType.Text,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    placeholder = { Text(placeholder, color = TextMuted) },
    singleLine = singleLine,
    minLines = if (singleLine) 1 else 3,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    shape = RoundedCornerShape(16.dp),
    colors =
      OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CrimsonRed,
        unfocusedBorderColor = ObsidianBorder,
        focusedContainerColor = ObsidianCardElevated,
        unfocusedContainerColor = ObsidianCard,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedLabelColor = CrimsonRed,
        unfocusedLabelColor = TextSecondary,
        cursorColor = CrimsonRed,
      ),
    modifier = Modifier.fillMaxWidth().testTag(tag),
  )
}
