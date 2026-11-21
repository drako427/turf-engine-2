package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.catalog.GambianSlotTimeHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FootballHubRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun completeOnboarding_authenticatedOwner_persistsLocationAndProfile() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FootballHubRepository(firestore)

    val result =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.completeOnboarding(
          displayName = "Mustapha",
          handle = "@mustapha10",
          city = "Serrekunda",
          position = "Midfielder",
          favoriteTeamId = "young_stars_fc",
        )
      }
    assertTrue("Onboarding should succeed: ${result.exceptionOrNull()}", result.isSuccess)

    val profile =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observePlayerProfile(uid).first { it != null && it.onboardingCompleted }
      }
    assertEquals("Serrekunda", profile?.city)
    assertEquals("Midfielder", profile?.position)
    assertEquals(true, profile?.onboardingCompleted)
  }

  @Test
  fun createAndObserveBooking_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FootballHubRepository(firestore)
    val turf = GambianFootballCatalog.turfs.first()

    val createResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createBooking(turf, "Sat Nov 22", "11:00–12:00")
      }
    assertTrue("Booking creation should succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
    val bookingId = createResult.getOrThrow()

    val bookings =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeBookings(uid).first { list -> list.any { it.id == bookingId } }
      }
    assertTrue(bookings.any { it.id == bookingId && it.turfName == "Paradise Turf" })
  }

  @Test
  fun createChallengeAndRegisterTournament_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FootballHubRepository(firestore)

    val challengeResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createChallenge(
          hostTeamName = "Brikama Warriors",
          turfName = "Paradise Turf",
          stakeLabel = "D500",
          format = "7-a-side",
          scheduleLabel = "Today • 6:00 PM",
        )
      }
    assertTrue(challengeResult.isSuccess)

    val challenges =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeChallenges(uid).first { it.isNotEmpty() }
      }
    assertTrue(challenges.any { it.hostTeamName == "Brikama Warriors" && it.status == "open" })

    val tournament = GambianFootballCatalog.tournaments.first()
    val regResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.registerForTournament(tournament, "Brikama Warriors")
      }
    assertTrue(regResult.isSuccess)
  }

  @Test
  fun crossUserBookingRead_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FootballHubRepository(firestore)
    val turf = GambianFootballCatalog.turfs.first()
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createBooking(turf, "Sat Nov 22", "11:00–12:00").getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).collection("bookings").get().await()
      }
      fail("Expected PERMISSION_DENIED for cross-user bookings query")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
  }

  @Test
  fun observeBookings_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = FootballHubRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeBookings("alice_unauth_check").first()
      }
      fail("Expected PERMISSION_DENIED when unauthenticated")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
  }

  @Test
  fun submitOwnerOnboarding_withValidCode_createsOwnerPendingAndPendingTurf() = runBlocking {
    seedOwnerMembershipCode(code = "TURF-GM-2026", active = true)
    val ownerUid = signInTestUser("owner.valid.${System.nanoTime()}@footballhub.gm")
    val repository = FootballHubRepository(firestore)

    val result =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.submitOwnerOnboarding(
          membershipCode = "TURF-GM-2026",
          ownerName = "Lamin Jallow",
          contactPhone = "+220 712 3456",
          turfName = "Bakau Coastal Turf",
          area = "Bakau",
          pricePerHour = 500L,
          primaryFormat = "7-a-side",
          hasFloodlights = true,
          description = "Synthetic 7-a-side pitch near Bakau.",
        )
      }
    assertTrue("Owner onboarding should succeed: ${result.exceptionOrNull()}", result.isSuccess)
    val turfId = result.getOrThrow()

    val ownerProfile =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observePlayerProfile(ownerUid).first { it != null }
      }
    assertEquals("owner_pending", ownerProfile?.role)
    assertEquals(turfId, ownerProfile?.primaryTurfId)

    val ownerTurfs =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeOwnerTurfs(ownerUid).first { it.any { t -> t.id == turfId } }
      }
    assertEquals("pending", ownerTurfs.first { it.id == turfId }.verificationStatus)
  }

  @Test
  fun submitOwnerOnboarding_withInvalidCode_failsWithPermissionDenied() = runBlocking {
    seedOwnerMembershipCode(code = "TURF-GM-2026", active = true)
    signInTestUser("owner.bad.${System.nanoTime()}@footballhub.gm")
    val repository = FootballHubRepository(firestore)

    val result =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.submitOwnerOnboarding(
          membershipCode = "WRONG-CODE",
          ownerName = "Lamin Jallow",
          contactPhone = "+220 712 3456",
          turfName = "Unauthorized Turf",
          area = "Bakau",
          pricePerHour = 500L,
          primaryFormat = "7-a-side",
          hasFloodlights = false,
          description = "",
        )
      }
    assertTrue("Wrong code should be rejected", result.isFailure)
    val fsEx = result.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
  }

  @Test
  fun observeVerifiedTurfs_hidesPendingTurfsFromPlayersUntilVerified() = runBlocking {
    seedOwnerMembershipCode(code = "TURF-GM-2026", active = true)
    signInTestUser("owner.vis.${System.nanoTime()}@footballhub.gm")
    val ownerRepo = FootballHubRepository(firestore)

    val turfId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo
          .submitOwnerOnboarding(
            membershipCode = "TURF-GM-2026",
            ownerName = "Ousman Touray",
            contactPhone = "+220 799 0011",
            turfName = "Kololi Night Arena",
            area = "Kololi",
            pricePerHour = 600L,
            primaryFormat = "7-a-side",
            hasFloodlights = true,
            description = "Floodlit turf",
          )
          .getOrThrow()
      }

    // Player sees NO pending turfs
    signInTestUser(BOB_EMAIL)
    val playerRepo = FootballHubRepository(firestore)
    val beforeVerify =
      withTimeout(FLOW_TIMEOUT_MS) { playerRepo.observeVerifiedTurfs().first() }
    assertTrue(
      "Players must not see pending turfs",
      beforeVerify.none { it.id == turfId },
    )

    // Admin verifies turf -> now player sees it
    adminVerifyTurf(turfId)
    val afterVerify =
      withTimeout(FLOW_TIMEOUT_MS) {
        playerRepo.observeVerifiedTurfs().first { list -> list.any { it.id == turfId } }
      }
    assertTrue(afterVerify.any { it.id == turfId && it.isVerified })
  }

  @Test
  fun verifyOwnerMembershipCode_checkDocument_allowsValidAndRejectsInvalidWithoutAuth() = runBlocking {
    seedOwnerMembershipCode(code = "TURF-GM-2026", active = true)
    auth.signOut()
    val repository = FootballHubRepository(firestore)

    val validResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.verifyOwnerMembershipCode("TURF-GM-2026")
      }
    assertTrue("Valid code check should succeed: ${validResult.exceptionOrNull()}", validResult.isSuccess)

    val invalidResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.verifyOwnerMembershipCode("WRONG-CODE")
      }
    assertTrue("Invalid code check should fail", invalidResult.isFailure)
    val fsEx = invalidResult.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
  }

  @Test
  fun ownerSlotsAndWalkInLifecycle_verifiedOwnerAndPlayerReadOnly() = runBlocking {
    seedOwnerMembershipCode(code = "TURF-GM-2026", active = true)
    val ownerUid = signInTestUser("owner.slots.${System.nanoTime()}@footballhub.gm")
    val ownerRepo = FootballHubRepository(firestore)

    val turfId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo
          .submitOwnerOnboarding(
            membershipCode = "TURF-GM-2026",
            ownerName = "Modou Ceesay",
            contactPhone = "+220 700 1122",
            turfName = "Serrekunda East Turf",
            area = "Serrekunda",
            pricePerHour = 500L,
            primaryFormat = "7-a-side",
            hasFloodlights = true,
            description = "Main turf",
          )
          .getOrThrow()
      }

    // Pending owner/turf cannot create slots yet
    val todayIso = GambianSlotTimeHelper.todayIsoDateGmt()
    val pendingAttempt =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo.ensureSlotsForDate(turfId, todayIso, "08:00", "23:00", 60L)
      }
    assertTrue("Pending owner must not be allowed to create slots", pendingAttempt.isFailure)

    // Promote owner and verify turf
    adminPromoteOwner(ownerUid)
    adminVerifyTurf(turfId)

    // Batch generation creates 15 slots idempotently
    val createdCount =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo.ensureSlotsForDate(turfId, todayIso, "08:00", "23:00", 60L).getOrThrow()
      }
    assertEquals(15, createdCount)

    val secondCount =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo.ensureSlotsForDate(turfId, todayIso, "08:00", "23:00", 60L).getOrThrow()
      }
    assertEquals(0, secondCount)

    val slots =
      withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeTurfSlots(turfId, todayIso).first { it.size == 15 }
      }
    val firstSlot = slots.first()
    val secondSlot = slots[1]

    // Walk-in -> Start session -> End session
    withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.createWalkInBooking(turfId, firstSlot, "Banjul Sharks", "+220 711 2233").getOrThrow()
    }
    val bookedSlot =
      withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeTurfSlots(turfId, todayIso).first { list ->
          list.first { it.id == firstSlot.id }.status == "booked"
        }.first { it.id == firstSlot.id }
      }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.startSlotSession(turfId, bookedSlot).getOrThrow()
    }
    val inProgressSlot =
      withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeTurfSlots(turfId, todayIso).first { list ->
          list.first { it.id == firstSlot.id }.status == "in_progress"
        }.first { it.id == firstSlot.id }
      }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.endSlotSession(turfId, inProgressSlot).getOrThrow()
    }

    // Block -> Unblock, and Walk-in -> No-show reverts to available
    withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.blockSlot(turfId, secondSlot.id).getOrThrow()
      ownerRepo.unblockSlot(turfId, secondSlot.id).getOrThrow()
      ownerRepo.createWalkInBooking(turfId, secondSlot, "Late Team", "+220 799 8877").getOrThrow()
    }
    val secondBooked =
      withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeTurfSlots(turfId, todayIso).first { list ->
          list.first { it.id == secondSlot.id }.status == "booked"
        }.first { it.id == secondSlot.id }
      }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.markSlotNoShow(turfId, secondBooked).getOrThrow()
    }
    val reverted =
      withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeTurfSlots(turfId, todayIso).first { list ->
          list.first { it.id == secondSlot.id }.status == "available"
        }.first { it.id == secondSlot.id }
      }
    assertEquals("available", reverted.status)

    // Player CAN read live slots of verified turf, CANNOT modify slots
    signInTestUser(ALICE_EMAIL)
    val playerRepo = FootballHubRepository(firestore)
    val playerSlots =
      withTimeout(FLOW_TIMEOUT_MS) {
        playerRepo.observeTurfSlots(turfId, todayIso).first { it.size == 15 }
      }
    assertEquals(15, playerSlots.size)

    val playerWriteAttempt =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        playerRepo.blockSlot(turfId, secondSlot.id)
      }
    assertTrue("Player must not be allowed to modify slots", playerWriteAttempt.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      playerWriteAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )
  }

  @Test
  fun multiSlotNewSessionAndSaveTurfDetails_respectsRulesAndFreesSlots() = runBlocking {
    seedOwnerMembershipCode(code = "TURF-GM-2026", active = true)
    val ownerUid = signInTestUser("owner.multisession.${System.nanoTime()}@footballhub.gm")
    val ownerRepo = FootballHubRepository(firestore)

    val turfId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo
          .submitOwnerOnboarding(
            membershipCode = "TURF-GM-2026",
            ownerName = "Ebrima Cham",
            contactPhone = "+220 701 2345",
            turfName = "Banjul Coastal Arena",
            area = "Banjul, The Gambia",
            pricePerHour = 450L,
            primaryFormat = "5-a-side",
            hasFloodlights = true,
            description = "Modern turf",
            openTime = "16:00",
            closeTime = "24:00",
            photos = listOf("data:image/jpeg;base64,testphoto"),
          )
          .getOrThrow()
      }

    // Editing a pending turf keeps verificationStatus == "pending" and saves area + city + photos
    withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo
        .saveTurfDetails(
          turfId = turfId,
          turfName = "Gambia Sports Arena",
          address = "Banjul, Gambia",
          latitude = 13.4549,
          longitude = -16.5790,
          pricePerHour = 450L,
          primaryFormat = "5-a-side",
          facilities = listOf("Floodlights", "Parking"),
          openTime = "16:00",
          closeTime = "24:00",
          slotMinutes = 60L,
          photos = listOf("data:image/jpeg;base64,updatedphoto"),
          description = "Modern turf with great atmosphere.",
        )
        .getOrThrow()
    }
    val pendingTurf =
      withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeOwnerTurfs(ownerUid).first { it.any { t -> t.id == turfId && t.name == "Gambia Sports Arena" } }
      }.first { it.id == turfId }
    assertEquals("pending", pendingTurf.verificationStatus)
    assertEquals("Banjul", pendingTurf.area)
    assertEquals("Banjul, Gambia", pendingTurf.city)
    assertEquals("24:00", pendingTurf.closeTime)

    adminPromoteOwner(ownerUid)
    adminVerifyTurf(turfId)

    val todayIso = GambianSlotTimeHelper.todayIsoDateGmt()
    withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.ensureSlotsForDate(turfId, todayIso, "16:00", "24:00", 60L).getOrThrow()
    }
    val slots =
      withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeTurfSlots(turfId, todayIso).first { it.size == 8 }
      }

    // 2-hour Start Timer session ("17:00" to "19:00") using Wave
    val twoHourSlots = GambianSlotTimeHelper.findCoveredSlotsForRange(slots, "17:00", "19:00")
    assertEquals(2, twoHourSlots.size)

    val sessionBookingId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo
          .createSessionBooking(
            turfId = turfId,
            dateIso = todayIso,
            coveredSlots = twoHourSlots,
            customerName = "Young Stars FC",
            phone = "+220 7012345",
            startTime = "17:00",
            endTime = "19:00",
            hours = 2.0,
            totalPrice = 850L,
            paymentMethod = "wave",
            startNow = true,
          )
          .getOrThrow()
      }

    // Overlap check rejects overlapping range
    val updatedSlots =
      withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeTurfSlots(turfId, todayIso).first { list ->
          list.count { it.status == "in_progress" } == 2
        }
      }
    val overlappingRange = GambianSlotTimeHelper.findCoveredSlotsForRange(updatedSlots, "18:00", "20:00")
    val overlapAttempt =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo.createSessionBooking(
          turfId = turfId,
          dateIso = todayIso,
          coveredSlots = overlappingRange,
          customerName = "Real Banjul FC",
          phone = "+220 7999999",
          startTime = "18:00",
          endTime = "20:00",
          hours = 2.0,
          totalPrice = 900L,
          paymentMethod = "cash",
          startNow = false,
        )
      }
    assertTrue("Overlapping booking must fail cleanly", overlapAttempt.isFailure)

    // Ending session frees both slots back to available & marks booking completed
    val activeSlots = updatedSlots.filter { it.bookingId == sessionBookingId }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.endSession(turfId, activeSlots, sessionBookingId).getOrThrow()
    }
    val freedSlots =
      withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeTurfSlots(turfId, todayIso).first { list ->
          list.all { it.status == "available" && it.bookingId == null }
        }
      }
    assertEquals(8, freedSlots.size)
  }

  private companion object {
    const val ALICE_EMAIL = "alice.player@footballhub.gm"
    const val BOB_EMAIL = "bob.player@footballhub.gm"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
