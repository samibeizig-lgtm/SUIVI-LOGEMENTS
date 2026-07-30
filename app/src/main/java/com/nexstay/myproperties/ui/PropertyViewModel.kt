package com.nexstay.myproperties.ui

import android.app.Application
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexstay.myproperties.MyPropertiesApp
import com.nexstay.myproperties.data.BackupManager
import com.nexstay.myproperties.data.Property
import com.nexstay.myproperties.data.PropertyMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PropertyViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as MyPropertiesApp).database.propertyDao()
    private val mediaDao = (application as MyPropertiesApp).database.mediaDao()

    val properties: StateFlow<List<Property>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun property(id: Long): Flow<Property?> = dao.observeById(id)

    /** Enregistre le logement (création ou mise à jour). La position vient du placement manuel sur la carte. */
    fun save(property: Property, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = if (property.id != 0L) dao.getById(property.id) else null
            val toSave = property.copy(
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            val id = if (toSave.id == 0L) dao.insert(toSave) else {
                dao.update(toSave)
                toSave.id
            }
            onSaved(id)
        }
    }

    fun delete(property: Property) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                File(BackupManager.mediaDir(getApplication()), "${property.id}")
                    .deleteRecursively()
            }
            mediaDao.deleteForProperty(property.id)
            dao.delete(property)
        }
    }

    // ── Photos & vidéos ────────────────────────────────────────────────────

    fun mediaFor(propertyId: Long): Flow<List<PropertyMedia>> =
        mediaDao.observeForProperty(propertyId)

    fun mediaFile(media: PropertyMedia): File =
        BackupManager.mediaFile(getApplication(), media)

    /** Copie les fichiers choisis dans le stockage de l'application puis les référence. */
    fun addMedia(propertyId: Long, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            uris.forEachIndexed { index, uri ->
                runCatching {
                    val mime = resolver.getType(uri) ?: "image/jpeg"
                    val isVideo = mime.startsWith("video")
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                        ?: if (isVideo) "mp4" else "jpg"
                    val fileName = "${System.currentTimeMillis()}_$index.$extension"
                    val target = File(
                        BackupManager.mediaDir(getApplication()),
                        "$propertyId/$fileName"
                    )
                    target.parentFile?.mkdirs()
                    resolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    } ?: error("Flux illisible")
                    mediaDao.insert(
                        PropertyMedia(
                            propertyId = propertyId,
                            fileName = fileName,
                            isVideo = isVideo
                        )
                    )
                }
            }
        }
    }

    fun deleteMedia(media: PropertyMedia) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { mediaFile(media).delete() }
            mediaDao.delete(media)
        }
    }

    // ── Sauvegarde ─────────────────────────────────────────────────────────

    fun exportBackup(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    BackupManager.export(getApplication(), uri, dao.getAll(), mediaDao.getAll())
                }.isSuccess
            }
            onResult(ok)
        }
    }

    /** Restaure la sauvegarde en remplaçant toutes les données actuelles. */
    fun importBackup(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                val data = withContext(Dispatchers.IO) {
                    BackupManager.restore(getApplication(), uri)
                }
                mediaDao.deleteAll()
                dao.deleteAll()
                data.properties.forEach { dao.insert(it) }
                data.media.forEach { mediaDao.insert(it) }
            }.isSuccess
            onResult(ok)
        }
    }
}
