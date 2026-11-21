package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.OsmPitch
import com.example.data.model.TurfVenue
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MapCameraTarget
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.backgroundColor
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textOptional
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val OPENFREEMAP_DARK_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"

private const val FALLBACK_OBSIDIAN_STYLE_JSON =
  """{
    "version": 8,
    "name": "FootballHubObsidianFallback",
    "sources": {},
    "layers": [
      {
        "id": "background",
        "type": "background",
        "paint": { "background-color": "#0A0A0E" }
      }
    ]
  }"""

private const val SOURCE_TURFS = "fh-turfs-source"
private const val LAYER_TURFS = "fh-turfs-layer"
private const val SOURCE_OSM_PITCHES = "fh-osm-pitches-source"
private const val LAYER_OSM_PITCHES = "fh-osm-pitches-layer"
private const val SOURCE_PLAYER_ANCHOR = "fh-player-anchor-source"
private const val LAYER_PLAYER_HALO = "fh-player-halo-layer"
private const val LAYER_PLAYER_DOT = "fh-player-dot-layer"

private const val IMG_TURF_SELECTED = "fh_pin_turf_selected"
private const val IMG_TURF_NORMAL = "fh_pin_turf_normal"
private const val IMG_OSM_SELECTED = "fh_pin_osm_selected"
private const val IMG_OSM_NORMAL = "fh_pin_osm_normal"

class OsmMapController {
  internal var map: MapLibreMap? = null

  fun zoomIn() {
    val m = map ?: return
    val targetZoom = (m.cameraPosition.zoom + 1.1).coerceAtMost(18.0)
    m.animateCamera(CameraUpdateFactory.zoomTo(targetZoom), 280)
  }

  fun zoomOut() {
    val m = map ?: return
    val targetZoom = (m.cameraPosition.zoom - 1.1).coerceAtLeast(8.0)
    m.animateCamera(CameraUpdateFactory.zoomTo(targetZoom), 280)
  }
}

@Composable
fun rememberOsmMapController(): OsmMapController = remember { OsmMapController() }

@Composable
fun FootballOsmMap(
  turfs: List<TurfVenue>,
  osmPitches: List<OsmPitch>,
  selectedTurfId: String,
  selectedOsmPitchId: String?,
  playerAnchorLat: Double,
  playerAnchorLon: Double,
  cameraTarget: MapCameraTarget?,
  controller: OsmMapController,
  onSelectTurf: (String) -> Unit,
  onSelectOsmPitch: (String) -> Unit,
  onViewportBoundsChanged: (south: Double, west: Double, north: Double, east: Double) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val currentOnSelectTurf by rememberUpdatedState(onSelectTurf)
  val currentOnSelectOsmPitch by rememberUpdatedState(onSelectOsmPitch)
  val currentOnBoundsChanged by rememberUpdatedState(onViewportBoundsChanged)

  val mapInitialized = remember(context) {
    runCatching {
      MapLibre.getInstance(context.applicationContext)
      true
    }.getOrDefault(false)
  }

  if (!mapInitialized) {
    Box(
      modifier = modifier.fillMaxSize().background(ObsidianBlack),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "OpenStreetMap initializing...",
        color = TextSecondary,
      )
    }
    return
  }

  var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
  var isStyleReady by remember { mutableStateOf(false) }

  val mapView = remember(context) {
    MapView(context).apply {
      onCreate(null)
    }
  }

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
      controller.map = null
      mapView.onDestroy()
    }
  }

  // Sync camera target transitions
  LaunchedEffect(cameraTarget?.nonce, mapLibreMap) {
    val m = mapLibreMap ?: return@LaunchedEffect
    val target = cameraTarget ?: return@LaunchedEffect
    m.animateCamera(
      CameraUpdateFactory.newCameraPosition(
        CameraPosition.Builder()
          .target(LatLng(target.latitude, target.longitude))
          .zoom(target.zoom)
          .build()
      ),
      650,
    )
  }

  // Sync GeoJSON sources whenever pins or anchor change
  LaunchedEffect(
    isStyleReady,
    turfs,
    osmPitches,
    selectedTurfId,
    selectedOsmPitchId,
    playerAnchorLat,
    playerAnchorLon,
  ) {
    if (!isStyleReady) return@LaunchedEffect
    val style = mapLibreMap?.style ?: return@LaunchedEffect
    updateGeoJsonLayers(
      style = style,
      turfs = turfs,
      osmPitches = osmPitches,
      selectedTurfId = selectedTurfId,
      selectedOsmPitchId = selectedOsmPitchId,
      playerAnchorLat = playerAnchorLat,
      playerAnchorLon = playerAnchorLon,
    )
  }

  AndroidView(
    modifier = modifier.fillMaxSize(),
    factory = {
      mapView.apply {
        addOnDidFailLoadingMapListener {
          getMapAsync { m ->
            if (m.style == null) {
              m.setStyle(Style.Builder().fromJson(FALLBACK_OBSIDIAN_STYLE_JSON)) { style ->
                installCustomLayers(context, style)
                isStyleReady = true
              }
            }
          }
        }
        getMapAsync { m ->
          mapLibreMap = m
          controller.map = m

          m.uiSettings.apply {
            isCompassEnabled = false
            isLogoEnabled = false
            isAttributionEnabled = false
            isRotateGesturesEnabled = true
            isTiltGesturesEnabled = false
          }

          m.cameraPosition =
            CameraPosition.Builder()
              .target(LatLng(playerAnchorLat, playerAnchorLon))
              .zoom(12.2)
              .build()

          m.setStyle(Style.Builder().fromUri(OPENFREEMAP_DARK_STYLE_URL)) { style ->
            applyObsidianCrimsonTheme(style)
            installCustomLayers(context, style)
            updateGeoJsonLayers(
              style = style,
              turfs = turfs,
              osmPitches = osmPitches,
              selectedTurfId = selectedTurfId,
              selectedOsmPitchId = selectedOsmPitchId,
              playerAnchorLat = playerAnchorLat,
              playerAnchorLon = playerAnchorLon,
            )
            isStyleReady = true
          }

          m.addOnMapClickListener { latLng ->
            val screenPoint = m.projection.toScreenLocation(latLng)
            val turfHit = m.queryRenderedFeatures(screenPoint, LAYER_TURFS).firstOrNull()
            if (turfHit != null) {
              val id = turfHit.getStringProperty("id")
              if (!id.isNullOrBlank()) {
                currentOnSelectTurf(id)
                return@addOnMapClickListener true
              }
            }
            val osmHit = m.queryRenderedFeatures(screenPoint, LAYER_OSM_PITCHES).firstOrNull()
            if (osmHit != null) {
              val id = osmHit.getStringProperty("id")
              if (!id.isNullOrBlank()) {
                currentOnSelectOsmPitch(id)
                return@addOnMapClickListener true
              }
            }
            false
          }

          m.addOnCameraIdleListener {
            val bounds = m.projection.visibleRegion.latLngBounds
            currentOnBoundsChanged(
              bounds.latitudeSouth,
              bounds.longitudeWest,
              bounds.latitudeNorth,
              bounds.longitudeEast,
            )
          }
        }
      }
    },
  )
}

private fun applyObsidianCrimsonTheme(style: Style) {
  style.layers.forEach { layer ->
    val id = layer.id.lowercase()
    when (layer) {
      is BackgroundLayer -> {
        layer.setProperties(backgroundColor(AndroidColor.parseColor("#0A0A0E")))
      }
      is FillLayer -> {
        when {
          id.contains("water") ->
            layer.setProperties(fillColor(AndroidColor.parseColor("#070B14")))
          id.contains("building") ->
            layer.setProperties(fillColor(AndroidColor.parseColor("#121520")))
          id.contains("park") || id.contains("pitch") || id.contains("grass") || id.contains("wood") ->
            layer.setProperties(fillColor(AndroidColor.parseColor("#0D1813")))
          id.contains("land") ->
            layer.setProperties(fillColor(AndroidColor.parseColor("#0B0D14")))
        }
      }
      is LineLayer -> {
        when {
          id.contains("waterway") ->
            layer.setProperties(lineColor(AndroidColor.parseColor("#0D192B")))
          id.contains("motorway") || id.contains("trunk") || id.contains("primary") ||
            id.contains("highway_major") ->
            layer.setProperties(lineColor(AndroidColor.parseColor("#9B141D")))
          id.contains("secondary") || id.contains("tertiary") || id.contains("road") ||
            id.contains("street") || id.contains("highway") ->
            layer.setProperties(lineColor(AndroidColor.parseColor("#1D2230")))
          id.contains("boundary") ->
            layer.setProperties(lineColor(AndroidColor.parseColor("#3D1418")))
        }
      }
    }
  }
}

private fun installCustomLayers(context: Context, style: Style) {
  val density = context.resources.displayMetrics.density
  if (style.getImage(IMG_TURF_SELECTED) == null) {
    style.addImage(IMG_TURF_SELECTED, createPinBitmap(density, isTurf = true, isSelected = true))
    style.addImage(IMG_TURF_NORMAL, createPinBitmap(density, isTurf = true, isSelected = false))
    style.addImage(IMG_OSM_SELECTED, createPinBitmap(density, isTurf = false, isSelected = true))
    style.addImage(IMG_OSM_NORMAL, createPinBitmap(density, isTurf = false, isSelected = false))
  }

  if (style.getSource(SOURCE_PLAYER_ANCHOR) == null) {
    style.addSource(GeoJsonSource(SOURCE_PLAYER_ANCHOR))
    style.addLayer(
      CircleLayer(LAYER_PLAYER_HALO, SOURCE_PLAYER_ANCHOR).withProperties(
        circleRadius(18f),
        circleColor(AndroidColor.parseColor("#E61E2A")),
        circleOpacity(0.22f),
      )
    )
    style.addLayer(
      CircleLayer(LAYER_PLAYER_DOT, SOURCE_PLAYER_ANCHOR).withProperties(
        circleRadius(6.5f),
        circleColor(AndroidColor.parseColor("#38BDF8")),
        circleStrokeWidth(2f),
        circleStrokeColor(AndroidColor.WHITE),
      )
    )
  }

  if (style.getSource(SOURCE_OSM_PITCHES) == null) {
    style.addSource(GeoJsonSource(SOURCE_OSM_PITCHES))
    style.addLayer(
      SymbolLayer(LAYER_OSM_PITCHES, SOURCE_OSM_PITCHES).withProperties(
        iconImage(get("icon")),
        iconAllowOverlap(true),
        iconIgnorePlacement(true),
        textField(get("label")),
        textSize(10f),
        textColor(AndroidColor.parseColor("#A7F3D0")),
        textOffset(arrayOf(0f, 1.9f)),
        textOptional(true),
        textAllowOverlap(false),
      )
    )
  }

  if (style.getSource(SOURCE_TURFS) == null) {
    style.addSource(GeoJsonSource(SOURCE_TURFS))
    style.addLayer(
      SymbolLayer(LAYER_TURFS, SOURCE_TURFS).withProperties(
        iconImage(get("icon")),
        iconAllowOverlap(true),
        iconIgnorePlacement(true),
        textField(get("label")),
        textSize(11.5f),
        textColor(AndroidColor.WHITE),
        textOffset(arrayOf(0f, 2.2f)),
        textOptional(true),
        textAllowOverlap(false),
      )
    )
  }
}

private fun updateGeoJsonLayers(
  style: Style,
  turfs: List<TurfVenue>,
  osmPitches: List<OsmPitch>,
  selectedTurfId: String,
  selectedOsmPitchId: String?,
  playerAnchorLat: Double,
  playerAnchorLon: Double,
) {
  val turfFeatures =
    turfs.map { turf ->
      val isSelected = selectedOsmPitchId == null && turf.id == selectedTurfId
      Feature.fromGeometry(Point.fromLngLat(turf.longitude, turf.latitude)).apply {
        addStringProperty("id", turf.id)
        addStringProperty("label", if (isSelected) turf.name else "")
        addStringProperty("icon", if (isSelected) IMG_TURF_SELECTED else IMG_TURF_NORMAL)
      }
    }
  style.getSourceAs<GeoJsonSource>(SOURCE_TURFS)?.setGeoJson(
    FeatureCollection.fromFeatures(turfFeatures)
  )

  val osmFeatures =
    osmPitches.map { pitch ->
      val isSelected = pitch.id == selectedOsmPitchId
      Feature.fromGeometry(Point.fromLngLat(pitch.longitude, pitch.latitude)).apply {
        addStringProperty("id", pitch.id)
        addStringProperty("label", if (isSelected) pitch.name else "")
        addStringProperty("icon", if (isSelected) IMG_OSM_SELECTED else IMG_OSM_NORMAL)
      }
    }
  style.getSourceAs<GeoJsonSource>(SOURCE_OSM_PITCHES)?.setGeoJson(
    FeatureCollection.fromFeatures(osmFeatures)
  )

  val anchorFeature =
    Feature.fromGeometry(Point.fromLngLat(playerAnchorLon, playerAnchorLat))
  style.getSourceAs<GeoJsonSource>(SOURCE_PLAYER_ANCHOR)?.setGeoJson(
    FeatureCollection.fromFeatures(listOf(anchorFeature))
  )
}

private fun createPinBitmap(
  density: Float,
  isTurf: Boolean,
  isSelected: Boolean,
): Bitmap {
  val sizeDp =
    when {
      isTurf && isSelected -> 54f
      isTurf -> 36f
      isSelected -> 44f
      else -> 28f
    }
  val sizePx = (sizeDp * density).toInt().coerceAtLeast(32)
  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val cx = sizePx / 2f
  val cy = sizePx / 2f

  val accentColor =
    if (isTurf) AndroidColor.parseColor("#E61E2A")
    else AndroidColor.parseColor("#22C55E")
  val fillBg =
    when {
      isTurf && isSelected -> AndroidColor.parseColor("#E61E2A")
      isTurf -> AndroidColor.parseColor("#1C1F2A")
      isSelected -> AndroidColor.parseColor("#16A34A")
      else -> AndroidColor.parseColor("#121B17")
    }

  if (isSelected) {
    val haloPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          RadialGradient(
            cx,
            cy,
            cx,
            intArrayOf(
              AndroidColor.argb(145, AndroidColor.red(accentColor), AndroidColor.green(accentColor), AndroidColor.blue(accentColor)),
              AndroidColor.TRANSPARENT,
            ),
            null,
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawCircle(cx, cy, cx, haloPaint)
  }

  val bodyRadius = if (isSelected) cx * 0.68f else cx * 0.82f
  val bodyPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = fillBg
    }
  canvas.drawCircle(cx, cy, bodyRadius, bodyPaint)

  val borderPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = (if (isSelected) 2.5f else 1.8f) * density
      color = if (isSelected) AndroidColor.WHITE else accentColor
    }
  canvas.drawCircle(cx, cy, bodyRadius, borderPaint)

  // Inner football / pitch emblem
  val innerPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = if (isTurf) Paint.Style.STROKE else Paint.Style.FILL
      strokeWidth = 1.6f * density
      color = if (isTurf) AndroidColor.WHITE else AndroidColor.parseColor("#DCFCE7")
    }
  if (isTurf) {
    val r = bodyRadius * 0.44f
    canvas.drawCircle(cx, cy, r, innerPaint)
    innerPaint.style = Paint.Style.FILL
    canvas.drawCircle(cx, cy, r * 0.36f, innerPaint)
  } else {
    canvas.drawCircle(cx, cy, bodyRadius * 0.34f, innerPaint)
  }

  return bitmap
}
