package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.model.TurfVenue
import com.example.data.repository.FootballHubRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.OwnerModePreferences
import com.example.ui.auth.signOut
import com.example.ui.components.FootballBottomNavBar
import com.example.ui.components.FootballBrandLogo
import com.example.ui.components.HubTab
import com.example.ui.screens.CompetitionsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OwnerOnboardingScreen
import com.example.ui.screens.OwnerPendingDashboardScreen
import com.example.ui.screens.OwnerPortalScreen
import com.example.ui.screens.PlayMapScreen
import com.example.ui.screens.PlayerAccountInOwnerModeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TeamsScreen
import com.example.ui.screens.TurfDetailScreen
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.FootballHubTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.FootballHubViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )
    setContent {
      FootballHubTheme {
        FootballHubApp()
      }
    }
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun FootballHubApp(auth: FirebaseAuth = Firebase.auth) {
  val context = LocalContext.current
  val currentUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  var ownerAccessCode by rememberSaveable {
    mutableStateOf(OwnerModePreferences.getSavedOwnerCode(context))
  }
  var skipAutoSignIn by rememberSaveable { mutableStateOf(false) }
  var welcomeErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      ownerAccessCode = ownerAccessCode,
      skipAutoSignIn = skipAutoSignIn,
      externalErrorMessage = welcomeErrorMessage,
      onClearExternalError = { welcomeErrorMessage = null },
      onOwnerCodeVerified = { code ->
        OwnerModePreferences.saveOwnerMode(context, code)
        ownerAccessCode = code
      },
      onOwnerModeCleared = {
        OwnerModePreferences.clearOwnerMode(context)
        ownerAccessCode = null
      },
      onAuthSuccess = {
        skipAutoSignIn = false
        welcomeErrorMessage = null
      },
    )
  } else {
    key(user.uid) {
      FootballHubAuthenticatedShell(
        currentUserId = user.uid,
        googleDisplayName = user.displayName,
        ownerAccessCode = ownerAccessCode,
        onClearOwnerAccessCode = {
          OwnerModePreferences.clearOwnerMode(context)
          ownerAccessCode = null
        },
        onCodeRejectedAtOnboarding = {
          OwnerModePreferences.clearOwnerMode(context)
          ownerAccessCode = null
          skipAutoSignIn = true
          welcomeErrorMessage = context.getString(R.string.invalid_membership_code_error)
        },
        onSignedOut = {
          OwnerModePreferences.clearOwnerMode(context)
          ownerAccessCode = null
          skipAutoSignIn = true
        },
      )
    }
  }
}

@Composable
fun FootballHubAuthenticatedShell(
  currentUserId: String,
  googleDisplayName: String?,
  ownerAccessCode: String? = null,
  onClearOwnerAccessCode: () -> Unit = {},
  onCodeRejectedAtOnboarding: () -> Unit = {},
  onSignedOut: () -> Unit = {},
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: FootballHubViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) as Application
            val repository = FootballHubRepository(app)
            FootballHubViewModel(
              repository = repository,
              currentUserId = currentUserId,
              isOwnerEntryMode = ownerAccessCode != null,
            )
          }
        },
    )

  val controls by viewModel.controls.collectAsStateWithLifecycle()
  val osmState by viewModel.osmState.collectAsStateWithLifecycle()
  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
  val registrationsState by viewModel.registrationsState.collectAsStateWithLifecycle()
  val challengesState by viewModel.challengesState.collectAsStateWithLifecycle()
  val favoritesState by viewModel.favoritesState.collectAsStateWithLifecycle()
  val verifiedTurfs by viewModel.verifiedTurfs.collectAsStateWithLifecycle()
  val remoteVerifiedTurfIds by viewModel.remoteVerifiedTurfIds.collectAsStateWithLifecycle()
  val homeVerifiedListingsState by viewModel.homeVerifiedListingsState.collectAsStateWithLifecycle()
  val homeTodaySlotsCache by viewModel.homeTodaySlotsCache.collectAsStateWithLifecycle()
  val isHomeRefreshing by viewModel.isHomeRefreshing.collectAsStateWithLifecycle()
  val homeRefreshNonce by viewModel.homeRefreshNonce.collectAsStateWithLifecycle()
  val ownerTurfsState by viewModel.ownerTurfsState.collectAsStateWithLifecycle()
  val selectedOwnerDateIso by viewModel.selectedOwnerDateIso.collectAsStateWithLifecycle()
  val ownerTodayNowSlots by viewModel.ownerTodayNowSlots.collectAsStateWithLifecycle()
  val ownerSelectedDateSlotsState by viewModel.ownerSelectedDateSlotsState.collectAsStateWithLifecycle()
  val ownerVenueBookingsState by viewModel.ownerVenueBookingsState.collectAsStateWithLifecycle()

  val profile = (profileState as? UiState.Success)?.data
  val bookings = (bookingsState as? UiState.Success)?.data.orEmpty()
  val registrations = (registrationsState as? UiState.Success)?.data.orEmpty()
  val challenges = (challengesState as? UiState.Success)?.data.orEmpty()
  val favorites = (favoritesState as? UiState.Success)?.data.orEmpty()

  val performSignOut: () -> Unit = {
    signOut(credentialManager, scope, onSignOutComplete = onSignedOut)
  }

  LaunchedEffect(controls.codeRejectedAtOnboarding) {
    if (controls.codeRejectedAtOnboarding) {
      viewModel.clearCodeRejectedAtOnboarding()
      signOut(credentialManager, scope, onSignOutComplete = onCodeRejectedAtOnboarding)
    }
  }

  if (profileState is UiState.Loading) {
    Box(
      modifier = Modifier.fillMaxSize().background(ObsidianBlack),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FootballBrandLogo(size = 64.dp)
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(color = CrimsonRed, modifier = Modifier.size(28.dp))
      }
    }
    return
  }

  // Returning owners open Owner UI directly (no code needed again)
  if (profile != null && (profile.role == "owner_pending" || profile.role == "owner")) {
    val verifiedOwnerTurf =
      (ownerTurfsState as? UiState.Success)?.data?.firstOrNull {
        it.verificationStatus == "verified"
      }
    if (profile.role == "owner" && verifiedOwnerTurf != null) {
      OwnerPortalScreen(
        profile = profile,
        turf = verifiedOwnerTurf,
        selectedDateIso = selectedOwnerDateIso,
        todayNowSlots = ownerTodayNowSlots,
        slotsState = ownerSelectedDateSlotsState,
        bookingsState = ownerVenueBookingsState,
        isBusy = controls.isActionBusy,
        statusMessage = controls.statusMessage,
        onClearStatusMessage = viewModel::clearStatusMessage,
        onInitializeDay = viewModel::initializeOwnerDay,
        onSelectDate = viewModel::selectOwnerDate,
        onAutoCompleteExpired = { nowMillis ->
          viewModel.autoCompleteExpiredSessions(verifiedOwnerTurf.id, ownerTodayNowSlots, nowMillis)
        },
        onCreateSessionBooking = {
          dateIso,
          coveredSlots,
          customerName,
          phone,
          startTime,
          endTime,
          hours,
          totalPrice,
          paymentMethod,
          startNow,
          onResult ->
          viewModel.createOwnerSessionBooking(
            turfId = verifiedOwnerTurf.id,
            dateIso = dateIso,
            coveredSlots = coveredSlots,
            customerName = customerName,
            phone = phone,
            startTime = startTime,
            endTime = endTime,
            hours = hours,
            totalPrice = totalPrice,
            paymentMethod = paymentMethod,
            startNow = startNow,
            onResult = onResult,
          )
        },
        onBlockSlot = { slot -> viewModel.blockOwnerSlot(verifiedOwnerTurf.id, slot.id) },
        onUnblockSlot = { slot -> viewModel.unblockOwnerSlot(verifiedOwnerTurf.id, slot.id) },
        onStartSession = { slots, bookingId ->
          viewModel.startOwnerSlotSession(verifiedOwnerTurf.id, slots, bookingId)
        },
        onEndSession = { slots, bookingId ->
          viewModel.endOwnerSession(verifiedOwnerTurf.id, slots, bookingId)
        },
        onMarkNoShow = { slots, bookingId ->
          viewModel.markOwnerSlotNoShow(verifiedOwnerTurf.id, slots, bookingId)
        },
        onSaveTurfDetails = {
          turfName,
          address,
          latitude,
          longitude,
          pricePerHour,
          primaryFormat,
          facilities,
          openTime,
          closeTime,
          slotMinutes,
          photos,
          description ->
          viewModel.saveOwnerTurfDetails(
            turfId = verifiedOwnerTurf.id,
            turfName = turfName,
            address = address,
            latitude = latitude,
            longitude = longitude,
            pricePerHour = pricePerHour,
            primaryFormat = primaryFormat,
            facilities = facilities,
            openTime = openTime,
            closeTime = closeTime,
            slotMinutes = slotMinutes,
            photos = photos,
            description = description,
          )
        },
        onSignOut = performSignOut,
      )
    } else {
      OwnerPendingDashboardScreen(
        profile = profile,
        ownerTurfsState = ownerTurfsState,
        isBusy = controls.isActionBusy,
        onSavePendingTurfDetails = { turfId, submission, slotMinutes ->
          viewModel.saveOwnerTurfDetails(
            turfId = turfId,
            turfName = submission.turfName,
            address = submission.address,
            latitude = submission.latitude,
            longitude = submission.longitude,
            pricePerHour = submission.pricePerHour,
            primaryFormat = submission.primaryFormat,
            facilities = submission.facilities,
            openTime = submission.openTime,
            closeTime = submission.closeTime,
            slotMinutes = slotMinutes,
            photos = submission.photos,
            description = submission.description,
          )
        },
        onSignOut = performSignOut,
      )
    }
    return
  }

  // Owner Mode entry flow (code entered on Welcome screen before sign-in)
  if (ownerAccessCode != null) {
    if (profile == null) {
      OwnerOnboardingScreen(
        googleDisplayName = googleDisplayName,
        initialMembershipCode = ownerAccessCode,
        isBusy = controls.isActionBusy,
        statusErrorMessage = controls.statusMessage,
        onSubmitOwnerOnboarding = {
          code,
          ownerName,
          contactPhone,
          turfName,
          area,
          pricePerHour,
          primaryFormat,
          hasFloodlights,
          description,
          latitude,
          longitude,
          facilities,
          openTime,
          closeTime,
          photos ->
          viewModel.submitOwnerOnboarding(
            membershipCode = code,
            ownerName = ownerName,
            contactPhone = contactPhone,
            turfName = turfName,
            area = area,
            pricePerHour = pricePerHour,
            primaryFormat = primaryFormat,
            hasFloodlights = hasFloodlights,
            description = description,
            latitude = latitude,
            longitude = longitude,
            facilities = facilities,
            openTime = openTime,
            closeTime = closeTime,
            photos = photos,
          )
        },
        onSignOut = performSignOut,
      )
      return
    } else {
      // Signed in with an existing player account while in Owner Mode
      PlayerAccountInOwnerModeScreen(
        onSignOut = performSignOut,
        onContinueAsPlayer = {
          onClearOwnerAccessCode()
          viewModel.ensurePlayerAccountSeeded()
        },
      )
      return
    }
  }

  if (profile == null || !profile.onboardingCompleted) {
    OnboardingScreen(
      initialProfile = profile,
      googleDisplayName = googleDisplayName,
      isBusy = controls.isActionBusy,
      placeSuggestions = osmState.placeSuggestions,
      isSearchingPlaces = osmState.isSearchingPlaces,
      onSearchPlaces = viewModel::searchOsmPlaces,
      onClearPlaceSuggestions = viewModel::clearPlaceSuggestions,
      onCompleteOnboarding = viewModel::completeOnboarding,
    )
    return
  }

  val favoriteTurfIds =
    remember(favorites) {
      favorites.filter { it.targetType == "turf" }.map { it.targetId }.toSet()
    }
  val followedTeamIds =
    remember(favorites) {
      favorites.filter { it.targetType == "team" }.map { it.targetId }.toSet()
    }

  var selectedTab by remember { mutableStateOf(HubTab.HOME) }
  var detailTurf by remember { mutableStateOf<TurfVenue?>(null) }
  var showNotificationsDialog by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(controls.statusMessage) {
    val msg = controls.statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  if (detailTurf != null) {
    BackHandler { detailTurf = null }
    val turf = detailTurf!!
    val liveSlotsToday by
      remember(turf.id) { viewModel.observePlayerLiveSlotsToday(turf.id) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    TurfDetailScreen(
      turf = turf,
      isFavorited = favoriteTurfIds.contains(turf.id),
      isBusy = controls.isActionBusy,
      onBack = { detailTurf = null },
      onToggleFavorite = {
        viewModel.toggleFavoriteTurf(turf, favoriteTurfIds.contains(turf.id))
      },
      onBookNow = { dateLabel, timeSlot ->
        viewModel.bookTurf(turf, dateLabel, timeSlot)
        detailTurf = null
        selectedTab = HubTab.PROFILE
      },
      liveSlotsToday = liveSlotsToday,
      hasLiveSlots = remoteVerifiedTurfIds.contains(turf.id),
    )
    return
  }

  if (selectedTab != HubTab.HOME) {
    BackHandler { selectedTab = HubTab.HOME }
  }

  Scaffold(
    containerColor = ObsidianBlack,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    bottomBar = {
      FootballBottomNavBar(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
      )
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 0.dp)) {
      val unreadCount = GambianFootballCatalog.notifications.count { it.isUnread }

      when (selectedTab) {
        HubTab.HOME -> {
          HomeScreen(
            verifiedListingsState = homeVerifiedListingsState,
            todaySlotsByTurf = homeTodaySlotsCache,
            favoriteTurfIds = favoriteTurfIds,
            selectedCity = profile?.city ?: controls.selectedCity,
            unreadNotificationsCount = unreadCount,
            isRefreshing = isHomeRefreshing,
            refreshNonce = homeRefreshNonce,
            onSelectCity = viewModel::selectCity,
            onOpenNotifications = { showNotificationsDialog = true },
            onOpenTurfDetail = { listing -> detailTurf = listing.toTurfVenue() },
            onToggleFavoriteTurf = { listing, isSaved ->
              viewModel.toggleFavoriteTurf(listing.toTurfVenue(), isSaved)
            },
            onCollectVisibleTurfSlots = viewModel::collectVisibleHomeTurfSlots,
            onPrefetchFreeNowSlots = viewModel::prefetchTodaySlotsOnceForFilter,
            onRefresh = viewModel::refreshHomeFeed,
            onOpenCompetitions = {
              viewModel.selectCompetitionsSubTab(0)
              selectedTab = HubTab.COMPETITIONS
            },
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding() * 0f),
          )
        }
        HubTab.COMPETITIONS -> {
          CompetitionsScreen(
            subTab = controls.competitionsSubTab,
            selectedTournamentTab = controls.selectedTournamentTab,
            selectedChallengeTab = controls.selectedChallengeTab,
            registrations = registrations,
            challenges = challenges,
            prefillChallengeTurf = controls.prefillChallengeTurf,
            onConsumePrefillChallengeTurf = { viewModel.setPrefillChallengeTurf(null) },
            onSelectSubTab = viewModel::selectCompetitionsSubTab,
            onSelectTournamentTab = viewModel::selectTournamentTab,
            onSelectChallengeTab = viewModel::selectChallengeTab,
            onRegisterTournament = viewModel::registerForTournament,
            onCreateChallenge = viewModel::createChallenge,
            onJoinSeedChallenge = viewModel::joinSeedChallenge,
            onMarkChallengeCompleted = viewModel::markChallengeCompleted,
            onOpenNotifications = { showNotificationsDialog = true },
          )
        }
        HubTab.PLAY -> {
          PlayMapScreen(
            searchQuery = controls.searchQuery,
            selectedFilter = controls.selectedMapFilter,
            selectedTurfId = controls.selectedTurfId,
            favoriteTurfIds = favoriteTurfIds,
            unreadNotificationsCount = unreadCount,
            osmState = osmState,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSelectFilter = viewModel::selectMapFilter,
            onSelectTurf = viewModel::selectMapTurf,
            onSelectOsmPitch = viewModel::selectOsmPitch,
            onSelectOsmPlace = viewModel::selectOsmPlaceOnMap,
            onViewportBoundsChanged = viewModel::loadOsmPitchesForBounds,
            onUseDeviceCoordinates = viewModel::updatePlayerLiveCoordinates,
            onRecenterAnchor = viewModel::recenterMapToAnchor,
            onOpenTurfDetail = { detailTurf = it },
            onHostChallengeAtPitch = { pitchName ->
              viewModel.setPrefillChallengeTurf(pitchName)
              viewModel.selectCompetitionsSubTab(1)
              selectedTab = HubTab.COMPETITIONS
            },
            onToggleFavoriteTurf = viewModel::toggleFavoriteTurf,
            onOpenNotifications = { showNotificationsDialog = true },
            onOpenTournaments = {
              viewModel.selectCompetitionsSubTab(0)
              selectedTab = HubTab.COMPETITIONS
            },
            onOpenTeams = { selectedTab = HubTab.TEAMS },
            verifiedTurfs = verifiedTurfs,
          )
        }
        HubTab.TEAMS -> {
          TeamsScreen(
            selectedTeamId = controls.selectedTeamId,
            followedTeamIds = followedTeamIds,
            onSelectTeam = viewModel::selectTeam,
            onToggleFollow = viewModel::toggleFollowTeam,
            onChallengeTeam = {
              viewModel.selectCompetitionsSubTab(1)
              selectedTab = HubTab.COMPETITIONS
            },
          )
        }
        HubTab.PROFILE -> {
          ProfileScreen(
            profile = profile,
            bookings = bookings,
            registrations = registrations,
            challenges = challenges,
            onOpenMyTeam = {
              viewModel.selectTeam("brikama_warriors")
              selectedTab = HubTab.TEAMS
            },
            onOpenNotifications = { showNotificationsDialog = true },
            onCancelBooking = viewModel::cancelBooking,
            onUpdateProfile = viewModel::updateProfile,
            onSignOut = performSignOut,
          )
        }
      }
    }
  }

  if (showNotificationsDialog) {
    AlertDialog(
      onDismissRequest = { showNotificationsDialog = false },
      containerColor = ObsidianCardElevated,
      title = { Text("Notifications", color = TextPrimary) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          GambianFootballCatalog.notifications.forEach { item ->
            Surface(
              color = ObsidianCard,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, ObsidianBorder),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(item.title, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.subtitle, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.timeAgo, color = CrimsonRed)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showNotificationsDialog = false }) {
          Text("Done", color = Color.White)
        }
      },
    )
  }
}
