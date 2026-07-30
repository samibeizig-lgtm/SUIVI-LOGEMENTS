package com.nexstay.myproperties.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexstay.myproperties.data.Property
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyMapScreen(
    properties: List<Property>,
    onBack: () -> Unit,
    onPropertyClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val located = remember(properties) {
        properties.filter { it.latitude != null && it.longitude != null }
    }
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Carte des logements") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    map.overlays.clear()
                    located.forEach { property ->
                        val marker = Marker(map).apply {
                            position = GeoPoint(property.latitude!!, property.longitude!!)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = property.name
                            snippet = property.address
                            setOnMarkerClickListener { m, _ ->
                                if (m.isInfoWindowShown) {
                                    onPropertyClick(property.id)
                                } else {
                                    m.showInfoWindow()
                                }
                                true
                            }
                        }
                        map.overlays.add(marker)
                    }
                    when {
                        located.size > 1 -> {
                            val box = BoundingBox.fromGeoPoints(
                                located.map { GeoPoint(it.latitude!!, it.longitude!!) }
                            )
                            map.post { map.zoomToBoundingBox(box.increaseByScale(1.4f), false) }
                        }
                        located.size == 1 -> {
                            map.controller.setZoom(15.5)
                            map.controller.setCenter(
                                GeoPoint(located[0].latitude!!, located[0].longitude!!)
                            )
                        }
                        else -> {
                            // Vue par défaut centrée sur la France.
                            map.controller.setZoom(6.0)
                            map.controller.setCenter(GeoPoint(46.6, 2.4))
                        }
                    }
                    map.invalidate()
                }
            )

            if (located.size < properties.size || properties.isEmpty()) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = when {
                            properties.isEmpty() ->
                                "Aucun logement enregistré."
                            located.isEmpty() ->
                                "Aucun logement localisé pour l'instant. La position est déterminée automatiquement à partir de l'adresse (connexion internet requise)."
                            else ->
                                "${located.size}/${properties.size} logement(s) localisé(s). Les autres n'ont pas encore d'adresse géocodée."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
