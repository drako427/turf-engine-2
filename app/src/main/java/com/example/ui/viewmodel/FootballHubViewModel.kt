package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.catalog.GambianSlotTimeHelper
import com.example.data.model.FootballTeam
import com.example.data.model.MatchChallenge
import com.example.data.model.OsmPitch
import com.example.data.model.OsmPlaceSuggestion
import com.example.data.model.PlayerFavorite
import com.example.data.model.PlayerProfile
import com.example.data.model.SeedChallenge
import com.example.data.model.Tournament
import com.example.data.model.TournamentRegistration
import com.example.data.model.TurfBooking
import com.example.data.model.TurfListing
import com.example.data.model.TurfSlot
import com.example.data.model.TurfVenue
import com.example.data.model.TurfVenueBooking
import com.example.data.remote.OpenStreetMapService
import com.example.data.repository.FootballHubRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

data class MapCameraTarget(
  val latitude: Double,
  val longitude: Double,
  val zoom: Double,
  val nonce: Long = System.nanoTime(),
)

data class OsmMapState(
  val osmPitches: List<OsmPitch> = emptyList(),
  val isLoadingPitches: Boolean = false,
  val osmErrorNote: String? = null,
  val placeSuggestions: List<OsmPlaceSuggestion> = emptyList(),
  val isSearchingPlaces: Boolean = false,
  val selectedOsmPitchId: String? = null,
  val playerAnchorLat: Double = 13.4549,
  val playerAnchorLon: Double = -16.5790,
  val playerAnchorLabel: String = "Banjul",
  val usingLiveGps: Boolean = false,
  val cameraTarget: MapCameraTarget? = null,
)

data class FootballHubUiControls(
  val selectedCity: String = "Banjul",
  val searchQuery: String = "",
  val selectedMapFilter: String = "All Turfs",
  val selectedTurfId: String = GambianFootballCatalog.turfs.first().id,
  val selectedTournamentTab: String = "Ongoing",
  val selectedChallengeTab: String = "Open",
  val selectedTeamId: String = GambianFootballCatalog.teams.first().id,
  val competitionsSubTab: Int = 0, // 0 = Tournaments, 1 = Challenges
  val prefillChallengeTurf: String? = null,
  val isActionBusy: Boolean = false,
  val statusMessage: String? = null,
  val codeRejectedAtOnboarding: Boolean = false,
)

class FootballHubViewModel(
  private val repository: FootballHubRepository,
  private val currentUserId: String,
  private val isOwnerEntryMode: Boolean = false,
) : ViewModel() {

  private val _controls = MutableStateFlow(FootballHubUiControls())
  val controls: StateFlow<FootballHubUiControls> = _controls.asStateFlow()

  private val _osmState = MutableStateFlow(OsmMapState())
  val osmState: StateFlow<OsmMapState> = _osmState.asStateFlow()

  private var placeSearchJob: Job? = null
  private var overpassJob: Job? = null

  val profileState: StateFlow<UiState<PlayerProfile?>> =
    repository
      .observePlayerProfile(currentUserId)
      .map<PlayerProfile?, UiState<PlayerProfile?>> { profile ->
        if (profile != null && profile.city.isNotBlank() && !_osmState.value.usingLiveGps) {
          val areaPoint = GambianFootballCatalog.resolveAreaPoint(profile.city)
          _osmState.update { state ->
            if (state.playerAnchorLabel == profile.city) state
            else {
              state.copy(
                playerAnchorLat = areaPoint.latitude,
                playerAnchorLon = areaPoint.longitude,
                playerAnchorLabel = profile.city,
              )
            }
          }
        }
        UiState.Success(profile)
      }
      .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load profile"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val bookingsState: StateFlow<UiState<List<TurfBooking>>> =
    repository
      .observeBookings(currentUserId)
      .map<List<TurfBooking>, UiState<List<TurfBooking>>> { UiState.Success(it) }
      .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load bookings"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val registrationsState: StateFlow<UiState<List<TournamentRegistration>>> =
    repository
      .observeRegistrations(currentUserId)
      .map<List<TournamentRegistration>, UiState<List<TournamentRegistration>>> {
        UiState.Success(it)
      }
      .catch { error ->
        emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load registrations")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val challengesState: StateFlow<UiState<List<MatchChallenge>>> =
    repository
      .observeChallenges(currentUserId)
      .map<List<MatchChallenge>, UiState<List<MatchChallenge>>> { UiState.Success(it) }
      .catch { error ->
        emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load challenges")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val favoritesState: StateFlow<UiState<List<PlayerFavorite>>> =
    repository
      .observeFavorites(currentUserId)
      .map<List<PlayerFavorite>, UiState<List<PlayerFavorite>>> { UiState.Success(it) }
      .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load favorites"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  private val _homeTodaySlotsCache = MutableStateFlow<Map<String, List<TurfSlot>>>(emptyMap())
  val homeTodaySlotsCache: StateFlow<Map<String, List<TurfSlot>>> = _homeTodaySlotsCache.asStateFlow()

  private val _isHomeRefreshing = MutableStateFlow(false)
  val isHomeRefreshing: StateFlow<Boolean> = _isHomeRefreshing.asStateFlow()

  private val _homeRefreshNonce = MutableStateFlow(0L)
  val homeRefreshNonce: StateFlow<Long> = _homeRefreshNonce.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  private val sharedVerifiedListingsFlow: Flow<List<TurfListing>> =
    _homeRefreshNonce.flatMapLatest { repository.observeVerifiedTurfListings() }

  val homeVerifiedListingsState: StateFlow<UiState<List<TurfListing>>> =
    sharedVerifiedListingsFlow
      .map<List<TurfListing>, UiState<List<TurfListing>>> { listings ->
        UiState.Success(
          listings
            .filter { it.verificationStatus == "verified" }
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        )
      }
      .catch { error ->
        emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load verified turfs")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  private val remoteVerifiedListings: StateFlow<List<TurfListing>> =
    sharedVerifiedListingsFlow
      .catch { emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val remoteVerifiedTurfIds: StateFlow<Set<String>> =
    remoteVerifiedListings
      .map { list -> list.map { it.id }.toSet() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

  val verifiedTurfs: StateFlow<List<TurfVenue>> =
    remoteVerifiedListings
      .map { remoteListings ->
        (remoteListings.map { it.toTurfVenue() } + GambianFootballCatalog.turfs).distinctBy { it.id }
      }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        GambianFootballCatalog.turfs,
      )

  val ownerTurfsState: StateFlow<UiState<List<TurfListing>>> =
    repository
      .observeOwnerTurfs(currentUserId)
      .map<List<TurfListing>, UiState<List<TurfListing>>> { UiState.Success(it) }
      .catch { error ->
        emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load owner turfs")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  private val _selectedOwnerDateIso = MutableStateFlow(GambianSlotTimeHelper.todayIsoDateGmt())
  val selectedOwnerDateIso: StateFlow<String> = _selectedOwnerDateIso.asStateFlow()

  private val verifiedOwnerTurfIdFlow: Flow<String?> =
    ownerTurfsState
      .map { state ->
        (state as? UiState.Success)?.data?.firstOrNull { it.verificationStatus == "verified" }?.id
      }
      .distinctUntilChanged()

  @OptIn(ExperimentalCoroutinesApi::class)
  val ownerTodayNowSlots: StateFlow<List<TurfSlot>> =
    verifiedOwnerTurfIdFlow
      .flatMapLatest { turfId ->
        if (turfId == null) flowOf(emptyList())
        else {
          repository
            .observeTurfSlots(turfId, GambianSlotTimeHelper.todayIsoDateGmt())
            .catch { emit(emptyList()) }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val ownerSelectedDateSlotsState: StateFlow<UiState<List<TurfSlot>>> =
    combine(verifiedOwnerTurfIdFlow, _selectedOwnerDateIso) { turfId, dateIso -> turfId to dateIso }
      .flatMapLatest { (turfId, dateIso) ->
        if (turfId == null) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeTurfSlots(turfId, dateIso)
            .map<List<TurfSlot>, UiState<List<TurfSlot>>> { UiState.Success(it) }
            .catch { error ->
              emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load daily slots")))
            }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  @OptIn(ExperimentalCoroutinesApi::class)
  val ownerVenueBookingsState: StateFlow<UiState<List<TurfVenueBooking>>> =
    verifiedOwnerTurfIdFlow
      .flatMapLatest { turfId ->
        if (turfId == null) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeTurfBookings(turfId)
            .map<List<TurfVenueBooking>, UiState<List<TurfVenueBooking>>> { UiState.Success(it) }
            .catch { error ->
              emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load venue bookings")))
            }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  init {
    if (!isOwnerEntryMode) {
      viewModelScope.launch {
        // Seed initial player record only if missing; never overwrite or pre-create in Owner Mode
        val displayName = Firebase.auth.currentUser?.displayName
        repository.ensurePlayerProfile(displayName)
      }
    }
    // Warm up Greater Banjul / Serrekunda OSM pitches on launch
    loadOsmPitchesForBounds(13.34, -16.76, 13.49, -16.55)
  }

  fun ensurePlayerAccountSeeded() {
    viewModelScope.launch {
      val displayName = Firebase.auth.currentUser?.displayName
      repository.ensurePlayerProfile(displayName)
    }
  }

  private val autoCompletingSessionKeys = mutableSetOf<String>()

  fun submitOwnerOnboarding(
    membershipCode: String,
    ownerName: String,
    contactPhone: String,
    turfName: String,
    area: String,
    pricePerHour: Long,
    primaryFormat: String,
    hasFloodlights: Boolean,
    description: String,
    latitude: Double? = null,
    longitude: Double? = null,
    facilities: List<String> = emptyList(),
    openTime: String = GambianSlotTimeHelper.DEFAULT_OPEN_TIME,
    closeTime: String = GambianSlotTimeHelper.DEFAULT_CLOSE_TIME,
    photos: List<String> = emptyList(),
  ) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result =
        repository.submitOwnerOnboarding(
          membershipCode = membershipCode,
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
      var isPermissionDenied = false
      val errorMsg =
        result.exceptionOrNull()?.let { err ->
          val fsError =
            generateSequence(err) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            isPermissionDenied = true
            "Invalid membership code. Visit or contact admin."
          } else {
            err.localizedMessage ?: "Could not complete owner onboarding."
          }
        }
      _controls.update {
        it.copy(
          isActionBusy = false,
          codeRejectedAtOnboarding = isPermissionDenied,
          statusMessage =
            if (result.isSuccess) {
              "Turf submitted! Pending admin verification."
            } else {
              errorMsg
            },
        )
      }
    }
  }

  fun clearCodeRejectedAtOnboarding() {
    _controls.update { it.copy(codeRejectedAtOnboarding = false) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  fun observePlayerLiveSlotsToday(turfId: String): Flow<List<TurfSlot>> =
    remoteVerifiedTurfIds
      .map { ids -> ids.contains(turfId) }
      .distinctUntilChanged()
      .flatMapLatest { isRemote ->
        if (!isRemote) {
          flowOf(emptyList())
        } else {
          repository
            .observeTurfSlots(turfId, GambianSlotTimeHelper.todayIsoDateGmt())
            .catch { emit(emptyList()) }
        }
      }

  suspend fun collectVisibleHomeTurfSlots(turfId: String) {
    val todayIso = GambianSlotTimeHelper.todayIsoDateGmt()
    repository
      .observeTurfSlots(turfId, todayIso)
      .catch { /* keep existing cached slots if offline */ }
      .collect { slots ->
        _homeTodaySlotsCache.update { current ->
          current + (turfId to slots)
        }
      }
  }

  fun prefetchTodaySlotsOnceForFilter(turfs: List<TurfListing>) {
    val todayIso = GambianSlotTimeHelper.todayIsoDateGmt()
    val cached = _homeTodaySlotsCache.value
    val missing = turfs.filter { !cached.containsKey(it.id) }
    if (missing.isEmpty()) return
    viewModelScope.launch {
      missing.forEach { turf ->
        repository.fetchTurfSlotsOnce(turf.id, todayIso).onSuccess { slots ->
          _homeTodaySlotsCache.update { it + (turf.id to slots) }
        }
      }
    }
  }

  fun refreshHomeFeed(visibleTurfIds: Set<String>) {
    if (_isHomeRefreshing.value) return
    viewModelScope.launch {
      _isHomeRefreshing.value = true
      try {
        _homeRefreshNonce.value = System.nanoTime()
        val todayIso = GambianSlotTimeHelper.todayIsoDateGmt()
        visibleTurfIds.forEach { turfId ->
          repository.fetchTurfSlotsOnce(turfId, todayIso).onSuccess { slots ->
            _homeTodaySlotsCache.update { it + (turfId to slots) }
          }
        }
        delay(350L)
      } finally {
        _isHomeRefreshing.value = false
      }
    }
  }

  fun selectOwnerDate(dateIso: String) {
    _selectedOwnerDateIso.value = dateIso
  }

  fun initializeOwnerDay(turf: TurfListing, dateIso: String) {
    viewModelScope.launch {
      repository.ensureTurfScheduleDefaults(turf.id)
      repository.ensureSlotsForDate(
        turfId = turf.id,
        dateIso = dateIso,
        openTime = turf.effectiveOpenTime(),
        closeTime = turf.effectiveCloseTime(),
        slotMinutes = turf.effectiveSlotMinutes(),
      )
    }
  }

  fun saveOwnerTurfDetails(
    turfId: String,
    turfName: String,
    address: String,
    latitude: Double,
    longitude: Double,
    pricePerHour: Long,
    primaryFormat: String,
    facilities: List<String>,
    openTime: String,
    closeTime: String,
    slotMinutes: Long,
    photos: List<String>,
    description: String,
  ) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result =
        repository.saveTurfDetails(
          turfId = turfId,
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
      if (result.isSuccess) {
        repository.ensureSlotsForDate(
          turfId = turfId,
          dateIso = _selectedOwnerDateIso.value,
          openTime = openTime,
          closeTime = closeTime,
          slotMinutes = slotMinutes,
        )
      }
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) "Turf details saved."
            else result.exceptionOrNull()?.localizedMessage ?: "Failed to save turf details.",
        )
      }
    }
  }

  fun updateOwnerTurfSettings(
    turfId: String,
    openTime: String,
    closeTime: String,
    slotMinutes: Long,
    pricePerHour: Long,
  ) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result =
        repository.updateTurfSettings(
          turfId = turfId,
          openTime = openTime,
          closeTime = closeTime,
          slotMinutes = slotMinutes,
          pricePerHour = pricePerHour,
        )
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) "Turf settings updated."
            else result.exceptionOrNull()?.localizedMessage ?: "Failed to update settings.",
        )
      }
    }
  }

  fun createOwnerSessionBooking(
    turfId: String,
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
  ) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result =
        repository.createSessionBooking(
          turfId = turfId,
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
        )
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) {
              if (startNow) "Session started for $customerName!"
              else "Booking confirmed for $customerName (${GambianSlotTimeHelper.formatHhMmTo12Hour(startTime)})."
            } else {
              null
            },
        )
      }
      onResult(result)
    }
  }

  fun createOwnerWalkInBooking(
    turfId: String,
    slot: TurfSlot,
    guestName: String,
    guestPhone: String,
    pricePerHour: Long = 450L,
  ) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result = repository.createWalkInBooking(turfId, slot, guestName, guestPhone, pricePerHour)
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) {
              "Walk-in booked for ${GambianSlotTimeHelper.formatHhMmTo12Hour(slot.startTime)}."
            } else {
              result.exceptionOrNull()?.localizedMessage ?: "Could not add walk-in booking."
            },
        )
      }
    }
  }

  fun blockOwnerSlot(turfId: String, slotId: String) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result = repository.blockSlot(turfId, slotId)
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) "Slot blocked."
            else result.exceptionOrNull()?.localizedMessage ?: "Failed to block slot.",
        )
      }
    }
  }

  fun unblockOwnerSlot(turfId: String, slotId: String) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result = repository.unblockSlot(turfId, slotId)
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) "Slot unblocked."
            else result.exceptionOrNull()?.localizedMessage ?: "Failed to unblock slot.",
        )
      }
    }
  }

  fun startOwnerSlotSession(turfId: String, slotsToStart: List<TurfSlot>, bookingId: String?) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result = repository.startSlotSession(turfId, slotsToStart, bookingId)
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) "Session started!"
            else result.exceptionOrNull()?.localizedMessage ?: "Failed to start session.",
        )
      }
    }
  }

  fun endOwnerSession(turfId: String, slotsToFree: List<TurfSlot>, bookingId: String?) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result = repository.endSession(turfId, slotsToFree, bookingId)
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) "Session ended. Slots are available again."
            else result.exceptionOrNull()?.localizedMessage ?: "Failed to end session.",
        )
      }
    }
  }

  fun autoCompleteExpiredSessions(turfId: String, slots: List<TurfSlot>, nowMillis: Long) {
    val expired = GambianSlotTimeHelper.findExpiredInProgressSessions(slots, nowMillis)
    if (expired.isEmpty()) return
    expired.forEach { session ->
      val key = "${turfId}_${session.bookingId ?: session.slots.firstOrNull()?.id.orEmpty()}"
      if (autoCompletingSessionKeys.add(key)) {
        viewModelScope.launch {
          try {
            repository.endSession(turfId, session.slots, session.bookingId)
          } finally {
            autoCompletingSessionKeys.remove(key)
          }
        }
      }
    }
  }

  fun markOwnerSlotNoShow(turfId: String, slotsToFree: List<TurfSlot>, bookingId: String?) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result = repository.markSlotNoShow(turfId, slotsToFree, bookingId)
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) "Marked as no-show; slot is available again."
            else result.exceptionOrNull()?.localizedMessage ?: "Failed to update slot.",
        )
      }
    }
  }

  fun completeOnboarding(
    displayName: String,
    handle: String,
    city: String,
    position: String,
    favoriteTeamId: String,
    customLatitude: Double? = null,
    customLongitude: Double? = null,
  ) {
    val resolvedPoint =
      if (customLatitude != null && customLongitude != null) {
        com.example.data.model.GeoPoint(customLatitude, customLongitude)
      } else {
        GambianFootballCatalog.resolveAreaPoint(city)
      }
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result =
        repository.completeOnboarding(
          displayName = displayName,
          handle = handle,
          city = city,
          position = position,
          favoriteTeamId = favoriteTeamId,
        )
      _osmState.update {
        it.copy(
          playerAnchorLat = resolvedPoint.latitude,
          playerAnchorLon = resolvedPoint.longitude,
          playerAnchorLabel = city.ifBlank { "Banjul" },
          usingLiveGps = false,
          placeSuggestions = emptyList(),
          cameraTarget =
            MapCameraTarget(
              latitude = resolvedPoint.latitude,
              longitude = resolvedPoint.longitude,
              zoom = 12.2,
            ),
        )
      }
      _controls.update {
        it.copy(
          isActionBusy = false,
          selectedCity = city.ifBlank { it.selectedCity },
          selectedTeamId =
            if (favoriteTeamId != "free_agent") favoriteTeamId else it.selectedTeamId,
          statusMessage =
            if (result.isSuccess) "Welcome to Football Hub, $displayName!"
            else "Could not save profile: ${result.exceptionOrNull()?.localizedMessage ?: "Error"}",
        )
      }
    }
  }

  private fun unwrapFirestoreMessage(error: Throwable, fallback: String): String {
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val root = firestoreError ?: error
    Log.w(TAG, fallback, root)
    return root.message ?: fallback
  }

  fun selectCity(city: String) {
    val areaPoint = GambianFootballCatalog.resolveAreaPoint(city)
    _controls.update { it.copy(selectedCity = city) }
    _osmState.update {
      it.copy(
        playerAnchorLat = areaPoint.latitude,
        playerAnchorLon = areaPoint.longitude,
        playerAnchorLabel = city,
        usingLiveGps = false,
        cameraTarget =
          MapCameraTarget(
            latitude = areaPoint.latitude,
            longitude = areaPoint.longitude,
            zoom = 12.2,
          ),
      )
    }
    viewModelScope.launch { repository.updatePlayerCity(city) }
  }

  fun updateSearchQuery(query: String) {
    _controls.update { it.copy(searchQuery = query) }
    searchOsmPlaces(query)
  }

  fun searchOsmPlaces(query: String) {
    placeSearchJob?.cancel()
    val trimmed = query.trim()
    if (trimmed.length < 2) {
      _osmState.update { it.copy(placeSuggestions = emptyList(), isSearchingPlaces = false) }
      return
    }
    placeSearchJob =
      viewModelScope.launch {
        _osmState.update { it.copy(isSearchingPlaces = true) }
        delay(350)
        val result = OpenStreetMapService.searchGambianPlaces(trimmed)
        _osmState.update { state ->
          state.copy(
            isSearchingPlaces = false,
            placeSuggestions = result.getOrDefault(emptyList()),
          )
        }
      }
  }

  fun clearPlaceSuggestions() {
    placeSearchJob?.cancel()
    _osmState.update { it.copy(placeSuggestions = emptyList(), isSearchingPlaces = false) }
  }

  fun selectOsmPlaceOnMap(place: OsmPlaceSuggestion) {
    _controls.update { it.copy(searchQuery = place.shortName) }
    _osmState.update {
      it.copy(
        placeSuggestions = emptyList(),
        playerAnchorLat = place.latitude,
        playerAnchorLon = place.longitude,
        playerAnchorLabel = place.shortName,
        usingLiveGps = false,
        cameraTarget =
          MapCameraTarget(
            latitude = place.latitude,
            longitude = place.longitude,
            zoom = 13.5,
          ),
      )
    }
    loadOsmPitchesForBounds(
      south = place.latitude - 0.06,
      west = place.longitude - 0.06,
      north = place.latitude + 0.06,
      east = place.longitude + 0.06,
    )
  }

  fun loadOsmPitchesForBounds(
    south: Double,
    west: Double,
    north: Double,
    east: Double,
  ) {
    overpassJob?.cancel()
    overpassJob =
      viewModelScope.launch {
        delay(450)
        _osmState.update { it.copy(isLoadingPitches = true, osmErrorNote = null) }
        val result = OpenStreetMapService.fetchFootballPitches(south, west, north, east)
        result.fold(
          onSuccess = { fetched ->
            _osmState.update { state ->
              val merged =
                (state.osmPitches + fetched)
                  .distinctBy { it.id }
                  .takeLast(120)
              state.copy(
                osmPitches = merged,
                isLoadingPitches = false,
                osmErrorNote = null,
              )
            }
          },
          onFailure = {
            _osmState.update { state ->
              state.copy(
                isLoadingPitches = false,
                osmErrorNote = "OSM live sync paused",
              )
            }
          },
        )
      }
  }

  fun updatePlayerLiveCoordinates(
    latitude: Double,
    longitude: Double,
    label: String,
    isGps: Boolean,
  ) {
    _osmState.update {
      it.copy(
        playerAnchorLat = latitude,
        playerAnchorLon = longitude,
        playerAnchorLabel = label,
        usingLiveGps = isGps,
        cameraTarget = MapCameraTarget(latitude, longitude, 13.4),
      )
    }
    loadOsmPitchesForBounds(
      south = latitude - 0.07,
      west = longitude - 0.07,
      north = latitude + 0.07,
      east = longitude + 0.07,
    )
  }

  fun recenterMapToAnchor() {
    val state = _osmState.value
    _osmState.update {
      it.copy(
        cameraTarget =
          MapCameraTarget(
            latitude = state.playerAnchorLat,
            longitude = state.playerAnchorLon,
            zoom = 12.6,
          )
      )
    }
  }

  fun selectMapFilter(filter: String) {
    _controls.update { it.copy(selectedMapFilter = filter) }
    if (filter == "OSM Pitches") {
      val firstOsm = _osmState.value.osmPitches.firstOrNull()
      if (firstOsm != null) {
        _osmState.update { it.copy(selectedOsmPitchId = firstOsm.id) }
      }
    } else {
      _osmState.update { it.copy(selectedOsmPitchId = null) }
    }
  }

  fun selectMapTurf(turfId: String) {
    _controls.update { it.copy(selectedTurfId = turfId) }
    _osmState.update { state ->
      val turf = verifiedTurfs.value.firstOrNull { it.id == turfId }
      state.copy(
        selectedOsmPitchId = null,
        cameraTarget =
          turf?.let { MapCameraTarget(it.latitude, it.longitude, 13.6) } ?: state.cameraTarget,
      )
    }
  }

  fun selectOsmPitch(pitchId: String) {
    _osmState.update { state ->
      val pitch = state.osmPitches.firstOrNull { it.id == pitchId }
      state.copy(
        selectedOsmPitchId = pitchId,
        cameraTarget =
          pitch?.let { MapCameraTarget(it.latitude, it.longitude, 14.2) } ?: state.cameraTarget,
      )
    }
  }

  fun setPrefillChallengeTurf(turfName: String?) {
    _controls.update { it.copy(prefillChallengeTurf = turfName) }
  }

  fun selectTournamentTab(tab: String) {
    _controls.update { it.copy(selectedTournamentTab = tab) }
  }

  fun selectChallengeTab(tab: String) {
    _controls.update { it.copy(selectedChallengeTab = tab) }
  }

  fun selectCompetitionsSubTab(index: Int) {
    _controls.update { it.copy(competitionsSubTab = index) }
  }

  fun selectTeam(teamId: String) {
    _controls.update { it.copy(selectedTeamId = teamId) }
  }

  fun clearStatusMessage() {
    _controls.update { it.copy(statusMessage = null) }
  }

  fun bookTurf(turf: TurfVenue, dateLabel: String, timeSlot: String) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result = repository.createBooking(turf, dateLabel, timeSlot)
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) {
              "Booked ${turf.name} for $dateLabel • $timeSlot"
            } else {
              "Could not complete booking: ${result.exceptionOrNull()?.localizedMessage ?: "Error"}"
            },
        )
      }
    }
  }

  fun cancelBooking(bookingId: String) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result = repository.cancelBooking(bookingId)
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) "Booking cancelled."
            else "Failed to cancel booking.",
        )
      }
    }
  }

  fun registerForTournament(tournament: Tournament, teamName: String) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result = repository.registerForTournament(tournament, teamName)
      _controls.update {
        it.copy(
          isActionBusy = false,
          statusMessage =
            if (result.isSuccess) "Registered $teamName for ${tournament.title}!"
            else "Registration failed.",
        )
      }
    }
  }

  fun createChallenge(
    hostTeamName: String,
    turfName: String,
    stakeLabel: String,
    format: String,
    scheduleLabel: String,
  ) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result =
        repository.createChallenge(hostTeamName, turfName, stakeLabel, format, scheduleLabel)
      _controls.update {
        it.copy(
          isActionBusy = false,
          selectedChallengeTab = "My Challenges",
          statusMessage =
            if (result.isSuccess) "Challenge posted for $turfName!"
            else "Failed to post challenge.",
        )
      }
    }
  }

  fun joinSeedChallenge(seed: SeedChallenge, playerTeamName: String) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result = repository.joinSeedChallenge(seed, playerTeamName)
      _controls.update {
        it.copy(
          isActionBusy = false,
          selectedChallengeTab = "Accepted",
          statusMessage =
            if (result.isSuccess) "Joined ${seed.hostTeamName} challenge at ${seed.turfName}!"
            else "Could not join challenge.",
        )
      }
    }
  }

  fun markChallengeCompleted(challengeId: String) {
    viewModelScope.launch {
      val result = repository.markChallengeCompleted(challengeId)
      if (result.isSuccess) {
        _controls.update { it.copy(statusMessage = "Match marked as completed.") }
      }
    }
  }

  fun toggleFavoriteTurf(turf: TurfVenue, currentlyFavorited: Boolean) {
    viewModelScope.launch {
      val result = repository.toggleFavorite(turf.id, "turf", currentlyFavorited)
      if (result.isSuccess) {
        _controls.update {
          it.copy(
            statusMessage =
              if (currentlyFavorited) "Removed ${turf.name} from saved turfs"
              else "Saved ${turf.name} to favorites"
          )
        }
      }
    }
  }

  fun toggleFollowTeam(team: FootballTeam, currentlyFollowing: Boolean) {
    viewModelScope.launch {
      val result = repository.toggleFavorite(team.id, "team", currentlyFollowing)
      if (result.isSuccess) {
        _controls.update {
          it.copy(
            statusMessage =
              if (currentlyFollowing) "Unfollowed ${team.name}"
              else "Following ${team.name}"
          )
        }
      }
    }
  }

  fun updateProfile(displayName: String, handle: String, city: String) {
    viewModelScope.launch {
      _controls.update { it.copy(isActionBusy = true) }
      val result = repository.updatePlayerProfile(displayName, handle, city)
      _controls.update {
        it.copy(
          isActionBusy = false,
          selectedCity = city.ifEmpty { it.selectedCity },
          statusMessage =
            if (result.isSuccess) "Player profile updated!"
            else "Failed to update profile.",
        )
      }
    }
  }

  private companion object {
    const val TAG = "FootballHubVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
