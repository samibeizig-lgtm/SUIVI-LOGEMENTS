package com.nexstay.myproperties.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Sauvegarde complète de l'application dans un fichier .zip :
 * data.json (logements + index des médias) et les fichiers médias.
 */
object BackupManager {

    class BackupData(
        val properties: List<Property>,
        val media: List<PropertyMedia>
    )

    fun mediaDir(context: Context): File = File(context.filesDir, "media")

    fun mediaFile(context: Context, media: PropertyMedia): File =
        File(mediaDir(context), "${media.propertyId}/${media.fileName}")

    fun export(
        context: Context,
        uri: Uri,
        properties: List<Property>,
        media: List<PropertyMedia>
    ) {
        val output = context.contentResolver.openOutputStream(uri)
            ?: error("Impossible d'ouvrir le fichier de destination")
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(toJson(properties, media).toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            media.forEach { item ->
                val file = mediaFile(context, item)
                if (file.exists()) {
                    zip.putNextEntry(ZipEntry("media/${item.propertyId}/${item.fileName}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    /** Extrait la sauvegarde et remplace le dossier des médias. Retourne les données à insérer en base. */
    fun restore(context: Context, uri: Uri): BackupData {
        val temp = File(context.cacheDir, "backup_import").apply {
            deleteRecursively()
            mkdirs()
        }
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Impossible d'ouvrir le fichier de sauvegarde")
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && !name.contains("..")) {
                    val target = File(temp, name)
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val dataFile = File(temp, "data.json")
        check(dataFile.exists()) { "data.json absent de la sauvegarde" }
        val data = fromJson(dataFile.readText(Charsets.UTF_8))

        mediaDir(context).deleteRecursively()
        File(temp, "media").takeIf { it.exists() }
            ?.copyRecursively(mediaDir(context), overwrite = true)
        temp.deleteRecursively()
        return data
    }

    private fun toJson(properties: List<Property>, media: List<PropertyMedia>): JSONObject =
        JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("properties", JSONArray().apply { properties.forEach { put(it.toJson()) } })
            put("media", JSONArray().apply { media.forEach { put(it.toJson()) } })
        }

    private fun fromJson(text: String): BackupData {
        val root = JSONObject(text)
        val properties = root.getJSONArray("properties").let { array ->
            (0 until array.length()).map { propertyFromJson(array.getJSONObject(it)) }
        }
        val media = root.optJSONArray("media")?.let { array ->
            (0 until array.length()).map { mediaFromJson(array.getJSONObject(it)) }
        } ?: emptyList()
        return BackupData(properties, media)
    }

    private fun Property.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("address", address)
        put("floor", floor)
        put("roomCount", roomCount)
        put("surface", surface)
        put("maxGuests", maxGuests)
        latitude?.let { put("latitude", it) }
        longitude?.let { put("longitude", it) }
        put("equipments", JSONArray(equipments))
        put("hasParking", hasParking)
        put("parkingNumber", parkingNumber)
        put("wifiOperator", wifiOperator)
        put("wifiContractCode", wifiContractCode)
        put("wifiName", wifiName)
        put("wifiPassword", wifiPassword)
        put("wifiSpeed", wifiSpeed)
        wifiType?.let { put("wifiType", it.name) }
        put("keyCount", keyCount)
        keyHandover?.let { put("keyHandover", it.name) }
        put("keyboxCode", keyboxCode)
        put("residenceAccessCode", residenceAccessCode)
        put("waterMeter", waterMeter)
        put("electricityMeter", electricityMeter)
        put("gasMeter", gasMeter)
        nexstayContractDate?.let { put("nexstayContractDate", it) }
        put("commissionRate", commissionRate)
        commissionTaxMode?.let { put("commissionTaxMode", it.name) }
        put("contractSigned", contractSigned)
        put("ownerName", ownerName)
        put("ownerPhone", ownerPhone)
        put("ownerEmail", ownerEmail)
        put("notes", notes)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun propertyFromJson(o: JSONObject): Property = Property(
        id = o.optLong("id"),
        name = o.optString("name"),
        address = o.optString("address"),
        floor = o.optString("floor"),
        roomCount = o.optString("roomCount"),
        surface = o.optString("surface"),
        maxGuests = o.optString("maxGuests"),
        latitude = if (o.isNull("latitude")) null else o.getDouble("latitude"),
        longitude = if (o.isNull("longitude")) null else o.getDouble("longitude"),
        equipments = o.optJSONArray("equipments")?.let { array ->
            (0 until array.length()).map { array.getString(it) }
        } ?: emptyList(),
        hasParking = o.optBoolean("hasParking"),
        parkingNumber = o.optString("parkingNumber"),
        wifiOperator = o.optString("wifiOperator"),
        wifiContractCode = o.optString("wifiContractCode"),
        wifiName = o.optString("wifiName"),
        wifiPassword = o.optString("wifiPassword"),
        wifiSpeed = o.optString("wifiSpeed"),
        wifiType = enumOrNull<WifiType>(o.optString("wifiType")),
        keyCount = o.optString("keyCount"),
        keyHandover = enumOrNull<KeyHandover>(o.optString("keyHandover")),
        keyboxCode = o.optString("keyboxCode"),
        residenceAccessCode = o.optString("residenceAccessCode"),
        waterMeter = o.optString("waterMeter"),
        electricityMeter = o.optString("electricityMeter"),
        gasMeter = o.optString("gasMeter"),
        nexstayContractDate = if (o.isNull("nexstayContractDate")) null else o.getLong("nexstayContractDate"),
        commissionRate = o.optString("commissionRate"),
        commissionTaxMode = enumOrNull<TaxMode>(o.optString("commissionTaxMode")),
        contractSigned = o.optBoolean("contractSigned"),
        ownerName = o.optString("ownerName"),
        ownerPhone = o.optString("ownerPhone"),
        ownerEmail = o.optString("ownerEmail"),
        notes = o.optString("notes"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
    )

    private fun PropertyMedia.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("propertyId", propertyId)
        put("fileName", fileName)
        put("isVideo", isVideo)
        put("addedAt", addedAt)
    }

    private fun mediaFromJson(o: JSONObject): PropertyMedia = PropertyMedia(
        id = o.optLong("id"),
        propertyId = o.optLong("propertyId"),
        fileName = o.optString("fileName"),
        isVideo = o.optBoolean("isVideo"),
        addedAt = o.optLong("addedAt", System.currentTimeMillis())
    )

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        if (name.isNullOrBlank()) null else runCatching { enumValueOf<T>(name) }.getOrNull()
}
