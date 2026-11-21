package com.example.data.catalog

import com.example.data.model.FootballTeam
import com.example.data.model.GeoPoint
import com.example.data.model.HubNotification
import com.example.data.model.SeedChallenge
import com.example.data.model.TeamMatchResult
import com.example.data.model.Tournament
import com.example.data.model.TurfVenue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object GambianFootballCatalog {
  val cities = listOf("Banjul", "Serrekunda", "Brikama", "Bakau", "Kololi")

  val cityCoordinates: Map<String, GeoPoint> =
    mapOf(
      "banjul" to GeoPoint(13.4549, -16.5790),
      "serrekunda" to GeoPoint(13.4383, -16.6781),
      "brikama" to GeoPoint(13.2714, -16.6494),
      "bakau" to GeoPoint(13.4706, -16.6778),
      "kololi" to GeoPoint(13.4436, -16.7239),
      "kanifing" to GeoPoint(13.4530, -16.6750),
      "brusubi" to GeoPoint(13.3955, -16.7235),
      "sukuta" to GeoPoint(13.4103, -16.7082),
      "banjulinding" to GeoPoint(13.3688, -16.6525),
      "tabokoto" to GeoPoint(13.4148, -16.6815),
    )

  fun resolveAreaPoint(cityOrArea: String): GeoPoint {
    val normalized = cityOrArea.trim().lowercase()
    cityCoordinates.entries.firstOrNull { (key, _) ->
      normalized == key || normalized.contains(key)
    }?.let { return it.value }
    return GeoPoint(13.4383, -16.6781) // Greater Banjul / Serrekunda center
  }

  fun haversineDistanceKm(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
  ): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a =
      sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) *
          cos(Math.toRadians(lat2)) *
          sin(dLon / 2) *
          sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return ((earthRadiusKm * c) * 10.0).roundToInt() / 10.0
  }

  fun estimateWalkMinutes(distanceKm: Double): Int =
    (distanceKm * 12.0).roundToInt().coerceAtLeast(3)

  val dateOptions = listOf("Sat Nov 22", "Sun Nov 23", "Mon Nov 24", "Tue Nov 25")

  val turfs =
    listOf(
      TurfVenue(
        id = "paradise_turf",
        name = "Paradise Turf",
        area = "Serrekunda",
        city = "Serrekunda, The Gambia",
        distanceKm = 1.5,
        walkMinutes = 12,
        rating = 4.7,
        reviewCountLabel = "1.2k reviews",
        pricePerHour = 400,
        isVerified = true,
        isOpen = true,
        primaryFormat = "7-a-side",
        amenities = listOf("7-a-side", "Floodlights", "Changing Rooms", "Parking"),
        timeSlots =
          listOf(
            "09:00–10:00",
            "10:00–11:00",
            "11:00–12:00",
            "12:00–13:00",
            "13:00–14:00",
            "14:00–15:00",
            "17:00–18:00",
            "19:00–20:00",
            "20:00–21:00",
          ),
        latitude = 13.4383,
        longitude = -16.6781,
        category = "All Turfs",
        description =
          "FIFA-grade synthetic 7-a-side turf in the heart of Serrekunda with pro LED floodlights, tiled changing rooms, and cold refreshments.",
      ),
      TurfVenue(
        id = "banjul_arch_pitch",
        name = "Arch 22 Arena",
        area = "Banjul",
        city = "Banjul, The Gambia",
        distanceKm = 0.8,
        walkMinutes = 8,
        rating = 4.8,
        reviewCountLabel = "840 reviews",
        pricePerHour = 450,
        isVerified = true,
        isOpen = true,
        primaryFormat = "7-a-side",
        amenities = listOf("7-a-side", "Floodlights", "Cafe", "Parking"),
        timeSlots =
          listOf(
            "10:00–11:00",
            "11:00–12:00",
            "16:00–17:00",
            "18:00–19:00",
            "20:00–21:00",
            "21:00–22:00",
          ),
        latitude = 13.4534,
        longitude = -16.5775,
        category = "Nearby",
        description =
          "Iconic urban turf steps from Arch 22 in Banjul, featuring cushioned 3G grass and night league matches.",
      ),
      TurfVenue(
        id = "senegambia_arena",
        name = "Senegambia 7s Hub",
        area = "Kololi",
        city = "Kololi, The Gambia",
        distanceKm = 3.2,
        walkMinutes = 24,
        rating = 4.6,
        reviewCountLabel = "610 reviews",
        pricePerHour = 500,
        isVerified = true,
        isOpen = true,
        primaryFormat = "7-a-side",
        amenities = listOf("7-a-side", "Floodlights", "Changing Rooms", "Lounge"),
        timeSlots =
          listOf(
            "09:00–10:00",
            "11:00–12:00",
            "15:00–16:00",
            "17:00–18:00",
            "19:00–20:00",
            "21:00–22:00",
          ),
        latitude = 13.4436,
        longitude = -16.7239,
        category = "Nearby",
        description =
          "Coastal resort-district turf near Senegambia Bridge with high-output floodlights and tournament seating.",
      ),
      TurfVenue(
        id = "brikama_box_bar",
        name = "Brikama Box Bar Turf",
        area = "Brikama",
        city = "Brikama, The Gambia",
        distanceKm = 4.4,
        walkMinutes = 35,
        rating = 4.5,
        reviewCountLabel = "520 reviews",
        pricePerHour = 350,
        isVerified = false,
        isOpen = true,
        primaryFormat = "7-a-side",
        amenities = listOf("7-a-side", "Floodlights", "Parking"),
        timeSlots =
          listOf(
            "09:00–10:00",
            "12:00–13:00",
            "16:00–17:00",
            "17:00–18:00",
            "18:00–19:00",
            "19:00–20:00",
          ),
        latitude = 13.2714,
        longitude = -16.6494,
        category = "Academy",
        description =
          "Home ground for West Coast youth academies and weekend 7-a-side knockouts.",
      ),
      TurfVenue(
        id = "banjulinding_pitch",
        name = "Banjulinding Green",
        area = "Banjulinding",
        city = "Banjulinding, The Gambia",
        distanceKm = 2.7,
        walkMinutes = 20,
        rating = 4.7,
        reviewCountLabel = "390 reviews",
        pricePerHour = 380,
        isVerified = true,
        isOpen = true,
        primaryFormat = "5-a-side",
        amenities = listOf("5-a-side", "Floodlights", "Changing Rooms"),
        timeSlots =
          listOf(
            "10:00–11:00",
            "13:00–14:00",
            "17:00–18:00",
            "18:00–19:00",
            "20:00–21:00",
            "21:00–22:00",
          ),
        latitude = 13.3688,
        longitude = -16.6525,
        category = "All Turfs",
        description =
          "Fast-paced caged 5-a-side turf engineered for intense street-style challenges.",
      ),
      TurfVenue(
        id = "bakau_coastal_turf",
        name = "Bakau Coastal Pitch",
        area = "Bakau",
        city = "Bakau, The Gambia",
        distanceKm = 2.1,
        walkMinutes = 17,
        rating = 4.6,
        reviewCountLabel = "480 reviews",
        pricePerHour = 420,
        isVerified = true,
        isOpen = true,
        primaryFormat = "7-a-side",
        amenities = listOf("7-a-side", "Floodlights", "Parking", "Showers"),
        timeSlots =
          listOf(
            "09:00–10:00",
            "11:00–12:00",
            "16:00–17:00",
            "18:00–19:00",
            "19:00–20:00",
            "20:00–21:00",
          ),
        latitude = 13.4706,
        longitude = -16.6778,
        category = "Nearby",
        description =
          "Ocean-breeze 7-a-side pitch near Independence Stadium with clean showers and spectator bleachers.",
      ),
      TurfVenue(
        id = "tabokoto_cage",
        name = "Tabokoto Night Cage",
        area = "Tabokoto",
        city = "Serrekunda, The Gambia",
        distanceKm = 3.8,
        walkMinutes = 29,
        rating = 4.4,
        reviewCountLabel = "310 reviews",
        pricePerHour = 350,
        isVerified = false,
        isOpen = false,
        primaryFormat = "5-a-side",
        amenities = listOf("5-a-side", "Floodlights", "Water"),
        timeSlots =
          listOf(
            "16:00–17:00",
            "17:00–18:00",
            "18:00–19:00",
            "19:00–20:00",
            "20:00–21:00",
            "21:00–22:00",
          ),
        latitude = 13.4148,
        longitude = -16.6815,
        category = "Academy",
        description =
          "Neighborhood floodlit cage favored by Real Tabokoto and local freestyle squads.",
      ),
    )

  val tournaments =
    listOf(
      Tournament(
        id = "gambia_football_cup_2026",
        title = "The Gambia Football Cup 2026",
        category = "Ongoing",
        teamCount = 16,
        prizePool = "D15,000 Prize",
        dateRange = "Dec 1 – Dec 22",
        location = "Banjul",
        entryFee = "D1,500 / team",
        isFeatured = true,
        description =
          "The flagship 7-a-side national turf showcase bringing together 16 elite squads across Greater Banjul and West Coast Region.",
      ),
      Tournament(
        id = "gambia_champions_cup",
        title = "Gambia Champions Cup",
        category = "Ongoing",
        teamCount = 16,
        prizePool = "D10,000 Prize",
        dateRange = "Dec 1 – Dec 14",
        location = "Banjul",
        entryFee = "D1,200 / team",
        isFeatured = false,
        description =
          "High-stakes knockout tournament featuring group stages at Paradise Turf and finals under the floodlights in Banjul.",
      ),
      Tournament(
        id = "brikama_summer_cup",
        title = "Brikama Summer Cup",
        category = "Upcoming",
        teamCount = 8,
        prizePool = "D5,000 Prize",
        dateRange = "Dec 10 – Dec 20",
        location = "Brikama",
        entryFee = "D800 / team",
        isFeatured = false,
        description =
          "Eight-team regional sprint cup at Brikama Box Bar Turf. Fast 30-minute halves with golden boot awards.",
      ),
      Tournament(
        id = "serrekunda_night_7s",
        title = "Serrekunda Night 7s",
        category = "Upcoming",
        teamCount = 12,
        prizePool = "D7,500 Prize",
        dateRange = "Dec 18 – Dec 28",
        location = "Serrekunda",
        entryFee = "D1,000 / team",
        isFeatured = false,
        description =
          "Evening floodlit competition hosted at Paradise Turf with live match tracking and MVP rankings.",
      ),
      Tournament(
        id = "bakau_independence_shield",
        title = "Bakau Coastal Shield",
        category = "Completed",
        teamCount = 16,
        prizePool = "D8,000 Prize",
        dateRange = "Oct 12 – Oct 28",
        location = "Bakau",
        entryFee = "D1,000 / team",
        isFeatured = false,
        description =
          "Brikama Warriors lifted the trophy after a thrilling 2-1 victory over Young Stars FC in the Bakau final.",
      ),
    )

  val teams =
    listOf(
      FootballTeam(
        id = "brikama_warriors",
        name = "Brikama Warriors",
        city = "Brikama",
        memberCount = 16,
        trophyCount = 2,
        matchesPlayed = 24,
        wins = 16,
        draws = 4,
        losses = 4,
        motto = "One Team. One Family.",
        bio = "Brikama Warriors — Fighting for glory across every turf in The Gambia.",
        isVerified = true,
        badgeTintHex = 0xFFF59E0B,
        recentMatches =
          listOf(
            TeamMatchResult("Young Stars FC", "2 - 1", "Win"),
            TeamMatchResult("Real Tabokoto", "3 - 1", "Win"),
            TeamMatchResult("Banjul United", "1 - 1", "Draw"),
          ),
        achievements =
          listOf(
            "Champions — Bakau Coastal Shield 2026",
            "Winners — West Coast 7s Invitational",
            "16-Match Unbeaten Home Streak",
          ),
      ),
      FootballTeam(
        id = "young_stars_fc",
        name = "Young Stars FC",
        city = "Serrekunda",
        memberCount = 14,
        trophyCount = 3,
        matchesPlayed = 28,
        wins = 19,
        draws = 5,
        losses = 4,
        motto = "Pace. Precision. Pride.",
        bio = "Serrekunda's technical 7-a-side powerhouse known for quick one-touch night football.",
        isVerified = true,
        badgeTintHex = 0xFFE61E2A,
        recentMatches =
          listOf(
            TeamMatchResult("Real Tabokoto", "4 - 2", "Win"),
            TeamMatchResult("Brikama Warriors", "1 - 2", "Loss"),
            TeamMatchResult("Dawda FC", "3 - 0", "Win"),
          ),
        achievements =
          listOf(
            "Champions — Serrekunda Night League",
            "Top Scorers — Gambia Champions Cup",
          ),
      ),
      FootballTeam(
        id = "real_tabokoto",
        name = "Real Tabokoto",
        city = "Tabokoto",
        memberCount = 15,
        trophyCount = 1,
        matchesPlayed = 20,
        wins = 11,
        draws = 3,
        losses = 6,
        motto = "Fear None on the Turf.",
        bio = "Street-forged squad bringing fearless pressing to 5s and 7s challenges.",
        isVerified = false,
        badgeTintHex = 0xFF38BDF8,
        recentMatches =
          listOf(
            TeamMatchResult("Dawda FC", "2 - 0", "Win"),
            TeamMatchResult("Young Stars FC", "2 - 4", "Loss"),
          ),
        achievements = listOf("Finalists — Tabokoto Cage Classic"),
      ),
      FootballTeam(
        id = "dawda_fc",
        name = "Dawda FC",
        city = "Banjul",
        memberCount = 12,
        trophyCount = 1,
        matchesPlayed = 18,
        wins = 10,
        draws = 4,
        losses = 4,
        motto = "Built in Banjul.",
        bio = "Banjul's open-challenge regular squad always ready for evening 7-a-side battles.",
        isVerified = true,
        badgeTintHex = 0xFF22C55E,
        recentMatches =
          listOf(
            TeamMatchResult("Bakau Coastal XI", "3 - 2", "Win"),
            TeamMatchResult("Real Tabokoto", "0 - 2", "Loss"),
          ),
        achievements = listOf("Winners — Arch 22 Mini-Cup"),
      ),
    )

  val seedChallenges =
    listOf(
      SeedChallenge(
        id = "seed_dawda_open",
        hostTeamName = "Dawda FC",
        opponentName = "Anyone",
        turfName = "Paradise Turf",
        stakeLabel = "D500",
        format = "7-a-side",
        scheduleLabel = "Today • 6:00 PM",
        playersCount = 8,
        maxPlayers = 14,
      ),
      SeedChallenge(
        id = "seed_young_stars_tabokoto",
        hostTeamName = "Young Stars FC",
        opponentName = "Real Tabokoto",
        turfName = "Senegambia 7s Hub",
        stakeLabel = "D1,000",
        format = "7-a-side",
        scheduleLabel = "Today • 7:00 PM",
        playersCount = 12,
        maxPlayers = 14,
      ),
      SeedChallenge(
        id = "seed_banjul_five",
        hostTeamName = "Banjul United",
        opponentName = "Anyone",
        turfName = "Arch 22 Arena",
        stakeLabel = "D400",
        format = "5-a-side",
        scheduleLabel = "Tomorrow • 5:30 PM",
        playersCount = 6,
        maxPlayers = 10,
      ),
    )

  val notifications =
    listOf(
      HubNotification(
        id = "n1",
        title = "Paradise Turf floodlight slot open",
        subtitle = "11:00–12:00 and 19:00–20:00 are available for Saturday.",
        timeAgo = "12m ago",
        isUnread = true,
      ),
      HubNotification(
        id = "n2",
        title = "Gambia Champions Cup group draw live",
        subtitle = "Brikama Warriors face Young Stars FC in Group A opener.",
        timeAgo = "2h ago",
        isUnread = true,
      ),
      HubNotification(
        id = "n3",
        title = "Dawda FC posted an open D500 challenge",
        subtitle = "7-a-side kickoff today at 6:00 PM • 8 players confirmed.",
        timeAgo = "5h ago",
        isUnread = true,
      ),
    )
}
