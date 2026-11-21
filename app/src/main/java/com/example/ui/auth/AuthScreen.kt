package com.example.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.repository.FootballHubRepository
import com.example.ui.components.FootballBrandLogo
import com.example.ui.components.GambianFlagBadge
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
  ownerAccessCode: String? = null,
  skipAutoSignIn: Boolean = false,
  externalErrorMessage: String? = null,
  onClearExternalError: () -> Unit = {},
  onOwnerCodeVerified: (String) -> Unit = {},
  onOwnerModeCleared: () -> Unit = {},
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val repository = remember(context) { FootballHubRepository(context) }

  var isLoading by remember { mutableStateOf(false) }
  var isVerifyingAccess by remember { mutableStateOf(false) }
  var localErrorMessage by remember { mutableStateOf<String?>(null) }
  var showMembershipCodeScreen by remember { mutableStateOf(false) }

  val displayError = localErrorMessage ?: externalErrorMessage

  LaunchedEffect(skipAutoSignIn, ownerAccessCode) {
    if (!skipAutoSignIn && ownerAccessCode == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = onAuthSuccess,
        onUnauthenticated = {},
        scope = scope,
      )
    }
  }

  fun startGoogleSignIn() {
    localErrorMessage = null
    onClearExternalError()
    isLoading = true
    onGoogleSignInClicked(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isLoading = false
        onAuthSuccess()
      },
      onAuthError = { msg ->
        isLoading = false
        localErrorMessage = msg
      },
      scope = scope,
      onAuthCancelled = { isLoading = false },
    )
  }

  fun verifyAndSwitchToOwnerMode(submittedCode: String) {
    localErrorMessage = null
    onClearExternalError()
    showMembershipCodeScreen = false
    isVerifyingAccess = true
    scope.launch {
      val startedAt = System.currentTimeMillis()
      val result = repository.verifyOwnerMembershipCode(submittedCode)
      if (result.isSuccess) {
        val elapsed = System.currentTimeMillis() - startedAt
        val minDisplayMs = 1500L
        if (elapsed < minDisplayMs) {
          delay(minDisplayMs - elapsed)
        }
        isVerifyingAccess = false
        onOwnerCodeVerified(submittedCode)
      } else {
        isVerifyingAccess = false
        onOwnerModeCleared()
        localErrorMessage = context.getString(R.string.invalid_membership_code_error)
      }
    }
  }

  if (isVerifyingAccess) {
    VerifyingAccessScreen()
    return
  }

  if (showMembershipCodeScreen) {
    BackHandler { showMembershipCodeScreen = false }
    TurfMembershipCodeScreen(
      onBack = { showMembershipCodeScreen = false },
      onSubmitCode = { code -> verifyAndSwitchToOwnerMode(code) },
    )
    return
  }

  if (ownerAccessCode != null) {
    OwnerWelcomeScreen(
      isLoading = isLoading,
      errorMessage = displayError,
      onGetStarted = { startGoogleSignIn() },
      onBackToPlayerApp = {
        localErrorMessage = null
        onClearExternalError()
        onOwnerModeCleared()
      },
    )
    return
  }

  Box(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
    Image(
      painter = painterResource(id = R.drawable.img_welcome_hero),
      contentDescription = stringResource(R.string.welcome_hero_desc),
      modifier = Modifier.fillMaxWidth().height(520.dp).align(Alignment.TopCenter),
      contentScale = ContentScale.Crop,
    )

    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              0.0f to Color.Black.copy(alpha = 0.25f),
              0.40f to ObsidianBlack.copy(alpha = 0.45f),
              0.62f to ObsidianBlack.copy(alpha = 0.94f),
              1.0f to ObsidianBlack,
            )
          )
    )

    // Small, neutral top-right icon (no "owner" or "admin" label)
    IconButton(
      onClick = {
        localErrorMessage = null
        onClearExternalError()
        showMembershipCodeScreen = true
      },
      modifier =
        Modifier.align(Alignment.TopEnd)
          .statusBarsPadding()
          .padding(top = 12.dp, end = 16.dp)
          .size(48.dp)
          .testTag("welcome_access_options_icon"),
    ) {
      Surface(
        color = ObsidianCard.copy(alpha = 0.55f),
        shape = CircleShape,
        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.65f)),
        modifier = Modifier.size(36.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Outlined.Tune,
            contentDescription = stringResource(R.string.access_options_desc),
            tint = TextSecondary.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp, vertical = 20.dp)
          .widthIn(max = 520.dp)
          .align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Bottom,
    ) {
      FootballBrandLogo(size = 76.dp)

      Spacer(modifier = Modifier.height(22.dp))

      Text(
        text =
          buildAnnotatedString {
            append("Play. Connect. ")
            withStyle(SpanStyle(color = CrimsonRed)) { append("Compete.") }
          },
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
        color = TextPrimary,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = stringResource(R.string.welcome_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(12.dp))

      GambianFlagBadge()

      Spacer(modifier = Modifier.height(28.dp))

      if (displayError != null) {
        Surface(
          color = CrimsonRed.copy(alpha = 0.16f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).testTag("welcome_error_banner"),
        ) {
          Text(
            text = displayError,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            textAlign = TextAlign.Center,
          )
        }
      }

      Button(
        onClick = { startGoogleSignIn() },
        enabled = !isLoading,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = CrimsonRed,
            contentColor = Color.White,
          ),
        shape = RoundedCornerShape(28.dp),
        modifier =
          Modifier.fillMaxWidth().height(56.dp).testTag("get_started_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.5.dp,
          )
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Text(
              text = stringResource(R.string.get_started),
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

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedButton(
        onClick = { startGoogleSignIn() },
        enabled = !isLoading,
        border = BorderStroke(1.dp, ObsidianBorder),
        colors =
          ButtonDefaults.outlinedButtonColors(
            containerColor = ObsidianCard.copy(alpha = 0.75f),
            contentColor = TextPrimary,
          ),
        shape = RoundedCornerShape(28.dp),
        modifier =
          Modifier.fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(28.dp))
            .testTag("google_sign_in_button"),
      ) {
        Text(
          text = stringResource(R.string.sign_in_with_google),
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun VerifyingAccessScreen() {
  Box(
    modifier = Modifier.fillMaxSize().background(ObsidianBlack).testTag("verifying_access_screen"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(24.dp),
    ) {
      FootballBrandLogo(size = 72.dp)
      Spacer(modifier = Modifier.height(24.dp))
      CircularProgressIndicator(
        color = CrimsonRed,
        strokeWidth = 3.dp,
        modifier = Modifier.size(34.dp),
      )
      Spacer(modifier = Modifier.height(18.dp))
      Text(
        text = stringResource(R.string.verifying_access),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = TextPrimary,
      )
    }
  }
}

@Composable
private fun OwnerWelcomeScreen(
  isLoading: Boolean,
  errorMessage: String?,
  onGetStarted: () -> Unit,
  onBackToPlayerApp: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize().background(ObsidianBlack).testTag("owner_welcome_screen")
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_turf_night),
      contentDescription = "Football turf pitch at night",
      modifier = Modifier.fillMaxWidth().height(520.dp).align(Alignment.TopCenter),
      contentScale = ContentScale.Crop,
    )

    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              0.0f to Color.Black.copy(alpha = 0.25f),
              0.38f to ObsidianBlack.copy(alpha = 0.48f),
              0.62f to ObsidianBlack.copy(alpha = 0.94f),
              1.0f to ObsidianBlack,
            )
          )
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp, vertical = 20.dp)
          .widthIn(max = 520.dp)
          .align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Bottom,
    ) {
      FootballBrandLogo(size = 76.dp)

      Spacer(modifier = Modifier.height(22.dp))

      Text(
        text = stringResource(R.string.owner_welcome_title),
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp),
        color = TextPrimary,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = stringResource(R.string.owner_welcome_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(28.dp))

      if (errorMessage != null) {
        Surface(
          color = CrimsonRed.copy(alpha = 0.16f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        ) {
          Text(
            text = errorMessage,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            textAlign = TextAlign.Center,
          )
        }
      }

      Button(
        onClick = onGetStarted,
        enabled = !isLoading,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = CrimsonRed,
            contentColor = Color.White,
          ),
        shape = RoundedCornerShape(28.dp),
        modifier =
          Modifier.fillMaxWidth().height(56.dp).testTag("owner_get_started_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.5.dp,
          )
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Text(
              text = stringResource(R.string.get_started),
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

      Spacer(modifier = Modifier.height(10.dp))

      TextButton(
        onClick = onBackToPlayerApp,
        enabled = !isLoading,
        modifier = Modifier.height(48.dp).testTag("back_to_player_app_link"),
      ) {
        Text(
          text = stringResource(R.string.back_to_player_app),
          style = MaterialTheme.typography.labelLarge,
          color = TextSecondary,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun TurfMembershipCodeScreen(
  onBack: () -> Unit,
  onSubmitCode: (String) -> Unit,
) {
  var codeInput by remember { mutableStateOf("") }
  var validationError by remember { mutableStateOf<String?>(null) }

  Box(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp, vertical = 16.dp)
          .widthIn(max = 520.dp)
          .align(Alignment.TopCenter),
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.size(48.dp).testTag("membership_code_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = TextPrimary,
        )
      }

      Spacer(modifier = Modifier.height(32.dp))

      Surface(
        color = ObsidianCardElevated,
        shape = CircleShape,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.size(56.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Outlined.Key,
            contentDescription = null,
            tint = CrimsonRed,
            modifier = Modifier.size(26.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = stringResource(R.string.membership_code_prompt),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
      )

      Spacer(modifier = Modifier.height(24.dp))

      OutlinedTextField(
        value = codeInput,
        onValueChange = {
          codeInput = it
          validationError = null
        },
        label = { Text(stringResource(R.string.membership_code_hint)) },
        singleLine = true,
        isError = validationError != null,
        shape = RoundedCornerShape(16.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CrimsonRed,
            unfocusedBorderColor = ObsidianBorder,
            focusedContainerColor = ObsidianCard,
            unfocusedContainerColor = ObsidianCard,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = CrimsonRed,
            unfocusedLabelColor = TextSecondary,
            cursorColor = CrimsonRed,
          ),
        modifier = Modifier.fillMaxWidth().testTag("membership_code_input"),
      )

      if (validationError != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = validationError.orEmpty(),
          style = MaterialTheme.typography.bodySmall,
          color = CrimsonRed,
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          val trimmed = codeInput.trim()
          if (trimmed.length < 4) {
            validationError = "Please enter the full membership code."
          } else {
            onSubmitCode(trimmed)
          }
        },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = CrimsonRed,
            contentColor = Color.White,
          ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("membership_code_submit_button"),
      ) {
        Text(
          text = stringResource(R.string.membership_code_continue),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
      }
    }
  }
}
