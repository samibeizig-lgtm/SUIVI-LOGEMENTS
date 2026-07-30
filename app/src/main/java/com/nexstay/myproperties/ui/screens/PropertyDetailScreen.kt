package com.nexstay.myproperties.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexstay.myproperties.data.KeyHandover
import com.nexstay.myproperties.data.Property
import com.nexstay.myproperties.ui.components.DetailRow
import com.nexstay.myproperties.ui.components.SectionCard
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PropertyDetailScreen(
    property: Property,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = property.name.ifBlank { "Logement" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Modifier")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = "Informations générales", icon = Icons.Outlined.HomeWork) {
                Column {
                    DetailRow("Adresse", property.address)
                    DetailRow("Étage", property.floor)
                    DetailRow("Nombre de pièces", property.roomCount)
                    DetailRow("Surface", property.surface.takeIf { it.isNotBlank() }?.let { "$it m²" })
                    DetailRow("Voyageurs max", property.maxGuests, showDivider = false)
                }
                if (property.latitude != null && property.longitude != null) {
                    MiniMap(
                        latitude = property.latitude,
                        longitude = property.longitude,
                        name = property.name
                    )
                }
            }

            if (property.equipments.isNotEmpty()) {
                SectionCard(title = "Équipements", icon = Icons.Outlined.Checklist) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        property.equipments.forEach { equipment ->
                            AssistChip(onClick = {}, label = { Text(equipment) })
                        }
                    }
                }
            }

            SectionCard(title = "Parking", icon = Icons.Outlined.DirectionsCar) {
                Column {
                    DetailRow("Place de parking", if (property.hasParking) "Oui" else "Non")
                    DetailRow("Numéro de place", property.parkingNumber, showDivider = false)
                }
            }

            val hasWifi = listOf(
                property.wifiOperator, property.wifiContractCode, property.wifiName,
                property.wifiPassword, property.wifiSpeed
            ).any { it.isNotBlank() } || property.wifiType != null
            if (hasWifi) {
                SectionCard(title = "WiFi & Internet", icon = Icons.Outlined.Wifi) {
                    Column {
                        DetailRow("Opérateur", property.wifiOperator)
                        DetailRow("Code contrat", property.wifiContractCode)
                        DetailRow("Nom du WiFi", property.wifiName)
                        DetailRow("Code WiFi", property.wifiPassword)
                        DetailRow("Débit", property.wifiSpeed.takeIf { it.isNotBlank() }?.let { "$it Mb/s" })
                        DetailRow("Nature", property.wifiType?.label, showDivider = false)
                    }
                }
            }

            SectionCard(title = "Accès & clés", icon = Icons.Outlined.Key) {
                Column {
                    DetailRow("Nombre de clés", property.keyCount)
                    DetailRow("Remise des clés", property.keyHandover?.label)
                    if (property.keyHandover == KeyHandover.KEYBOX) {
                        DetailRow("Code keybox", property.keyboxCode)
                    }
                    DetailRow("Code accès résidence", property.residenceAccessCode, showDivider = false)
                }
            }

            val hasMeters = listOf(property.waterMeter, property.electricityMeter, property.gasMeter)
                .any { it.isNotBlank() }
            if (hasMeters) {
                SectionCard(title = "Compteurs", icon = Icons.Outlined.Speed) {
                    Column {
                        DetailRow("Eau", property.waterMeter)
                        DetailRow("Électricité", property.electricityMeter)
                        DetailRow("Gaz", property.gasMeter, showDivider = false)
                    }
                }
            }

            SectionCard(title = "Contrat Nexstay", icon = Icons.Outlined.Gavel) {
                Column {
                    DetailRow(
                        "Date du contrat",
                        property.nexstayContractDate?.let { dateFormat.format(Date(it)) }
                    )
                    DetailRow(
                        "Commission",
                        property.commissionRate.takeIf { it.isNotBlank() }?.let {
                            "$it % ${property.commissionTaxMode?.label ?: ""}".trim()
                        }
                    )
                    DetailRow("Contrat signé", if (property.contractSigned) "Oui" else "Non", showDivider = false)
                }
            }

            val hasOwner = listOf(property.ownerName, property.ownerPhone, property.ownerEmail)
                .any { it.isNotBlank() }
            if (hasOwner) {
                SectionCard(title = "Propriétaire", icon = Icons.Outlined.Person) {
                    Column {
                        DetailRow("Nom", property.ownerName)
                        DetailRow("Téléphone", property.ownerPhone)
                        DetailRow("Email", property.ownerEmail, showDivider = false)
                    }
                }
            }

            if (property.notes.isNotBlank()) {
                SectionCard(title = "Notes", icon = Icons.Outlined.Description) {
                    Text(text = property.notes, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer ce logement ?") },
            text = {
                Text("« ${property.name} » et toutes ses informations seront définitivement supprimés.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }
}

/** Mini-carte non interactive montrant l'emplacement du logement (tuiles en cache = visible hors-ligne). */
@Composable
private fun MiniMap(latitude: Double, longitude: Double, name: String) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setOnTouchListener { _, _ -> true }
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    AndroidView(
        factory = { mapView },
        update = { map ->
            map.controller.setZoom(15.5)
            map.controller.setCenter(GeoPoint(latitude, longitude))
            map.overlays.clear()
            map.overlays.add(
                Marker(map).apply {
                    position = GeoPoint(latitude, longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = name
                    setInfoWindow(null)
                }
            )
            map.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(MaterialTheme.shapes.medium)
    )
}
