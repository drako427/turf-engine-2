package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.catalog.GambianSlotTimeHelper
import com.example.data.model.MatchChallenge
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
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FootballHubRepository(private val db: FirebaseFirestore) {

  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Football Hub data.")
  }

  private fun <T> wrapSnapshotCatch(
    source: Flow<T>,
    uid: String,
    operationType: OperationType,
    path: String,
  ): Flow<T> =
    source.catch { error ->
      val firestoreError =
        generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val exceptionToLog = firestoreError ?: (error as? Exception)
      if (exceptionToLog != null && auth.currentUser?.uid == uid) {
        handleFirestoreError(exceptionToLog, operationType, path)
      }
      throw error
    }

  fun observePlayerProfile(userId: String): Flow<PlayerProfile?> = flow {
    val path = "users/$userId"
    emitAll(
      wrapSnapshotCatch(
        db.collection("users").document(userId).snapshots().map { snapshot ->
          snapshot.toObject(
            PlayerProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        },
        userId,
        OperationType.GET,
        path,
      )
    )
  }

  suspend fun ensurePlayerProfile(displayName: String?, preferredCity: String = "Banjul"): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid)
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val cleanName = displayName?.trim()?.takeIf { it.isNotEmpty() }?.take(50) ?: "Drako"
        val cleanHandle =
          "@" + cleanName.lowercase().replace(Regex("[^a-z0-9]"), "").ifEmpty { "player" }.take(18) + "427"
        val payload =
          mapOf(
            "userId" to uid,
            "role" to "player",
            "displayName" to cleanName,
            "handle" to cleanHandle,
            "city" to preferredCity,
            "position" to "Forward",
            "favoriteTeamId" to "brikama_warriors",
            "onboardingCompleted" to false,
            "matchesPlayed" to 24L,
            "wins" to 16L,
            "trophies" to 4L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
      Result.failure(e)
    }
  }

  suspend fun completeOnboarding(
    displayName: String,
    handle: String,
    city: String,
    position: String,
    favoriteTeamId: String,
  ): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid)
      val cleanName = displayName.trim().take(50).ifEmpty { "Drako" }
      val formattedHandle =
        handle.trim().let { if (it.startsWith("@")) it else "@$it" }.take(40).ifEmpty { "@drako427" }
      val cleanCity = city.trim().take(50).ifEmpty { "Banjul" }
      val cleanPos = position.trim().take(30).ifEmpty { "Forward" }
      val cleanTeam = favoriteTeamId.trim().take(64).ifEmpty { "brikama_warriors" }

      val existing = docRef.get().await()
      if (!existing.exists()) {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "role" to "player",
              "displayName" to cleanName,
              "handle" to formattedHandle,
              "city" to cleanCity,
              "position" to cleanPos,
              "favoriteTeamId" to cleanTeam,
              "onboardingCompleted" to true,
              "matchesPlayed" to 24L,
              "wins" to 16L,
              "trophies" to 4L,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .update(
            mapOf(
              "displayName" to cleanName,
              "handle" to formattedHandle,
              "city" to cleanCity,
              "position" to cleanPos,
              "favoriteTeamId" to cleanTeam,
              "onboardingCompleted" to true,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
      Result.failure(e)
    }
  }

  suspend fun updatePlayerCity(city: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid)
      docRef.update(mapOf("city" to city.take(50), "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
      Result.failure(e)
    }
  }

  suspend fun updatePlayerProfile(displayName: String, handle: String, city: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      val cleanName = displayName.trim().take(50).ifEmpty { "Drako" }
      val formattedHandle =
        handle.trim().let { if (it.startsWith("@")) it else "@$it" }.take(40).ifEmpty { "@drako427" }
      val docRef = db.collection("users").document(uid)
      docRef
        .update(
          mapOf(
            "displayName" to cleanName,
            "handle" to formattedHandle,
            "city" to city.trim().take(50).ifEmpty { "Banjul" },
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
      Result.failure(e)
    }
  }

  fun observeVerifiedTurfListings(): Flow<List<TurfListing>> = flow {
    val uid = auth.currentUser?.uid.orEmpty()
    val path = "turfs"
    emitAll(
      wrapSnapshotCatch(
        db.collection("turfs")
          .whereEqualTo("verificationStatus", "verified")
          .snapshots()
          .map { snapshot ->
            snapshot.toObjects(
              TurfListing::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          },
        uid,
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun fetchVerifiedTurfListingsOnce(): Result<List<TurfListing>> {
    return try {
      requireUserId()
      val snapshot =
        db.collection("turfs")
          .whereEqualTo("verificationStatus", "verified")
          .get()
          .await()
      val list =
        snapshot.toObjects(
          TurfListing::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      Result.success(list)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun observeVerifiedTurfs(): Flow<List<TurfVenue>> =
    observeVerifiedTurfListings().map { listings -> listings.map { it.toTurfVenue() } }

  fun observeOwnerTurfs(ownerId: String): Flow<List<TurfListing>> = flow {
    val path = "turfs"
    emitAll(
      wrapSnapshotCatch(
        db.collection("turfs")
          .whereEqualTo("ownerId", ownerId)
          .snapshots()
          .map { snapshot ->
            snapshot.toObjects(
              TurfListing::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          },
        ownerId,
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun verifyOwnerMembershipCode(code: String): Result<Unit> {
    return try {
      val cleanCode = code.trim()
      require(cleanCode.length in 4..64) { "Invalid membership code." }
      val checkId = "chk_${System.currentTimeMillis()}_${(1000..9999).random()}"
      val payload =
        mapOf(
          "code" to cleanCode,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      db.collection("code_checks").document(checkId).set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun submitOwnerOnboarding(
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
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val cleanCode = membershipCode.trim().take(64)
      require(cleanCode.length >= 4) { "Invalid membership code." }
      val cleanOwnerName = ownerName.trim().take(50).ifEmpty { "Turf Owner" }
      val cleanPhone = contactPhone.trim().take(30)
      val cleanTurfName = turfName.trim().take(80)
      require(cleanTurfName.length >= 2) { "Please enter your turf name." }

      val (cleanArea, cleanCityLabel) = deriveAreaAndCity(area)
      val cleanFormat = primaryFormat.trim().take(30).ifEmpty { "7-a-side" }
      val safePrice = pricePerHour.coerceIn(50L, 50000L)
      val cleanDesc = description.trim().take(500)
      val cleanOpen =
        GambianSlotTimeHelper.normalizeTimeOrDefault(openTime, GambianSlotTimeHelper.DEFAULT_OPEN_TIME)
      val cleanClose =
        GambianSlotTimeHelper.normalizeTimeOrDefault(closeTime, GambianSlotTimeHelper.DEFAULT_CLOSE_TIME)
      val handleSlug =
        "@" +
          cleanOwnerName
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .ifEmpty { "owner" }
            .take(18) +
          "_turf"

      val turfId = "turf_${uid.take(12)}_${System.currentTimeMillis()}"
      val fallbackPoint = GambianFootballCatalog.resolveAreaPoint(cleanArea)
      val safeLat = latitude?.takeIf { it in -90.0..90.0 } ?: fallbackPoint.latitude
      val safeLon = longitude?.takeIf { it in -180.0..180.0 } ?: fallbackPoint.longitude

      val amenities =
        if (facilities.isNotEmpty()) {
          (listOf(cleanFormat) + facilities).map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(12)
        } else {
          buildList {
            add(cleanFormat)
            if (hasFloodlights) add("Floodlights")
            add("Changing rooms")
          }
        }
      val generatedSlots =
        GambianSlotTimeHelper.generateSlotWindows(
            cleanOpen,
            cleanClose,
            GambianSlotTimeHelper.DEFAULT_SLOT_MINUTES,
          )
          .map { "${it.startTime}–${it.endTime}" }
          .take(24)
          .ifEmpty { listOf("16:00–17:00", "17:00–18:00", "18:00–19:00") }

      val cleanPhotos = photos.filter { it.isNotBlank() && it.length <= 140_000 }.take(6)

      val userRef = db.collection("users").document(uid)
      val turfRef = db.collection("turfs").document(turfId)

      val userPayload =
        mapOf(
          "userId" to uid,
          "role" to "owner_pending",
          "membershipCode" to cleanCode,
          "primaryTurfId" to turfId,
          "contactPhone" to cleanPhone,
          "displayName" to cleanOwnerName,
          "handle" to handleSlug,
          "city" to cleanArea.take(50),
          "onboardingCompleted" to true,
          "matchesPlayed" to 0L,
          "wins" to 0L,
          "trophies" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )

      val turfPayload =
        mapOf(
          "ownerId" to uid,
          "name" to cleanTurfName,
          "area" to cleanArea,
          "city" to cleanCityLabel,
          "latitude" to safeLat,
          "longitude" to safeLon,
          "pricePerHour" to safePrice,
          "primaryFormat" to cleanFormat,
          "amenities" to amenities,
          "timeSlots" to generatedSlots,
          "openTime" to cleanOpen,
          "closeTime" to cleanClose,
          "slotMinutes" to GambianSlotTimeHelper.DEFAULT_SLOT_MINUTES,
          "photos" to cleanPhotos,
          "description" to cleanDesc,
          "contactPhone" to cleanPhone,
          "verificationStatus" to "pending",
          "isOpen" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )

      val batch = db.batch()
      batch.set(userRef, userPayload)
      batch.set(turfRef, turfPayload)
      batch.commit().await()
      Result.success(turfId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
      Result.failure(e)
    }
  }

  private fun deriveAreaAndCity(rawAddress: String): Pair<String, String> {
    val cleanAddress = rawAddress.trim().take(80).ifEmpty { "Serrekunda, The Gambia" }
    val firstSegment = cleanAddress.split(",").firstOrNull()?.trim().orEmpty()
    val area =
      when {
        firstSegment.length >= 2 -> firstSegment.take(60)
        cleanAddress.length >= 2 -> cleanAddress.take(60)
        else -> "Serrekunda"
      }
    val city =
      if (cleanAddress.length >= 2) cleanAddress.take(80)
      else "$area, The Gambia".take(80)
    return area to city
  }

  suspend fun saveTurfDetails(
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
  ): Result<Unit> {
    return try {
      requireUserId()
      val cleanName = turfName.trim().take(80)
      require(cleanName.length >= 2) { "Please enter your turf name." }
      val (cleanArea, cleanCity) = deriveAreaAndCity(address)
      val cleanFormat = primaryFormat.trim().take(30).ifEmpty { "7-a-side" }
      val safePrice = pricePerHour.coerceIn(50L, 50000L)
      val cleanOpen =
        GambianSlotTimeHelper.normalizeTimeOrDefault(openTime, GambianSlotTimeHelper.DEFAULT_OPEN_TIME)
      val cleanClose =
        GambianSlotTimeHelper.normalizeTimeOrDefault(closeTime, GambianSlotTimeHelper.DEFAULT_CLOSE_TIME)
      val openMins = GambianSlotTimeHelper.parseHhMmToMinutes(cleanOpen) ?: (8 * 60)
      val closeMins = GambianSlotTimeHelper.parseHhMmToMinutes(cleanClose) ?: (23 * 60)
      require(closeMins > openMins) { "Closing time must be after opening time." }
      val safeDuration = GambianSlotTimeHelper.normalizeSlotMinutes(slotMinutes)
      val amenities =
        (listOf(cleanFormat) + facilities)
          .map { it.trim() }
          .filter { it.isNotEmpty() }
          .distinct()
          .take(12)
      val timeSlots =
        GambianSlotTimeHelper.generateSlotWindows(cleanOpen, cleanClose, safeDuration)
          .map { "${it.startTime}–${it.endTime}" }
          .take(24)
          .ifEmpty { listOf("16:00–17:00") }
      val cleanPhotos = photos.filter { it.isNotBlank() && it.length <= 140_000 }.take(6)

      db.collection("turfs")
        .document(turfId)
        .update(
          mapOf(
            "name" to cleanName,
            "area" to cleanArea,
            "city" to cleanCity,
            "latitude" to latitude.coerceIn(-90.0, 90.0),
            "longitude" to longitude.coerceIn(-180.0, 180.0),
            "pricePerHour" to safePrice,
            "primaryFormat" to cleanFormat,
            "amenities" to amenities,
            "timeSlots" to timeSlots,
            "openTime" to cleanOpen,
            "closeTime" to cleanClose,
            "slotMinutes" to safeDuration,
            "photos" to cleanPhotos,
            "description" to description.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "turfs/$turfId")
      Result.failure(e)
    }
  }

  suspend fun ensureTurfScheduleDefaults(turfId: String): Result<Unit> {
    return try {
      val turfRef = db.collection("turfs").document(turfId)
      val doc = turfRef.get().await()
      if (!doc.exists()) return Result.success(Unit)

      val timeRegex = Regex("^(([01][0-9]|2[0-3]):[0-5][0-9]|24:00)$")
      val updates = mutableMapOf<String, Any>()
      val existingOpen = doc.getString("openTime")
      if (existingOpen.isNullOrBlank() || !existingOpen.matches(timeRegex)) {
        updates["openTime"] = GambianSlotTimeHelper.DEFAULT_OPEN_TIME
      }
      val existingClose = doc.getString("closeTime")
      if (existingClose.isNullOrBlank() || !existingClose.matches(timeRegex)) {
        updates["closeTime"] = GambianSlotTimeHelper.DEFAULT_CLOSE_TIME
      }
      val existingDuration = doc.getLong("slotMinutes")
      if (existingDuration != 60L && existingDuration != 90L) {
        updates["slotMinutes"] = GambianSlotTimeHelper.DEFAULT_SLOT_MINUTES
      }
      val existingPrice = doc.getLong("pricePerHour")
      if (existingPrice == null || existingPrice < 50L) {
        updates["pricePerHour"] = 400L
      }
      if (updates.isNotEmpty()) {
        updates["updatedAt"] = FieldValue.serverTimestamp()
        turfRef.update(updates).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "turfs/$turfId")
      Result.failure(e)
    }
  }

  suspend fun updateTurfSettings(
    turfId: String,
    openTime: String,
    closeTime: String,
    slotMinutes: Long,
    pricePerHour: Long,
  ): Result<Unit> {
    return try {
      requireUserId()
      val cleanOpen =
        GambianSlotTimeHelper.normalizeTimeOrDefault(openTime, GambianSlotTimeHelper.DEFAULT_OPEN_TIME)
      val cleanClose =
        GambianSlotTimeHelper.normalizeTimeOrDefault(closeTime, GambianSlotTimeHelper.DEFAULT_CLOSE_TIME)
      val openMins = GambianSlotTimeHelper.parseHhMmToMinutes(cleanOpen) ?: (8 * 60)
      val closeMins = GambianSlotTimeHelper.parseHhMmToMinutes(cleanClose) ?: (23 * 60)
      require(closeMins > openMins) { "Closing time must be after opening time." }
      val safeDuration = GambianSlotTimeHelper.normalizeSlotMinutes(slotMinutes)
      val safePrice = pricePerHour.coerceIn(50L, 50000L)

      db.collection("turfs")
        .document(turfId)
        .update(
          mapOf(
            "openTime" to cleanOpen,
            "closeTime" to cleanClose,
            "slotMinutes" to safeDuration,
            "pricePerHour" to safePrice,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "turfs/$turfId")
      Result.failure(e)
    }
  }

  suspend fun fetchTurfSlotsOnce(turfId: String, dateIso: String): Result<List<TurfSlot>> {
    return try {
      requireUserId()
      val snapshot =
        db.collection("turfs")
          .document(turfId)
          .collection("slots")
          .whereEqualTo("date", dateIso)
          .get()
          .await()
      val list =
        snapshot
          .toObjects(
            TurfSlot::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedBy { it.startTime }
      Result.success(list)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun observeTurfSlots(turfId: String, dateIso: String): Flow<List<TurfSlot>> = flow {
    val uid = auth.currentUser?.uid.orEmpty()
    val path = "turfs/$turfId/slots"
    emitAll(
      wrapSnapshotCatch(
        db.collection("turfs")
          .document(turfId)
          .collection("slots")
          .whereEqualTo("date", dateIso)
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(
                TurfSlot::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
              .sortedBy { it.startTime }
          },
        uid,
        OperationType.LIST,
        path,
      )
    )
  }

  fun observeTurfBookings(turfId: String): Flow<List<TurfVenueBooking>> = flow {
    val uid = auth.currentUser?.uid.orEmpty()
    val path = "turfs/$turfId/bookings"
    emitAll(
      wrapSnapshotCatch(
        db.collection("turfs")
          .document(turfId)
          .collection("bookings")
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(
                TurfVenueBooking::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
              .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
          },
        uid,
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun ensureSlotsForDate(
    turfId: String,
    dateIso: String,
    openTime: String,
    closeTime: String,
    slotMinutes: Long,
  ): Result<Int> {
    return try {
      requireUserId()
      val slotsCollection = db.collection("turfs").document(turfId).collection("slots")
      val existing = slotsCollection.whereEqualTo("date", dateIso).get().await()
      val existingIds = existing.documents.map { it.id }.toSet()
      val windows = GambianSlotTimeHelper.generateSlotWindows(openTime, closeTime, slotMinutes)
      val missingWindows =
        windows.filter { window ->
          !existingIds.contains(GambianSlotTimeHelper.slotDocId(dateIso, window.startTime))
        }
      if (missingWindows.isEmpty()) {
        return Result.success(0)
      }
      val batch = db.batch()
      missingWindows.forEach { window ->
        val slotId = GambianSlotTimeHelper.slotDocId(dateIso, window.startTime)
        val slotRef = slotsCollection.document(slotId)
        val payload =
          mapOf(
            "date" to dateIso,
            "startTime" to window.startTime,
            "endTime" to window.endTime,
            "status" to "available",
            "bookingId" to null,
            "startedAt" to null,
            "endsAt" to null,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        batch.set(slotRef, payload)
      }
      batch.commit().await()
      Result.success(missingWindows.size)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "turfs/$turfId/slots")
      Result.failure(e)
    }
  }

  suspend fun createSessionBooking(
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
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val cleanName = customerName.trim().take(80)
      val cleanPhone = phone.trim().take(30)
      require(cleanName.isNotEmpty()) { "Please enter the customer or team name." }
      require(cleanPhone.isNotEmpty()) { "Please enter a phone number." }
      require(coveredSlots.isNotEmpty()) { "Selected time range has no matching pitch slots." }
      require(hours > 0.0 && hours <= 16.0) { "Finish time must be after start time." }

      val nowMs = System.currentTimeMillis()
      val conflictingSlot =
        coveredSlots.firstOrNull { slot ->
          slot.effectiveStatus(nowMs) in listOf("booked", "in_progress", "blocked")
        }
      if (conflictingSlot != null) {
        val label =
          "${GambianSlotTimeHelper.formatHhMmTo12Hour(conflictingSlot.startTime)}–${GambianSlotTimeHelper.formatHhMmTo12Hour(conflictingSlot.endTime)}"
        val reason =
          when (conflictingSlot.effectiveStatus(nowMs)) {
            "in_progress" -> "already in use"
            "booked" -> "already booked"
            else -> "blocked"
          }
        return Result.failure(IllegalStateException("Slot $label is $reason. Pick an open time range."))
      }

      val cleanMethod = if (paymentMethod.equals("wave", ignoreCase = true)) "wave" else "cash"
      val safeTotal = totalPrice.coerceIn(0L, 500_000L)
      val turfRef = db.collection("turfs").document(turfId)
      val bookingRef = turfRef.collection("bookings").document()

      val bookingPayload =
        mapOf(
          "source" to "walk_in",
          "customerName" to cleanName,
          "phone" to cleanPhone,
          "date" to dateIso,
          "startTime" to startTime,
          "endTime" to endTime,
          "hours" to hours,
          "totalPrice" to safeTotal,
          "paymentMethod" to cleanMethod,
          "paid" to true,
          "slotId" to coveredSlots.first().id,
          "slotIds" to coveredSlots.map { it.id },
          "turfId" to turfId,
          "ownerId" to uid,
          "status" to if (startNow) "in_progress" else "confirmed",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )

      val endsAt =
        if (startNow) {
          Timestamp(Date(GambianSlotTimeHelper.computeSlotEndEpochMillis(dateIso, endTime)))
        } else {
          null
        }

      val batch = db.batch()
      batch.set(bookingRef, bookingPayload)
      coveredSlots.forEach { slot ->
        val slotUpdates =
          if (startNow) {
            mapOf(
              "status" to "in_progress",
              "bookingId" to bookingRef.id,
              "startedAt" to FieldValue.serverTimestamp(),
              "endsAt" to endsAt,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          } else {
            mapOf(
              "status" to "booked",
              "bookingId" to bookingRef.id,
              "startedAt" to null,
              "endsAt" to null,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          }
        batch.update(turfRef.collection("slots").document(slot.id), slotUpdates)
      }
      batch.commit().await()
      Result.success(bookingRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "turfs/$turfId/bookings")
      Result.failure(e)
    }
  }

  suspend fun createWalkInBooking(
    turfId: String,
    slot: TurfSlot,
    guestName: String,
    guestPhone: String,
    pricePerHour: Long = 450L,
  ): Result<String> {
    val hours = GambianSlotTimeHelper.hoursBetween(slot.startTime, slot.endTime).coerceAtLeast(1.0)
    val totalPrice = (hours * pricePerHour).toLong()
    val safePhone = guestPhone.trim().ifEmpty { "Walk-in" }
    return createSessionBooking(
      turfId = turfId,
      dateIso = slot.date,
      coveredSlots = listOf(slot),
      customerName = guestName,
      phone = safePhone,
      startTime = slot.startTime,
      endTime = slot.endTime,
      hours = hours,
      totalPrice = totalPrice,
      paymentMethod = "cash",
      startNow = false,
    )
  }

  suspend fun blockSlot(turfId: String, slotId: String): Result<Unit> {
    return try {
      requireUserId()
      db.collection("turfs")
        .document(turfId)
        .collection("slots")
        .document(slotId)
        .update(
          mapOf(
            "status" to "blocked",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "turfs/$turfId/slots/$slotId")
      Result.failure(e)
    }
  }

  suspend fun unblockSlot(turfId: String, slotId: String): Result<Unit> {
    return try {
      requireUserId()
      db.collection("turfs")
        .document(turfId)
        .collection("slots")
        .document(slotId)
        .update(
          mapOf(
            "status" to "available",
            "bookingId" to null,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "turfs/$turfId/slots/$slotId")
      Result.failure(e)
    }
  }

  suspend fun startSlotSession(
    turfId: String,
    slotsToStart: List<TurfSlot>,
    bookingId: String?,
  ): Result<Unit> {
    return try {
      requireUserId()
      require(slotsToStart.isNotEmpty())
      val ordered = slotsToStart.sortedBy { it.startTime }
      val lastSlot = ordered.last()
      val endEpoch = GambianSlotTimeHelper.computeSlotEndEpochMillis(lastSlot.date, lastSlot.endTime)
      val endsAt = Timestamp(Date(endEpoch))
      val turfRef = db.collection("turfs").document(turfId)

      val batch = db.batch()
      ordered.forEach { slot ->
        batch.update(
          turfRef.collection("slots").document(slot.id),
          mapOf(
            "status" to "in_progress",
            "startedAt" to FieldValue.serverTimestamp(),
            "endsAt" to endsAt,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      if (!bookingId.isNullOrBlank()) {
        batch.update(
          turfRef.collection("bookings").document(bookingId),
          mapOf(
            "status" to "in_progress",
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "turfs/$turfId/slots")
      Result.failure(e)
    }
  }

  suspend fun startSlotSession(turfId: String, slot: TurfSlot): Result<Unit> =
    startSlotSession(turfId, listOf(slot), slot.bookingId)

  suspend fun endSession(
    turfId: String,
    slotsToFree: List<TurfSlot>,
    bookingId: String?,
  ): Result<Unit> {
    return try {
      requireUserId()
      val turfRef = db.collection("turfs").document(turfId)
      val batch = db.batch()
      slotsToFree.forEach { slot ->
        batch.update(
          turfRef.collection("slots").document(slot.id),
          mapOf(
            "status" to "available",
            "bookingId" to null,
            "startedAt" to null,
            "endsAt" to null,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      if (!bookingId.isNullOrBlank()) {
        batch.update(
          turfRef.collection("bookings").document(bookingId),
          mapOf(
            "status" to "completed",
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "turfs/$turfId/slots")
      Result.failure(e)
    }
  }

  suspend fun endSlotSession(turfId: String, slot: TurfSlot): Result<Unit> =
    endSession(turfId, listOf(slot), slot.bookingId)

  suspend fun markSlotNoShow(
    turfId: String,
    slotsToFree: List<TurfSlot>,
    bookingId: String?,
  ): Result<Unit> {
    return try {
      requireUserId()
      val turfRef = db.collection("turfs").document(turfId)
      val batch = db.batch()
      slotsToFree.forEach { slot ->
        batch.update(
          turfRef.collection("slots").document(slot.id),
          mapOf(
            "status" to "available",
            "bookingId" to null,
            "startedAt" to null,
            "endsAt" to null,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      if (!bookingId.isNullOrBlank()) {
        batch.update(
          turfRef.collection("bookings").document(bookingId),
          mapOf(
            "status" to "no_show",
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "turfs/$turfId/slots")
      Result.failure(e)
    }
  }

  suspend fun markSlotNoShow(turfId: String, slot: TurfSlot): Result<Unit> =
    markSlotNoShow(turfId, listOf(slot), slot.bookingId)

  fun observeBookings(userId: String): Flow<List<TurfBooking>> = flow {
    val path = "users/$userId/bookings"
    emitAll(
      wrapSnapshotCatch(
        db.collection("users").document(userId).collection("bookings").snapshots().map { snapshot ->
          snapshot.toObjects(
            TurfBooking::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        },
        userId,
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun createBooking(
    turf: TurfVenue,
    dateLabel: String,
    timeSlot: String,
    pitchFormat: String = turf.primaryFormat,
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid).collection("bookings").document()
      val payload =
        mapOf(
          "userId" to uid,
          "turfId" to turf.id,
          "turfName" to turf.name,
          "turfLocation" to turf.city,
          "pricePerHour" to turf.pricePerHour.toLong(),
          "dateLabel" to dateLabel,
          "timeSlot" to timeSlot,
          "pitchFormat" to pitchFormat,
          "status" to "confirmed",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/bookings")
      Result.failure(e)
    }
  }

  suspend fun cancelBooking(bookingId: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
      docRef.update(mapOf("status" to "cancelled", "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/bookings/$bookingId")
      Result.failure(e)
    }
  }

  fun observeRegistrations(userId: String): Flow<List<TournamentRegistration>> = flow {
    val path = "users/$userId/registrations"
    emitAll(
      wrapSnapshotCatch(
        db.collection("users").document(userId).collection("registrations").snapshots().map { snapshot ->
          snapshot.toObjects(
            TournamentRegistration::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        },
        userId,
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun registerForTournament(tournament: Tournament, teamName: String): Result<String> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid).collection("registrations").document(tournament.id)
      val existing = docRef.get().await()
      if (existing.exists() && existing.getString("status") == "registered") {
        return Result.success(docRef.id)
      }
      val payload =
        mapOf(
          "userId" to uid,
          "tournamentId" to tournament.id,
          "tournamentTitle" to tournament.title,
          "teamName" to teamName.trim().take(80).ifEmpty { "Brikama Warriors" },
          "location" to tournament.location,
          "dateRange" to tournament.dateRange,
          "prizePool" to tournament.prizePool,
          "status" to "registered",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/registrations")
      Result.failure(e)
    }
  }

  fun observeChallenges(userId: String): Flow<List<MatchChallenge>> = flow {
    val path = "users/$userId/challenges"
    emitAll(
      wrapSnapshotCatch(
        db.collection("users").document(userId).collection("challenges").snapshots().map { snapshot ->
          snapshot.toObjects(
            MatchChallenge::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        },
        userId,
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun createChallenge(
    hostTeamName: String,
    turfName: String,
    stakeLabel: String,
    format: String,
    scheduleLabel: String,
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid).collection("challenges").document()
      val maxPlayers = if (format.contains("5")) 10L else 14L
      val payload =
        mapOf(
          "userId" to uid,
          "hostTeamName" to hostTeamName.trim().take(80).ifEmpty { "Brikama Warriors" },
          "opponentName" to "Anyone",
          "turfName" to turfName.trim().take(80).ifEmpty { "Paradise Turf" },
          "stakeLabel" to stakeLabel.trim().take(40).ifEmpty { "D500" },
          "format" to format.trim().take(30).ifEmpty { "7-a-side" },
          "scheduleLabel" to scheduleLabel.trim().take(60).ifEmpty { "Today • 7:00 PM" },
          "playersCount" to 7L,
          "maxPlayers" to maxPlayers,
          "isCreatedByUser" to true,
          "status" to "open",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/challenges")
      Result.failure(e)
    }
  }

  suspend fun joinSeedChallenge(seed: SeedChallenge, playerTeamName: String): Result<String> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid).collection("challenges").document(seed.id)
      val payload =
        mapOf(
          "userId" to uid,
          "hostTeamName" to seed.hostTeamName,
          "opponentName" to
            if (seed.opponentName == "Anyone") playerTeamName.trim().ifEmpty { "Brikama Warriors" }
            else seed.opponentName,
          "turfName" to seed.turfName,
          "stakeLabel" to seed.stakeLabel,
          "format" to seed.format,
          "scheduleLabel" to seed.scheduleLabel,
          "playersCount" to (seed.playersCount + 1).coerceAtMost(seed.maxPlayers).toLong(),
          "maxPlayers" to seed.maxPlayers.toLong(),
          "isCreatedByUser" to false,
          "status" to "accepted",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/challenges/${seed.id}")
      Result.failure(e)
    }
  }

  suspend fun markChallengeCompleted(challengeId: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid).collection("challenges").document(challengeId)
      docRef.update(mapOf("status" to "completed", "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/challenges/$challengeId")
      Result.failure(e)
    }
  }

  fun observeFavorites(userId: String): Flow<List<PlayerFavorite>> = flow {
    val path = "users/$userId/favorites"
    emitAll(
      wrapSnapshotCatch(
        db.collection("users").document(userId).collection("favorites").snapshots().map { snapshot ->
          snapshot.toObjects(
            PlayerFavorite::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        },
        userId,
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun toggleFavorite(
    targetId: String,
    targetType: String,
    currentlyFavorited: Boolean,
  ): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docId = "${targetType}_$targetId"
      val docRef = db.collection("users").document(uid).collection("favorites").document(docId)
      if (currentlyFavorited) {
        docRef.delete().await()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "targetId" to targetId,
              "targetType" to targetType,
              "createdAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/favorites")
      Result.failure(e)
    }
  }
}
