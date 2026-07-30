package com.nexstay.myproperties.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexstay.myproperties.data.Property
import com.nexstay.myproperties.ui.map.MapMarker
import com.nexstay.myproperties.ui.map.NexstayCoral
import com.nexstay.myproperties.ui.map.TunisiaMap

private val MapBackground = Color(0xFF121212)
private val MapSurface = Color(0xFF1F1F1F)
private val MapText = Color(0xFFF5F5F5)
private val MapTextMuted = Color(0xFFB0B0B0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyMapScreen(
    properties: List<Property>,
    onBack: () -> Unit,
    onPropertyClick: (Long) -> Unit
) {
    val located = remember(properties) {
        properties.filter { it.latitude != null && it.longitude != null }
    }
    val markers = remember(located) {
        located.map { MapMarker(it.id, it.latitude!!, it.longitude!!, it.name) }
    }

    Scaffold(
        containerColor = MapBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Carte des logements", color = MapText)
                        Text(
                            text = "NEXSTAY",
                            style = MaterialTheme.typography.labelMedium,
                            color = NexstayCoral
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MapText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MapBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TunisiaMap(
                markers = markers,
                onMarkerOpen = onPropertyClick,
                showcase = true,
                modifier = Modifier.fillMaxSize()
            )

            if (properties.isEmpty() || located.size < properties.size) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MapSurface),
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
                                "Aucun logement placé sur la carte. Modifiez un logement puis « Placer sur la carte » dans la rubrique Localisation."
                            else ->
                                "${properties.size - located.size} logement(s) sans position. Modifiez-les pour les placer sur la carte."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MapTextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
