package com.nexstay.myproperties.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/** Nature de la connexion internet. */
enum class WifiType(val label: String) {
    FIBRE("Fibre"),
    ADSL("ADSL"),
    VDSL("VDSL"),
    G5("5G")
}

/** Mode de remise des clés au voyageur. */
enum class KeyHandover(val label: String) {
    IN_PERSON("En personne"),
    KEYBOX("Keybox"),
    SMART_LOCK("Serrure connectée")
}

/** Taux de commission exprimé hors taxes ou toutes taxes comprises. */
enum class TaxMode(val label: String) {
    HT("HT"),
    TTC("TTC")
}

/** Liste des équipements proposés dans le formulaire. */
val ALL_EQUIPMENTS = listOf(
    "Lave-linge",
    "Sèche-linge",
    "Lave-vaisselle",
    "Réfrigérateur",
    "Congélateur",
    "Four",
    "Micro-ondes",
    "Plaques de cuisson",
    "Cafetière",
    "Bouilloire",
    "Grille-pain",
    "Télévision",
    "Climatisation",
    "Chauffage",
    "Fer à repasser",
    "Aspirateur",
    "Sèche-cheveux",
    "Linge de maison",
    "Baby-foot / Jeux",
    "Balcon",
    "Terrasse",
    "Jardin",
    "Piscine",
    "Ascenseur",
    "Coffre-fort",
    "Détecteur de fumée",
    "Extincteur",
    "Trousse de premiers secours"
)

@Entity(tableName = "properties")
data class Property(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // Informations générales
    val name: String = "",
    val address: String = "",
    val floor: String = "",
    val roomCount: String = "",
    val surface: String = "",
    val maxGuests: String = "",

    // Localisation (géocodée depuis l'adresse)
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Équipements
    val equipments: List<String> = emptyList(),

    // Parking
    val hasParking: Boolean = false,
    val parkingNumber: String = "",

    // WiFi
    val wifiOperator: String = "",
    val wifiContractCode: String = "",
    val wifiName: String = "",
    val wifiPassword: String = "",
    val wifiSpeed: String = "",
    val wifiType: WifiType? = null,

    // Accès & clés
    val keyCount: String = "",
    val keyHandover: KeyHandover? = null,
    val keyboxCode: String = "",
    val residenceAccessCode: String = "",

    // Compteurs
    val waterMeter: String = "",
    val electricityMeter: String = "",
    val gasMeter: String = "",

    // Contrat Nexstay
    val nexstayContractDate: Long? = null,
    val commissionRate: String = "",
    val commissionTaxMode: TaxMode? = null,
    val contractSigned: Boolean = false,

    // Propriétaire
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerEmail: String = "",

    // Notes libres
    val notes: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun fromEquipmentList(value: List<String>): String = value.joinToString("|")

    @TypeConverter
    fun toEquipmentList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|")

    @TypeConverter
    fun fromWifiType(value: WifiType?): String? = value?.name

    @TypeConverter
    fun toWifiType(value: String?): WifiType? = value?.let { WifiType.valueOf(it) }

    @TypeConverter
    fun fromKeyHandover(value: KeyHandover?): String? = value?.name

    @TypeConverter
    fun toKeyHandover(value: String?): KeyHandover? = value?.let { KeyHandover.valueOf(it) }

    @TypeConverter
    fun fromTaxMode(value: TaxMode?): String? = value?.name

    @TypeConverter
    fun toTaxMode(value: String?): TaxMode? = value?.let { TaxMode.valueOf(it) }
}
