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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
import com.nexstay.myproperties.data.governorateIndexOf
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt

/** Corail Nexstay, utilisé par la carte vitrine et l'écran carte. */
val NexstayCoral = Color(0xFFE8734A)

private val ShowcaseBackground = Color(0xFF121212)
private val ShowcaseLand = Color(0xFF2E2E2E)
private val ShowcaseBorder = Color(0xFF5C5C5C)
private val ShowcaseText = Color(0xFFF5F5F5)

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
 * Carte de Tunisie dessinée localement (aucune connexion).
 *
 * Deux styles :
 * - normal : tous les gouvernorats aux couleurs du thème, marqueurs 🏠 —
 *   utilisé par la mini-carte de la fiche et le sélecteur de position ;
 * - vitrine ([showcase]) : fond sombre façon affiche Nexstay, seuls les
 *   gouvernorats contenant des logements sont dessinés, cadrage automatique
 *   sur la zone couverte, épingles corail avec le nom affiché à côté.
 */
@Composable
fun TunisiaMap(
    markers: List<MapMarker>,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    focus: MapFocus? = null,
    placementPosition: Pair<Double, Double>? = null,
    onPlace: ((Double, Double) -> Unit)? = null,
    onMarkerOpen: ((Long) -> Unit)? = null,
    showcase: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val governorates = remember { TunisiaGeo.load(context) }
    val bounds = remember { TunisiaGeo.bounds(context) }
    val communes = remember { TunisiaGeo.loadCommunes(context) }
    // Gouvernorat d'appartenance de chaque commune (via son centroïde).
    val communeGovernorate = remember {
        communes.map {
            governorates.governorateIndexOf(it.centroidLat.toDouble(), it.centroidLon.toDouble())
        }
    }

    val occupiedIndices = remember(markers) {
        markers.map { governorates.governorateIndexOf(it.latitude, it.longitude) }
            .filter { it >= 0 }
            .toSet()
    }
    val visibleIndices =
        if (showcase && occupiedIndices.isNotEmpty()) occupiedIndices
        else governorates.indices.toSet()

    val landColor = if (showcase) ShowcaseLand else MaterialTheme.colorScheme.surface
    val borderColor =
        if (showcase) ShowcaseBorder else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
    val seaColor =
        if (showcase) ShowcaseBackground
        else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val communeBorderColor =
        if (showcase) Color(0xFF484848)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val communeLabelColor =
        if (showcase) Color(0xFF9E9E9E) else MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleColor = if (showcase) NexstayCoral else MaterialTheme.colorScheme.primary
    val bubbleTextColor = if (showcase) ShowcaseText else MaterialTheme.colorScheme.onPrimary

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

        LaunchedEffect(projection, focus, showcase, occupiedIndices) {
            when {
                focus != null -> {
                    scale = focus.zoom
                    val world = projection.world(focus.latitude, focus.longitude)
                    offset = Offset(
                        widthPx / 2f - world.x * focus.zoom,
                        heightPx / 2f - world.y * focus.zoom
                    )
                }
                showcase && occupiedIndices.isNotEmpty() -> {
                    // Cadrage automatique sur les gouvernorats occupés.
                    var minLat = Float.MAX_VALUE
                    var maxLat = -Float.MAX_VALUE
                    var minLon = Float.MAX_VALUE
                    var maxLon = -Float.MAX_VALUE
                    occupiedIndices.forEach { index ->
                        governorates[index].rings.forEach { ring ->
                            for (i in 0 until ring.size / 2) {
                                val lon = ring[i * 2]
                                val lat = ring[i * 2 + 1]
                                if (lon < minLon) minLon = lon
                                if (lon > maxLon) maxLon = lon
                                if (lat < minLat) minLat = lat
                                if (lat > maxLat) maxLat = lat
                            }
                        }
                    }
                    val topLeft = projection.world(maxLat.toDouble(), minLon.toDouble())
                    val bottomRight = projection.world(minLat.toDouble(), maxLon.toDouble())
                    val boxWidth = maxOf(bottomRight.x - topLeft.x, 1f)
                    val boxHeight = maxOf(bottomRight.y - topLeft.y, 1f)
                    val fitScale = min((widthPx * 0.7f) / boxWidth, (heightPx * 0.7f) / boxHeight)
                        .coerceIn(1f, 12f)
                    scale = fitScale
                    val centerX = (topLeft.x + bottomRight.x) / 2f
                    val centerY = (topLeft.y + bottomRight.y) / 2f
                    offset = Offset(
                        widthPx / 2f - centerX * fitScale,
                        heightPx / 2f - centerY * fitScale
                    )
                }
                else -> {
                    scale = 1f
                    offset = Offset(
                        (widthPx - projection.worldWidth) / 2f,
                        (heightPx - projection.worldHeight) / 2f
                    )
                }
            }
        }

        fun buildPaths(regions: List<com.nexstay.myproperties.data.Governorate>): List<Path> =
            regions.map { region ->
                Path().apply {
                    region.rings.forEach { ring ->
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

        val paths = remember(projection) { buildPaths(governorates) }
        val communePaths = remember(projection) { buildPaths(communes) }

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
        val pinLabelPaint = remember {
            android.graphics.Paint().apply {
                isAntiAlias = true
                color = ShowcaseText.toArgb()
                textSize = with(density) { 12.sp.toPx() }
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
        val communeLabelPaint = remember(communeLabelColor) {
            android.graphics.Paint().apply {
                isAntiAlias = true
                color = communeLabelColor.toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = with(density) { 9.sp.toPx() }
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
        }
        val labelMinHeightPx = with(density) { 60.dp.toPx() }
        val communeLabelMinHeightPx = with(density) { 42.dp.toPx() }

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
                        val hitRadius = 28.dp.toPx()
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
            val showCommunes = showcase || scale >= 2.2f
            fun communeVisible(index: Int): Boolean =
                !showcase || occupiedIndices.isEmpty() ||
                    communeGovernorate[index] in occupiedIndices

            drawRect(seaColor)
            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                val strokeWidth = 1.2.dp.toPx() / scale
                paths.forEachIndexed { index, path ->
                    if (index in visibleIndices) {
                        drawPath(path, landColor)
                    }
                }
                if (showCommunes) {
                    val communeStroke = 0.7.dp.toPx() / scale
                    communePaths.forEachIndexed { index, path ->
                        if (communeVisible(index)) {
                            drawPath(
                                path,
                                communeBorderColor,
                                style = Stroke(width = communeStroke)
                            )
                        }
                    }
                }
                paths.forEachIndexed { index, path ->
                    if (index in visibleIndices) {
                        drawPath(path, borderColor, style = Stroke(width = strokeWidth))
                    }
                }
            }

            if (showcase) {
                // Épingles corail façon affiche Nexstay.
                val pinRadius = 8.dp.toPx()
                markers.forEach { marker ->
                    val tip = screenOf(marker.latitude, marker.longitude)
                    val circleCenter = Offset(tip.x, tip.y - pinRadius * 1.8f)
                    val pin = Path().apply {
                        moveTo(tip.x, tip.y)
                        arcTo(
                            rect = Rect(
                                circleCenter.x - pinRadius,
                                circleCenter.y - pinRadius,
                                circleCenter.x + pinRadius,
                                circleCenter.y + pinRadius
                            ),
                            startAngleDegrees = 150f,
                            sweepAngleDegrees = 240f,
                            forceMoveTo = false
                        )
                        close()
                    }
                    drawPath(pin, NexstayCoral)
                    drawCircle(
                        color = ShowcaseBackground,
                        radius = pinRadius * 0.4f,
                        center = circleCenter
                    )

                    // Trait de rappel vers le nom, côté opposé au bord de l'écran.
                    val toRight = tip.x < widthPx / 2f
                    val lineStartX = if (toRight) tip.x + pinRadius + 4.dp.toPx()
                    else tip.x - pinRadius - 4.dp.toPx()
                    val lineEndX = if (toRight) lineStartX + 14.dp.toPx()
                    else lineStartX - 14.dp.toPx()
                    drawLine(
                        color = NexstayCoral,
                        start = Offset(lineStartX, circleCenter.y),
                        end = Offset(lineEndX, circleCenter.y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            drawIntoCanvas { canvas ->
                if (showCommunes) {
                    communes.forEachIndexed { index, commune ->
                        val heightOnScreen =
                            commune.latSpan * projection.pixelsPerDegreeLat * scale
                        if (communeVisible(index) && heightOnScreen > communeLabelMinHeightPx) {
                            val position = screenOf(
                                commune.centroidLat.toDouble(),
                                commune.centroidLon.toDouble()
                            )
                            canvas.nativeCanvas.drawText(
                                commune.name,
                                position.x,
                                position.y,
                                communeLabelPaint
                            )
                        }
                    }
                }
                if (!showcase) {
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
                } else {
                    val pinRadius = with(density) { 8.dp.toPx() }
                    val gap = with(density) { 22.dp.toPx() }
                    markers.forEach { marker ->
                        val tip = screenOf(marker.latitude, marker.longitude)
                        val labelY = tip.y - pinRadius * 1.8f + pinLabelPaint.textSize * 0.35f
                        val toRight = tip.x < widthPx / 2f
                        pinLabelPaint.textAlign =
                            if (toRight) android.graphics.Paint.Align.LEFT
                            else android.graphics.Paint.Align.RIGHT
                        val textX = if (toRight) tip.x + pinRadius + gap else tip.x - pinRadius - gap
                        canvas.nativeCanvas.drawText(
                            marker.label.ifBlank { "Sans nom" },
                            textX,
                            labelY,
                            pinLabelPaint
                        )
                    }
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
                            (position.y - with(density) { 96.dp.toPx() }).roundToInt()
                        )
                    }
                    .width(240.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = { onMarkerOpen?.invoke(selected.id) },
                    shape = MaterialTheme.shapes.medium,
                    color = bubbleColor,
                    contentColor = bubbleTextColor,
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
