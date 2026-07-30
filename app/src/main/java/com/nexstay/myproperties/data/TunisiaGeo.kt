package com.nexstay.myproperties.data

import android.content.Context

/**
 * Contour d'un gouvernorat tunisien.
 * Chaque anneau est un tableau plat [lon0, lat0, lon1, lat1, …].
 */
class Governorate(
    val name: String,
    val rings: List<FloatArray>,
    val centroidLon: Float,
    val centroidLat: Float,
    val latSpan: Float
)

class GeoBounds(
    val minLon: Float,
    val maxLon: Float,
    val minLat: Float,
    val maxLat: Float
)

/** Test point-dans-polygone (lancer de rayon) sur les contours du gouvernorat. */
fun Governorate.contains(latitude: Double, longitude: Double): Boolean {
    rings.forEach { ring ->
        var inside = false
        val pointCount = ring.size / 2
        var j = pointCount - 1
        for (i in 0 until pointCount) {
            val xi = ring[i * 2].toDouble()
            val yi = ring[i * 2 + 1].toDouble()
            val xj = ring[j * 2].toDouble()
            val yj = ring[j * 2 + 1].toDouble()
            if ((yi > latitude) != (yj > latitude) &&
                longitude < (xj - xi) * (latitude - yi) / (yj - yi) + xi
            ) {
                inside = !inside
            }
            j = i
        }
        if (inside) return true
    }
    return false
}

/**
 * Index du gouvernorat contenant le point ; à défaut (point placé en bord de
 * contour simplifié), le gouvernorat au centroïde le plus proche.
 */
fun List<Governorate>.governorateIndexOf(latitude: Double, longitude: Double): Int {
    val exact = indexOfFirst { it.contains(latitude, longitude) }
    if (exact >= 0) return exact
    var best = -1
    var bestDistance = Double.MAX_VALUE
    forEachIndexed { index, gov ->
        val dLat = gov.centroidLat - latitude
        val dLon = gov.centroidLon - longitude
        val distance = dLat * dLat + dLon * dLon
        if (distance < bestDistance) {
            bestDistance = distance
            best = index
        }
    }
    return best
}

/** Charge les contours des gouvernorats et communes depuis les assets (format compact généré depuis geoBoundaries). */
object TunisiaGeo {

    @Volatile
    private var cache: List<Governorate>? = null

    @Volatile
    private var communesCache: List<Governorate>? = null

    @Volatile
    private var cachedBounds: GeoBounds? = null

    fun load(context: Context): List<Governorate> =
        cache ?: synchronized(this) {
            cache ?: parse(context, "tunisia_governorates.txt", updateBounds = true)
                .also { cache = it }
        }

    /** Délégations/communes (niveau ADM2), noms en français. */
    fun loadCommunes(context: Context): List<Governorate> =
        communesCache ?: synchronized(this) {
            communesCache ?: parse(context, "tunisia_communes.txt", updateBounds = false)
                .also { communesCache = it }
        }

    fun bounds(context: Context): GeoBounds {
        load(context)
        return cachedBounds!!
    }

    private fun parse(
        context: Context,
        fileName: String,
        updateBounds: Boolean
    ): List<Governorate> {
        var minLon = Float.MAX_VALUE
        var maxLon = -Float.MAX_VALUE
        var minLat = Float.MAX_VALUE
        var maxLat = -Float.MAX_VALUE

        val regions = context.assets.open(fileName)
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split('|')
                val name = parts[0]
                var sumLon = 0f
                var sumLat = 0f
                var count = 0
                var govMinLat = Float.MAX_VALUE
                var govMaxLat = -Float.MAX_VALUE
                val rings = parts.drop(1).map { ring ->
                    val points = ring.split(';')
                    val array = FloatArray(points.size * 2)
                    points.forEachIndexed { i, point ->
                        val comma = point.indexOf(',')
                        val lon = point.substring(0, comma).toFloat()
                        val lat = point.substring(comma + 1).toFloat()
                        array[i * 2] = lon
                        array[i * 2 + 1] = lat
                        sumLon += lon
                        sumLat += lat
                        count++
                        if (lon < minLon) minLon = lon
                        if (lon > maxLon) maxLon = lon
                        if (lat < minLat) minLat = lat
                        if (lat > maxLat) maxLat = lat
                        if (lat < govMinLat) govMinLat = lat
                        if (lat > govMaxLat) govMaxLat = lat
                    }
                    array
                }
                Governorate(
                    name = name,
                    rings = rings,
                    centroidLon = sumLon / count,
                    centroidLat = sumLat / count,
                    latSpan = govMaxLat - govMinLat
                )
            }

        if (updateBounds) {
            cachedBounds = GeoBounds(minLon, maxLon, minLat, maxLat)
        }
        return regions
    }
}
