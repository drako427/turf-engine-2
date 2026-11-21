package com.example.data.remote

import android.util.Log
import com.example.data.catalog.GambianFootballCatalog
import com.example.data.model.OsmPitch
import com.example.data.model.OsmPlaceSuggestion
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

private interface OverpassApi {
  @FormUrlEncoded
  @POST("api/interpreter")
  suspend fun queryPitches(@Field("data") query: String): OverpassResponse
}

private interface NominatimApi {
  @GET("search")
  suspend fun searchPlaces(
    @Query("q") query: String,
    @Query("countrycodes") countryCodes: String = "gm",
    @Query("format") format: String = "jsonv2",
    @Query("addressdetails") addressDetails: Int = 1,
    @Query("limit") limit: Int = 6,
  ): List<NominatimPlaceDto>
}

data class OverpassResponse(
  val elements: List<OverpassElementDto> = emptyList(),
)

data class OverpassElementDto(
  val type: String = "way",
  val id: Long = 0L,
  val lat: Double? = null,
  val lon: Double? = null,
  val center: OverpassCenterDto? = null,
  val tags: Map<String, String>? = null,
)

data class OverpassCenterDto(
  val lat: Double = 0.0,
  val lon: Double = 0.0,
)

data class NominatimPlaceDto(
  @Json(name = "place_id") val placeId: Long = 0L,
  val lat: String = "0.0",
  val lon: String = "0.0",
  @Json(name = "display_name") val displayName: String = "",
  val name: String? = null,
  val type: String? = null,
  val address: Map<String, String>? = null,
)

object OpenStreetMapService {
  private const val TAG = "OpenStreetMapService"
  private const val USER_AGENT = "FootballHubGambia/1.0 (Android; OSM Integration)"

  private val moshi: Moshi by lazy {
    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(18, TimeUnit.SECONDS)
      .addInterceptor { chain ->
        val req =
          chain.request()
            .newBuilder()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        chain.proceed(req)
      }
      .build()
  }

  private val overpassApi: OverpassApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://overpass-api.de/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OverpassApi::class.java)
  }

  private val nominatimApi: NominatimApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://nominatim.openstreetmap.org/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(NominatimApi::class.java)
  }

  // Simple in-memory cache keyed by rounded bounding box so panning nearby doesn't spam Overpass
  private val cachedPitches = linkedMapOf<String, List<OsmPitch>>()

  suspend fun fetchFootballPitches(
    south: Double,
    west: Double,
    north: Double,
    east: Double,
  ): Result<List<OsmPitch>> =
    withContext(Dispatchers.IO) {
      // Clamp bbox to reasonable Gambian viewport to keep queries fast & polite
      val s = south.coerceIn(13.05, 13.75)
      val n = north.coerceIn(13.10, 13.85).coerceAtLeast(s + 0.02)
      val w = west.coerceIn(-16.95, -13.70)
      val e = east.coerceIn(-16.90, -13.65).coerceAtLeast(w + 0.02)

      val cacheKey =
        "${((s * 20).toInt())}_${((w * 20).toInt())}_${((n * 20).toInt())}_${((e * 20).toInt())}"
      cachedPitches[cacheKey]?.let { return@withContext Result.success(it) }

      val overpassQuery =
        """
        [out:json][timeout:15];
        (
          node["leisure"="pitch"]["sport"~"soccer|football"]($s,$w,$n,$e);
          way["leisure"="pitch"]["sport"~"soccer|football"]($s,$w,$n,$e);
          node["leisure"="stadium"]["sport"~"soccer|football"]($s,$w,$n,$e);
          way["leisure"="stadium"]["sport"~"soccer|football"]($s,$w,$n,$e);
          way["leisure"="pitch"][!"sport"]($s,$w,$n,$e);
        );
        out center tags 80;
        """.trimIndent()

      runCatching {
        val response = overpassApi.queryPitches(overpassQuery)
        val parsed =
          response.elements
            .mapNotNull { elem ->
              val lat = elem.lat ?: elem.center?.lat ?: return@mapNotNull null
              val lon = elem.lon ?: elem.center?.lon ?: return@mapNotNull null
              val tags = elem.tags.orEmpty()
              val isExplicitOtherSport =
                tags["sport"]?.let { sTag ->
                  sTag.isNotBlank() &&
                    !sTag.contains("soccer", ignoreCase = true) &&
                    !sTag.contains("football", ignoreCase = true)
                } == true
              if (isExplicitOtherSport) return@mapNotNull null

              // Skip OSM pitches that overlap a curated Football Hub turf (< 90m)
              val nearCurated =
                GambianFootballCatalog.turfs.any { turf ->
                  GambianFootballCatalog.haversineDistanceKm(lat, lon, turf.latitude, turf.longitude) <
                    0.09
                }
              if (nearCurated) return@mapNotNull null

              val areaName =
                tags["addr:city"]
                  ?: tags["addr:suburb"]
                  ?: tags["addr:town"]
                  ?: inferGambianArea(lat, lon)
              val rawName = tags["name"]?.takeIf { it.isNotBlank() }
              val isStadium = tags["leisure"] == "stadium"
              val pitchTitle =
                rawName ?: if (isStadium) "$areaName Stadium Pitch" else "$areaName Community Pitch"

              OsmPitch(
                id = "osm_${elem.type}_${elem.id}",
                osmId = elem.id,
                osmType = elem.type,
                name = pitchTitle,
                area = areaName,
                latitude = lat,
                longitude = lon,
                surface = formatSurface(tags["surface"]),
                isFloodlit = tags["lit"].equals("yes", ignoreCase = true),
                accessLabel = formatAccess(tags["access"]),
              )
            }
            .distinctBy { "${(it.latitude * 1000).toInt()}_${(it.longitude * 1000).toInt()}" }

        if (cachedPitches.size > 16) {
          cachedPitches.remove(cachedPitches.keys.first())
        }
        cachedPitches[cacheKey] = parsed
        parsed
      }.onFailure { err ->
        Log.w(TAG, "Overpass pitch query failed", err)
      }
    }

  suspend fun searchGambianPlaces(query: String): Result<List<OsmPlaceSuggestion>> =
    withContext(Dispatchers.IO) {
      val trimmed = query.trim()
      if (trimmed.length < 2) return@withContext Result.success(emptyList())

      runCatching {
        val results = nominatimApi.searchPlaces(query = trimmed)
        results.mapNotNull { dto ->
          val lat = dto.lat.toDoubleOrNull() ?: return@mapNotNull null
          val lon = dto.lon.toDoubleOrNull() ?: return@mapNotNull null
          val addr = dto.address.orEmpty()
          val primary =
            dto.name?.takeIf { it.isNotBlank() }
              ?: addr["town"]
              ?: addr["city"]
              ?: addr["suburb"]
              ?: addr["village"]
              ?: dto.displayName.substringBefore(",").trim()
          val region =
            addr["state"] ?: addr["county"] ?: addr["region"] ?: "The Gambia"

          OsmPlaceSuggestion(
            id = "nom_${dto.placeId}",
            shortName = primary,
            displayName = dto.displayName,
            areaType = dto.type?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Place",
            latitude = lat,
            longitude = lon,
          ).copy(
            displayName = if (region.isNotBlank()) "$primary, $region" else dto.displayName
          )
        }.distinctBy { it.shortName.lowercase() }
      }.onFailure { err ->
        Log.w(TAG, "Nominatim search failed", err)
      }
    }

  private fun inferGambianArea(lat: Double, lon: Double): String {
    val closest =
      GambianFootballCatalog.cityCoordinates.minByOrNull { (_, point) ->
        GambianFootballCatalog.haversineDistanceKm(lat, lon, point.latitude, point.longitude)
      }
    return closest?.key?.replaceFirstChar { it.uppercase() } ?: "Greater Banjul"
  }

  private fun formatSurface(raw: String?): String =
    when (raw?.lowercase()) {
      "artificial_turf", "turf", "synthetic" -> "Artificial Turf"
      "grass" -> "Natural Grass"
      "sand" -> "Sand Pitch"
      "dirt", "ground", "earth" -> "Hard Earth"
      "asphalt", "concrete" -> "Street Court"
      else -> "Football Pitch"
    }

  private fun formatAccess(raw: String?): String =
    when (raw?.lowercase()) {
      "yes", "public" -> "Public Pitch"
      "customers" -> "Paid / Booking"
      "private" -> "Club / School"
      else -> "OSM Mapped"
    }
}
