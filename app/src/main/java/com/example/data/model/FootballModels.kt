package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PlayerProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val role: String = "player",
  val primaryTurfId: String = "",
  val contactPhone: String = "",
  val displayName: String = "Drako",
  val handle: String = "@drako427",
  val city: String = "Banjul",
  val position: String = "Forward",
  val favoriteTeamId: String = "brikama_warriors",
  val onboardingCompleted: Boolean = false,
  val matchesPlayed: Long = 24L,
  val wins: Long = 16L,
  val trophies: Long = 4L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TurfListing(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val name: String = "",
  val area: String = "",
  val city: String = "",
  val latitude: Double = 13.4383,
  val longitude: Double = -16.6781,
  val pricePerHour: Long = 400L,
  val primaryFormat: String = "7-a-side",
  val amenities: List<String> = emptyList(),
  val timeSlots: List<String> = emptyList(),
  val openTime: String = "08:00",
  val closeTime: String = "23:00",
  val slotMinutes: Long = 60L,
  val photos: List<String> = emptyList(),
  val logoUrl: String = "",
  val description: String = "",
  val contactPhone: String = "",
  val verificationStatus: String = "pending",
  val isOpen: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun effectiveOpenTime(): String =
    openTime.takeIf { it.matches(Regex("^(([01][0-9]|2[0-3]):[0-5][0-9]|24:00)$")) } ?: "08:00"

  fun effectiveCloseTime(): String =
    closeTime.takeIf { it.matches(Regex("^(([01][0-9]|2[0-3]):[0-5][0-9]|24:00)$")) } ?: "23:00"

  fun effectiveSlotMinutes(): Long = if (slotMinutes == 90L) 90L else 60L

  fun toTurfVenue(): TurfVenue =
    TurfVenue(
      id = id,
      name = name,
      area = area,
      city = city,
      distanceKm = 1.8,
      walkMinutes = 15,
      rating = 4.9,
      reviewCountLabel = "Verified venue",
      pricePerHour = pricePerHour.toInt(),
      isVerified = verificationStatus == "verified",
      isOpen = isOpen,
      primaryFormat = primaryFormat,
      amenities = amenities.ifEmpty { listOf(primaryFormat) },
      timeSlots =
        timeSlots.ifEmpty {
          listOf("16:00–17:00", "18:00–19:00", "20:00–21:00")
        },
      latitude = latitude,
      longitude = longitude,
      category = "All Turfs",
      description = description.ifBlank { "Verified synthetic football turf in $area." },
      photos = photos,
      logoUrl = logoUrl,
      openTime = effectiveOpenTime(),
      closeTime = effectiveCloseTime(),
      slotMinutes = effectiveSlotMinutes(),
      createdAtMillis = createdAt?.toDate()?.time ?: 0L,
    )
}

@IgnoreExtraProperties
data class TurfSlot(
  @DocumentId val id: String = "",
  val date: String = "",
  val startTime: String = "",
  val endTime: String = "",
  val status: String = "available",
  val bookingId: String? = null,
  val startedAt: Timestamp? = null,
  val endsAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun effectiveStatus(nowMillis: Long = System.currentTimeMillis()): String {
    if (status == "in_progress") {
      val endMs = endsAt?.toDate()?.time
      if (endMs != null && endMs <= nowMillis) {
        return "completed"
      }
    }
    return status
  }
}

@IgnoreExtraProperties
data class TurfVenueBooking(
  @DocumentId val id: String = "",
  val source: String = "walk_in",
  val customerName: String = "",
  val phone: String = "",
  val date: String = "",
  val startTime: String = "",
  val endTime: String = "",
  val hours: Double = 1.0,
  val totalPrice: Long = 0L,
  val paymentMethod: String = "cash",
  val paid: Boolean = true,
  val slotId: String = "",
  val slotIds: List<String> = emptyList(),
  val turfId: String = "",
  val ownerId: String = "",
  val status: String = "confirmed",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun displayTeamName(): String = customerName.ifBlank { "Walk-in" }
}

@IgnoreExtraProperties
data class TurfBooking(
  @DocumentId val id: String = "",
  val userId: String = "",
  val turfId: String = "",
  val turfName: String = "",
  val turfLocation: String = "",
  val pricePerHour: Long = 0L,
  val dateLabel: String = "",
  val timeSlot: String = "",
  val pitchFormat: String = "7-a-side",
  val status: String = "confirmed",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TournamentRegistration(
  @DocumentId val id: String = "",
  val userId: String = "",
  val tournamentId: String = "",
  val tournamentTitle: String = "",
  val teamName: String = "",
  val location: String = "",
  val dateRange: String = "",
  val prizePool: String = "",
  val status: String = "registered",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MatchChallenge(
  @DocumentId val id: String = "",
  val userId: String = "",
  val hostTeamName: String = "",
  val opponentName: String = "Anyone",
  val turfName: String = "",
  val stakeLabel: String = "D500",
  val format: String = "7-a-side",
  val scheduleLabel: String = "Today • 6:00 PM",
  val playersCount: Long = 8L,
  val maxPlayers: Long = 14L,
  val isCreatedByUser: Boolean = true,
  val status: String = "open",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PlayerFavorite(
  @DocumentId val id: String = "",
  val userId: String = "",
  val targetId: String = "",
  val targetType: String = "turf",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

data class GeoPoint(
  val latitude: Double,
  val longitude: Double,
)

data class TurfVenue(
  val id: String,
  val name: String,
  val area: String,
  val city: String,
  val distanceKm: Double,
  val walkMinutes: Int,
  val rating: Double,
  val reviewCountLabel: String,
  val pricePerHour: Int,
  val isVerified: Boolean,
  val isOpen: Boolean,
  val primaryFormat: String,
  val amenities: List<String>,
  val timeSlots: List<String>,
  val latitude: Double,
  val longitude: Double,
  val category: String,
  val description: String,
  val photos: List<String> = emptyList(),
  val logoUrl: String = "",
  val openTime: String = "08:00",
  val closeTime: String = "23:00",
  val slotMinutes: Long = 60L,
  val createdAtMillis: Long = 0L,
)

data class OsmPitch(
  val id: String,
  val osmId: Long,
  val osmType: String,
  val name: String,
  val area: String,
  val latitude: Double,
  val longitude: Double,
  val surface: String,
  val isFloodlit: Boolean,
  val accessLabel: String,
)

data class OsmPlaceSuggestion(
  val id: String,
  val shortName: String,
  val displayName: String,
  val areaType: String,
  val latitude: Double,
  val longitude: Double,
)

data class Tournament(
  val id: String,
  val title: String,
  val category: String,
  val teamCount: Int,
  val prizePool: String,
  val dateRange: String,
  val location: String,
  val entryFee: String,
  val isFeatured: Boolean,
  val description: String,
)

data class TeamMatchResult(
  val opponent: String,
  val score: String,
  val outcome: String,
)

data class FootballTeam(
  val id: String,
  val name: String,
  val city: String,
  val memberCount: Int,
  val trophyCount: Int,
  val matchesPlayed: Int,
  val wins: Int,
  val draws: Int,
  val losses: Int,
  val motto: String,
  val bio: String,
  val isVerified: Boolean,
  val badgeTintHex: Long,
  val recentMatches: List<TeamMatchResult>,
  val achievements: List<String>,
)

data class SeedChallenge(
  val id: String,
  val hostTeamName: String,
  val opponentName: String,
  val turfName: String,
  val stakeLabel: String,
  val format: String,
  val scheduleLabel: String,
  val playersCount: Int,
  val maxPlayers: Int,
)

data class HubNotification(
  val id: String,
  val title: String,
  val subtitle: String,
  val timeAgo: String,
  val isUnread: Boolean,
)
