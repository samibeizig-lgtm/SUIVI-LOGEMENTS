package com.nexstay.myproperties.ui.map

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexstay.myproperties.data.GeoBounds
import com.nexstay.myproperties.data.TunisiaGeo
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt

/** Marqueur affiché sur la carte. */
data class MapMarker(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val label: String
)

/** Centrage initial de la carte sur un point donné. */
data class MapFocus(
    val latitude: Double,
    val longitude: Double,
    val zoom: Float = 5f
)

private class Projection(bounds: GeoBounds, width: Float, height: Float, private val padding: Float) {
    private val minLon = bounds.minLon
    private val maxLat = bounds.maxLat
    private val cosMid =
        cos(Math.toRadians(((bounds.minLat + bounds.maxLat) / 2f).toDouble())).toFloat()
    val pixelsPerDegreeLat: Float
    val worldWidth: Float
    val worldHeight: Float

    init {
        val lonSpan = (bounds.maxLon - bounds.minLon) * cosMid
        val latSpan = bounds.maxLat - bounds.minLat
        pixelsPerDegreeLat = min(
            (width - 2 * padding) / lonSpan,
            (height - 2 * padding) / latSpan
        )
        worldWidth = lonSpan * pixelsPerDegreeLat + 2 * padding
        worldHeight = latSpan * pixelsPerDegreeLat + 2 * padding
    }

    fun world(latitude: Double, longitude: Double): Offset = Offset(
        padding + (longitude - minLon).toFloat() * cosMid * pixelsPerDegreeLat,
        padding + (maxLat - latitude).toFloat() * pixelsPerDegreeLat
    )

    fun geo(world: Offset): Pair<Double, Double> {
        val longitude = minLon + (world.x - padding) / (cosMid * pixelsPerDegreeLat)
        val latitude = maxLat - (world.y - padding) / pixelsPerDegreeLat
        return latitude.toDouble() to longitude.toDouble()
    }
}

/**
 * Carte stylisée de la Tunisie avec les gouvernorats, dessinée localement
 * (aucune connexion nécessaire). Supporte le zoom/déplacement, des marqueurs
 * 🏠 avec bulle du nom au toucher, et un mode placement (toucher = position).
 */
@Composable
fun TunisiaMap(
    markers: List<MapMarker>,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    focus: MapFocus? = null,
    placementPosition: Pair<Double, Double>? = null,
    onPlace: ((Double, Double) -> Unit)? = null,
    onMarkerOpen: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val governorates = remember { TunisiaGeo.load(context) }
    val bounds = remember { TunisiaGeo.bounds(context) }

    val landColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
    val outlineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val seaColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val paddingPx = with(density) { 18.dp.toPx() }
        val projection = remember(widthPx, heightPx) {
            Projection(bounds, widthPx, heightPx, paddingPx)
        }

        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var selectedId by remember { mutableStateOf<Long?>(null) }

        fun clampOffset(candidate: Offset, atScale: Float): Offset {
            val slackX = widthPx * 0.5f
            val slackY = heightPx * 0.5f
            val loX = min(widthPx - projection.worldWidth * atScale - slackX, slackX)
            val hiX = maxOf(widthPx - projection.worldWidth * atScale - slackX, slackX)
            val loY = min(heightPx - projection.worldHeight * atScale - slackY, slackY)
            val hiY = maxOf(heightPx - projection.worldHeight * atScale - slackY, slackY)
            return Offset(candidate.x.coerceIn(loX, hiX), candidate.y.coerceIn(loY, hiY))
        }

        fun screenOf(latitude: Double, longitude: Double): Offset =
            projection.world(latitude, longitude) * scale + offset

        LaunchedEffect(projection) {
            if (focus != null) {
                scale = focus.zoom
                val world = projection.world(focus.latitude, focus.longitude)
                offset = Offset(
                    widthPx / 2f - world.x * focus.zoom,
                    heightPx / 2f - world.y * focus.zoom
                )
            } else {
                scale = 1f
                offset = Offset(
                    (widthPx - projection.worldWidth) / 2f,
                    (heightPx - projection.worldHeight) / 2f
                )
            }
        }

        val paths = remember(projection) {
            governorates.map { gov ->
                Path().apply {
                    gov.rings.forEach { ring ->
                        for (i in 0 until ring.size / 2) {
                            val point = projection.world(
                                ring[i * 2 + 1].toDouble(),
                                ring[i * 2].toDouble()
                            )
                            if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                        }
                        close()
                    }
                }
            }
        }

        val labelPaint = remember(labelColor) {
            android.graphics.Paint().apply {
                isAntiAlias = true
                color = labelColor.toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = with(density) { 10.sp.toPx() }
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                letterSpacing = 0.1f
            }
        }
        val emojiPaint = remember {
            android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = with(density) { 26.sp.toPx() }
            }
        }
        val labelMinHeightPx = with(density) { 60.dp.toPx() }

        var mapModifier: Modifier = Modifier.fillMaxSize()
        if (interactive) {
            mapModifier = mapModifier
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 16f)
                        val zoomed = centroid - (centroid - offset) * (newScale / scale)
                        offset = clampOffset(zoomed + pan, newScale)
                        scale = newScale
                    }
                }
                .pointerInput(markers, onPlace) {
                    detectTapGestures { tap ->
                        val hitRadius = 26.dp.toPx()
                        val hit = markers
                            .minByOrNull { (screenOf(it.latitude, it.longitude) - tap).getDistance() }
                            ?.takeIf {
                                (screenOf(it.latitude, it.longitude) - tap).getDistance() < hitRadius
                            }
                        when {
                            hit != null -> selectedId = if (selectedId == hit.id) null else hit.id
                            onPlace != null -> {
                                val (latitude, longitude) = projection.geo((tap - offset) / scale)
                                selectedId = null
                                onPlace(latitude, longitude)
                            }
                            else -> selectedId = null
                        }
                    }
                }
        }

        Canvas(modifier = mapModifier) {
            drawRect(seaColor)
            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                val strokeWidth = 1.2.dp.toPx() / scale
                paths.forEach { path ->
                    drawPath(path, landColor)
                    drawPath(path, borderColor, style = Stroke(width = strokeWidth))
                }
                paths.forEach { path ->
                    drawPath(
                        path,
                        outlineColor,
                        style = Stroke(width = strokeWidth * 0.6f)
                    )
                }
            }

            drawIntoCanvas { canvas ->
                governorates.forEach { gov ->
                    val heightOnScreen =
                        gov.latSpan * projection.pixelsPerDegreeLat * scale
                    if (heightOnScreen > labelMinHeightPx) {
                        val position = screenOf(
                            gov.centroidLat.toDouble(),
                            gov.centroidLon.toDouble()
                        )
                        canvas.nativeCanvas.drawText(
                            gov.name.uppercase(),
                            position.x,
                            position.y,
                            labelPaint
                        )
                    }
                }
                markers.forEach { marker ->
                    val position = screenOf(marker.latitude, marker.longitude)
                    canvas.nativeCanvas.drawText("🏠", position.x, position.y, emojiPaint)
                }
                placementPosition?.let { (latitude, longitude) ->
                    val position = screenOf(latitude, longitude)
                    canvas.nativeCanvas.drawText("📍", position.x, position.y, emojiPaint)
                }
            }
        }

        val selected = markers.firstOrNull { it.id == selectedId }
        if (selected != null) {
            val position = screenOf(selected.latitude, selected.longitude)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (position.x - with(density) { 120.dp.toPx() }).roundToInt(),
                            (position.y - with(density) { 92.dp.toPx() }).roundToInt()
                        )
                    }
                    .width(240.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = { onMarkerOpen?.invoke(selected.id) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = selected.label.ifBlank { "Sans nom" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
