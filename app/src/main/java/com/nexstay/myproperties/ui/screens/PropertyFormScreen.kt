package com.nexstay.myproperties.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexstay.myproperties.data.ALL_EQUIPMENTS
import com.nexstay.myproperties.data.KeyHandover
import com.nexstay.myproperties.data.Property
import com.nexstay.myproperties.data.TaxMode
import com.nexstay.myproperties.data.WifiType
import com.nexstay.myproperties.ui.components.AppDropdown
import com.nexstay.myproperties.ui.components.AppTextField
import com.nexstay.myproperties.ui.components.SectionCard
import com.nexstay.myproperties.ui.components.SwitchRow
import com.nexstay.myproperties.ui.map.TunisiaMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PropertyFormScreen(
    initial: Property?,
    onBack: () -> Unit,
    onSave: (Property) -> Unit
) {
    val editing = initial != null
    var draft by remember(initial) { mutableStateOf(initial ?: Property()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val canSave = draft.name.isNotBlank()

    Box {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Modifier le logement" else "Nouveau logement") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(draft) },
                        enabled = canSave
                    ) {
                        Text("Enregistrer", color = if (canSave) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline)
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
                .imePadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Informations générales ─────────────────────────────────────
            SectionCard(title = "Informations générales", icon = Icons.Outlined.HomeWork) {
                AppTextField(draft.name, { draft = draft.copy(name = it) }, "Nom du logement *")
                AppTextField(draft.address, { draft = draft.copy(address = it) }, "Adresse")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        draft.floor, { draft = draft.copy(floor = it) }, "Étage",
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        draft.roomCount, { draft = draft.copy(roomCount = it) }, "Pièces",
                        modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        draft.surface, { draft = draft.copy(surface = it) }, "Surface (m²)",
                        modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number
                    )
                    AppTextField(
                        draft.maxGuests, { draft = draft.copy(maxGuests = it) }, "Voyageurs max",
                        modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number
                    )
                }
            }

            // ── Localisation ───────────────────────────────────────────────
            SectionCard(title = "Localisation", icon = Icons.Outlined.Place) {
                Text(
                    text = if (draft.latitude != null)
                        "Position définie sur la carte ✓"
                    else
                        "Aucune position. Placez le logement sur la carte pour le voir avec les autres.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (draft.latitude != null) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showMapPicker = true },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (draft.latitude != null) "Modifier la position" else "Placer sur la carte")
                    }
                    if (draft.latitude != null) {
                        TextButton(onClick = {
                            draft = draft.copy(latitude = null, longitude = null)
                        }) {
                            Text("Retirer", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // ── Équipements ────────────────────────────────────────────────
            SectionCard(title = "Équipements", icon = Icons.Outlined.Checklist) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ALL_EQUIPMENTS.forEach { equipment ->
                        val selected = equipment in draft.equipments
                        FilterChip(
                            selected = selected,
                            onClick = {
                                draft = draft.copy(
                                    equipments = if (selected) draft.equipments - equipment
                                    else draft.equipments + equipment
                                )
                            },
                            label = { Text(equipment) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
            }

            // ── Parking ────────────────────────────────────────────────────
            SectionCard(title = "Parking", icon = Icons.Outlined.DirectionsCar) {
                SwitchRow("Place de parking", draft.hasParking) {
                    draft = draft.copy(hasParking = it)
                }
                if (draft.hasParking) {
                    AppTextField(
                        draft.parkingNumber, { draft = draft.copy(parkingNumber = it) },
                        "Numéro de place"
                    )
                }
            }

            // ── WiFi ───────────────────────────────────────────────────────
            SectionCard(title = "WiFi & Internet", icon = Icons.Outlined.Wifi) {
                AppTextField(draft.wifiOperator, { draft = draft.copy(wifiOperator = it) }, "Opérateur")
                AppTextField(draft.wifiContractCode, { draft = draft.copy(wifiContractCode = it) }, "Code contrat")
                AppTextField(draft.wifiName, { draft = draft.copy(wifiName = it) }, "Nom du WiFi (SSID)")
                AppTextField(draft.wifiPassword, { draft = draft.copy(wifiPassword = it) }, "Code WiFi")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        draft.wifiSpeed, { draft = draft.copy(wifiSpeed = it) }, "Débit (Mb/s)",
                        modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number
                    )
                    AppDropdown(
                        label = "Nature",
                        options = WifiType.entries,
                        selected = draft.wifiType,
                        optionLabel = { it.label },
                        onSelected = { draft = draft.copy(wifiType = it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Accès & clés ───────────────────────────────────────────────
            SectionCard(title = "Accès & clés", icon = Icons.Outlined.Key) {
                AppTextField(
                    draft.keyCount, { draft = draft.copy(keyCount = it) },
                    "Nombre de clés", keyboardType = KeyboardType.Number
                )
                AppDropdown(
                    label = "Remise des clés",
                    options = KeyHandover.entries,
                    selected = draft.keyHandover,
                    optionLabel = { it.label },
                    onSelected = { draft = draft.copy(keyHandover = it) }
                )
                if (draft.keyHandover == KeyHandover.KEYBOX) {
                    AppTextField(
                        draft.keyboxCode, { draft = draft.copy(keyboxCode = it) },
                        "Code keybox"
                    )
                }
                AppTextField(
                    draft.residenceAccessCode, { draft = draft.copy(residenceAccessCode = it) },
                    "Code accès résidence"
                )
            }

            // ── Compteurs ──────────────────────────────────────────────────
            SectionCard(title = "Compteurs", icon = Icons.Outlined.Speed) {
                AppTextField(draft.waterMeter, { draft = draft.copy(waterMeter = it) }, "N° compteur eau")
                AppTextField(draft.electricityMeter, { draft = draft.copy(electricityMeter = it) }, "N° compteur électricité")
                AppTextField(draft.gasMeter, { draft = draft.copy(gasMeter = it) }, "N° compteur gaz")
            }

            // ── Contrat Nexstay ────────────────────────────────────────────
            SectionCard(title = "Contrat Nexstay", icon = Icons.Outlined.Gavel) {
                OutlinedTextField(
                    value = draft.nexstayContractDate?.let { dateFormat.format(Date(it)) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date du contrat") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.DateRange, contentDescription = "Choisir la date")
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        focusedLabelColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        draft.commissionRate, { draft = draft.copy(commissionRate = it) },
                        "Commission (%)", modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal
                    )
                    AppDropdown(
                        label = "HT / TTC",
                        options = TaxMode.entries,
                        selected = draft.commissionTaxMode,
                        optionLabel = { it.label },
                        onSelected = { draft = draft.copy(commissionTaxMode = it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                SwitchRow("Contrat signé", draft.contractSigned) {
                    draft = draft.copy(contractSigned = it)
                }
            }

            // ── Propriétaire ───────────────────────────────────────────────
            SectionCard(title = "Propriétaire", icon = Icons.Outlined.Person) {
                AppTextField(draft.ownerName, { draft = draft.copy(ownerName = it) }, "Nom")
                AppTextField(
                    draft.ownerPhone, { draft = draft.copy(ownerPhone = it) },
                    "Téléphone", keyboardType = KeyboardType.Phone
                )
                AppTextField(
                    draft.ownerEmail, { draft = draft.copy(ownerEmail = it) },
                    "Email", keyboardType = KeyboardType.Email
                )
            }

            // ── Notes ──────────────────────────────────────────────────────
            SectionCard(title = "Notes", icon = Icons.Outlined.Description) {
                AppTextField(
                    draft.notes, { draft = draft.copy(notes = it) },
                    "Informations complémentaires", singleLine = false, minLines = 3
                )
            }

            Button(
                onClick = { onSave(draft) },
                enabled = canSave,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(if (editing) "Enregistrer les modifications" else "Créer le logement")
            }
        }
    }

    if (showMapPicker) {
        var pickedLatitude by remember { mutableStateOf(draft.latitude) }
        var pickedLongitude by remember { mutableStateOf(draft.longitude) }
        BackHandler { showMapPicker = false }
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    // Avale les touchers pour que le formulaire en dessous soit inerte.
                    .pointerInput(Unit) { detectTapGestures { } },
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.systemBarsPadding()) {
                    Text(
                        text = "Placer le logement",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)
                    )
                    Text(
                        text = "Touchez la carte pour positionner le logement 📍. Pincez pour zoomer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    TunisiaMap(
                        markers = emptyList(),
                        placementPosition = pickedLatitude?.let { lat ->
                            pickedLongitude?.let { lon -> lat to lon }
                        },
                        onPlace = { latitude, longitude ->
                            pickedLatitude = latitude
                            pickedLongitude = longitude
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showMapPicker = false },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f)
                        ) { Text("Annuler") }
                        Button(
                            onClick = {
                                draft = draft.copy(
                                    latitude = pickedLatitude,
                                    longitude = pickedLongitude
                                )
                                showMapPicker = false
                            },
                            enabled = pickedLatitude != null,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f)
                        ) { Text("Valider") }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = draft.nexstayContractDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    draft = draft.copy(nexstayContractDate = datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
